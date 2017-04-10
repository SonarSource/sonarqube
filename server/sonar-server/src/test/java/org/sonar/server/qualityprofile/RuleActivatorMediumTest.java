/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.INFO;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.db.qualityprofile.ActiveRuleDto.INHERITED;
import static org.sonar.db.qualityprofile.ActiveRuleDto.OVERRIDES;
import static org.sonar.db.rule.RuleTesting.XOO_X1;
import static org.sonar.db.rule.RuleTesting.XOO_X2;
import static org.sonar.db.rule.RuleTesting.newCustomRule;
import static org.sonar.db.rule.RuleTesting.newDto;
import static org.sonar.db.rule.RuleTesting.newTemplateRule;
import static org.sonar.db.rule.RuleTesting.newXooX1;
import static org.sonar.db.rule.RuleTesting.newXooX2;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P1_KEY;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P2_KEY;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P3_KEY;

// TODO Replace ServerTester by EsTester and DbTester
public class RuleActivatorMediumTest {

  static final RuleKey TEMPLATE_RULE_KEY = RuleKey.of("xoo", "template1");
  static final RuleKey CUSTOM_RULE_KEY = RuleKey.of("xoo", "custom1");

  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  DbClient db;
  DbSession dbSession;

  RuleActivator ruleActivator;

  RuleIndexer ruleIndexer;

  ActiveRuleIndexer activeRuleIndexer;

  QualityProfileDto profileDto;
  private OrganizationDto organization;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);
    ruleActivator = tester.get(RuleActivator.class);
    activeRuleIndexer = tester.get(ActiveRuleIndexer.class);
    ruleIndexer = tester.get(RuleIndexer.class);
    String defaultOrganizationUuid = tester.get(DefaultOrganizationProvider.class).get().getUuid();
    organization = db.organizationDao().selectByUuid(dbSession, defaultOrganizationUuid).orElseThrow(() -> new IllegalStateException(String.format("Cannot find default organization '%s'", defaultOrganizationUuid)));

    // create pre-defined rules
    RuleDto javaRule = newDto(RuleKey.of("squid", "j1"))
      .setSeverity("MAJOR").setLanguage("java");
    RuleDto xooRule1 = newXooX1().setSeverity("MINOR");
    RuleDto xooRule2 = newXooX2().setSeverity("INFO");
    RuleDto xooTemplateRule1 = newTemplateRule(TEMPLATE_RULE_KEY)
      .setSeverity("MINOR").setLanguage("xoo");

    // store pre-defined rules in database
    asList(javaRule, xooRule1, xooRule2, xooTemplateRule1).stream()
      .map(RuleDto::getDefinition)
      .forEach(definition -> db.ruleDao().insert(dbSession, definition));
    db.ruleDao().insertRuleParam(dbSession, xooRule1.getDefinition(), RuleParamDto.createFor(xooRule1.getDefinition())
      .setName("max").setDefaultValue("10").setType(RuleParamType.INTEGER.type()));
    db.ruleDao().insertRuleParam(dbSession, xooRule1.getDefinition(), RuleParamDto.createFor(xooRule1.getDefinition())
      .setName("min").setType(RuleParamType.INTEGER.type()));
    db.ruleDao().insertRuleParam(dbSession, xooTemplateRule1.getDefinition(), RuleParamDto.createFor(xooTemplateRule1.getDefinition())
      .setName("format").setType(RuleParamType.STRING.type()));

    // create custom rule
    RuleDto xooCustomRule1 = newCustomRule(xooTemplateRule1).setRuleKey(CUSTOM_RULE_KEY.rule())
      .setSeverity("MINOR").setLanguage("xoo");

    // store custom rule in database
    db.ruleDao().insert(dbSession, xooCustomRule1.getDefinition());
    db.ruleDao().insertRuleParam(dbSession, xooCustomRule1.getDefinition(), RuleParamDto.createFor(xooTemplateRule1.getDefinition())
      .setName("format").setDefaultValue("txt").setType(RuleParamType.STRING.type()));

    // create pre-defined profile P1
    profileDto = QProfileTesting.newXooP1(organization);
    db.qualityProfileDao().insert(dbSession, profileDto);

    // index all rules
    dbSession.commit();
    ruleIndexer.indexRuleDefinitions(asList(javaRule, xooRule1, xooRule2, xooTemplateRule1, xooCustomRule1).stream().map(RuleDto::getKey).collect(Collectors.toList()));
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void activate() {
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activation.setParameter("max", "7");
    activation.setParameter("min", "3");
    List<ActiveRuleChange> changes = ruleActivator.activate(dbSession, activation, XOO_P1_KEY);
    dbSession.commit();
    dbSession.clearCache();

    assertThat(countActiveRules(XOO_P1_KEY)).isEqualTo(1);
    verifyHasActiveRuleInDb(ActiveRuleKey.of(XOO_P1_KEY, XOO_X1), BLOCKER, null,
      ImmutableMap.of("max", "7", "min", "3"));
    assertThat(changes).hasSize(1);
    assertThat(changes.get(0).getType()).isEqualTo(ActiveRuleChange.Type.ACTIVATED);
  }

  @Test
  public void automatic_activation_does_not_update_intended_column() {
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activation.setParameter("max", "7");
    activation.setParameter("min", "3");
    List<ActiveRuleChange> changes = ruleActivator.activate(dbSession, activation, XOO_P1_KEY);
    dbSession.commit();
    dbSession.clearCache();
    userSessionRule.anonymous();

    assertThat(countActiveRules(XOO_P1_KEY)).isEqualTo(1);
    verifyHasActiveRuleInDb(ActiveRuleKey.of(XOO_P1_KEY, XOO_X1), BLOCKER, null,
      ImmutableMap.of("max", "7", "min", "3"));
    assertThat(changes).hasSize(1);
    assertThat(changes.get(0).getType()).isEqualTo(ActiveRuleChange.Type.ACTIVATED);
    assertProfileHasBeenUpdatedAutomatically(XOO_P1_KEY);
  }

  @Test
  public void activate_with_profile_dto() {
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activation.setParameter("max", "7");
    activation.setParameter("min", "3");
    userSessionRule.logIn().setRoot();
    List<ActiveRuleChange> changes = ruleActivator.activate(dbSession, activation, profileDto);
    dbSession.commit();
    dbSession.clearCache();

    assertThat(countActiveRules(XOO_P1_KEY)).isEqualTo(1);
    verifyHasActiveRuleInDb(ActiveRuleKey.of(XOO_P1_KEY, XOO_X1), BLOCKER, null,
      ImmutableMap.of("max", "7", "min", "3"));
    assertThat(changes).hasSize(1);
    assertThat(changes.get(0).getType()).isEqualTo(ActiveRuleChange.Type.ACTIVATED);
    assertProfileHasBeenUpdatedManually(profileDto.getKey());
  }

  @Test
  public void activate_with_default_severity_and_parameter() {
    activate(new RuleActivation(XOO_X1), XOO_P1_KEY);

    assertThat(countActiveRules(XOO_P1_KEY)).isEqualTo(1);
    verifyHasActiveRuleInDb(ActiveRuleKey.of(XOO_P1_KEY, XOO_X1), MINOR, null,
      ImmutableMap.of("max", "10"));
  }

  /**
   * SONAR-5841
   */
  @Test
  public void activate_with_empty_parameter_having_no_default_value() {
    activate(new RuleActivation(XOO_X1)
      .setParameter("min", ""),
      XOO_P1_KEY);

    assertThat(countActiveRules(XOO_P1_KEY)).isEqualTo(1);
    verifyHasActiveRuleInDb(ActiveRuleKey.of(XOO_P1_KEY, XOO_X1), MINOR, null,
      // Max should be set to default value, min has no value it should be ignored
      ImmutableMap.of("max", "10"));
  }

  /**
   * SONAR-5841
   */
  @Test
  public void activate_with_empty_parameters() {
    activate(new RuleActivation(XOO_X1)
      .setParameters(ImmutableMap.of("max", "", "min", "")),
      XOO_P1_KEY);

    assertThat(countActiveRules(XOO_P1_KEY)).isEqualTo(1);
    // Max should be set to default value, min has not value it should be ignored
    verifyHasActiveRuleInDb(ActiveRuleKey.of(XOO_P1_KEY, XOO_X1), MINOR, null,
      ImmutableMap.of("max", "10"));
  }

  /**
   * SONAR-5840
   */
  @Test
  public void activate_rule_with_negative_integer_value_on_parameter_having_no_default_value() {
    activate(new RuleActivation(XOO_X1)
      .setParameter("min", "-10"),
      XOO_P1_KEY);

    assertThat(countActiveRules(XOO_P1_KEY)).isEqualTo(1);
    // Max should be set to default value, min should be set to -10
    verifyHasActiveRuleInDb(ActiveRuleKey.of(XOO_P1_KEY, XOO_X1), MINOR, null,
      ImmutableMap.of("max", "10", "min", "-10"));
  }

  @Test
  public void activation_ignores_unsupported_parameters() {
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setParameter("xxx", "yyy");
    activate(activation, XOO_P1_KEY);

    assertThat(countActiveRules(XOO_P1_KEY)).isEqualTo(1);
    verifyHasActiveRuleInDb(ActiveRuleKey.of(XOO_P1_KEY, XOO_X1), MINOR, null, ImmutableMap.of("max", "10"));
  }

  @Test
  public void update_activation_severity_and_parameters() {
    // initial activation
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activate(activation, XOO_P1_KEY);

    // update
    RuleActivation update = new RuleActivation(XOO_X1);
    update.setSeverity(CRITICAL);
    update.setParameter("max", "42");
    List<ActiveRuleChange> changes = activate(update, XOO_P1_KEY);

    assertThat(countActiveRules(XOO_P1_KEY)).isEqualTo(1);
    verifyHasActiveRuleInDb(ActiveRuleKey.of(XOO_P1_KEY, XOO_X1), CRITICAL, null, ImmutableMap.of("max", "42"));
    assertThat(changes).hasSize(1);
    assertThat(changes.get(0).getType()).isEqualTo(ActiveRuleChange.Type.UPDATED);
  }

  @Test
  public void update_activation_with_parameter_without_default_value() {
    // initial activation -> param "max" has a default value
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activate(activation, XOO_P1_KEY);
    verifyHasActiveRuleInDb(ActiveRuleKey.of(XOO_P1_KEY, XOO_X1), BLOCKER, null,
      ImmutableMap.of("max", "10"));

    // update param "min", which has no default value
    RuleActivation update = new RuleActivation(XOO_X1);
    update.setParameter("min", "3");
    List<ActiveRuleChange> changes = activate(update, XOO_P1_KEY);
    assertThat(countActiveRules(XOO_P1_KEY)).isEqualTo(1);
    verifyHasActiveRuleInDb(ActiveRuleKey.of(XOO_P1_KEY, XOO_X1), BLOCKER, null,
      ImmutableMap.of("min", "3", "max", "10"));
    assertThat(changes).hasSize(1);
    assertThat(changes.get(0).getType()).isEqualTo(ActiveRuleChange.Type.UPDATED);
  }

  @Test
  public void update_activation_remove_parameter_value_having_default_value() {
    // initial activation
    activate(new RuleActivation(XOO_X1).setSeverity(BLOCKER).setParameter("max", "20"), XOO_P1_KEY);

    // update
    activate(new RuleActivation(XOO_X1).setParameter("max", null), XOO_P1_KEY);

    verifyHasActiveRuleInDb(ActiveRuleKey.of(XOO_P1_KEY, XOO_X1), BLOCKER, null, ImmutableMap.of("max", "10"));
  }

  @Test
  public void update_activation_remove_parameter_value_without_default_value() {
    // initial activation -> param "min" has a no default value
    activate(new RuleActivation(XOO_X1).setSeverity(BLOCKER).setParameter("min", "5"), XOO_P1_KEY);

    // update param "min" with empty value
    activate(new RuleActivation(XOO_X1).setParameter("min", null), XOO_P1_KEY);

    verifyHasActiveRuleInDb(ActiveRuleKey.of(XOO_P1_KEY, XOO_X1), BLOCKER, null, ImmutableMap.of("max", "10"));
  }

  @Test
  public void update_activation_but_new_parameter() {
    // initial activation
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(XOO_P1_KEY, XOO_X1);
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activate(activation, XOO_P1_KEY);

    assertThat(db.activeRuleDao().selectParamByKeyAndName(activeRuleKey, "max", dbSession)).isNotNull();
    db.activeRuleDao().deleteParamByKeyAndName(dbSession, activeRuleKey, "max");
    dbSession.commit();
    assertThat(db.activeRuleDao().selectParamByKeyAndName(activeRuleKey, "max", dbSession)).isNull();
    dbSession.clearCache();

    // update
    RuleActivation update = new RuleActivation(XOO_X1);
    update.setSeverity(CRITICAL);
    update.setParameter("max", "42");
    // contrary to activerule, the param 'max' is supposed to be inserted but not updated
    activate(update, XOO_P1_KEY);

    assertThat(countActiveRules(XOO_P1_KEY)).isEqualTo(1);
    verifyHasActiveRuleInDb(activeRuleKey, CRITICAL, null, ImmutableMap.of("max", "42"));
  }

  @Test
  public void ignore_activation_without_changes() {
    // initial activation
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activate(activation, XOO_P1_KEY);

    // update with exactly the same severity and params
    RuleActivation update = new RuleActivation(XOO_X1);
    update.setSeverity(BLOCKER);
    List<ActiveRuleChange> changes = activate(update, XOO_P1_KEY);
    assertThat(changes).isEmpty();
  }

  @Test
  public void do_not_change_severity_and_params_if_unset_and_already_activated() {
    // initial activation
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(XOO_P1_KEY, XOO_X1);
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activation.setParameter("max", "7");
    activate(activation, XOO_P1_KEY);

    // update without any severity or params => keep
    RuleActivation update = new RuleActivation(XOO_X1);
    activate(update, XOO_P1_KEY);

    assertThat(countActiveRules(XOO_P1_KEY)).isEqualTo(1);
    verifyHasActiveRuleInDb(activeRuleKey, BLOCKER, null, ImmutableMap.of("max", "7"));
  }

  @Test
  public void revert_activation_to_default_severity_and_parameters() {
    // initial activation
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(XOO_P1_KEY, XOO_X1);
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activation.setParameter("max", "7");
    activation.setParameter("min", "3");
    activate(activation, XOO_P1_KEY);

    // update without any severity or params = reset
    RuleActivation update = new RuleActivation(XOO_X1).setReset(true);
    activate(update, XOO_P1_KEY);
    assertThat(countActiveRules(XOO_P1_KEY)).isEqualTo(1);
    verifyHasActiveRuleInDb(activeRuleKey, MINOR, null,
      // only default values
      ImmutableMap.of("max", "10"));
  }

  @Test
  public void ignore_parameters_when_activating_custom_rule() {
    // initial activation
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(XOO_P1_KEY, CUSTOM_RULE_KEY);
    RuleActivation activation = new RuleActivation(CUSTOM_RULE_KEY);
    activate(activation, XOO_P1_KEY);

    // update
    RuleActivation update = new RuleActivation(CUSTOM_RULE_KEY)
      .setParameter("format", "xls");
    activate(update, XOO_P1_KEY);

    assertThat(countActiveRules(XOO_P1_KEY)).isEqualTo(1);
    verifyHasActiveRuleInDb(activeRuleKey, MINOR, null, ImmutableMap.of("format", "txt"));
  }

  @Test
  public void fail_to_activate_if_template() {
    RuleActivation activation = new RuleActivation(TEMPLATE_RULE_KEY);

    try {
      activate(activation, XOO_P1_KEY);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Rule template can't be activated on a Quality profile: xoo:template1");
      verifyZeroActiveRules(XOO_P1_KEY);
    }
  }

  @Test
  public void fail_to_activate_if_different_languages() {
    // profile and rule have different languages
    RuleActivation activation = new RuleActivation(RuleKey.of("squid", "j1"));

    try {
      activate(activation, XOO_P1_KEY);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Rule squid:j1 and profile XOO_P1 have different languages");
      verifyZeroActiveRules(XOO_P1_KEY);
    }
  }

  @Test
  public void fail_to_activate_if_unknown_rule() {
    // profile and rule have different languages
    RuleActivation activation = new RuleActivation(RuleKey.of("xoo", "x3"));

    try {
      activate(activation, XOO_P1_KEY);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Rule not found: xoo:x3");
      verifyZeroActiveRules(XOO_P1_KEY);
    }
  }

  @Test
  public void fail_to_activate_if_rule_with_removed_status() {
    RuleDefinitionDto ruleDto = db.ruleDao().selectOrFailDefinitionByKey(dbSession, XOO_X1);
    ruleDto.setStatus(RuleStatus.REMOVED);
    db.ruleDao().update(dbSession, ruleDto);
    dbSession.commit();
    dbSession.clearCache();

    RuleActivation activation = new RuleActivation(XOO_X1);

    try {
      activate(activation, XOO_P1_KEY);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Rule was removed: xoo:x1");
      verifyZeroActiveRules(XOO_P1_KEY);
    }
  }

  @Test
  public void fail_to_activate_if_unknown_profile() {
    try {
      activate(new RuleActivation(XOO_X1), "unknown");
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Quality profile not found: unknown");
    }
  }

  @Test
  public void fail_to_activate_if_invalid_parameter() {
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setParameter("max", "foo");

    try {
      activate(activation, XOO_P1_KEY);
      fail();
    } catch (BadRequestException e) {
      assertThat(e.getMessage()).isEqualTo("Value 'foo' must be an integer.");
      verifyZeroActiveRules(XOO_P1_KEY);
    }
  }

  @Test
  public void deactivate() {
    // activation
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activation.setParameter("max", "7");
    userSessionRule.logIn();
    activate(activation, XOO_P1_KEY);

    // deactivation
    ruleActivator.deactivateAndUpdateIndex(dbSession, ActiveRuleKey.of(XOO_P1_KEY, XOO_X1));

    verifyZeroActiveRules(XOO_P1_KEY);
    assertProfileHasBeenUpdatedManually(XOO_P1_KEY);
  }

  @Test
  public void ignore_deactivation_if_rule_not_activated() {
    // deactivation
    ActiveRuleKey key = ActiveRuleKey.of(XOO_P1_KEY, XOO_X1);
    ruleActivator.deactivateAndUpdateIndex(dbSession, key);

    verifyZeroActiveRules(XOO_P1_KEY);
  }

  @Test
  public void deactivation_fails_if_rule_not_found() {
    ActiveRuleKey key = ActiveRuleKey.of(XOO_P1_KEY, RuleKey.of("xoo", "x3"));
    try {
      ruleActivator.deactivateAndUpdateIndex(dbSession, key);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Rule not found: xoo:x3");
      verifyZeroActiveRules(XOO_P1_KEY);
    }
  }

  @Test
  public void deactivation_fails_if_profile_not_found() {
    ActiveRuleKey key = ActiveRuleKey.of("unknown", XOO_X1);
    try {
      ruleActivator.deactivateAndUpdateIndex(dbSession, key);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Quality profile not found: unknown");
    }
  }

  @Test
  public void allow_to_deactivate_removed_rule() {
    // activation
    RuleActivation activation = new RuleActivation(XOO_X1);
    activate(activation, XOO_P1_KEY);

    // set rule as removed
    RuleDefinitionDto rule = db.ruleDao().selectOrFailDefinitionByKey(dbSession, XOO_X1);
    rule.setStatus(RuleStatus.REMOVED);
    db.ruleDao().update(dbSession, rule);
    dbSession.commit();
    dbSession.clearCache();

    // deactivation
    ruleActivator.deactivateAndUpdateIndex(dbSession, ActiveRuleKey.of(XOO_P1_KEY, XOO_X1));

    verifyZeroActiveRules(XOO_P1_KEY);
  }

  // INHERITANCE OF PROFILES
  @Test
  public void activate_on_child_profile_but_not_on_parent() {
    createChildProfiles();

    // activate on child profile, but not on root
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activation.setParameter("max", "7");
    activate(activation, XOO_P2_KEY);

    verifyZeroActiveRules(XOO_P1_KEY);
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "7"));

    // update severity on child
    activation = new RuleActivation(XOO_X1);
    activation.setSeverity(MINOR);
    activation.setParameter("max", "77");
    activate(activation, XOO_P2_KEY);

    verifyZeroActiveRules(XOO_P1_KEY);
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, MINOR, null, ImmutableMap.of("max", "77"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, MINOR, INHERITED, ImmutableMap.of("max", "77"));
  }

  @Test
  public void propagate_activation_on_child_profiles() {
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activation.setParameter("max", "7");
    List<ActiveRuleChange> changes = activate(activation, XOO_P1_KEY);

    assertThat(changes).hasSize(3);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "7"));
  }

  @Test
  public void propagate_activation_update_on_child_profiles() {
    createChildProfiles();
    userSessionRule.logIn();

    // activate on root profile
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activation.setParameter("max", "7");
    activate(activation, XOO_P1_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "7"));

    // update on parent
    activation = new RuleActivation(XOO_X1);
    activation.setSeverity(INFO);
    activation.setParameter("max", "8");
    activate(activation, XOO_P1_KEY);

    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, INFO, null, ImmutableMap.of("max", "8"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, INFO, INHERITED, ImmutableMap.of("max", "8"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, INFO, INHERITED, ImmutableMap.of("max", "8"));

    // update on child -> propagate on grand child only
    activation = new RuleActivation(XOO_X1);
    activation.setSeverity(MINOR);
    activation.setParameter("max", "9");
    activate(activation, XOO_P2_KEY);

    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, INFO, null, ImmutableMap.of("max", "8"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, MINOR, OVERRIDES, ImmutableMap.of("max", "9"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, MINOR, INHERITED, ImmutableMap.of("max", "9"));

    // update on grand child
    activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activation.setParameter("max", "10");
    activate(activation, XOO_P3_KEY);

    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, INFO, null, ImmutableMap.of("max", "8"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, MINOR, OVERRIDES, ImmutableMap.of("max", "9"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, BLOCKER, OVERRIDES, ImmutableMap.of("max", "10"));

    assertProfileHasBeenUpdatedManually(XOO_P1_KEY);
    assertProfileHasBeenUpdatedManually(XOO_P2_KEY);
    assertProfileHasBeenUpdatedManually(XOO_P3_KEY);
  }

  @Test
  public void do_not_propagate_activation_update_on_child_overrides() {
    createChildProfiles();

    // activate on root profile P1
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(INFO);
    activation.setParameter("max", "7");
    activate(activation, XOO_P1_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, INFO, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, INFO, INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, INFO, INHERITED, ImmutableMap.of("max", "7"));

    // override on child P2
    activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activation.setParameter("max", "8");
    activate(activation, XOO_P2_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, INFO, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, BLOCKER, OVERRIDES, ImmutableMap.of("max", "8"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "8"));

    // change on parent -> do not propagate on children because they're overriding values
    activation = new RuleActivation(XOO_X1);
    activation.setSeverity(CRITICAL);
    activation.setParameter("max", "9");
    activate(activation, XOO_P1_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, CRITICAL, null, ImmutableMap.of("max", "9"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, BLOCKER, OVERRIDES, ImmutableMap.of("max", "8"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "8"));

    // reset on parent (use default severity and params) -> do not propagate on children because they're overriding values
    activation = new RuleActivation(XOO_X1).setReset(true);
    activate(activation, XOO_P1_KEY);

    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, MINOR, null, ImmutableMap.of("max", "10"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, BLOCKER, OVERRIDES, ImmutableMap.of("max", "8"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "8"));
  }

  @Test
  public void active_on_parent_a_rule_already_activated_on_child() {
    createChildProfiles();

    // activate on child profile
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(INFO);
    activation.setParameter("max", "7");
    activate(activation, XOO_P2_KEY);
    verifyZeroActiveRules(XOO_P1_KEY);
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, INFO, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, INFO, INHERITED, ImmutableMap.of("max", "7"));

    // active the same rule on root profile -> mark the child profile as OVERRIDES
    activation = new RuleActivation(XOO_X1);
    activation.setSeverity(MAJOR);
    activation.setParameter("max", "8");
    activate(activation, XOO_P1_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, MAJOR, null, ImmutableMap.of("max", "8"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, INFO, OVERRIDES, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, INFO, INHERITED, ImmutableMap.of("max", "7"));
  }

  @Test
  public void do_not_override_on_child_if_same_values() {
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(INFO);
    activation.setParameter("max", "7");
    activate(activation, XOO_P1_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, INFO, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, INFO, INHERITED, ImmutableMap.of("max", "7"));

    // override on child P2 with same severity and params -> do nothing (still INHERITED but not OVERRIDDEN)
    activation = new RuleActivation(XOO_X1);
    activation.setSeverity(INFO);
    activation.setParameter("max", "7");
    activate(activation, XOO_P2_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, INFO, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, INFO, INHERITED, ImmutableMap.of("max", "7"));
  }

  @Test
  public void propagate_deactivation_on_child_profiles() {
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activation.setParameter("max", "7");
    activate(activation, XOO_P1_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "7"));

    // deactivate on root
    ruleActivator.deactivateAndUpdateIndex(dbSession, ActiveRuleKey.of(XOO_P1_KEY, XOO_X1));

    verifyZeroActiveRules(XOO_P1_KEY);
    verifyZeroActiveRules(XOO_P2_KEY);
    verifyZeroActiveRules(XOO_P3_KEY);
  }

  @Test
  public void propagate_deactivation_even_on_child_overrides() {
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(INFO);
    activation.setParameter("max", "7");
    activate(activation, XOO_P1_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, INFO, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, INFO, INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, INFO, INHERITED, ImmutableMap.of("max", "7"));

    // override on child
    activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activation.setParameter("max", "8");
    activate(activation, XOO_P2_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, INFO, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, BLOCKER, OVERRIDES, ImmutableMap.of("max", "8"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "8"));

    // deactivate on parent -> do not propagate on children because they're overriding values
    ruleActivator.deactivateAndUpdateIndex(dbSession, ActiveRuleKey.of(XOO_P1_KEY, XOO_X1));
    dbSession.clearCache();
    verifyZeroActiveRules(XOO_P1_KEY);
    verifyZeroActiveRules(XOO_P2_KEY);
    verifyZeroActiveRules(XOO_P3_KEY);
  }

  @Test
  public void do_not_deactivate_inherited_or_overridden_rule() {
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activation.setParameter("max", "7");
    activate(activation, XOO_P1_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "7"));

    // try to deactivate on child
    try {
      ruleActivator.deactivateAndUpdateIndex(dbSession, ActiveRuleKey.of(XOO_P2_KEY, XOO_X1));
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Cannot deactivate inherited rule 'xoo:x1'");
    }
  }

  @Test
  public void reset_child_profile() {
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activation.setParameter("max", "7");
    activate(activation, XOO_P1_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "7"));

    // override
    activation = new RuleActivation(XOO_X1);
    activation.setSeverity(INFO);
    activation.setParameter("max", "10");
    activate(activation, XOO_P2_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, INFO, OVERRIDES, ImmutableMap.of("max", "10"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, INFO, INHERITED, ImmutableMap.of("max", "10"));

    // reset -> remove overridden values
    activation = new RuleActivation(XOO_X1).setReset(true);
    activate(activation, XOO_P2_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "7"));
  }

  @Test
  public void reset_is_not_propagated_to_child_overrides() {
    createChildProfiles();

    // activate on root profile
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(BLOCKER);
    activation.setParameter("max", "7");
    activate(activation, XOO_P1_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "7"));

    // override on child
    activation = new RuleActivation(XOO_X1);
    activation.setSeverity(INFO);
    activation.setParameter("max", "10");
    activate(activation, XOO_P2_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, INFO, OVERRIDES, ImmutableMap.of("max", "10"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, INFO, INHERITED, ImmutableMap.of("max", "10"));

    // override on grand child
    activation = new RuleActivation(XOO_X1);
    activation.setSeverity(MINOR);
    activation.setParameter("max", "20");
    activate(activation, XOO_P3_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, INFO, OVERRIDES, ImmutableMap.of("max", "10"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, MINOR, OVERRIDES, ImmutableMap.of("max", "20"));

    // reset child P2 -> keep the overridden grand-child P3
    activation = new RuleActivation(XOO_X1).setReset(true);
    activate(activation, XOO_P2_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, BLOCKER, INHERITED, ImmutableMap.of("max", "7"));
    verifyOneActiveRuleInDb(XOO_P3_KEY, XOO_X1, MINOR, OVERRIDES, ImmutableMap.of("max", "20"));
  }

  @Test
  public void ignore_reset_if_not_activated() {
    createChildProfiles();
    RuleActivation activation = new RuleActivation(XOO_X1).setReset(true);
    activate(activation, XOO_P1_KEY);

    verifyZeroActiveRules(XOO_P1_KEY);
    verifyZeroActiveRules(XOO_P2_KEY);
  }

  @Test
  public void bulk_activation() {
    // Generate more rules than the search's max limit
    int bulkSize = SearchOptions.MAX_LIMIT + 10;
    List<RuleKey> keys = new ArrayList<>();
    for (int i = 0; i < bulkSize; i++) {
      RuleDefinitionDto ruleDefinitionDto = newDto(RuleKey.of("bulk", "r_" + i)).setLanguage("xoo").getDefinition();
      db.ruleDao().insert(dbSession, ruleDefinitionDto);
      keys.add(ruleDefinitionDto.getKey());
    }
    dbSession.commit();
    ruleIndexer.indexRuleDefinitions(keys);

    // 0. No active rules so far (base case) and plenty rules available
    verifyZeroActiveRules(XOO_P1_KEY);
    assertThat(tester.get(RuleIndex.class)
      .search(new RuleQuery().setRepositories(asList("bulk")), new SearchOptions()).getTotal())
        .isEqualTo(bulkSize);

    // 1. bulk activate all the rules
    BulkChangeResult result = ruleActivator.bulkActivate(
      new RuleQuery().setRepositories(asList("bulk")), XOO_P1_KEY, "MINOR");

    // 2. assert that all activation has been commit to DB and ES
    dbSession.clearCache();
    assertThat(db.activeRuleDao().selectByProfileKey(dbSession, XOO_P1_KEY)).hasSize(bulkSize);
    assertThat(db.activeRuleDao().selectByProfileKey(dbSession, XOO_P1_KEY)).hasSize(bulkSize);
    assertThat(result.countSucceeded()).isEqualTo(bulkSize);
    assertThat(result.countFailed()).isEqualTo(0);
  }

  @Test
  public void bulk_activation_ignores_errors() {
    // 1. bulk activate all the rules, even non xoo-rules and xoo templates
    BulkChangeResult result = ruleActivator.bulkActivate(new RuleQuery(), XOO_P1_KEY, "MINOR");

    // 2. assert that all activations have been commit to DB and ES
    // -> xoo rules x1, x2 and custom1
    dbSession.clearCache();
    assertThat(db.activeRuleDao().selectByProfileKey(dbSession, XOO_P1_KEY)).hasSize(3);
    assertThat(result.countSucceeded()).isEqualTo(3);
    assertThat(result.countFailed()).isGreaterThan(0);
  }

  @Test
  public void set_and_unset_parent_profile() {
    // x1 is activated on the "future parent" P1
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity("MAJOR");
    activate(activation, XOO_P1_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, MAJOR, null, ImmutableMap.of("max", "10"));

    // create profile P2 with x2
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP2("org-123"));
    activation = new RuleActivation(XOO_X2);
    activation.setSeverity("MAJOR");
    activate(activation, XOO_P2_KEY);

    // set parent -> child profile inherits rule x1 and still has x2
    ruleActivator.setParent(dbSession, XOO_P2_KEY, XOO_P1_KEY);
    dbSession.clearCache();
    assertThat(db.qualityProfileDao().selectByKey(dbSession, XOO_P2_KEY).getParentKee()).isEqualTo(XOO_P1_KEY);

    verifyHasActiveRuleInDbAndIndex(ActiveRuleKey.of(XOO_P2_KEY, XOO_X1), MAJOR, INHERITED, ImmutableMap.of("max", "10"));
    verifyHasActiveRuleInDbAndIndex(ActiveRuleKey.of(XOO_P2_KEY, XOO_X2), MAJOR, null, Collections.emptyMap());

    // unset parent
    dbSession.clearCache();
    ruleActivator.setParent(dbSession, XOO_P2_KEY, null);
    assertThat(countActiveRules(XOO_P2_KEY)).isEqualTo(1);
    assertThat(db.qualityProfileDao().selectByKey(dbSession, XOO_P2_KEY).getParentKee()).isNull();
    verifyHasActiveRuleInDbAndIndex(ActiveRuleKey.of(XOO_P2_KEY, XOO_X2), MAJOR, null, Collections.emptyMap());
  }

  @Test
  public void unset_no_parent_does_not_fail() {
    // P1 has no parent !
    ruleActivator.setParent(dbSession, XOO_P1_KEY, null);
    assertThat(db.qualityProfileDao().selectByKey(dbSession, XOO_P1_KEY).getParentKee()).isNull();
  }

  @Test
  public void fail_if_set_child_as_parent() {
    createChildProfiles();

    try {
      ruleActivator.setParent(dbSession, XOO_P1_KEY, XOO_P3_KEY);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Descendant profile 'XOO_P3' can not be selected as parent of 'XOO_P1'");
    }
  }

  @Test
  public void keep_overridden_rules_when_unsetting_parent() {
    // x1 is activated on the "future parent"
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity("MAJOR");
    activate(activation, XOO_P1_KEY);
    verifyOneActiveRuleInDb(XOO_P1_KEY, XOO_X1, MAJOR, null, ImmutableMap.of("max", "10"));

    // create empty profile P2
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP2("org-123"));
    dbSession.commit();
    dbSession.clearCache();

    // set parent -> child profile inherits rule x1
    ruleActivator.setParent(dbSession, XOO_P2_KEY, XOO_P1_KEY);
    verifyOneActiveRuleInDbAndIndex(XOO_P2_KEY, XOO_X1, MAJOR, INHERITED, ImmutableMap.of("max", "10"));

    // override x1
    activation = new RuleActivation(XOO_X1);
    activation.setSeverity("BLOCKER").setParameter("max", "333");
    activate(activation, XOO_P2_KEY);
    verifyOneActiveRuleInDb(XOO_P2_KEY, XOO_X1, BLOCKER, OVERRIDES, ImmutableMap.of("max", "333"));

    // unset parent -> keep x1
    ruleActivator.setParent(dbSession, XOO_P2_KEY, null);
    dbSession.clearCache();
    assertThat(db.qualityProfileDao().selectByKey(dbSession, XOO_P2_KEY).getParentKee()).isNull();
    verifyOneActiveRuleInDbAndIndex(XOO_P2_KEY, XOO_X1, BLOCKER, null, ImmutableMap.of("max", "333"));
  }

  @Test
  public void ignore_activation_errors_when_setting_parent() {
    // x1 and x2 are activated on the "future parent" P1
    RuleActivation activation = new RuleActivation(XOO_X1).setSeverity("MAJOR");
    activate(activation, XOO_P1_KEY);
    activation = new RuleActivation(XOO_X2).setSeverity("MAJOR");
    activate(activation, XOO_P1_KEY);

    // create profile P2
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP2("org-123"));

    // mark rule x1 as REMOVED
    RuleDefinitionDto rule = db.ruleDao().selectOrFailDefinitionByKey(dbSession, XOO_X1);
    rule.setStatus(RuleStatus.REMOVED);
    db.ruleDao().update(dbSession, rule);
    dbSession.commit();
    dbSession.clearCache();

    // set parent -> child profile inherits x2 but not x1
    ruleActivator.setParent(dbSession, XOO_P2_KEY, XOO_P1_KEY);
    dbSession.clearCache();

    assertThat(db.qualityProfileDao().selectByKey(dbSession, XOO_P2_KEY).getParentKee()).isEqualTo(XOO_P1_KEY);
    assertThat(countActiveRules(XOO_P2_KEY)).isEqualTo(1);
    verifyHasActiveRuleInDbAndIndex(ActiveRuleKey.of(XOO_P2_KEY, XOO_X2), MAJOR, INHERITED, Collections.emptyMap());
  }

  @Test
  public void bulk_deactivate() {
    activate(new RuleActivation(XOO_X1), XOO_P1_KEY);
    activate(new RuleActivation(XOO_X2), XOO_P1_KEY);
    assertThat(countActiveRules(XOO_P1_KEY)).isEqualTo(2);

    BulkChangeResult result = ruleActivator.bulkDeactivate(new RuleQuery().setActivation(true).setQProfileKey(XOO_P1_KEY), XOO_P1_KEY);

    dbSession.clearCache();
    assertThat(countActiveRules(XOO_P1_KEY)).isEqualTo(0);
    assertThat(result.countFailed()).isEqualTo(0);
    assertThat(result.countSucceeded()).isEqualTo(2);
    assertThat(result.getChanges()).hasSize(2);
  }

  @Test
  public void bulk_deactivation_ignores_errors() {
    // activate on parent profile P1
    createChildProfiles();
    activate(new RuleActivation(XOO_X1), XOO_P1_KEY);
    assertThat(countActiveRules(XOO_P2_KEY)).isEqualTo(1);

    // bulk deactivate on child profile P2 -> not possible
    BulkChangeResult result = ruleActivator.bulkDeactivate(new RuleQuery().setActivation(true).setQProfileKey(XOO_P2_KEY), XOO_P2_KEY);

    dbSession.clearCache();
    assertThat(countActiveRules(XOO_P2_KEY)).isEqualTo(1);
    assertThat(result.countFailed()).isEqualTo(1);
    assertThat(result.countSucceeded()).isEqualTo(0);
    assertThat(result.getChanges()).hasSize(0);
  }

  @Test
  public void bulk_change_severity() {
    createChildProfiles();

    // activate two rules on root profile P1 (propagated to P2 and P3)
    RuleActivation activation = new RuleActivation(XOO_X1).setSeverity(INFO).setParameter("max", "7");
    activate(activation, XOO_P1_KEY);
    activation = new RuleActivation(XOO_X2).setSeverity(INFO);
    activate(activation, XOO_P1_KEY);

    // bulk change severity to BLOCKER. Parameters are not set.
    RuleQuery query = new RuleQuery().setActivation(true).setQProfileKey(XOO_P1_KEY);
    BulkChangeResult result = ruleActivator.bulkActivate(query, XOO_P1_KEY, "BLOCKER");
    assertThat(result.countSucceeded()).isEqualTo(2);

    verifyHasActiveRuleInDbAndIndex(ActiveRuleKey.of(XOO_P1_KEY, XOO_X1), BLOCKER, null, ImmutableMap.of("max", "7"));
    verifyHasActiveRuleInDbAndIndex(ActiveRuleKey.of(XOO_P1_KEY, XOO_X2), BLOCKER, null, Collections.<String, String>emptyMap());
    verifyHasActiveRuleInDbAndIndex(ActiveRuleKey.of(XOO_P2_KEY, XOO_X1), BLOCKER, INHERITED, ImmutableMap.of("max", "7"));
    verifyHasActiveRuleInDbAndIndex(ActiveRuleKey.of(XOO_P2_KEY, XOO_X2), BLOCKER, INHERITED, Collections.<String, String>emptyMap());
    verifyHasActiveRuleInDbAndIndex(ActiveRuleKey.of(XOO_P3_KEY, XOO_X1), BLOCKER, INHERITED, ImmutableMap.of("max", "7"));
    verifyHasActiveRuleInDbAndIndex(ActiveRuleKey.of(XOO_P3_KEY, XOO_X2), BLOCKER, INHERITED, Collections.<String, String>emptyMap());
  }

  private int countActiveRules(String profileKey) {
    List<ActiveRuleDto> activeRuleDtos = db.activeRuleDao().selectByProfileKey(dbSession, profileKey);
    return activeRuleDtos.size();
  }

  private void verifyOneActiveRuleInDb(String profileKey, RuleKey ruleKey, String expectedSeverity,
    @Nullable String expectedInheritance, Map<String, String> expectedParams) {
    assertThat(countActiveRules(profileKey)).isEqualTo(1);
    verifyHasActiveRuleInDb(ActiveRuleKey.of(profileKey, ruleKey), expectedSeverity, expectedInheritance, expectedParams);
  }

  private void verifyOneActiveRuleInDbAndIndex(String profileKey, RuleKey ruleKey, String expectedSeverity,
    @Nullable String expectedInheritance, Map<String, String> expectedParams) {
    assertThat(countActiveRules(profileKey)).isEqualTo(1);
    verifyHasActiveRuleInDbAndIndex(ActiveRuleKey.of(profileKey, ruleKey), expectedSeverity, expectedInheritance, expectedParams);
  }

  private void verifyHasActiveRuleInDb(ActiveRuleKey activeRuleKey, String expectedSeverity,
    @Nullable String expectedInheritance, Map<String, String> expectedParams) {
    // verify db
    boolean found = false;
    List<ActiveRuleDto> activeRuleDtos = db.activeRuleDao().selectByProfileKey(dbSession, activeRuleKey.qProfile());
    for (ActiveRuleDto activeRuleDto : activeRuleDtos) {
      if (activeRuleDto.getKey().equals(activeRuleKey)) {
        found = true;
        assertThat(activeRuleDto.getSeverityString()).isEqualTo(expectedSeverity);
        assertThat(activeRuleDto.getInheritance()).isEqualTo(expectedInheritance);
        // Dates should be set
        assertThat(activeRuleDto.getCreatedAt()).isNotNull();
        assertThat(activeRuleDto.getUpdatedAt()).isNotNull();

        List<ActiveRuleParamDto> paramDtos = db.activeRuleDao().selectParamsByActiveRuleId(dbSession, activeRuleDto.getId());
        assertThat(paramDtos).hasSize(expectedParams.size());
        for (Map.Entry<String, String> entry : expectedParams.entrySet()) {
          ActiveRuleParamDto paramDto = db.activeRuleDao().selectParamByKeyAndName(activeRuleDto.getKey(), entry.getKey(), dbSession);
          assertThat(paramDto).isNotNull();
          assertThat(paramDto.getValue()).isEqualTo(entry.getValue());
        }
      }
    }
    assertThat(found).as("Rule is not activated in db").isTrue();
  }

  private void verifyHasActiveRuleInIndex(ActiveRuleKey activeRuleKey, String expectedSeverity, @Nullable String expectedInheritance) {
    // verify es
    List<RuleKey> ruleKeys = newArrayList(tester.get(RuleIndex.class).searchAll(
      new RuleQuery()
        .setKey(activeRuleKey.ruleKey().toString())
        .setQProfileKey(activeRuleKey.qProfile())
        .setActivation(true)
        .setInheritance(singleton(expectedInheritance == null ? ActiveRule.Inheritance.NONE.name() : ActiveRule.Inheritance.valueOf(expectedInheritance).name()))
        .setActiveSeverities(singleton(expectedSeverity))));
    assertThat(ruleKeys).as("Rule is not activated in index").hasSize(1);
  }

  private void verifyHasActiveRuleInDbAndIndex(ActiveRuleKey activeRuleKey, String expectedSeverity,
    @Nullable String expectedInheritance, Map<String, String> expectedParams) {
    verifyHasActiveRuleInDb(activeRuleKey, expectedSeverity, expectedInheritance, expectedParams);
    verifyHasActiveRuleInIndex(activeRuleKey, expectedSeverity, expectedInheritance);
  }

  private void createChildProfiles() {
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP2("org-123").setParentKee(XOO_P1_KEY));
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP3("org-123").setParentKee(XOO_P2_KEY));
    dbSession.commit();
  }

  private List<ActiveRuleChange> activate(RuleActivation activation, String profileKey) {
    List<ActiveRuleChange> changes = ruleActivator.activate(dbSession, activation, profileKey);
    dbSession.commit();
    dbSession.clearCache();
    activeRuleIndexer.index(changes);
    return changes;
  }

  private void verifyZeroActiveRules(String key) {
    // verify db
    dbSession.clearCache();
    List<ActiveRuleDto> activeRuleDtos = db.activeRuleDao().selectByProfileKey(dbSession, key);
    assertThat(activeRuleDtos).isEmpty();

    // verify es
    assertThat(tester.get(RuleIndex.class).searchAll(
      new RuleQuery()
        .setQProfileKey(key)
        .setActivation(true))).isEmpty();
  }

  private void assertProfileHasBeenUpdatedManually(String profileKey) {
    QualityProfileDto profile = db.qualityProfileDao().selectByKey(dbSession, profileKey);
    assertThat(profile.getRulesUpdatedAt()).isNotEmpty();
    assertThat(profile.getUserUpdatedAt()).isNotNull();
  }

  private void assertProfileHasBeenUpdatedAutomatically(String profileKey) {
    QualityProfileDto profile = db.qualityProfileDao().selectByKey(dbSession, profileKey);
    assertThat(profile.getRulesUpdatedAt()).isNotEmpty();
    assertThat(profile.getUserUpdatedAt()).isNull();
  }
}
