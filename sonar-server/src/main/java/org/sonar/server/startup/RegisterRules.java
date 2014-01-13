/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.startup;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.elasticsearch.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.rule.*;
import org.sonar.server.configuration.ProfilesManager;
import org.sonar.server.rule.RuleRegistry;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

public final class RegisterRules {

  private static final Logger LOG = LoggerFactory.getLogger(RegisterRules.class);
  private final ProfilesManager profilesManager;
  private final List<RuleRepository> repositories;
  private final RuleI18nManager ruleI18nManager;
  private final RuleRegistry ruleRegistry;
  private final MyBatis myBatis;
  private final RuleDao ruleDao;
  private final ActiveRuleDao activeRuleDao;
  private SqlSession sqlSession;

  public RegisterRules(RuleRepository[] repos, RuleI18nManager ruleI18nManager, ProfilesManager profilesManager, RuleRegistry ruleRegistry,
    MyBatis myBatis, RuleDao ruleDao, ActiveRuleDao activeRuleDao) {
    this.profilesManager = profilesManager;
    this.repositories = newArrayList(repos);
    this.ruleI18nManager = ruleI18nManager;
    this.ruleRegistry = ruleRegistry;
    this.myBatis = myBatis;
    this.ruleDao = ruleDao;
    this.activeRuleDao = activeRuleDao;
  }

  public RegisterRules(RuleI18nManager ruleI18nManager, ProfilesManager profilesManager, RuleRegistry ruleRegistry, MyBatis myBatis,
    RuleDao ruleDao, ActiveRuleDao activeRuleDao) {
    this(new RuleRepository[0], ruleI18nManager, profilesManager, ruleRegistry, myBatis, ruleDao, activeRuleDao);
  }

  public void start() {
    sqlSession = myBatis.openSession();

    RulesByRepository existingRules = new RulesByRepository(
        findAllRules(), ruleDao.selectParameters(sqlSession), ruleDao.selectTags(sqlSession));

    try {
      List<RuleDto> registeredRules = registerRules(existingRules);

      LOG.info("Removing deprecated rules");
      Set<RuleDto> disabledRules = newHashSet();
      disabledRules.addAll(disableDeprecatedRules(existingRules, registeredRules));
      disabledRules.addAll(disableDeprecatedRepositories(existingRules));
      sqlSession.commit();
      removeActiveRulesOnDisabledRules(disabledRules);

      ruleRegistry.bulkRegisterRules();
    } finally {
      sqlSession.close();
    }
  }

  private List<RuleDto> findAllRules() {
    return ruleDao.selectNonManual();
  }

  private List<RuleDto> registerRules(RulesByRepository existingRules) {
    TimeProfiler profiler = new TimeProfiler();
    List<RuleDto> registeredRules = newArrayList();
    for (RuleRepository repository : repositories) {
      profiler.start("Register rules [" + repository.getKey() + "/" + StringUtils.defaultString(repository.getLanguage(), "-") + "]");
      registeredRules.addAll(registerRepositoryRules(repository, existingRules));
      profiler.stop();
    }
    // Template rules have to be registered after all rules in order for their parent to be updated.
    registeredRules.addAll(registerTemplateRules(registeredRules, existingRules));
    return registeredRules;
  }

  private List<RuleDto> registerRepositoryRules(RuleRepository repository, RulesByRepository existingRules) {
    List<RuleDto> registeredRules = newArrayList();
    Map<String, Rule> ruleByKey = newHashMap();

    for (Rule rule : repository.createRules()) {
      updateRuleFromRepositoryInfo(rule, repository);
      validateRule(rule, repository.getKey());
      ruleByKey.put(rule.getKey(), rule);
      registeredRules.add(dtoFrom(rule));
    }
    LOG.debug(ruleByKey.size() + " rules");

    for (RuleDto persistedRule : existingRules.get(repository.getKey())) {
      Rule rule = ruleByKey.get(persistedRule.getRuleKey());
      if (rule != null) {
        registeredRules.remove(dtoFrom(rule));
        updateExistingRule(persistedRule, existingRules.params(persistedRule.getId()), existingRules.tags(persistedRule.getId()), rule);
        ruleDao.update(persistedRule, sqlSession);
        registeredRules.add(persistedRule);
        ruleByKey.remove(rule.getKey());
      }
    }
    saveNewRules(ruleByKey.values());
    return registeredRules;
  }

  /**
   * Template rules do not exists in rule repositories, only in database, they have to be updated from their parent.
   */
  private List<RuleDto> registerTemplateRules(List<RuleDto> registeredRules, RulesByRepository existingRules) {
    List<RuleDto> templateRules = newArrayList();
    for (RuleDto persistedRule : existingRules.rules()) {
      RuleDto parent = existingRules.ruleById(persistedRule.getParentId());
      if (parent != null && registeredRules.contains(parent)) {
        persistedRule.setRepositoryKey(parent.getRepositoryKey());
        persistedRule.setLanguage(parent.getLanguage());
        persistedRule.setStatus(Objects.firstNonNull(persistedRule.getStatus(), Rule.STATUS_READY));
        persistedRule.setCreatedAt(Objects.firstNonNull(persistedRule.getCreatedAt(), new Date()));
        persistedRule.setUpdatedAt(new Date());

        ruleDao.update(persistedRule, sqlSession);
        templateRules.add(persistedRule);
      }
    }
    return templateRules;
  }

  private void updateRuleFromRepositoryInfo(Rule rule, RuleRepository repository) {
    rule.setRepositoryKey(repository.getKey());
    rule.setLanguage(repository.getLanguage());
    rule.setStatus(Objects.firstNonNull(rule.getStatus(), Rule.STATUS_READY));
  }

  private void validateRule(Rule rule, String repositoryKey) {
    validateRuleRepositoryName(rule, repositoryKey);
    validateRuleDescription(rule, repositoryKey);
  }

  private void validateRuleRepositoryName(Rule rule, String repositoryKey) {
    String nameFromBundle = ruleI18nManager.getName(repositoryKey, rule.getKey());
    if (!Strings.isNullOrEmpty(nameFromBundle)) {
      rule.setName(nameFromBundle);
    }
    if (Strings.isNullOrEmpty(rule.getName())) {
      throw new SonarException("The following rule (repository: " + repositoryKey + ") must have a name: " + rule);
    }
  }

  private void validateRuleDescription(Rule rule, String repositoryKey) {
    String descriptionFromBundle = ruleI18nManager.getDescription(repositoryKey, rule.getKey());
    if (!Strings.isNullOrEmpty(descriptionFromBundle)) {
      rule.setDescription(descriptionFromBundle);
    }
    if (Strings.isNullOrEmpty(rule.getDescription())) {
      if (!Strings.isNullOrEmpty(rule.getName()) && Strings.isNullOrEmpty(ruleI18nManager.getName(repositoryKey, rule.getKey()))) {
        // specific case
        throw new SonarException("No description found for the rule '" + rule.getName() + "' (repository: " + repositoryKey + ") because the entry 'rule."
          + repositoryKey + "." + rule.getKey() + ".name' is missing from the bundle.");
      } else {
        throw new SonarException("The following rule (repository: " + repositoryKey + ") must have a description: " + rule);
      }
    }
  }

  private void updateExistingRule(RuleDto persistedRule, Collection<RuleParamDto> ruleParams, Collection<RuleTagDto> persistedTags, Rule rule) {
    LOG.debug("Update existing rule " + rule);

    persistedRule.setName(rule.getName());
    persistedRule.setConfigKey(rule.getConfigKey());
    persistedRule.setDescription(rule.getDescription());
    persistedRule.setSeverity(rule.getSeverity().ordinal());
    persistedRule.setCardinality(rule.getCardinality());
    persistedRule.setStatus(rule.getStatus());
    persistedRule.setLanguage(rule.getLanguage());
    persistedRule.setUpdatedAt(new Date());

    // delete deprecated params
    deleteDeprecatedParameters(persistedRule, ruleParams, rule);

    // add new params and update existing params
    updateParameters(persistedRule, ruleParams, rule);

    synchronizeTags(persistedRule, persistedTags, rule);
  }

  private void updateParameters(RuleDto persistedRule, Collection<RuleParamDto> ruleParams, Rule rule) {
    Map<String, RuleParamDto> paramsByKey = Maps.newHashMap();
    for (RuleParamDto param: ruleParams) {
      paramsByKey.put(param.getName(), param);
    }

    if (rule.getParams() != null) {
      for (RuleParam param : rule.getParams()) {
        RuleParamDto persistedParam = paramsByKey.get(param.getKey());
        if (persistedParam == null) {
          persistedParam = new RuleParamDto()
            .setRuleId(persistedRule.getId())
            .setName(param.getKey())
            .setType(param.getType());
          ruleDao.insert(persistedParam, sqlSession);
        }
        String desc = StringUtils.defaultIfEmpty(
          ruleI18nManager.getParamDescription(rule.getRepositoryKey(), rule.getKey(), param.getKey()),
          param.getDescription()
        );
        persistedParam.setDescription(desc);
        persistedParam.setType(param.getType());
        persistedParam.setDefaultValue(param.getDefaultValue());

        ruleDao.update(persistedParam, sqlSession);
      }
    }
  }

  private void deleteDeprecatedParameters(RuleDto persistedRule, Collection<RuleParamDto> ruleParams, Rule rule) {
    if (ruleParams != null && !ruleParams.isEmpty()) {
      for (Iterator<RuleParamDto> it = ruleParams.iterator(); it.hasNext(); ) {
        RuleParamDto persistedParam = it.next();
        if (rule.getParam(persistedParam.getName()) == null) {
          it.remove();
          activeRuleDao.deleteParametersWithParamId(persistedParam.getId(), sqlSession);
          ruleDao.deleteParam(persistedParam, sqlSession);
        }
      }
    }
  }

  void synchronizeTags(RuleDto persistedRule, Collection<RuleTagDto> persistedTags, Rule rule) {
    Set<String> existingSystemTags = Sets.newHashSet();

    for (RuleTagDto existingTag: persistedTags) {
      String existingTagValue = existingTag.getTag();

      if (existingTag.getType() == RuleTagType.SYSTEM) {
        existingSystemTags.add(existingTagValue);
        if (! rule.getTags().contains(existingTagValue)) {
          ruleDao.deleteTag(existingTag, sqlSession);
        }
      } else {
        if (rule.getTags().contains(existingTagValue)) {
          // Existing admin tag with same value as system tag must be converted
          ruleDao.deleteTag(existingTag, sqlSession);
          existingSystemTags.add(existingTagValue);
          ruleDao.insert(dtoFrom(existingTagValue, persistedRule.getId()), sqlSession);
        }
      }
    }

    for (String newTag: rule.getTags()) {
      if (! existingSystemTags.contains(newTag)) {
        ruleDao.insert(dtoFrom(newTag, persistedRule.getId()), sqlSession);
      }
    }
  }


  private void saveNewRules(Collection<Rule> rules) {
    for (Rule rule : rules) {
      LOG.debug("Save new rule " + rule);
      RuleDto newRule = dtoFrom(rule);
      newRule.setCreatedAt(new Date());
      ruleDao.insert(newRule, sqlSession);

      for(RuleParam param : rule.getParams()) {
        RuleParamDto newParam = dtoFrom(param, newRule.getId());
        ruleDao.insert(newParam, sqlSession);
      }

      for(String tag : rule.getTags()) {
        RuleTagDto newTag = dtoFrom(tag, newRule.getId());
        ruleDao.insert(newTag, sqlSession);
      }
    }
  }

  private Collection<RuleDto> disableDeprecatedRules(RulesByRepository existingRules, List<RuleDto> registeredRules) {
    List<RuleDto> disabledRules = newArrayList();
    for (RuleDto rule : existingRules.rules()) {
      if (!registeredRules.contains(rule)) {
        disableRule(rule);
        disabledRules.add(rule);
      }
    }
    return disabledRules;
  }

  private Collection<RuleDto> disableDeprecatedRepositories(RulesByRepository existingRules) {
    List<RuleDto> disabledRules = newArrayList();
    for (final String repositoryKey : existingRules.repositories()) {
      if (!Iterables.any(repositories, new Predicate<RuleRepository>() {
        public boolean apply(RuleRepository input) {
          return input.getKey().equals(repositoryKey);
        }
      })) {
        for (RuleDto rule : existingRules.get(repositoryKey)) {
          disableRule(rule);
          disabledRules.add(rule);
        }
      }
    }
    return disabledRules;
  }

  private void disableRule(RuleDto rule) {
    if (!rule.getStatus().equals(Rule.STATUS_REMOVED)) {
      LOG.info("Removing rule " + rule.getRuleKey());
      rule.setStatus(Rule.STATUS_REMOVED);
      rule.setUpdatedAt(new Date());
      ruleDao.update(rule, sqlSession);
    }
  }

  private void removeActiveRulesOnDisabledRules(Set<RuleDto> disabledRules) {
    for (RuleDto rule : disabledRules) {
      profilesManager.removeActivatedRules(rule.getId());
    }
  }

  static RuleDto dtoFrom(Rule rule) {
    return new RuleDto()
      .setCardinality(rule.getCardinality())
      .setConfigKey(rule.getConfigKey())
      .setDescription(rule.getDescription())
      .setLanguage(rule.getLanguage())
      .setName(rule.getName())
      .setRepositoryKey(rule.getRepositoryKey())
      .setRuleKey(rule.getKey())
      .setSeverity(rule.getSeverity().ordinal())
      .setStatus(rule.getStatus());
  }

  static RuleParamDto dtoFrom(RuleParam param, Integer ruleId) {
    return new RuleParamDto()
      .setRuleId(ruleId)
      .setDefaultValue(param.getDefaultValue())
      .setDescription(param.getDescription())
      .setName(param.getKey())
      .setType(param.getType());
  }

  static RuleTagDto dtoFrom(String tag, Integer ruleId) {
    return new RuleTagDto()
      .setRuleId(ruleId)
      .setTag(tag)
      .setType(RuleTagType.SYSTEM);
  }

  static class RulesByRepository {
    Multimap<String, RuleDto> ruleRepositoryList;
    Map<Integer, RuleDto> rulesById;
    Multimap<Integer, RuleParamDto> params;
    Multimap<Integer, RuleTagDto> tags;

    RulesByRepository(List<RuleDto> rules, List<RuleParamDto> params, List<RuleTagDto> tags) {
      ruleRepositoryList = ArrayListMultimap.create();
      rulesById = Maps.newHashMap();
      for (RuleDto rule : rules) {
        ruleRepositoryList.put(rule.getRepositoryKey(), rule);
        rulesById.put(rule.getId(), rule);
      }
      this.params = ArrayListMultimap.create();
      for (RuleParamDto param: params) {
        this.params.put(param.getRuleId(), param);
      }
      this.tags = ArrayListMultimap.create();
      for (RuleTagDto tag: tags) {
        this.tags.put(tag.getRuleId(), tag);
      }
    }

    Collection<RuleDto> get(String repositoryKey) {
      return ruleRepositoryList.get(repositoryKey);
    }

    Collection<String> repositories() {
      return ruleRepositoryList.keySet();
    }

    Collection<RuleDto> rules() {
      return ruleRepositoryList.values();
    }

    RuleDto ruleById(Integer id) {
      return rulesById.get(id);
    }

    Collection<RuleParamDto> params(Integer ruleId) {
      return params.get(ruleId);
    }

    Collection<RuleTagDto> tags(Integer ruleId) {
      return tags.get(ruleId);
    }
  }
}
