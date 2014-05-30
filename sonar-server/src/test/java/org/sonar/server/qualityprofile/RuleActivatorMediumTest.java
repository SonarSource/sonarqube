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
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.check.Cardinality;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class RuleActivatorMediumTest {

  static final QualityProfileKey XOO_PROFILE_KEY = QualityProfileKey.of("P1", "xoo");
  static final QualityProfileKey XOO_CHILD_PROFILE_KEY = QualityProfileKey.of("P2", "xoo");
  static final QualityProfileKey XOO_GRAND_CHILD_PROFILE_KEY = QualityProfileKey.of("P3", "xoo");

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
    RuleDto xooRule1 = RuleTesting.newDto(RuleKey.of("xoo", "x1"))
      .setSeverity("MINOR").setLanguage("xoo");
    RuleDto xooTemplateRule1 = RuleTesting.newDto(RuleKey.of("xoo", "template1"))
      .setSeverity("MINOR").setLanguage("xoo").setCardinality(Cardinality.MULTIPLE);
    db.ruleDao().insert(dbSession, javaRule, xooRule1, xooTemplateRule1);
    db.ruleDao().addRuleParam(dbSession, xooRule1, RuleParamDto.createFor(xooRule1)
      .setName("max").setDefaultValue("10").setType(RuleParamType.INTEGER.type()));

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
    grantPermission();

    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);

    verifyOneActiveRule(XOO_PROFILE_KEY, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
  }

  @Test
  public void activate_with_default_severity_and_parameter() throws Exception {
    grantPermission();
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    ruleActivator.activate(activation);

    verifyOneActiveRule(XOO_PROFILE_KEY, Severity.MINOR, null, ImmutableMap.of("max", "10"));
  }

  @Test
  public void update_activation_severity_and_parameters() throws Exception {
    // initial activation
    grantPermission();
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.BLOCKER);
    ruleActivator.activate(activation);

    // update
    RuleActivation update = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    update.setSeverity(Severity.CRITICAL);
    update.setParameter("max", "42");
    ruleActivator.activate(update);

    verifyOneActiveRule(XOO_PROFILE_KEY, Severity.CRITICAL, null, ImmutableMap.of("max", "42"));
  }

  @Test
  public void update_activation_but_new_parameter() throws Exception {
    // initial activation
    grantPermission();
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1"));
    RuleActivation activation = new RuleActivation(activeRuleKey);
    activation.setSeverity(Severity.BLOCKER);
    ruleActivator.activate(activation);


    assertThat(db.activeRuleDao().getParamByKeyAndName(activeRuleKey, "max", dbSession)).isNotNull();
    db.activeRuleDao().removeParamByKeyAndName(dbSession, activeRuleKey, "max");
    dbSession.commit();
    assertThat(db.activeRuleDao().getParamByKeyAndName(activeRuleKey, "max", dbSession)).isNull();
    dbSession.clearCache();

    // update
    RuleActivation update = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    update.setSeverity(Severity.CRITICAL);
    update.setParameter("max", "42");
    // contrary to activerule, the param 'max' is supposed to be inserted but not updated
    ruleActivator.activate(update);

    verifyOneActiveRule(XOO_PROFILE_KEY, Severity.CRITICAL, null, ImmutableMap.of("max", "42"));
  }

  @Test
  public void revert_activation_to_default_severity_and_parameters() throws Exception {
    // initial activation
    grantPermission();
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);

    // update
    RuleActivation update = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    ruleActivator.activate(update);

    verifyOneActiveRule(XOO_PROFILE_KEY, Severity.MINOR, null, ImmutableMap.of("max", "10"));
  }

  @Test
  public void fail_to_activate_if_not_granted() throws Exception {
    MockUserSession.set().setLogin("marius");
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));

    try {
      ruleActivator.activate(activation);
      fail();
    } catch (ForbiddenException e) {
      verifyZeroActiveRules(XOO_PROFILE_KEY);
    }
  }

  @Test
  public void fail_to_activate_if_template() throws Exception {
    grantPermission();
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "template1")));

    try {
      ruleActivator.activate(activation);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("A rule template can't be activated on a Quality profile: xoo:template1");
      verifyZeroActiveRules(XOO_PROFILE_KEY);
    }
  }

  @Test
  public void fail_to_activate_if_different_languages() throws Exception {
    // profile and rule have different languages
    grantPermission();
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("squid", "j1")));

    try {
      ruleActivator.activate(activation);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Rule squid:j1 and profile P1:xoo have different languages");
      verifyZeroActiveRules(XOO_PROFILE_KEY);
    }
  }

  @Test
  public void fail_to_activate_if_unknown_rule() throws Exception {
    // profile and rule have different languages
    grantPermission();
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x3")));

    try {
      ruleActivator.activate(activation);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Rule not found: xoo:x3");
      verifyZeroActiveRules(XOO_PROFILE_KEY);
    }
  }

  @Test
  public void fail_to_activate_if_unknown_profile() throws Exception {
    // profile and rule have different languages
    grantPermission();
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(QualityProfileKey.of("other", "js"), RuleKey.of("xoo", "x1")));

    try {
      ruleActivator.activate(activation);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Quality profile not found: other:js");
    }
  }

  @Test
  public void fail_to_activate_if_invalid_parameter() throws Exception {
    grantPermission();
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
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
  public void deactivate() throws Exception {
    // activation
    grantPermission();
    ActiveRuleKey key = ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1"));
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
    grantPermission();

    // deactivation
    ActiveRuleKey key = ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1"));
    ruleActivator.deactivate(key);

    verifyZeroActiveRules(XOO_PROFILE_KEY);
  }

  @Test
  public void deactivation_fails_if_rule_not_found() throws Exception {
    grantPermission();
    ActiveRuleKey key = ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x3"));
    try {
      ruleActivator.deactivate(key);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Rule not found: xoo:x3");
      verifyZeroActiveRules(XOO_PROFILE_KEY);
    }
  }

  @Test
  public void deactivation_fails_if_profile_not_found() throws Exception {
    grantPermission();
    ActiveRuleKey key = ActiveRuleKey.of(QualityProfileKey.of("other", "js"), RuleKey.of("xoo", "x1"));
    try {
      ruleActivator.deactivate(key);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Quality profile not found: other:js");
    }
  }

  // INHERITANCE OF PROFILES
  @Test
  public void activate_on_child_profile() throws Exception {
    grantPermission();
    createChildProfiles();

    // activate on child profile, but not on root
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_CHILD_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);

    verifyZeroActiveRules(XOO_PROFILE_KEY);
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
  }

  @Test
  public void propagate_activation_on_child_profiles() throws Exception {
    grantPermission();
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);

    verifyOneActiveRule(XOO_PROFILE_KEY, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
  }

  @Test
  public void propagate_activation_update_on_child_profiles() throws Exception {
    grantPermission();
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);
    verifyOneActiveRule(XOO_PROFILE_KEY, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));

    // update on parent
    activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "8");
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(XOO_PROFILE_KEY, Severity.INFO, null, ImmutableMap.of("max", "8"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, Severity.INFO, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "8"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, Severity.INFO, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "8"));

    // update on child -> propagate on grand child only
    activation = new RuleActivation(ActiveRuleKey.of(XOO_CHILD_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.MINOR);
    activation.setParameter("max", "9");
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(XOO_PROFILE_KEY, Severity.INFO, null, ImmutableMap.of("max", "8"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, Severity.MINOR, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "9"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, Severity.MINOR, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "9"));

    // update on grand child
    activation = new RuleActivation(ActiveRuleKey.of(XOO_GRAND_CHILD_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "10");
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(XOO_PROFILE_KEY, Severity.INFO, null, ImmutableMap.of("max", "8"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, Severity.MINOR, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "9"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, Severity.BLOCKER, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "10"));
  }

  @Test
  public void do_not_propagate_activation_update_on_overriding_child_profiles() throws Exception {
    grantPermission();
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);
    verifyOneActiveRule(XOO_PROFILE_KEY, Severity.INFO, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, Severity.INFO, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, Severity.INFO, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));

    // override on child
    activation = new RuleActivation(ActiveRuleKey.of(XOO_CHILD_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "8");
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(XOO_PROFILE_KEY, Severity.INFO, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, Severity.BLOCKER, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "8"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "8"));

    // change on parent -> do not propagate on children because they're overriding values
    activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.CRITICAL);
    activation.setParameter("max", "10");
    ruleActivator.activate(activation);
    dbSession.clearCache();
    verifyOneActiveRule(XOO_PROFILE_KEY, Severity.CRITICAL, null, ImmutableMap.of("max", "10"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, Severity.BLOCKER, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "8"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "8"));
  }

  @Test
  public void propagate_deactivation_on_child_profiles() throws Exception {
    grantPermission();
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);
    verifyOneActiveRule(XOO_PROFILE_KEY, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));

    // deactivate on root
    ruleActivator.deactivate(activation.getKey());

    verifyZeroActiveRules(XOO_PROFILE_KEY);
    verifyZeroActiveRules(XOO_CHILD_PROFILE_KEY);
    verifyZeroActiveRules(XOO_GRAND_CHILD_PROFILE_KEY);
  }

  @Test
  public void do_not_deactivate_inherited_or_overridden_rule() throws Exception {
    grantPermission();
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);
    verifyOneActiveRule(XOO_PROFILE_KEY, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));

    // try to deactivate on child
    try {
      ruleActivator.deactivate(ActiveRuleKey.of(XOO_CHILD_PROFILE_KEY, RuleKey.of("xoo", "x1")));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Cannot deactivate inherited rule 'xoo:x1'");
    }
  }

  @Test
  public void reset_activation_on_child_profile() throws Exception {
    grantPermission();
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);
    verifyOneActiveRule(XOO_PROFILE_KEY, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));

    // override
    activation = new RuleActivation(ActiveRuleKey.of(XOO_CHILD_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "10");
    ruleActivator.activate(activation);
    verifyOneActiveRule(XOO_PROFILE_KEY, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, Severity.INFO, ActiveRuleDto.OVERRIDES, ImmutableMap.of("max", "10"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, Severity.INFO, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "10"));

    // reset -> remove overridden values
    activation = new RuleActivation(ActiveRuleKey.of(XOO_CHILD_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    ruleActivator.reset(activation);
    verifyOneActiveRule(XOO_PROFILE_KEY, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_CHILD_PROFILE_KEY, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRule(XOO_GRAND_CHILD_PROFILE_KEY, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
  }

  private void verifyOneActiveRule(QualityProfileKey profileKey, String expectedSeverity,
                                   @Nullable String expectedInheritance, Map<String, String> expectedParams) {

    // verify db
    List<ActiveRuleDto> activeRuleDtos = db.activeRuleDao().findByProfileKey(dbSession, profileKey);
    assertThat(activeRuleDtos).hasSize(1);
    ActiveRuleDto activeRuleDto = activeRuleDtos.get(0);
    assertThat(activeRuleDto.getSeverityString()).isEqualTo(expectedSeverity);
    assertThat(activeRuleDto.getInheritance()).isEqualTo(expectedInheritance);

    // verify parameters in db
    List<ActiveRuleParamDto> paramDtos = db.activeRuleDao().findParamsByActiveRuleKey(dbSession, activeRuleDto.getKey());
    assertThat(paramDtos).hasSize(expectedParams.size());
    for (Map.Entry<String, String> entry : expectedParams.entrySet()) {
      ActiveRuleParamDto paramDto = db.activeRuleDao().getParamByKeyAndName(activeRuleDto.getKey(), entry.getKey(), dbSession);
      assertThat(paramDto).isNotNull();
      assertThat(paramDto.getValue()).isEqualTo(entry.getValue());
    }

    // verify es
    List<ActiveRule> activeRules = index.findByProfile(profileKey);
    assertThat(activeRules).hasSize(1);
    ActiveRule activeRule = activeRules.get(0);
    assertThat(activeRule.severity()).isEqualTo(expectedSeverity);
    assertThat(activeRule.inheritance()).isEqualTo(expectedInheritance == null ? ActiveRule.Inheritance.NONE : ActiveRule.Inheritance.valueOf(expectedInheritance));

    // verify parameters in es
    assertThat(activeRule.params()).hasSize(expectedParams.size());
    for (Map.Entry<String, String> entry : expectedParams.entrySet()) {
      String value = activeRule.params().get(entry.getKey());
      assertThat(value).isEqualTo(entry.getValue());
    }
  }

  private void createChildProfiles() {
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(XOO_CHILD_PROFILE_KEY)
      .setParent(XOO_PROFILE_KEY.name()));
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(XOO_GRAND_CHILD_PROFILE_KEY)
      .setParent(XOO_CHILD_PROFILE_KEY.name()));
    dbSession.commit();
  }

  private void grantPermission() {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("marius");
  }

  private void verifyZeroActiveRules(QualityProfileKey key) {
    // verify db
    dbSession.clearCache();
    List<ActiveRuleDto> activeRuleDtos = db.activeRuleDao().findByProfileKey(dbSession, key);
    assertThat(activeRuleDtos).isEmpty();
    //TODO check that assertion is required with Simon
    //assertThat(db.activeRuleDao().findParamsByActiveRuleKey(dbSession,key)).isEmpty();

    // verify es
    List<ActiveRule> activeRules = index.findByProfile(key);
    assertThat(activeRules).isEmpty();
  }
}
