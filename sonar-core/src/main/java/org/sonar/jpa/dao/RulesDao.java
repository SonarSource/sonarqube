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
package org.sonar.jpa.dao;

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RulesDao extends BaseDao {

  private List<RulesCategory> rulesCategories;

  public RulesDao(DatabaseSession session) {
    super(session);
  }

  public List<Rule> getRules() {
    return getSession().getResults(Rule.class, "enabled", true);
  }

  public List<Rule> getRulesByPlugin(String pluginKey) {
    return getSession().getResults(Rule.class, "pluginName", pluginKey, "enabled", true);
  }

  public List<Rule> getRulesByCategory(RulesCategory categ) {
    List<Rule> result = new ArrayList<Rule>();
    for (Rule rule : getRules()) {
      if (rule.getRulesCategory().equals(categ)) {
        result.add(rule);
      }
    }
    return result;
  }

  public Rule getRuleByKey(String pluginKey, String ruleKey) {
    return getSession().getSingleResult(Rule.class, "key", ruleKey, "pluginName", pluginKey, "enabled", true);
  }

  public Long countRules(List<String> plugins, String categoryName) {
    return (Long) getSession().createQuery(
        "SELECT COUNT(r) FROM Rule r WHERE r.pluginName IN (:pluginNames) AND r.rulesCategory=:rulesCategory AND r.enabled=true").
        setParameter("pluginNames", plugins).
        setParameter("rulesCategory", getCategory(categoryName)).
        getSingleResult();
  }

  public List<RulesCategory> getCategories() {
    if (rulesCategories == null) {
      rulesCategories = getSession().getResults(RulesCategory.class);
    }
    return rulesCategories;
  }

  public RulesCategory getCategory(String key) {
    return getSession().getSingleResult(RulesCategory.class, "name", key);
  }


  public List<RuleParam> getRuleParams() {
    return getSession().getResults(RuleParam.class);
  }

  public RuleParam getRuleParam(Rule rule, String paramKey) {
    return getSession().getSingleResult(RuleParam.class, "rule", rule, "key", paramKey);
  }

  public void addActiveRulesToProfile(List<ActiveRule> activeRules, int profileId, String pluginKey) {
    RulesProfile rulesProfile = getProfileById(profileId);
    for (ActiveRule activeRule : activeRules) {
      synchronizeRuleOfActiveRule(activeRule, pluginKey);
      activeRule.setRulesProfile(rulesProfile);
      getSession().save(activeRule);
    }
  }

  public void deleteActiveRuleParameters(RuleParam ruleParam) {
    getSession().createQuery(
        "DELETE FROM ActiveRuleParam arp WHERE ruleParam=:param")
        .setParameter("param", ruleParam)
        .executeUpdate();
  }

  public List<RuleFailureModel> getViolations(Snapshot snapshot) {
    return getSession().getResults(RuleFailureModel.class, "snapshotId", snapshot.getId());
  }

  public void synchronizeRuleOfActiveRule(ActiveRule activeRule, String pluginKey) {
    Rule rule = activeRule.getRule();
    Rule ruleFromDataBase = getRuleByKey(pluginKey, rule.getKey());
    activeRule.setRule(ruleFromDataBase);
    List<RuleParam> ruleParamsFromDataBase = getRuleParams();
    for (ActiveRuleParam activeRuleParam : activeRule.getActiveRuleParams()) {
      boolean found = false;
      Iterator<RuleParam> iterator = ruleParamsFromDataBase.iterator();
      while (iterator.hasNext() && !found) {
        RuleParam ruleParamFromDataBase = iterator.next();
        if (isRuleParamEqual(activeRuleParam.getRuleParam(), ruleParamFromDataBase, rule.getKey(), pluginKey)) {
          activeRuleParam.setRuleParam(ruleParamFromDataBase);
          found = true;
        }
      }
    }
  }

  public boolean isRuleParamEqual(RuleParam ruleParam, RuleParam ruleParamFromDatabase, String ruleKey, String pluginKey) {
    return ruleParam.getKey().equals(ruleParamFromDatabase.getKey()) &&
        ruleKey.equals(ruleParamFromDatabase.getRule().getKey()) &&
        ruleParamFromDatabase.getRule().getPluginName().equals(pluginKey);
  }

  public RulesProfile getProfileById(int profileId) {
    return getSession().getEntityManager().getReference(RulesProfile.class, profileId);
  }

}
