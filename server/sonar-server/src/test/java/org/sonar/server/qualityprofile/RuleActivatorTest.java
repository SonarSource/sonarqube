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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.PropertyType;
import org.sonar.api.config.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.qualityprofile.index.ActiveRuleIteratorFactory;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.IntegerTypeValidation;
import org.sonar.server.util.StringTypeValidation;
import org.sonar.server.util.TypeValidations;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.MAJOR;

public class RuleActivatorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private System2 system2 = new AlwaysIncreasingSystem2();
  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public EsTester es = new EsTester(RuleIndexDefinition.createForTest(new MapSettings()));
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private RuleIndex ruleIndex = new RuleIndex(es.client());
  private RuleActivatorContextFactory contextFactory = new RuleActivatorContextFactory(db.getDbClient());
  private ActiveRuleIteratorFactory activeRuleIteratorFactory = new ActiveRuleIteratorFactory(db.getDbClient());
  private ActiveRuleIndexer activeRuleIndexer = new ActiveRuleIndexer(db.getDbClient(), es.client(), activeRuleIteratorFactory);
  private TypeValidations typeValidations = new TypeValidations(asList(new StringTypeValidation(), new IntegerTypeValidation()));

  private RuleActivator underTest = new RuleActivator(system2, db.getDbClient(), ruleIndex, contextFactory, typeValidations, activeRuleIndexer,
    userSession);

  @Test
  public void system_activates_rule_without_parameters() {
    RuleDefinitionDto rule = createRule();
    QProfileDto profile = createProfile(rule);
    RuleActivation activation = new RuleActivation(rule.getKey());
    activation.setSeverity(BLOCKER);
    List<ActiveRuleChange> changes = activate(profile, activation);

    assertThatRuleIsActivated(profile, rule, changes, BLOCKER, null, emptyMap());
    assertThatProfileIsUpdatedBySystem(profile);
  }

  @Test
  public void user_activates_rule_without_parameters() {
    userSession.logIn();
    RuleDefinitionDto rule = createRule();
    QProfileDto profile = createProfile(rule);
    RuleActivation activation = new RuleActivation(rule.getKey());
    activation.setSeverity(BLOCKER);
    List<ActiveRuleChange> changes = activate(profile, activation);

    assertThatRuleIsActivated(profile, rule, changes, BLOCKER, null, emptyMap());
    assertThatProfileIsUpdatedByUser(profile);
  }

  @Test
  public void activate_rule_with_default_severity_and_parameters() {
    RuleDefinitionDto rule = createRule();
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    RuleActivation activation = new RuleActivation(rule.getKey());
    List<ActiveRuleChange> changes = activate(profile, activation);

    assertThatRuleIsActivated(profile, rule, changes, rule.getSeverityString(), null, of("min", "10"));
    assertThatProfileIsUpdatedBySystem(profile);
  }

  @Test
  public void activate_rule_with_parameters() {
    RuleDefinitionDto rule = createRule();
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    RuleActivation activation = new RuleActivation(rule.getKey())
      .setParameter(ruleParam.getName(), "15");
    List<ActiveRuleChange> changes = activate(profile, activation);

    assertThatRuleIsActivated(profile, rule, changes, rule.getSeverityString(), null, of("min", "15"));
    assertThatProfileIsUpdatedBySystem(profile);
  }

  @Test
  public void activate_rule_with_default_severity() {
    RuleDefinitionDto rule = createRule();
    QProfileDto profile = createProfile(rule);

    RuleActivation activation = new RuleActivation(rule.getKey());
    List<ActiveRuleChange> changes = activate(profile, activation);

    assertThatRuleIsActivated(profile, rule, changes, rule.getSeverityString(), null, emptyMap());
    assertThatProfileIsUpdatedBySystem(profile);
  }

  /**
      * SONAR-5841
      */
  @Test
  public void activate_rule_with_empty_parameter_having_no_default_value() {
    RuleDefinitionDto rule = createRule();
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    RuleActivation activation = new RuleActivation(rule.getKey())
      .setParameter("min", "");
    List<ActiveRuleChange> changes = activate(profile, activation);

    assertThatRuleIsActivated(profile, rule, changes, rule.getSeverityString(), null, of("min", "10"));
    assertThatProfileIsUpdatedBySystem(profile);
  }

  /**
   //   * SONAR-5840
   //   */
  @Test
  public void activate_rule_with_negative_integer_value_on_parameter_having_no_default_value() {
    RuleDefinitionDto rule = createRule();
    RuleParamDto paramWithoutDefault = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue(null));
    RuleParamDto paramWithDefault = db.rules().insertRuleParam(rule, p -> p.setName("max").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    RuleActivation activation = new RuleActivation(rule.getKey())
      .setParameter(paramWithoutDefault.getName(), "-10");
    List<ActiveRuleChange> changes = activate(profile, activation);

    assertThatRuleIsActivated(profile, rule, changes, rule.getSeverityString(), null,
      of(paramWithoutDefault.getName(), "-10", paramWithDefault.getName(), paramWithDefault.getDefaultValue()));
    assertThatProfileIsUpdatedBySystem(profile);
  }

  @Test
  public void activation_ignores_unsupported_parameters() {
    RuleDefinitionDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule, p -> p.setName("max").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    RuleActivation activation = new RuleActivation(rule.getKey())
      .setParameter("xxx", "yyy");
    List<ActiveRuleChange> changes = activate(profile, activation);

    assertThatRuleIsActivated(profile, rule, changes, rule.getSeverityString(), null, of(param.getName(), param.getDefaultValue()));
    assertThatProfileIsUpdatedBySystem(profile);
  }

  @Test
  public void update_an_already_activated_rule() {
    RuleDefinitionDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule, p -> p.setName("max").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    // initial activation
    RuleActivation activation = new RuleActivation(rule.getKey())
      .setSeverity(MAJOR);
    activate(profile, activation);

    // update
    RuleActivation updateActivation = new RuleActivation(rule.getKey())
      .setSeverity(CRITICAL)
      .setParameter(param.getName(), "20");
    List<ActiveRuleChange> changes = activate(profile, updateActivation);

    assertThatRuleIsUpdated(profile, rule, CRITICAL, null, of(param.getName(), "20"));
    assertThatProfileIsUpdatedBySystem(profile);
  }

  @Test
  public void update_activation_with_parameter_without_default_value() {
    RuleDefinitionDto rule = createRule();
    RuleParamDto paramWithoutDefault = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue(null));
    RuleParamDto paramWithDefault = db.rules().insertRuleParam(rule, p -> p.setName("max").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    // initial activation -> param "max" has a default value
    RuleActivation activation = new RuleActivation(rule.getKey());
    activate(profile, activation);

    // update param "min", which has no default value
    RuleActivation updateActivation = new RuleActivation(rule.getKey())
      .setSeverity(MAJOR)
      .setParameter(paramWithoutDefault.getName(), "3");
    List<ActiveRuleChange> changes = activate(profile, updateActivation);

    assertThatRuleIsUpdated(profile, rule, MAJOR, null, of(paramWithDefault.getName(), "10", paramWithoutDefault.getName(), "3"));
    assertThatProfileIsUpdatedBySystem(profile);
  }

  @Test
  public void reset_parameter_to_default_value() {
    RuleDefinitionDto rule = createRule();
    RuleParamDto paramWithDefault = db.rules().insertRuleParam(rule, p -> p.setName("max").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    // initial activation -> param "max" has a default value
    RuleActivation activation = new RuleActivation(rule.getKey())
      .setParameter(paramWithDefault.getName(), "20");
    activate(profile, activation);

    // reset to default_value
    RuleActivation updateActivation = new RuleActivation(rule.getKey())
      .setParameter(paramWithDefault.getName(), null);
    List<ActiveRuleChange> changes = activate(profile, updateActivation);

    assertThatRuleIsUpdated(profile, rule, rule.getSeverityString(), null, of(paramWithDefault.getName(), "10"));
    assertThat(changes).hasSize(1);
  }

  @Test
  public void update_activation_removes_parameter_without_default_value() {
    RuleDefinitionDto rule = createRule();
    RuleParamDto paramWithoutDefault = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue(null));
    RuleParamDto paramWithDefault = db.rules().insertRuleParam(rule, p -> p.setName("max").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    // initial activation -> param "max" has a default value
    RuleActivation activation = new RuleActivation(rule.getKey())
      .setParameter(paramWithoutDefault.getName(), "20");
    activate(profile, activation);

    // remove parameter
    RuleActivation updateActivation = new RuleActivation(rule.getKey())
      .setParameter(paramWithoutDefault.getName(), null);
    List<ActiveRuleChange> changes = activate(profile, updateActivation);

    assertThatRuleIsUpdated(profile, rule, rule.getSeverityString(), null, of(paramWithDefault.getName(), paramWithDefault.getDefaultValue()));
    assertThat(changes).hasSize(1);
  }

  @Test
  public void update_activation_with_new_parameter() {
    RuleDefinitionDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule, p -> p.setName("max").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    // initial activation -> param "max" has a default value
    RuleActivation activation = new RuleActivation(rule.getKey());
    List<ActiveRuleChange> changes = activate(profile, activation);
    db.getDbClient().activeRuleDao().deleteParametersByRuleProfileUuids(db.getSession(), asList(profile.getRulesProfileUuid()));
    assertThatRuleIsActivated(profile, rule, changes, rule.getSeverityString(), null, emptyMap());

    // contrary to activerule, the param is supposed to be inserted but not updated
    RuleActivation updateActivation = new RuleActivation(rule.getKey())
      .setParameter(param.getName(), null);
    changes = activate(profile, updateActivation);

    assertThatRuleIsUpdated(profile, rule, rule.getSeverityString(), null, of(param.getName(), param.getDefaultValue()));
    assertThat(changes).hasSize(1);
  }

  @Test
  public void ignore_activation_without_changes() {
    RuleDefinitionDto rule = createRule();
    QProfileDto profile = createProfile(rule);

    // initial activation
    RuleActivation activation = new RuleActivation(rule.getKey());
    activate(profile, activation);

    // update with exactly the same severity and params
    activation = new RuleActivation(rule.getKey());
    List<ActiveRuleChange> changes = activate(profile, activation);

    assertThat(changes).isEmpty();
  }

  @Test
  public void do_not_change_severity_and_params_if_unset_and_already_activated() {
    RuleDefinitionDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule, p -> p.setName("max").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    // initial activation -> param "max" has a default value
    RuleActivation activation = new RuleActivation(rule.getKey())
      .setSeverity(BLOCKER)
      .setParameter(param.getName(), "20");
    activate(profile, activation);

    // update without any severity or params => keep
    RuleActivation update = new RuleActivation(rule.getKey());
    List<ActiveRuleChange> changes = activate(profile, update);

    assertThat(changes).isEmpty();
  }

  @Test
  public void activation_fails_if_rule_does_not_exist() {
    RuleDefinitionDto rule = createRule();
    QProfileDto profile = createProfile(rule);
    RuleKey ruleKey = RuleKey.parse("unknown:xxx");
    RuleActivation activation = new RuleActivation(ruleKey);

    expectFailure("Rule not found: " + ruleKey, () -> activate(profile, activation));
  }

  @Test
  public void fail_to_activate_rule_if_profile_is_on_different_languages() {
    RuleDefinitionDto rule = createJavaRule();
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage("js"));
    RuleActivation activation = new RuleActivation(rule.getKey());

    expectFailure("Rule " + rule.getKey() + " and profile " + profile.getKee() + " have different languages", () -> activate(profile, activation));
  }

  @Test
  public void fail_to_activate_rule_if_rule_has_REMOVED_status() {
    RuleDefinitionDto rule = db.rules().insert(r -> r.setStatus(RuleStatus.REMOVED));
    QProfileDto profile = createProfile(rule);
    RuleActivation activation = new RuleActivation(rule.getKey());

    expectFailure("Rule was removed: " + rule.getKey(), () -> activate(profile, activation));
  }

  @Test
  public void fail_to_activate_if_template() {
    RuleDefinitionDto rule = db.rules().insert(r -> r.setIsTemplate(true));
    QProfileDto profile = createProfile(rule);
    RuleActivation activation = new RuleActivation(rule.getKey());

    expectFailure("Rule template can't be activated on a Quality profile: " + rule.getKey(), () -> activate(profile, activation));
  }

  @Test
  public void fail_to_activate_if_invalid_parameter() {
    RuleDefinitionDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule, p -> p.setName("max").setDefaultValue("10").setType(PropertyType.INTEGER.name()));
    QProfileDto profile = createProfile(rule);

    RuleActivation activation = new RuleActivation(rule.getKey())
      .setParameter(param.getName(), "foo");
    expectFailure("Value 'foo' must be an integer.", () -> activate(profile, activation));
  }

  @Test
  public void user_deactivates_a_rule() {
    userSession.logIn();
    RuleDefinitionDto rule = createRule();
    QProfileDto profile = createProfile(rule);
    RuleActivation activation = new RuleActivation(rule.getKey());
    activate(profile, activation);

    List<ActiveRuleChange> changes = deactivate(profile, rule);
    verifyNoActiveRules();
    assertThatProfileIsUpdatedByUser(profile);
    assertThat(changes).hasSize(1);
    assertThat(changes.get(0).getType()).isEqualTo(ActiveRuleChange.Type.DEACTIVATED);
  }

  @Test
  public void system_deactivates_a_rule() {
    RuleDefinitionDto rule = createRule();
    QProfileDto profile = createProfile(rule);
    RuleActivation activation = new RuleActivation(rule.getKey());
    activate(profile, activation);

    List<ActiveRuleChange> changes = deactivate(profile, rule);
    verifyNoActiveRules();
    assertThatProfileIsUpdatedBySystem(profile);
    assertThatChangeIsDeactivation(changes, rule);
  }

  private void assertThatChangeIsDeactivation(List<ActiveRuleChange> changes, RuleDefinitionDto rule) {
    assertThat(changes).hasSize(1);
    ActiveRuleChange change = changes.get(0);
    assertThat(change.getType()).isEqualTo(ActiveRuleChange.Type.DEACTIVATED);
    assertThat(change.getKey().getRuleKey()).isEqualTo(rule.getKey());
  }

  @Test
  public void ignore_deactivation_if_rule_is_not_activated() {
    RuleDefinitionDto rule = createRule();
    QProfileDto profile = createProfile(rule);

    List<ActiveRuleChange> changes = deactivate(profile, rule);
    verifyNoActiveRules();
    assertThat(changes).hasSize(0);
  }

  @Test
  public void deactivation_fails_if_rule_does_not_exist() {
    RuleDefinitionDto rule = createRule();
    QProfileDto profile = createProfile(rule);
    RuleKey ruleKey = RuleKey.parse("unknown:xxx");

    expectFailure("Rule not found: " + ruleKey, () -> underTest.deactivate(db.getSession(), profile, ruleKey));
  }

  @Test
  public void deactivate_rule_that_has_REMOVED_status() {
    RuleDefinitionDto rule = createRule();
    QProfileDto profile = createProfile(rule);
    RuleActivation activation = new RuleActivation(rule.getKey());
    activate(profile, activation);

    rule.setStatus(RuleStatus.REMOVED);
    db.getDbClient().ruleDao().update(db.getSession(), rule);

    List<ActiveRuleChange> changes = deactivate(profile, rule);
    verifyNoActiveRules();
    assertThatChangeIsDeactivation(changes, rule);
  }

  @Test
  public void activation_on_child_profile_is_propagated_to_descendants() {
    RuleDefinitionDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);
    QProfileDto grandChildProfile = createChildProfile(childProfile);

    List<ActiveRuleChange> changes = activate(childProfile, new RuleActivation(rule.getKey()));
    assertThatProfileHasNoActiveRules(parentProfile);
    assertThatRuleIsActivated(childProfile, rule, changes, rule.getSeverityString(), null, emptyMap());
    assertThatRuleIsActivated(grandChildProfile, rule, changes, rule.getSeverityString(), ActiveRule.Inheritance.INHERITED, emptyMap());
  }

  @Test
  public void update_on_child_profile_is_propagated_to_descendants() {
    RuleDefinitionDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);
    QProfileDto grandChildProfile = createChildProfile(childProfile);

    RuleActivation initialActivation = new RuleActivation(rule.getKey())
      .setSeverity(MAJOR)
      .setParameter(param.getName(), "foo");
    activate(childProfile, initialActivation);

    RuleActivation updateActivation = new RuleActivation(rule.getKey())
      .setSeverity(CRITICAL)
      .setParameter(param.getName(), "bar");
    List<ActiveRuleChange> changes = activate(childProfile, updateActivation);

    assertThatProfileHasNoActiveRules(parentProfile);
    assertThatRuleIsUpdated(childProfile, rule, CRITICAL, null, of(param.getName(), "bar"));
    assertThatRuleIsUpdated(grandChildProfile, rule, CRITICAL, ActiveRule.Inheritance.INHERITED, of(param.getName(), "bar"));
    assertThat(changes).hasSize(2);
  }

  @Test
  public void override_activation_of_inherited_profile() {
    RuleDefinitionDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);
    QProfileDto grandChildProfile = createChildProfile(childProfile);

    RuleActivation initialActivation = new RuleActivation(rule.getKey())
      .setSeverity(MAJOR)
      .setParameter(param.getName(), "foo");
    activate(childProfile, initialActivation);

    RuleActivation overrideActivation = new RuleActivation(rule.getKey())
      .setSeverity(CRITICAL)
      .setParameter(param.getName(), "bar");
    List<ActiveRuleChange> changes = activate(grandChildProfile, overrideActivation);

    assertThatProfileHasNoActiveRules(parentProfile);
    assertThatRuleIsUpdated(childProfile, rule, MAJOR, null, of(param.getName(), "foo"));
    assertThatRuleIsUpdated(grandChildProfile, rule, CRITICAL, ActiveRule.Inheritance.OVERRIDES, of(param.getName(), "bar"));
    assertThat(changes).hasSize(1);
  }

  @Test
  public void updated_activation_on_parent_is_not_propagated_to_overridden_profiles() {
    RuleDefinitionDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);
    QProfileDto grandChildProfile = createChildProfile(childProfile);

    RuleActivation initialActivation = new RuleActivation(rule.getKey())
      .setSeverity(MAJOR)
      .setParameter(param.getName(), "foo");
    activate(childProfile, initialActivation);

    RuleActivation overrideActivation = new RuleActivation(rule.getKey())
      .setSeverity(CRITICAL)
      .setParameter(param.getName(), "bar");
    activate(grandChildProfile, overrideActivation);

    // update child --> do not touch grandChild
    RuleActivation updateActivation = new RuleActivation(rule.getKey())
      .setSeverity(BLOCKER)
      .setParameter(param.getName(), "baz");
    List<ActiveRuleChange> changes = activate(childProfile, updateActivation);

    assertThatProfileHasNoActiveRules(parentProfile);
    assertThatRuleIsUpdated(childProfile, rule, BLOCKER, null, of(param.getName(), "baz"));
    assertThatRuleIsUpdated(grandChildProfile, rule, CRITICAL, ActiveRule.Inheritance.OVERRIDES, of(param.getName(), "bar"));
    assertThat(changes).hasSize(1);
  }

  @Test
  public void reset_on_parent_is_not_propagated_to_overridden_profiles() {
    RuleDefinitionDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);
    QProfileDto grandChildProfile = createChildProfile(childProfile);

    RuleActivation initialActivation = new RuleActivation(rule.getKey())
      .setSeverity(MAJOR)
      .setParameter(param.getName(), "foo");
    activate(parentProfile, initialActivation);

    RuleActivation overrideActivation = new RuleActivation(rule.getKey())
      .setSeverity(CRITICAL)
      .setParameter(param.getName(), "bar");
    activate(grandChildProfile, overrideActivation);

    // reset parent --> touch child but not grandChild
    RuleActivation updateActivation = new RuleActivation(rule.getKey())
      .setReset(true);
    List<ActiveRuleChange> changes = activate(parentProfile, updateActivation);

    assertThatRuleIsUpdated(parentProfile, rule, rule.getSeverityString(), null, of(param.getName(), param.getDefaultValue()));
    assertThatRuleIsUpdated(childProfile, rule, rule.getSeverityString(), ActiveRule.Inheritance.INHERITED, of(param.getName(), param.getDefaultValue()));
    assertThatRuleIsUpdated(grandChildProfile, rule, CRITICAL, ActiveRule.Inheritance.OVERRIDES, of(param.getName(), "bar"));
    assertThat(changes).hasSize(2);
  }

  @Test
  public void active_on_parent_a_rule_already_activated_on_child() {
    RuleDefinitionDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    RuleActivation childActivation = new RuleActivation(rule.getKey())
      .setSeverity(MAJOR)
      .setParameter(param.getName(), "foo");
    activate(childProfile, childActivation);

    RuleActivation parentActivation = new RuleActivation(rule.getKey())
      .setSeverity(CRITICAL)
      .setParameter(param.getName(), "bar");
    List<ActiveRuleChange> changes = activate(parentProfile, parentActivation);

    assertThatRuleIsUpdated(parentProfile, rule, CRITICAL, null, of(param.getName(), "bar"));
    assertThatRuleIsUpdated(childProfile, rule, MAJOR, ActiveRule.Inheritance.OVERRIDES, of(param.getName(), "foo"));
    assertThat(changes).hasSize(2);
  }

  @Test
  public void do_not_mark_as_overridden_if_same_values_than_parent() {
    RuleDefinitionDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    RuleActivation parentActivation = new RuleActivation(rule.getKey())
      .setSeverity(MAJOR)
      .setParameter(param.getName(), "foo");
    activate(parentProfile, parentActivation);

    RuleActivation overrideActivation = new RuleActivation(rule.getKey())
      .setSeverity(MAJOR)
      .setParameter(param.getName(), "foo");
    List<ActiveRuleChange> changes = activate(childProfile, overrideActivation);

    assertThatRuleIsUpdated(childProfile, rule, MAJOR, ActiveRule.Inheritance.INHERITED, of(param.getName(), "foo"));
    assertThat(changes).hasSize(0);
  }

  @Test
  public void propagate_deactivation_on_children() {
    RuleDefinitionDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    RuleActivation activation = new RuleActivation(rule.getKey());
    List<ActiveRuleChange> changes = activate(parentProfile, activation);
    assertThatRuleIsActivated(parentProfile, rule, changes, rule.getSeverityString(), null, emptyMap());
    assertThatRuleIsActivated(childProfile, rule, changes, rule.getSeverityString(), ActiveRule.Inheritance.INHERITED, emptyMap());

    changes = deactivate(parentProfile, rule);
    assertThatProfileHasNoActiveRules(parentProfile);
    assertThatProfileHasNoActiveRules(childProfile);
    assertThat(changes).hasSize(2);
  }

  @Test
  public void propagate_deactivation_on_children_even_when_overridden() {
    RuleDefinitionDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    RuleActivation activation = new RuleActivation(rule.getKey());
    List<ActiveRuleChange> changes = activate(parentProfile, activation);
    assertThatRuleIsActivated(parentProfile, rule, changes, rule.getSeverityString(), null, emptyMap());
    assertThatRuleIsActivated(childProfile, rule, changes, rule.getSeverityString(), ActiveRule.Inheritance.INHERITED, emptyMap());

    activation = new RuleActivation(rule.getKey())
      .setSeverity(CRITICAL);
    activate(childProfile, activation);

    changes = deactivate(parentProfile, rule);
    assertThatProfileHasNoActiveRules(parentProfile);
    assertThatProfileHasNoActiveRules(childProfile);
    assertThat(changes).hasSize(2);
  }

  @Test
  public void cannot_deactivate_rule_inherited() {
    RuleDefinitionDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    RuleActivation activation = new RuleActivation(rule.getKey());
    List<ActiveRuleChange> changes = activate(parentProfile, activation);
    assertThatRuleIsActivated(parentProfile, rule, changes, rule.getSeverityString(), null, emptyMap());
    assertThatRuleIsActivated(childProfile, rule, changes, rule.getSeverityString(), ActiveRule.Inheritance.INHERITED, emptyMap());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Cannot deactivate inherited rule");
    deactivate(childProfile, rule);
  }

  @Test
  public void reset_child_profile_do_not_change_parent() {
    RuleDefinitionDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    RuleActivation activation = new RuleActivation(rule.getKey())
      .setSeverity(CRITICAL);
    List<ActiveRuleChange> changes = activate(parentProfile, activation);
    assertThatRuleIsActivated(parentProfile, rule, changes, CRITICAL, null, emptyMap());
    assertThatRuleIsActivated(childProfile, rule, changes, CRITICAL, ActiveRule.Inheritance.INHERITED, emptyMap());
    assertThat(changes).hasSize(2);

    RuleActivation childActivation = new RuleActivation(rule.getKey())
      .setSeverity(BLOCKER);
    changes = activate(childProfile, childActivation);
    assertThatRuleIsUpdated(childProfile, rule, BLOCKER, ActiveRule.Inheritance.OVERRIDES, emptyMap());
    assertThat(changes).hasSize(1);

    RuleActivation resetActivation = new RuleActivation(rule.getKey()).setReset(true);
    changes = activate(childProfile, resetActivation);
    assertThatRuleIsUpdated(childProfile, rule, CRITICAL, ActiveRule.Inheritance.INHERITED, emptyMap());
    assertThatRuleIsUpdated(parentProfile, rule, CRITICAL, null, emptyMap());
    assertThat(changes).hasSize(1);
  }

  @Test
  public void reset_parent_is_not_propagated_when_child_overrides() {
    RuleDefinitionDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);
    QProfileDto grandchildProfile = createChildProfile(childProfile);

    RuleActivation activation = new RuleActivation(rule.getKey())
      .setSeverity(CRITICAL);
    List<ActiveRuleChange> changes = activate(parentProfile, activation);
    assertThatRuleIsActivated(parentProfile, rule, changes, CRITICAL, null, emptyMap());
    assertThatRuleIsActivated(childProfile, rule, changes, CRITICAL, ActiveRule.Inheritance.INHERITED, emptyMap());
    assertThatRuleIsActivated(grandchildProfile, rule, changes, CRITICAL, ActiveRule.Inheritance.INHERITED, emptyMap());
    assertThat(changes).hasSize(3);

    RuleActivation childActivation = new RuleActivation(rule.getKey())
      .setSeverity(BLOCKER);
    changes = activate(childProfile, childActivation);
    assertThatRuleIsUpdated(childProfile, rule, BLOCKER, ActiveRule.Inheritance.OVERRIDES, emptyMap());
    assertThatRuleIsUpdated(grandchildProfile, rule, BLOCKER, ActiveRule.Inheritance.INHERITED, emptyMap());
    assertThat(changes).hasSize(2);

    // Reset on parent do not change child nor grandchild
    RuleActivation resetActivation = new RuleActivation(rule.getKey()).setReset(true);
    changes = activate(parentProfile, resetActivation);
    assertThatRuleIsUpdated(parentProfile, rule, rule.getSeverityString(), null, emptyMap());
    assertThatRuleIsUpdated(childProfile, rule, BLOCKER, ActiveRule.Inheritance.OVERRIDES, emptyMap());
    assertThatRuleIsUpdated(grandchildProfile, rule, BLOCKER, ActiveRule.Inheritance.INHERITED, emptyMap());
    assertThat(changes).hasSize(1);

    // Reset on child change grandchild
    resetActivation = new RuleActivation(rule.getKey()).setReset(true);
    changes = activate(childProfile, resetActivation);
    assertThatRuleIsUpdated(parentProfile, rule, rule.getSeverityString(), null, emptyMap());
    assertThatRuleIsUpdated(childProfile, rule, rule.getSeverityString(), ActiveRule.Inheritance.INHERITED, emptyMap());
    assertThatRuleIsUpdated(grandchildProfile, rule, rule.getSeverityString(), ActiveRule.Inheritance.INHERITED, emptyMap());
    assertThat(changes).hasSize(2);
  }

  @Test
  public void ignore_reset_if_not_activated()  {
    RuleDefinitionDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    RuleActivation resetActivation = new RuleActivation(rule.getKey()).setReset(true);
    List<ActiveRuleChange> changes = activate(parentProfile, resetActivation);
    verifyNoActiveRules();
    assertThat(changes).hasSize(0);
  }

  @Test
  public void unset_parent_when_no_parent_does_not_fail()  {
    RuleDefinitionDto rule = createRule();
    QProfileDto profile = createProfile(rule);
    underTest.setParent(db.getSession(), profile, null);
  }

  @Test
  public void set_itself_as_parent_fails() {
    RuleDefinitionDto rule = createRule();
    QProfileDto profile = createProfile(rule);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(" can not be selected as parent of ");
    underTest.setParent(db.getSession(), profile, profile);
  }

  @Test
  public void set_child_as_parent_fails() {
    RuleDefinitionDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(" can not be selected as parent of ");
    underTest.setParent(db.getSession(), parentProfile, childProfile);
  }

  @Test
  public void set_grandchild_as_parent_fails() {
    RuleDefinitionDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);
    QProfileDto grandchildProfile = createChildProfile(childProfile);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(" can not be selected as parent of ");
    underTest.setParent(db.getSession(), parentProfile, grandchildProfile);
  }

  @Test
  public void cannot_set_parent_if_language_is_different()  {
    RuleDefinitionDto rule1 = db.rules().insert(r -> r.setLanguage("foo"));
    RuleDefinitionDto rule2 = db.rules().insert(r -> r.setLanguage("bar"));

    QProfileDto parentProfile = createProfile(rule1);
    List<ActiveRuleChange> changes = activate(parentProfile, new RuleActivation(rule1.getKey()));
    assertThat(changes).hasSize(1);

    QProfileDto childProfile = createProfile(rule2);
    changes = activate(childProfile, new RuleActivation(rule2.getKey()));
    assertThat(changes).hasSize(1);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Cannot set the profile");

    underTest.setParent(db.getSession(), childProfile, parentProfile);
  }

  @Test
  public void set_then_unset_parent()  {
    RuleDefinitionDto rule1 = createJavaRule();
    RuleDefinitionDto rule2 = createJavaRule();

    QProfileDto profile1 = createProfile(rule1);
    List<ActiveRuleChange> changes = activate(profile1, new RuleActivation(rule1.getKey()));
    assertThat(changes).hasSize(1);

    QProfileDto profile2 = createProfile(rule2);
    changes = activate(profile2, new RuleActivation(rule2.getKey()));
    assertThat(changes).hasSize(1);

    changes = underTest.setParent(db.getSession(), profile2, profile1);
    assertThat(changes).hasSize(1);
    assertThatRuleIsActivated(profile2, rule1, changes, rule1.getSeverityString(), ActiveRule.Inheritance.INHERITED, emptyMap());
    assertThatRuleIsActivated(profile2, rule2, null, rule2.getSeverityString(), null, emptyMap());

    changes = underTest.setParent(db.getSession(), profile2, null);
    assertThat(changes).hasSize(1);
    assertThatRuleIsActivated(profile2, rule2, null, rule2.getSeverityString(), null, emptyMap());
    assertThatRuleIsNotPresent(profile2, rule1);
  }

  @Test
  public void set_then_unset_parent_keep_overridden_rules()  {
    RuleDefinitionDto rule1 = createJavaRule();
    RuleDefinitionDto rule2 = createJavaRule();
    QProfileDto profile1 = createProfile(rule1);
    List<ActiveRuleChange> changes = activate(profile1, new RuleActivation(rule1.getKey()));
    assertThat(changes).hasSize(1);

    QProfileDto profile2 = createProfile(rule2);
    changes = activate(profile2, new RuleActivation(rule2.getKey()));
    assertThat(changes).hasSize(1);

    changes = underTest.setParent(db.getSession(), profile2, profile1);
    assertThat(changes).hasSize(1);
    assertThatRuleIsActivated(profile2, rule1, changes, rule1.getSeverityString(), ActiveRule.Inheritance.INHERITED, emptyMap());
    assertThatRuleIsActivated(profile2, rule2, null, rule2.getSeverityString(), null, emptyMap());

    RuleActivation activation = new RuleActivation(rule1.getKey())
      .setSeverity(BLOCKER);
    changes = activate(profile2, activation);
    assertThat(changes).hasSize(1);
    assertThatRuleIsUpdated(profile2, rule1, BLOCKER, ActiveRule.Inheritance.OVERRIDES, emptyMap());
    assertThatRuleIsActivated(profile2, rule2, null, rule2.getSeverityString(), null, emptyMap());

    changes = underTest.setParent(db.getSession(), profile2, null);
    assertThat(changes).hasSize(1);
    // Not testing changes here since severity is not set in changelog
    assertThatRuleIsActivated(profile2, rule1, null, BLOCKER, null, emptyMap());
    assertThatRuleIsActivated(profile2, rule2, null, rule2.getSeverityString(), null, emptyMap());
  }

  private void assertThatProfileHasNoActiveRules(QProfileDto profile) {
    List<OrgActiveRuleDto> activeRules = db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), profile);
    assertThat(activeRules).isEmpty();
  }

  private List<ActiveRuleChange> deactivate(QProfileDto profile, RuleDefinitionDto rule) {
    return underTest.deactivate(db.getSession(), profile, rule.getKey());
  }

  private List<ActiveRuleChange> activate(QProfileDto profile, RuleActivation activation) {
    return underTest.activate(db.getSession(), activation, profile);
  }

  private QProfileDto createProfile(RuleDefinitionDto rule) {
    return db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(rule.getLanguage()));
  }

  private QProfileDto createChildProfile(QProfileDto parent) {
    return db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(parent.getLanguage()).setParentKee(parent.getKee()));
  }

  private void assertThatProfileIsUpdatedByUser(QProfileDto profile) {
    QProfileDto loaded = db.getDbClient().qualityProfileDao().selectByUuid(db.getSession(), profile.getKee());
    assertThat(loaded.getUserUpdatedAt()).isNotNull();
    assertThat(loaded.getRulesUpdatedAt()).isNotEmpty();
  }

  private void assertThatProfileIsUpdatedBySystem(QProfileDto profile) {
    QProfileDto loaded = db.getDbClient().qualityProfileDao().selectByUuid(db.getSession(), profile.getKee());
    assertThat(loaded.getUserUpdatedAt()).isNull();
    assertThat(loaded.getRulesUpdatedAt()).isNotEmpty();
  }

  private void assertThatRuleIsActivated(QProfileDto profile, RuleDefinitionDto rule, @Nullable List<ActiveRuleChange> changes,
    String expectedSeverity, @Nullable ActiveRule.Inheritance expectedInheritance, Map<String, String> expectedParams) {
    OrgActiveRuleDto activeRule = db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), profile)
      .stream()
      .filter(ar -> ar.getRuleKey().equals(rule.getKey()))
      .findFirst()
      .orElseThrow(IllegalStateException::new);

    assertThat(activeRule.getSeverityString()).isEqualTo(expectedSeverity);
    assertThat(activeRule.getInheritance()).isEqualTo(expectedInheritance != null ? expectedInheritance.name() : null);
    assertThat(activeRule.getCreatedAt()).isNotNull();
    assertThat(activeRule.getUpdatedAt()).isNotNull();

    List<ActiveRuleParamDto> params = db.getDbClient().activeRuleDao().selectParamsByActiveRuleId(db.getSession(), activeRule.getId());
    assertThat(params).hasSize(expectedParams.size());

    if (changes != null) {
      ActiveRuleChange change = changes.stream()
        .filter(c -> c.getActiveRule().getId().equals(activeRule.getId()))
        .findFirst().orElseThrow(IllegalStateException::new);
      assertThat(change.getInheritance()).isEqualTo(expectedInheritance);
      assertThat(change.getSeverity()).isEqualTo(expectedSeverity);
      assertThat(change.getType()).isEqualTo(ActiveRuleChange.Type.ACTIVATED);
    }
  }

  private void assertThatRuleIsNotPresent(QProfileDto profile, RuleDefinitionDto rule) {
    Optional<OrgActiveRuleDto> activeRule = db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), profile)
      .stream()
      .filter(ar -> ar.getRuleKey().equals(rule.getKey()))
      .findFirst();

    assertThat(activeRule).isEmpty();
  }

  private void assertThatRuleIsUpdated(QProfileDto profile, RuleDefinitionDto rule,
    String expectedSeverity, @Nullable ActiveRule.Inheritance expectedInheritance, Map<String, String> expectedParams) {
    OrgActiveRuleDto activeRule = db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), profile)
      .stream()
      .filter(ar -> ar.getRuleKey().equals(rule.getKey()))
      .findFirst()
      .orElseThrow(IllegalStateException::new);

    assertThat(activeRule.getSeverityString()).isEqualTo(expectedSeverity);
    assertThat(activeRule.getInheritance()).isEqualTo(expectedInheritance != null ? expectedInheritance.name() : null);
    assertThat(activeRule.getCreatedAt()).isNotNull();
    assertThat(activeRule.getUpdatedAt()).isNotNull();

    List<ActiveRuleParamDto> params = db.getDbClient().activeRuleDao().selectParamsByActiveRuleId(db.getSession(), activeRule.getId());
    assertThat(params).hasSize(expectedParams.size());
  }

  private void expectFailure(String expectedMessage, Runnable runnable) {
    try {
      runnable.run();
      fail();
    } catch (BadRequestException e) {
      assertThat(e.getMessage()).isEqualTo(expectedMessage);
    }
    verifyNoActiveRules();
  }

  private void verifyNoActiveRules() {
    assertThat(db.countRowsOfTable(db.getSession(), "active_rules")).isEqualTo(0);
  }

  private RuleDefinitionDto createRule() {
    return db.rules().insert(r -> r.setSeverity(Severity.MAJOR));
  }

  private RuleDefinitionDto createJavaRule() {
    return db.rules().insert(r -> r.setSeverity(Severity.MAJOR).setLanguage("java"));
  }
}
