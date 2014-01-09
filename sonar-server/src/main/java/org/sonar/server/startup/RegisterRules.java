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
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.server.configuration.ProfilesManager;
import org.sonar.server.rule.RuleRegistry;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

public final class RegisterRules {

  private static final Logger LOG = LoggerFactory.getLogger(RegisterRules.class);
  private final DatabaseSessionFactory sessionFactory;
  private final ProfilesManager profilesManager;
  private final List<RuleRepository> repositories;
  private final RuleI18nManager ruleI18nManager;
  private final RuleRegistry ruleRegistry;

  private DatabaseSession session;

  public RegisterRules(DatabaseSessionFactory sessionFactory, RuleRepository[] repos, RuleI18nManager ruleI18nManager, ProfilesManager profilesManager, RuleRegistry ruleRegistry) {
    this.sessionFactory = sessionFactory;
    this.profilesManager = profilesManager;
    this.repositories = newArrayList(repos);
    this.ruleI18nManager = ruleI18nManager;
    this.ruleRegistry = ruleRegistry;
  }

  public RegisterRules(DatabaseSessionFactory sessionFactory, RuleI18nManager ruleI18nManager, ProfilesManager profilesManager, RuleRegistry ruleRegistry) {
    this(sessionFactory, new RuleRepository[0], ruleI18nManager, profilesManager, ruleRegistry);
  }

  public void start() {
    session = sessionFactory.getSession();
    RulesByRepository existingRules = new RulesByRepository(findAllRules());

    List<Rule> registeredRules = registerRules(existingRules);

    LOG.info("Removing deprecated rules");
    disableDeprecatedRules(existingRules, registeredRules);
    disableDeprecatedRepositories(existingRules);

    session.commit();

    ruleRegistry.bulkRegisterRules();
  }

  private List<Rule> findAllRules() {
    // the hardcoded repository "manual" is used for manual violations
    return session.createQuery("from " + Rule.class.getSimpleName() + " r WHERE r.pluginName<>:repository")
      .setParameter("repository", "manual")
      .getResultList();
  }

  private List<Rule> registerRules(RulesByRepository existingRules) {
    TimeProfiler profiler = new TimeProfiler();
    List<Rule> registeredRules = newArrayList();
    for (RuleRepository repository : repositories) {
      profiler.start("Register rules [" + repository.getKey() + "/" + StringUtils.defaultString(repository.getLanguage(), "-") + "]");
      registeredRules.addAll(registerRepositoryRules(repository, existingRules));
      profiler.stop();
    }
    // Template rules have to be registered after all rules in order for their parent to be updated.
    registeredRules.addAll(registerTemplateRules(registeredRules, existingRules));
    return registeredRules;
  }

  private List<Rule> registerRepositoryRules(RuleRepository repository, RulesByRepository existingRules) {
    List<Rule> registeredRules = newArrayList();
    Map<String, Rule> ruleByKey = newHashMap();
    for (Rule rule : repository.createRules()) {
      updateRuleFromRepositoryInfo(rule, repository);
      validateRule(rule, repository.getKey());
      ruleByKey.put(rule.getKey(), rule);
      registeredRules.add(rule);
    }
    LOG.debug(ruleByKey.size() + " rules");

    for (Rule persistedRule : existingRules.get(repository.getKey())) {
      Rule rule = ruleByKey.get(persistedRule.getKey());
      if (rule != null) {
        updateExistingRule(persistedRule, rule);
        session.saveWithoutFlush(persistedRule);
        ruleByKey.remove(rule.getKey());
      }
    }
    saveNewRules(ruleByKey.values());
    return registeredRules;
  }

  /**
   * Template rules do not exists in rule repositories, only in database, they have to be updated from their parent.
   */
  private List<Rule> registerTemplateRules(List<Rule> registeredRules, RulesByRepository existingRules) {
    List<Rule> templateRules = newArrayList();
    for (Rule persistedRule : existingRules.rules()) {
      Rule parent = persistedRule.getParent();
      if (parent != null && registeredRules.contains(parent)) {
        persistedRule.setRepositoryKey(parent.getRepositoryKey());
        persistedRule.setLanguage(parent.getLanguage());
        persistedRule.setStatus(Objects.firstNonNull(persistedRule.getStatus(), Rule.STATUS_READY));
        persistedRule.setCreatedAt(Objects.firstNonNull(persistedRule.getCreatedAt(), new Date()));
        persistedRule.setUpdatedAt(new Date());

        session.saveWithoutFlush(persistedRule);
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

  private void updateExistingRule(Rule persistedRule, Rule rule) {
    LOG.debug("Update existing rule " + rule);

    persistedRule.setName(rule.getName());
    persistedRule.setConfigKey(rule.getConfigKey());
    persistedRule.setDescription(rule.getDescription());
    persistedRule.setSeverity(rule.getSeverity());
    persistedRule.setCardinality(rule.getCardinality());
    persistedRule.setStatus(rule.getStatus());
    persistedRule.setLanguage(rule.getLanguage());
    persistedRule.setUpdatedAt(new Date());

    // delete deprecated params
    deleteDeprecatedParameters(persistedRule, rule);

    // add new params and update existing params
    updateParameters(persistedRule, rule);
  }

  private void updateParameters(Rule persistedRule, Rule rule) {
    if (rule.getParams() != null) {
      for (RuleParam param : rule.getParams()) {
        RuleParam persistedParam = persistedRule.getParam(param.getKey());
        if (persistedParam == null) {
          persistedParam = persistedRule.createParameter(param.getKey());
        }
        String desc = StringUtils.defaultIfEmpty(
          ruleI18nManager.getParamDescription(rule.getRepositoryKey(), rule.getKey(), param.getKey()),
          param.getDescription()
        );
        persistedParam.setDescription(desc);
        persistedParam.setType(param.getType());
        persistedParam.setDefaultValue(param.getDefaultValue());
      }
    }
  }

  private void deleteDeprecatedParameters(Rule persistedRule, Rule rule) {
    if (persistedRule.getParams() != null && !persistedRule.getParams().isEmpty()) {
      for (Iterator<RuleParam> it = persistedRule.getParams().iterator(); it.hasNext(); ) {
        RuleParam persistedParam = it.next();
        if (rule.getParam(persistedParam.getKey()) == null) {
          it.remove();
          session
            .createQuery("delete from " + ActiveRuleParam.class.getSimpleName() + " where ruleParam=:param")
            .setParameter("param", persistedParam)
            .executeUpdate();
        }
      }
    }
  }

  private void saveNewRules(Collection<Rule> rules) {
    for (Rule rule : rules) {
      LOG.debug("Save new rule " + rule);
      rule.setCreatedAt(new Date());
      session.saveWithoutFlush(rule);
    }
  }

  private void disableDeprecatedRules(RulesByRepository existingRules, List<Rule> registeredRules) {
    for (Rule rule : existingRules.rules()) {
      if (!registeredRules.contains(rule)) {
        disable(rule);
      }
    }
  }

  private void disableDeprecatedRepositories(RulesByRepository existingRules) {
    for (final String repositoryKey : existingRules.repositories()) {
      if (!Iterables.any(repositories, new Predicate<RuleRepository>() {
        public boolean apply(RuleRepository input) {
          return input.getKey().equals(repositoryKey);
        }
      })) {
        for (Rule rule : existingRules.get(repositoryKey)) {
          disable(rule);
        }
      }
    }
  }

  private void disable(Rule rule) {
    if (!rule.getStatus().equals(Rule.STATUS_REMOVED)) {
      LOG.info("Removing rule " + rule.ruleKey());
      profilesManager.removeActivatedRules(rule.getId());
      rule = session.reattach(Rule.class, rule.getId());
      rule.setStatus(Rule.STATUS_REMOVED);
      rule.setUpdatedAt(new Date());
      session.save(rule);
      session.commit();
    }
  }

  static class RulesByRepository {
    Multimap<String, Rule> ruleRepositoryList;

    RulesByRepository(List<Rule> rules) {
      ruleRepositoryList = ArrayListMultimap.create();
      for (Rule rule : rules) {
        ruleRepositoryList.put(rule.getRepositoryKey(), rule);
      }
    }

    Collection<Rule> get(String repositoryKey) {
      return ruleRepositoryList.get(repositoryKey);
    }

    Collection<String> repositories() {
      return ruleRepositoryList.keySet();
    }

    Collection<Rule> rules() {
      return ruleRepositoryList.values();
    }
  }

}
