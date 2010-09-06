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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.*;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class RulesDaoTest extends AbstractDbUnitTestCase {

  private RulesDao rulesDao;

  @Before
  public void setup() {
    rulesDao = new RulesDao(getSession());
  }

  @Test
  public void shouldGetRules() {
    setupData("shouldGetRules");

    List<Rule> rules = rulesDao.getRules();
    assertThat(rules, notNullValue());
    assertThat(rules.size(), is(2));

    assertEquals("rule_one", rules.get(0).getKey());
    assertEquals(1, rules.get(0).getParams().size());
    assertEquals("category one", rules.get(0).getRulesCategory().getName());
  }

  @Test
  public void shouldGetRuleWithRuleKeyAndPluginKey() {
    setupData("shouldGetRuleWithRuleKeyAndPluginKey");

    Rule rule = rulesDao.getRuleByKey("plugin", "checkstyle.rule1");
    assertThat(rule, notNullValue());
    assertThat(rule.getId(), notNullValue());

    Rule rule2 = rulesDao.getRuleByKey("plugin", "key not found");
    assertThat(rule2, nullValue());
  }

  @Test
  public void shouldCountNumberOfRulesOfACategoryForGivenPlugins() {
    setupData("shouldCountNumberOfRulesOfACategoryForGivenPlugins");

    Long result = rulesDao.countRules(Arrays.asList("plugin1", "plugin2"), "category one");
    assertThat(result, is(3L));

    result = rulesDao.countRules(Arrays.asList("plugin1", "plugin2"), "category two");
    assertThat(result, is(1L));
  }

  @Test
  public void shouldGetRuleParams() {
    setupData("shouldGetRuleParams");

    List<RuleParam> ruleParams = rulesDao.getRuleParams();

    assertThat(ruleParams.size(), is(3));
  }

  @Test
  public void shouldSynchronizeRuleOfActiveRule() {
    setupData("shouldSynchronizeRuleOfActiveRule");
    Rule rule = new Rule(null, "other key", (String) null, null, null);
    RuleParam ruleParam = new RuleParam(null, "rule1_param1", null, null);
    rule.setParams(Arrays.asList(ruleParam));
    ActiveRule activeRule = new ActiveRule(null, rule, null);
    ActiveRuleParam activeRuleParam = new ActiveRuleParam(activeRule, ruleParam, null);
    activeRule.setActiveRuleParams(Arrays.asList(activeRuleParam));

    rulesDao.synchronizeRuleOfActiveRule(activeRule, "plugin");

    assertThat(activeRule.getRule().getId(), notNullValue());
    assertThat(activeRule.getActiveRuleParams().size(), is(1));
  }

  @Test
  public void shouldGetRulesProfileById() {
    RulesProfile rulesProfile = new RulesProfile("profil", "java", true, true);
    getSession().save(rulesProfile);

    RulesProfile rulesProfileExpected = rulesDao.getProfileById(rulesProfile.getId());

    assertThat(rulesProfileExpected, is(rulesProfile));
  }

  @Test
  public void shouldAddActiveRulesToProfile() {
    setupData("shouldAddActiveRulesToProfile");

    Rule rule = new Rule("rule1", "key1", "config1", new RulesCategory("test"), null);
    RuleParam ruleParam = new RuleParam(null, "param1", null, null);
    rule.setParams(Arrays.asList(ruleParam));

    ActiveRule activeRule = new ActiveRule(null, rule, RulePriority.MAJOR);
    ActiveRuleParam activeRuleParam = new ActiveRuleParam(activeRule, ruleParam, "20");
    activeRule.setActiveRuleParams(Arrays.asList(activeRuleParam));
    rulesDao.addActiveRulesToProfile(Arrays.asList(activeRule), 1, "plugin");

    checkTables("shouldAddActiveRulesToProfile", "rules", "active_rules", "rules_profiles");
  }

}
