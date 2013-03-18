/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.server.startup;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
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
import org.sonar.check.Status;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.rule.RuleStatus;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.server.configuration.ProfilesManager;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

public final class RegisterRules {

  private static final Logger LOG = LoggerFactory.getLogger(RegisterRules.class);
  private final DatabaseSessionFactory sessionFactory;
  private final ProfilesManager profilesManager;
  private final List<RuleRepository> repositories;
  private final RuleI18nManager ruleI18nManager;

  public RegisterRules(DatabaseSessionFactory sessionFactory, RuleRepository[] repos, RuleI18nManager ruleI18nManager, ProfilesManager profilesManager) {
    this.sessionFactory = sessionFactory;
    this.profilesManager = profilesManager;
    this.repositories = newArrayList(repos);
    this.ruleI18nManager = ruleI18nManager;
  }

  public RegisterRules(DatabaseSessionFactory sessionFactory, RuleI18nManager ruleI18nManager, ProfilesManager profilesManager) {
    this(sessionFactory, new RuleRepository[0], ruleI18nManager, profilesManager);
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler();
    DatabaseSession session = sessionFactory.getSession();
    RulesByRepository existingRules = new RulesByRepository(findAllRules(session));

    List<Rule> registeredRules = registerRules(existingRules, profiler, session);
    disableDeprecatedRules(existingRules, registeredRules, session);
    disableDeprecatedRepositories(existingRules, session);
    disableDeprecatedUserRules(profiler, existingRules, session);

    session.commit();

//    notifyForRemovedRules(existingRules);
  }

  private List<Rule> findAllRules(DatabaseSession session) {
    // the hardcoded repository "manual" is used for manual violations
    return session.createQuery("from " + Rule.class.getSimpleName() + " r WHERE r.parent IS NULL AND r.pluginName<>:repository")
        .setParameter("repository", "manual")
        .getResultList();
  }

  private List<Rule> registerRules(RulesByRepository existingRules, TimeProfiler profiler, DatabaseSession session){
    List<Rule> registeredRules = newArrayList();
    for (RuleRepository repository : repositories) {
      profiler.start("Register rules [" + repository.getKey() + "/" + StringUtils.defaultString(repository.getLanguage(), "-") + "]");

      registeredRules.addAll(registerRepository(repository, existingRules, session));
      profiler.stop();
    }
    return registeredRules;
  }

  private List<Rule> registerRepository(RuleRepository repository, RulesByRepository existingRules, DatabaseSession session) {
    List<Rule> registeredRules = newArrayList();
    Map<String, Rule> ruleByKey = newHashMap();
    for (Rule rule : repository.createRules()) {
      rule.setRepositoryKey(repository.getKey());
      rule.setLanguage(repository.getLanguage());
      rule.setStatus(!Strings.isNullOrEmpty(rule.getStatus()) ? rule.getStatus() : RuleStatus.defaultValue().name());
      validateRule(rule, repository.getKey());
      ruleByKey.put(rule.getKey(), rule);
      registeredRules.add(rule);
    }
    LOG.debug(ruleByKey.size() + " rules");

    for (Rule persistedRule : existingRules.get(repository.getKey())) {
      Rule rule = ruleByKey.get(persistedRule.getKey());
      if (rule != null) {
        updateRule(persistedRule, rule, session);
        session.saveWithoutFlush(persistedRule);
        ruleByKey.remove(rule.getKey());
      }
    }
    saveNewRules(ruleByKey.values(), session);
    return registeredRules;
  }

  private void validateRule(Rule rule, String repositoryKey) {
    validateRuleRepositoryName(rule, repositoryKey);
    validateRuleDescription(rule, repositoryKey);
    validateStatus(rule);
  }

  private void validateRuleRepositoryName(Rule rule, String repositoryKey) {
    if (Strings.isNullOrEmpty(rule.getName()) && Strings.isNullOrEmpty(ruleI18nManager.getName(repositoryKey, rule.getKey(), Locale.ENGLISH))) {
      throw new SonarException("The following rule (repository: " + repositoryKey + ") must have a name: " + rule);
    }
  }

  private void validateRuleDescription(Rule rule, String repositoryKey) {
    if (Strings.isNullOrEmpty(rule.getDescription()) && Strings.isNullOrEmpty(ruleI18nManager.getDescription(repositoryKey, rule.getKey(), Locale.ENGLISH))) {
      if (!Strings.isNullOrEmpty(rule.getName()) && Strings.isNullOrEmpty(ruleI18nManager.getName(repositoryKey, rule.getKey(), Locale.ENGLISH))) {
        // specific case
        throw new SonarException("No description found for the rule '" + rule.getName() + "' (repository: " + repositoryKey + ") because the entry 'rule."
            + repositoryKey + "." + rule.getKey() + ".name' is missing from the bundle.");
      } else {
        throw new SonarException("The following rule (repository: " + repositoryKey + ") must have a description: " + rule);
      }
    }
  }

  private void validateStatus(Rule rule) {
    try {
      Status.valueOf(rule.getStatus());
    } catch (IllegalArgumentException e) {
      throw new SonarException("The status of a rule can only contains : " + Joiner.on(", ").join(Status.values()), e);
    }
  }

  private void updateRule(Rule persistedRule, Rule rule, DatabaseSession session) {
    LOG.debug("Update rule " + rule);

    persistedRule.setName(rule.getName());
    persistedRule.setConfigKey(rule.getConfigKey());
    persistedRule.setDescription(rule.getDescription());
    persistedRule.setSeverity(rule.getSeverity());
    persistedRule.setCardinality(rule.getCardinality());
    persistedRule.setStatus(rule.getStatus());
    persistedRule.setLanguage(rule.getLanguage());
    persistedRule.setUpdatedAt(new Date());

    // delete deprecated params
    deleteDeprecatedParameters(persistedRule, rule, session);

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
        persistedParam.setDescription(param.getDescription());
        persistedParam.setType(param.getType());
        persistedParam.setDefaultValue(param.getDefaultValue());
      }
    }
  }

  private void deleteDeprecatedParameters(Rule persistedRule, Rule rule, DatabaseSession session) {
    if (persistedRule.getParams() != null && persistedRule.getParams().size() > 0) {
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

  private void saveNewRules(Collection<Rule> rules, DatabaseSession session) {
    for (Rule rule : rules) {
      LOG.debug("Save new rule " + rule);
      rule.setCreatedAt(new Date());
      session.saveWithoutFlush(rule);
    }
  }

  private void disableDeprecatedRules(RulesByRepository existingRules, List<Rule> registeredRules, DatabaseSession session) {
    for(Rule rule : existingRules.rules()) {
      if (!registeredRules.contains(rule)) {
        disable(rule, existingRules, session);
      }
    }
  }

  private void disableDeprecatedRepositories(RulesByRepository existingRules, DatabaseSession session) {
    for(final String repositoryKey : existingRules.repositories()) {
      if (!Iterables.any(repositories, new Predicate<RuleRepository>() {
        public boolean apply(RuleRepository input) {
          return input.getKey().equals(repositoryKey);
        }
      })) {
        for (Rule rule : existingRules.get(repositoryKey)) {
          disable(rule, existingRules, session);
        }
      }
    }
  }

  private void disableDeprecatedUserRules(TimeProfiler profiler, RulesByRepository existingRules, DatabaseSession session) {
    profiler.start("Disable deprecated user rules");
    List<Integer> deprecatedUserRuleIds = Lists.newLinkedList();
    deprecatedUserRuleIds.addAll(session.createQuery(
        "SELECT r.id FROM " + Rule.class.getSimpleName() +
            " r WHERE r.parent IS NOT NULL AND NOT EXISTS(FROM " + Rule.class.getSimpleName() + " p WHERE r.parent=p)").getResultList());

    deprecatedUserRuleIds.addAll(session.createQuery(
        "SELECT r.id FROM " + Rule.class.getSimpleName() +
            " r WHERE r.parent IS NOT NULL AND EXISTS(FROM " + Rule.class.getSimpleName() + " p WHERE r.parent=p and p.status=:status)")
        .setParameter("status", RuleStatus.REMOVED.name())
        .getResultList());

    for (Integer deprecatedUserRuleId : deprecatedUserRuleIds) {
      Rule rule = session.getSingleResult(Rule.class, "id", deprecatedUserRuleId);
      disable(rule, existingRules, session);
    }
    profiler.stop();
  }

  private void disable(Rule rule, RulesByRepository existingRules, DatabaseSession session){
    if (!rule.getStatus().equals(RuleStatus.REMOVED.name())) {
      LOG.debug("Disable rule " + rule);
      rule.setStatus(RuleStatus.REMOVED.name());
      rule.setUpdatedAt(new Date());
      session.saveWithoutFlush(rule);
      existingRules.addRuleToRemove(rule);
    }
  }

  private void notifyForRemovedRules(RulesByRepository existingRules) {
    for (Rule rule : existingRules.getRulesToRemove()) {
      profilesManager.ruleRemoved(rule.getId());
    }
  }

  static class RulesByRepository {
    Multimap<String, Rule> ruleRepositoryList;
    List<Rule> rulesToRemove;

    public RulesByRepository() {
      ruleRepositoryList = ArrayListMultimap.create();
      rulesToRemove = newArrayList();
    }

    public RulesByRepository(List<Rule> rules){
      this();
      for (Rule rule :rules) {
        add(rule);
      }
    }

    public void add(Rule rule){
      ruleRepositoryList.put(rule.getRepositoryKey(), rule);
    }

    public Collection<Rule> get(String repositoryKey) {
      return ruleRepositoryList.get(repositoryKey);
    }

    public Collection<String> repositories(){
      return ruleRepositoryList.keySet();
    }

    public Collection<Rule> rules(){
      return ruleRepositoryList.values();
    }

    public void addRuleToRemove(Rule rule){
      rulesToRemove.add(rule);
    }

    public List<Rule> getRulesToRemove() {
      return rulesToRemove;
    }
  }

}
