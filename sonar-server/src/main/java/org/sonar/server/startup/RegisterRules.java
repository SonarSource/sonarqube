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
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RegisterRules {

  private static final Logger LOG = LoggerFactory.getLogger(RegisterRules.class);
  private final DatabaseSessionFactory sessionFactory;
  private final List<RuleRepository> repositories;
  private final RuleI18nManager ruleI18nManager;

  public RegisterRules(DatabaseSessionFactory sessionFactory, RuleRepository[] repos, RuleI18nManager ruleI18nManager) {
    this.sessionFactory = sessionFactory;
    this.repositories = Arrays.asList(repos);
    this.ruleI18nManager = ruleI18nManager;
  }

  public RegisterRules(DatabaseSessionFactory sessionFactory, RuleI18nManager ruleI18nManager) {
    this(sessionFactory, new RuleRepository[0], ruleI18nManager);
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler();

    DatabaseSession session = sessionFactory.getSession();
    disableAllRules(session);
    for (RuleRepository repository : repositories) {
      profiler.start("Register rules [" + repository.getKey() + "/" + StringUtils.defaultString(repository.getLanguage(), "-") + "]");
      registerRepository(repository, session);
      profiler.stop();
    }

    profiler.start("Disable deprecated user rules");
    disableDeprecatedUserRules(session);
    profiler.stop();

    session.commit();
  }

  private void disableDeprecatedUserRules(DatabaseSession session) {
    List<Integer> deprecatedUserRuleIds = Lists.newLinkedList();
    deprecatedUserRuleIds.addAll(session.createQuery(
        "SELECT r.id FROM " + Rule.class.getSimpleName() +
            " r WHERE r.parent IS NOT NULL AND NOT EXISTS(FROM " + Rule.class.getSimpleName() + " p WHERE r.parent=p)").getResultList());

    deprecatedUserRuleIds.addAll(session.createQuery(
        "SELECT r.id FROM " + Rule.class.getSimpleName() +
            " r WHERE r.parent IS NOT NULL AND EXISTS(FROM " + Rule.class.getSimpleName() + " p WHERE r.parent=p and p.enabled=false)").getResultList());

    for (Integer deprecatedUserRuleId : deprecatedUserRuleIds) {
      Rule rule = session.getSingleResult(Rule.class, "id", deprecatedUserRuleId);
      rule.setEnabled(false);
      session.saveWithoutFlush(rule);
    }

  }

  private void disableAllRules(DatabaseSession session) {
    // the hardcoded repository "manual" is used for manual violations
    session.createQuery("UPDATE " + Rule.class.getSimpleName() + " SET enabled=false WHERE parent IS NULL AND pluginName<>'manual'").executeUpdate();
  }

  private void registerRepository(RuleRepository repository, DatabaseSession session) {
    Map<String, Rule> rulesByKey = Maps.newHashMap();
    for (Rule rule : repository.createRules()) {
      validateRule(rule, repository.getKey());
      rule.setRepositoryKey(repository.getKey());
      rule.setLanguage(repository.getLanguage());
      rule.setStatus(!Strings.isNullOrEmpty(rule.getStatus()) ? rule.getStatus() : Status.defaultValue().name());
      rulesByKey.put(rule.getKey(), rule);
    }
    LOG.info(rulesByKey.size() + " rules");

    List<Rule> persistedRules = session.getResults(Rule.class, "pluginName", repository.getKey());
    for (Rule persistedRule : persistedRules) {
      Rule rule = rulesByKey.get(persistedRule.getKey());
      if (rule != null) {
        updateRule(persistedRule, rule, session);
        rulesByKey.remove(rule.getKey());
      }
    }

    saveNewRules(rulesByKey.values(), session);
  }

  private void validateRule(Rule rule, String repositoryKey) {
    if (StringUtils.isBlank(rule.getName()) && StringUtils.isBlank(ruleI18nManager.getName(repositoryKey, rule.getKey(), Locale.ENGLISH))) {
      throw new SonarException("The following rule (repository: " + repositoryKey + ") must have a name: " + rule);
    }
    if (StringUtils.isBlank(rule.getDescription()) && StringUtils.isBlank(ruleI18nManager.getDescription(repositoryKey, rule.getKey(), Locale.ENGLISH))) {
      if (StringUtils.isNotBlank(rule.getName()) && StringUtils.isBlank(ruleI18nManager.getName(repositoryKey, rule.getKey(), Locale.ENGLISH))) {
        // specific case
        throw new SonarException("No description found for the rule '" + rule.getName() + "' (repository: " + repositoryKey + ") because the entry 'rule."
            + repositoryKey + "." + rule.getKey() + ".name' is missing from the bundle.");
      } else {
        throw new SonarException("The following rule (repository: " + repositoryKey + ") must have a description: " + rule);
      }
    }
    if (!Strings.isNullOrEmpty(rule.getStatus())) {
      try {
        Status.valueOf(rule.getStatus());
      } catch (IllegalArgumentException e) {
        throw new SonarException("The status of a rule can only contains : " + Joiner.on(", ").join(Status.values()), e);
      }
    }
  }

  private void updateRule(Rule persistedRule, Rule rule, DatabaseSession session) {
    persistedRule.setName(rule.getName());
    persistedRule.setConfigKey(rule.getConfigKey());
    persistedRule.setDescription(rule.getDescription());
    persistedRule.setSeverity(rule.getSeverity());
    persistedRule.setEnabled(true);
    persistedRule.setCardinality(rule.getCardinality());
    persistedRule.setStatus(rule.getStatus());
    persistedRule.setLanguage(rule.getLanguage());
    persistedRule.setUpdatedAt(new Date());

    // delete deprecated params
    deleteDeprecatedParameters(persistedRule, rule, session);

    // add new params and update existing params
    updateParameters(persistedRule, rule);

    session.saveWithoutFlush(persistedRule);
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
      rule.setEnabled(true);
      rule.setCreatedAt(new Date());
      session.saveWithoutFlush(rule);
    }
  }

}
