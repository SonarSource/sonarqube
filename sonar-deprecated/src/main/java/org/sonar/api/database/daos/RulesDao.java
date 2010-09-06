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
package org.sonar.api.database.daos;

import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RulesCategory;

import java.util.List;

@Deprecated
public class RulesDao {

  private org.sonar.jpa.dao.RulesDao target;

  public RulesDao(org.sonar.jpa.dao.RulesDao target) {
    this.target = target;
  }

  public List<Rule> getRules() {
    return target.getRules();
  }

  public List<Rule> getRulesByPlugin(String pluginKey) {
    return target.getRulesByPlugin(pluginKey);
  }

  public List<Rule> getRulesByCategory(RulesCategory categ) {
    return target.getRulesByCategory(categ);
  }

  public Rule getRuleByKey(String pluginKey, String ruleKey) {
    return target.getRuleByKey(pluginKey, ruleKey);
  }

  public Long countRules(List<String> plugins, String categoryName) {
    return target.countRules(plugins, categoryName);
  }

  public List<RulesCategory> getCategories() {
    return target.getCategories();
  }

  public RulesCategory getCategory(String key) {
    return target.getCategory(key);
  }


  public List<RuleParam> getRuleParams() {
    return target.getRuleParams();
  }

  public RuleParam getRuleParam(Rule rule, String paramKey) {
    return target.getRuleParam(rule, paramKey);
  }

  public void addActiveRulesToProfile(List<ActiveRule> activeRules, int profileId, String pluginKey) {
    target.addActiveRulesToProfile(activeRules, profileId, pluginKey);
  }

  public List<RuleFailureModel> getViolations(Snapshot snapshot) {
    return target.getViolations(snapshot);
  }

  public void synchronizeRuleOfActiveRule(ActiveRule activeRule, String pluginKey) {
    target.synchronizeRuleOfActiveRule(activeRule, pluginKey);
  }

  public boolean isRuleParamEqual(RuleParam ruleParam, RuleParam ruleParamFromDatabase, String ruleKey, String pluginKey) {
    return target.isRuleParamEqual(ruleParam, ruleParamFromDatabase, ruleKey, pluginKey);
  }

  public RulesProfile getProfileById(int profileId) {
    return target.getProfileById(profileId);
  }
}
