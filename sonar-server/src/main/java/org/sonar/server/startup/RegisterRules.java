/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.rules.*;
import org.sonar.api.utils.Logs;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.server.rules.DeprecatedRuleRepositories;

import java.util.*;

public final class RegisterRules {

  private DatabaseSessionFactory sessionFactory;
  private List<RuleRepository> repositories = new ArrayList<RuleRepository>();

  public RegisterRules(DatabaseSessionFactory sessionFactory, DeprecatedRuleRepositories repositories, RuleRepository[] repos) {
    this.sessionFactory = sessionFactory;
    this.repositories.addAll(Arrays.asList(repos));
    if (repositories != null) {
      this.repositories.addAll(repositories.create());
    }
  }

  public RegisterRules(DatabaseSessionFactory sessionFactory, DeprecatedRuleRepositories repositories) {
    this(sessionFactory, repositories, new RuleRepository[0]);
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

    profiler.start("Disable deprecated active rules");
    deleteDisabledActiveRules(session);
    profiler.stop();
  }

  private void disableDeprecatedUserRules(DatabaseSession session) {
    List<Integer> deprecatedUserRuleIds = new ArrayList<Integer>();
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
    session.commit();
  }

  private void disableAllRules(DatabaseSession session) {
    session.createQuery("UPDATE " + Rule.class.getSimpleName() + " SET enabled=false WHERE parent IS NULL").executeUpdate();
    session.commit();
  }

  private void registerRepository(RuleRepository repository, DatabaseSession session) {
    Map<String, Rule> rulesByKey = new HashMap<String, Rule>();
    for (Rule rule : repository.createRules()) {
      rule.setRepositoryKey(repository.getKey());
      rulesByKey.put(rule.getKey(), rule);
    }
    Logs.INFO.info(rulesByKey.size() + " rules");

    List<Rule> persistedRules = session.getResults(Rule.class, "pluginName", repository.getKey());
    for (Rule persistedRule : persistedRules) {
      Rule rule = rulesByKey.get(persistedRule.getKey());
      if (rule != null) {
        updateRule(persistedRule, rule, session);
        rulesByKey.remove(rule.getKey());
      }
    }

    saveNewRules(rulesByKey.values(), session);
    session.commit();
  }

  private void deleteDisabledActiveRules(DatabaseSession session) {
    List<ActiveRule> deprecatedActiveRules = session
        .createQuery("from " + ActiveRule.class.getSimpleName() + " where rule.enabled=:enabled")
        .setParameter("enabled", false)
        .getResultList();
    for (ActiveRule deprecatedActiveRule : deprecatedActiveRules) {
      session.removeWithoutFlush(deprecatedActiveRule);
    }
    if (!deprecatedActiveRules.isEmpty()) {
      session.commit();
    }
  }

  private void updateRule(Rule persistedRule, Rule rule, DatabaseSession session) {
    persistedRule.setName(rule.getName());
    persistedRule.setConfigKey(rule.getConfigKey());
    persistedRule.setDescription(rule.getDescription());
    persistedRule.setRulesCategory(reattachCategory(rule.getRulesCategory(), session));
    persistedRule.setPriority(rule.getPriority());
    persistedRule.setEnabled(true);
    persistedRule.setCardinality(rule.getCardinality());

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
      }
    }
  }

  private void deleteDeprecatedParameters(Rule persistedRule, Rule rule, DatabaseSession session) {
    if (persistedRule.getParams() != null && persistedRule.getParams().size() > 0) {
      for (Iterator<RuleParam> it = persistedRule.getParams().iterator(); it.hasNext();) {
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

  private RulesCategory reattachCategory(RulesCategory category, DatabaseSession session) {
    if (category != null) {
      return session.getSingleResult(RulesCategory.class, "name", category.getName());
    }
    return null;
  }

  private void saveNewRules(Collection<Rule> rules, DatabaseSession session) {
    for (Rule rule : rules) {
      rule.setEnabled(true);
      if (rule.getRulesCategory() != null) {
        rule.setRulesCategory(reattachCategory(rule.getRulesCategory(), session));
      }
      session.saveWithoutFlush(rule);
    }
    session.commit();
  }
}
