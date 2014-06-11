/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.qualityprofile;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.*;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.tester.ServerTester;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class RuleActivatorMediumTest {

  static final QualityProfileKey XOO_PROFILE_KEY = QualityProfileKey.of("P1", "xoo");
  static final QualityProfileKey XOO_CHILD_PROFILE_KEY = QualityProfileKey.of("P2", "xoo");
  static final QualityProfileKey XOO_GRAND_CHILD_PROFILE_KEY = QualityProfileKey.of("P3", "xoo");
  static final RuleKey MANUAL_RULE_KEY = RuleKey.of(Rule.MANUAL_REPOSITORY_KEY, "m1");
  static final RuleKey TEMPLATE_RULE_KEY = RuleKey.of("xoo", "template1");
  static final RuleKey CUSTOM_RULE_KEY = RuleKey.of(TEMPLATE_RULE_KEY.repository(), "custom1");
  static final RuleKey XOO_RULE_1 = RuleKey.of("xoo", "x1");
  static final RuleKey XOO_RULE_2 = RuleKey.of("xoo", "x2");

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  DbSession dbSession;
  RuleActivator ruleActivator;
  ActiveRuleIndex index;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);
    ruleActivator = tester.get(RuleActivator.class);
    index = tester.get(ActiveRuleIndex.class);

    // create pre-defined rules
    RuleDto javaRule = RuleTesting.newDto(RuleKey.of("squid", "j1"))
      .setSeverity("MAJOR").setLanguage("java");
    RuleDto xooRule1 = RuleTesting.newDto(XOO_RULE_1)
      .setSeverity("MINOR").setLanguage("xoo");
    RuleDto xooRule2 = RuleTesting.newDto(XOO_RULE_2).setLanguage("xoo");
    RuleDto xooTemplateRule1 = RuleTesting.newTemplateRule(TEMPLATE_RULE_KEY)
      .setSeverity("MINOR").setLanguage("xoo");
    RuleDto manualRule = RuleTesting.newDto(MANUAL_RULE_KEY);
    db.ruleDao().insert(dbSession, javaRule, xooRule1, xooRule2, xooTemplateRule1, manualRule);
    db.ruleDao().addRuleParam(dbSession, xooRule1, RuleParamDto.createFor(xooRule1)
      .setName("max").setDefaultValue("10").setType(RuleParamType.INTEGER.type()));
    db.ruleDao().addRuleParam(dbSession, xooRule1, RuleParamDto.createFor(xooRule1)
      .setName("min").setType(RuleParamType.INTEGER.type()));
    db.ruleDao().addRuleParam(dbSession, xooTemplateRule1, RuleParamDto.createFor(xooTemplateRule1)
      .setName("format").setType(RuleParamType.STRING.type()));

    RuleDto xooCustomRule1 = RuleTesting.newCustomRule(xooTemplateRule1).setRuleKey(CUSTOM_RULE_KEY.rule())
      .setSeverity("MINOR").setLanguage("xoo");
    db.ruleDao().insert(dbSession, xooCustomRule1);
    db.ruleDao().addRuleParam(dbSession, xooTemplateRule1, RuleParamDto.createFor(xooTemplateRule1)
      .setName("format").setDefaultValue("txt").setType(RuleParamType.STRING.type()));

    // create pre-defined profile
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(XOO_PROFILE_KEY));
    dbSession.commit();
    dbSession.clearCache();
  }

  @After
  public void after() throws Exception {
    dbSession.close();
  }

  @Test
  public void activate() throws Exception {
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1);
    RuleActivation activation = new RuleActivation(activeRuleKey);
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);

    assertThat(countActiveRules(XOO_PROFILE_KEY)).isEqualTo(1);
    verifyHasActiveRule(activeRuleKey, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
  }

  @Test
  public void activate_with_default_severity_and_parameter() throws Exception {
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1);
    RuleActivation activation = new RuleActivation(activeRuleKey);
    ruleActivator.activate(activation);

    assertThat(countActiveRules(XOO_PROFILE_KEY)).isEqualTo(1);
    verifyHasActiveRule(activeRuleKey, Severity.MINOR, null, ImmutableMap.of("max", "10"));
  }

  @Test
  public void activation_ignores_unsupported_parameters() throws Exception {
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1);
    RuleActivation activation = new RuleActivation(activeRuleKey);
    activation.setParameter("xxx", "yyy");
    ruleActivator.activate(activation);

    assertThat(countActiveRules(XOO_PROFILE_KEY)).isEqualTo(1);
    verifyHasActiveRule(activeRuleKey, Severity.MINOR, null, ImmutableMap.of("max", "10"));
  }

  @Test
  public void update_activation_severity_and_parameters() throws Exception {
    // initial activation
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1);
    RuleActivation activation = new RuleActivation(activeRuleKey);
    activation.setSeverity(Severity.BLOCKER);
    ruleActivator.activate(activation);

    // update
    RuleActivation update = new RuleActivation(activeRuleKey);
    update.setSeverity(Severity.CRITICAL);
    update.setParameter("max", "42");
    ruleActivator.activate(update);

    assertThat(countActiveRules(XOO_PROFILE_KEY)).isEqualTo(1);
    verifyHasActiveRule(activeRuleKey, Severity.CRITICAL, null, ImmutableMap.of("max", "42"));
  }

  @Test
  public void update_activation_but_new_parameter() throws Exception {
    // initial activation
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1);
    RuleActivation activation = new RuleActivation(activeRuleKey);
    activation.setSeverity(Severity.BLOCKER);
    ruleActivator.activate(activation);


    assertThat(db.activeRuleDao().getParamByKeyAndName(activeRuleKey, "max", dbSession)).isNotNull();
    db.activeRuleDao().removeParamByKeyAndName(dbSession, activeRuleKey, "max");
    dbSession.commit();
    assertThat(db.activeRuleDao().getParamByKeyAndName(activeRuleKey, "max", dbSession)).isNull();
    dbSession.clearCache();

    // update
    RuleActivation update = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1));
    update.setSeverity(Severity.CRITICAL);
    update.setParameter("max", "42");
    // contrary to activerule, the param 'max' is supposed to be inserted but not updated
    ruleActivator.activate(update);

    assertThat(countActiveRules(XOO_PROFILE_KEY)).isEqualTo(1);
    verifyHasActiveRule(activeRuleKey, Severity.CRITICAL, null, ImmutableMap.of("max", "42"));
  }

  @Test
  public void revert_activation_to_default_severity_and_parameters() throws Exception {
    // initial activation
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1);
    RuleActivation activation = new RuleActivation(activeRuleKey);
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);

    // update
    RuleActivation update = new RuleActivation(activeRuleKey);
    ruleActivator.activate(update);

    assertThat(countActiveRules(XOO_PROFILE_KEY)).isEqualTo(1);
    verifyHasActiveRule(activeRuleKey, Severity.MINOR, null, ImmutableMap.of("max", "10"));
  }

  @Test
  public void fail_to_activate_if_template() throws Exception {
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, TEMPLATE_RULE_KEY));

    try {
      ruleActivator.activate(activation);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Rule template can't be activated on a Quality profile: xoo:template1");
      verifyZeroActiveRules(XOO_PROFILE_KEY);
    }
  }

  @Test
  public void fail_to_activate_if_different_languages() throws Exception {
    // profile and rule have different languages
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("squid", "j1")));

    try {
      ruleActivator.activate(activation);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Rule squid:j1 and profile P1:xoo have different languages");
      verifyZeroActiveRules(XOO_PROFILE_KEY);
    }
  }

  @Test
  public void fail_to_activate_if_unknown_rule() throws Exception {
    // profile and rule have different languages
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x3")));

    try {
      ruleActivator.activate(activation);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Rule not found: xoo:x3");
      verifyZeroActiveRules(XOO_PROFILE_KEY);
    }
  }

  @Test
  public void fail_to_activate_if_rule_with_removed_status() throws Exception {
    RuleDto ruleDto = db.ruleDao().getByKey(dbSession, XOO_RULE_1);
    ruleDto.setStatus(RuleStatus.REMOVED);
    db.ruleDao().update(dbSession, ruleDto);
    dbSession.commit();
    dbSession.clearCache();

    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1));

    try {
      ruleActivator.activate(activation);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Rule was removed: xoo:x1");
      verifyZeroActiveRules(XOO_PROFILE_KEY);
    }
  }

  @Test
  public void fail_to_activate_if_manual_rule() throws Exception {
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, MANUAL_RULE_KEY));

    try {
      ruleActivator.activate(activation);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Manual rule can't be activated on a Quality profile: manual:m1");
      verifyZeroActiveRules(XOO_PROFILE_KEY);
    }
  }

  @Test
  public void fail_to_activate_if_unknown_profile() throws Exception {
    // profile and rule have different languages
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(QualityProfileKey.of("other", "js"), XOO_RULE_1));

    try {
      ruleActivator.activate(activation);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Quality profile not found: other:js");
    }
  }

  @Test
  public void fail_to_activate_if_invalid_parameter() throws Exception {
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1));
    activation.setParameter("max", "foo");

    try {
      ruleActivator.activate(activation);
      fail();
    } catch (BadRequestException e) {
      assertThat(e.l10nKey()).isEqualTo("errors.type.notInteger");
      verifyZeroActiveRules(XOO_PROFILE_KEY);
    }
  }

  @Test
  public void fail_to_activate_if_custom_rule_template_and_parameters_are_set() throws Exception {
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, CUSTOM_RULE_KEY))
      .setParameter("format", "xls");

    try {
      ruleActivator.activate(activation);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Parameters cannot be set when activating the custom rule 'xoo:custom1'");
      verifyZeroActiveRules(XOO_PROFILE_KEY);
    }
  }

  @Test
  public void deactivate() throws Exception {
    // activation
    ActiveRuleKey key = ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1);
    RuleActivation activation = new RuleActivation(key);
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);

    // deactivation
    ruleActivator.deactivate(key);

    verifyZeroActiveRules(XOO_PROFILE_KEY);
  }

  @Test
  public void ignore_deactivation_if_rule_not_activated() throws Exception {
    // deactivation
    ActiveRuleKey key = ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1);
    ruleActivator.deactivate(key);

    verifyZeroActiveRules(XOO_PROFILE_KEY);
  }

  @Test
  public void deactivation_fails_if_rule_not_found() throws Exception {
    ActiveRuleKey key = ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x3"));
    try {
      ruleActivator.deactivate(key);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Rule not found: xoo:x3");
      verifyZeroActiveRules(XOO_PROFILE_KEY);
    }
  }

  @Test
  public void deactivation_fails_if_profile_not_found() throws Exception {
    ActiveRuleKey key = ActiveRuleKey.of(QualityProfileKey.of("other", "js"), XOO_RULE_1);
    try {
      ruleActivator.deactivate(key);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Quality profile not found: other:js");
    }
  }

  @Test
  public void allow_to_deactivate_removed_rule() throws Exception {
    // activation
    ActiveRuleKey key = ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1);
    RuleActivation activation = new RuleActivation(key);
    ruleActivator.activate(activation);

    // set rule as removed
    RuleDto rule = db.ruleDao().getByKey(dbSession, XOO_RULE_1);
    rule.setStatus(RuleStatus.REMOVED);
    db.ruleDao().update(dbSession, rule);
    dbSession.commit();
    dbSession.clearCache();

    // deactivation
    ruleActivator.deactivate(key);

    verifyZeroActiveRules(XOO_PROFILE_KEY);
  }

  // INHERITANCE OF PROFILES
  @Test
  public void activate_on_child_profile() throws Exception {
    createChildProfiles();

    // activate on child profile, but not on root
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_CHILD_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);

    verifyZeroActiveRules(XOO_PROFILE_KEY);
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
  }

  @Test
  public void propagate_activation_on_child_profiles() throws Exception {
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);

    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
  }

  @Test
  public void propagate_activation_update_on_child_profiles() throws Exception {
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));

    // update on parent
    activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "8");
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.INFO, null, ImmutableMap.of("max", "8"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.INFO, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "8"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.INFO, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "8"));

    // update on child -> propagate on grand child only
    activation = new RuleActivation(ActiveRuleKey.of(XOO_CHILD_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.MINOR);
    activation.setParameter("max", "9");
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.INFO, null, ImmutableMap.of("max", "8"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.MINOR, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "9"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.MINOR, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "9"));

    // update on grand child
    activation = new RuleActivation(ActiveRuleKey.of(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "10");
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.INFO, null, ImmutableMap.of("max", "8"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.MINOR, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "9"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "10"));
  }

  @Test
  public void do_not_propagate_activation_update_on_child_overrides() throws Exception {
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.INFO, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.INFO, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.INFO, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));

    // override on child
    activation = new RuleActivation(ActiveRuleKey.of(XOO_CHILD_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "8");
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.INFO, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "8"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "8"));

    // change on parent -> do not propagate on children because they're overriding values
    activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.CRITICAL);
    activation.setParameter("max", "9");
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.CRITICAL, null, ImmutableMap.of("max", "9"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "8"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "8"));

    // reset on parent (use default severity and params) -> do not propagate on children because they're overriding values
    activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1));
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.MINOR, null, ImmutableMap.of("max", "10"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "8"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "8"));
  }

  @Test
  public void active_on_parent_a_rule_already_activated_on_child() throws Exception {
    createChildProfiles();

    // activate on child profile
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_CHILD_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);
    verifyZeroActiveRules(XOO_PROFILE_KEY);
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.INFO, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.INFO, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));

    // active the same rule on root profile -> mark the child profile as OVERRIDES
    activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.MAJOR);
    activation.setParameter("max", "8");
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.MAJOR, null, ImmutableMap.of("max", "8"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.INFO, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.INFO, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
  }


  @Test
  public void do_not_override_on_child_if_same_values() throws Exception {
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.INFO, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.INFO, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));

    // override on child with same severity and params -> do nothing (still INHERITED but not OVERRIDDEN)
    activation = new RuleActivation(ActiveRuleKey.of(XOO_CHILD_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.INFO, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.INFO, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
  }

  @Test
  public void propagate_deactivation_on_child_profiles() throws Exception {
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));

    // deactivate on root
    ruleActivator.deactivate(activation.getKey());

    verifyZeroActiveRules(XOO_PROFILE_KEY);
    verifyZeroActiveRules(XOO_CHILD_PROFILE_KEY);
    verifyZeroActiveRules(XOO_GRAND_CHILD_PROFILE_KEY);
  }

  @Test
  public void propagate_deactivation_even_on_child_overrides() throws Exception {
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.INFO, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.INFO, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.INFO, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));

    // override on child
    activation = new RuleActivation(ActiveRuleKey.of(XOO_CHILD_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "8");
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.INFO, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "8"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "8"));

    // deactivate on parent -> do not propagate on children because they're overriding values
    ruleActivator.deactivate(ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1));
    dbSession.clearCache();
    verifyZeroActiveRules(XOO_PROFILE_KEY);
    verifyZeroActiveRules(XOO_CHILD_PROFILE_KEY);
    verifyZeroActiveRules(XOO_GRAND_CHILD_PROFILE_KEY);
  }

  @Test
  public void do_not_deactivate_inherited_or_overridden_rule() throws Exception {
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));

    // try to deactivate on child
    try {
      ruleActivator.deactivate(ActiveRuleKey.of(XOO_CHILD_PROFILE_KEY, XOO_RULE_1));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Cannot deactivate inherited rule 'xoo:x1'");
    }
  }

  @Test
  public void reset_activation_on_child_profile() throws Exception {
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));

    // override
    activation = new RuleActivation(ActiveRuleKey.of(XOO_CHILD_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "10");
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.INFO, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "10"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.INFO, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "10"));

    // reset -> remove overridden values
    activation = new RuleActivation(ActiveRuleKey.of(XOO_CHILD_PROFILE_KEY, XOO_RULE_1));
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
  }

  @Test
  public void activation_reset_does_not_propagate_to_child_overrides() throws Exception {
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));

    // override on child
    activation = new RuleActivation(ActiveRuleKey.of(XOO_CHILD_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "10");
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.INFO, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "10"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.INFO, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "10"));

    // override on grand child
    activation = new RuleActivation(ActiveRuleKey.of(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity(Severity.MINOR);
    activation.setParameter("max", "20");
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.INFO, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "10"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.MINOR, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "20"));

    // reset child -> keep the overridden grand-child
    activation = new RuleActivation(ActiveRuleKey.of(XOO_CHILD_PROFILE_KEY, XOO_RULE_1));
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, XOO_RULE_1, Severity.MINOR, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "20"));
  }

  @Test
  public void mass_activation() {
    // Generate more rules than the search's max limit
    int bulkSize = QueryOptions.MAX_LIMIT + 10;
    for (int i = 0; i < bulkSize; i++) {
      db.ruleDao().insert(dbSession, RuleTesting.newDto(RuleKey.of("bulk", "r_" + i)).setLanguage("xoo"));
    }
    dbSession.commit();

    // 0. No active rules so far (base case) and plenty rules available
    verifyZeroActiveRules(XOO_PROFILE_KEY);
    assertThat(tester.get(RuleIndex.class)
      .search(new RuleQuery().setRepositories(Arrays.asList("bulk")), new QueryOptions()).getTotal())
      .isEqualTo(bulkSize);

    // 1. bulk activate all the rules
    ruleActivator.bulkActivate(
      new RuleQuery().setRepositories(Arrays.asList("bulk")), XOO_PROFILE_KEY, "MINOR");

    // 2. assert that all activation has been commited to DB and ES
    dbSession.clearCache();
    assertThat(db.activeRuleDao().findByProfileKey(dbSession, XOO_PROFILE_KEY)).hasSize(bulkSize);
    assertThat(index.findByProfile(XOO_PROFILE_KEY)).hasSize(bulkSize);

  }

  @Test
  public void set_and_unset_parent_profile() {
    // x1 is activated on the "future parent"
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity("MAJOR");
    ruleActivator.activate(activation);
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.MAJOR, null, ImmutableMap.of("max", "10"));

    // create profile with x2
    QualityProfileKey childKey = QualityProfileKey.of("newChild", "xoo");
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(childKey));
    activation = new RuleActivation(ActiveRuleKey.of(childKey, XOO_RULE_2));
    activation.setSeverity("MAJOR");
    ruleActivator.activate(dbSession, activation);
    dbSession.commit();
    dbSession.clearCache();

    // set parent -> child profile inherits rule x1 and still has x2
    ruleActivator.setParent(childKey, XOO_PROFILE_KEY);
    assertThat(db.qualityProfileDao().getByKey(dbSession, childKey).getParentKey()).isEqualTo(XOO_PROFILE_KEY);
    verifyHasActiveRule(ActiveRuleKey.of(childKey, XOO_RULE_1), Severity.MAJOR, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "10"));
    verifyHasActiveRule(ActiveRuleKey.of(childKey, XOO_RULE_2), Severity.MAJOR, null, Collections.<String, String>emptyMap());

    // unset parent
    dbSession.clearCache();
    ruleActivator.setParent(childKey, null);
    assertThat(countActiveRules(childKey)).isEqualTo(1);
    assertThat(db.qualityProfileDao().getByKey(dbSession, childKey).getParentKey()).isNull();
    verifyHasActiveRule(ActiveRuleKey.of(childKey, XOO_RULE_2), Severity.MAJOR, null, Collections.<String, String>emptyMap());
  }

  @Test
  public void keep_overridden_rules_when_unsetting_parent() {
    // x1 is activated on the "future parent"
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, XOO_RULE_1));
    activation.setSeverity("MAJOR");
    ruleActivator.activate(activation);
    verifyOneActiveRule(XOO_PROFILE_KEY, XOO_RULE_1, Severity.MAJOR, null, ImmutableMap.of("max", "10"));

    // create empty profile
    QualityProfileKey childKey = QualityProfileKey.of("newChild", "xoo");
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(childKey));
    dbSession.commit();
    dbSession.clearCache();

    // set parent -> child profile inherits rule x1
    ruleActivator.setParent(childKey, XOO_PROFILE_KEY);
    verifyOneActiveRule(childKey, XOO_RULE_1, Severity.MAJOR, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "10"));

    // override x1
    activation = new RuleActivation(ActiveRuleKey.of(childKey, XOO_RULE_1));
    activation.setSeverity("BLOCKER").setParameter("max", "333");
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(childKey, XOO_RULE_1, Severity.BLOCKER, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "333"));

    // unset parent -> keep x1
    ruleActivator.setParent(childKey, null);
    dbSession.clearCache();
    assertThat(db.qualityProfileDao().getByKey(dbSession, childKey).getParentKey()).isNull();
    verifyOneActiveRule(childKey, XOO_RULE_1, Severity.BLOCKER, null, ImmutableMap.of("max", "333"));
  }

  private int countActiveRules(QualityProfileKey profileKey) {
    List<ActiveRuleDto> activeRuleDtos = db.activeRuleDao().findByProfileKey(dbSession, profileKey);
    List<ActiveRule> activeRules = index.findByProfile(profileKey);
    assertThat(activeRuleDtos.size()).as("Not same active rules between db and index").isEqualTo(activeRules.size());
    return activeRuleDtos.size();
  }

  private void verifyOneActiveRule(QualityProfileKey profileKey, RuleKey ruleKey, String expectedSeverity,
                                   @Nullable String expectedInheritance, Map<String, String> expectedParams) {
    assertThat(countActiveRules(profileKey)).isEqualTo(1);
    verifyHasActiveRule(profileKey, ruleKey, expectedSeverity, expectedInheritance, expectedParams);
  }

  private void verifyHasActiveRule(QualityProfileKey profileKey, RuleKey ruleKey, String expectedSeverity,
                                   @Nullable String expectedInheritance, Map<String, String> expectedParams) {
    verifyHasActiveRule(ActiveRuleKey.of(profileKey, ruleKey), expectedSeverity, expectedInheritance, expectedParams);
  }

  private void verifyHasActiveRule(ActiveRuleKey activeRuleKey, String expectedSeverity,
                                   @Nullable String expectedInheritance, Map<String, String> expectedParams) {
    // verify db
    boolean found = false;
    List<ActiveRuleDto> activeRuleDtos = db.activeRuleDao().findByProfileKey(dbSession, activeRuleKey.qProfile());
    for (ActiveRuleDto activeRuleDto : activeRuleDtos) {
      if (activeRuleDto.getKey().equals(activeRuleKey)) {
        found = true;
        assertThat(activeRuleDto.getSeverityString()).isEqualTo(expectedSeverity);
        assertThat(activeRuleDto.getInheritance()).isEqualTo(expectedInheritance);
        List<ActiveRuleParamDto> paramDtos = db.activeRuleDao().findParamsByActiveRuleKey(dbSession, activeRuleDto.getKey());
        assertThat(paramDtos).hasSize(expectedParams.size());
        for (Map.Entry<String, String> entry : expectedParams.entrySet()) {
          ActiveRuleParamDto paramDto = db.activeRuleDao().getParamByKeyAndName(activeRuleDto.getKey(), entry.getKey(), dbSession);
          assertThat(paramDto).isNotNull();
          assertThat(paramDto.getValue()).isEqualTo(entry.getValue());
        }
      }
    }
    assertThat(found).as("Rule is not activated in db").isTrue();

    // verify es
    List<ActiveRule> activeRules = index.findByProfile(activeRuleKey.qProfile());
    found = false;
    for (ActiveRule activeRule : activeRules) {
      if (activeRule.key().equals(activeRuleKey)) {
        found = true;
        assertThat(activeRule.severity()).isEqualTo(expectedSeverity);
        assertThat(activeRule.inheritance()).isEqualTo(expectedInheritance == null ? ActiveRule.Inheritance.NONE : ActiveRule.Inheritance.valueOf(expectedInheritance));

        // verify parameters in es
        assertThat(activeRule.params()).hasSize(expectedParams.size());
        for (Map.Entry<String, String> entry : expectedParams.entrySet()) {
          String value = activeRule.params().get(entry.getKey());
          assertThat(value).isEqualTo(entry.getValue());
        }
      }
    }
    assertThat(found).as("Rule is not activated in index").isTrue();
  }

  private void createChildProfiles() {
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(XOO_CHILD_PROFILE_KEY)
      .setParent(XOO_PROFILE_KEY.name()));
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(XOO_GRAND_CHILD_PROFILE_KEY)
      .setParent(XOO_CHILD_PROFILE_KEY.name()));
    dbSession.commit();
  }

  private void verifyZeroActiveRules(QualityProfileKey key) {
    // verify db
    dbSession.clearCache();
    List<ActiveRuleDto> activeRuleDtos = db.activeRuleDao().findByProfileKey(dbSession, key);
    assertThat(activeRuleDtos).isEmpty();

    // verify es
    List<ActiveRule> activeRules = index.findByProfile(key);
    assertThat(activeRules).isEmpty();
  }
}
