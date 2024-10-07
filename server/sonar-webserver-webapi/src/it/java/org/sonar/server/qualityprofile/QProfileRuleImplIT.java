/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.sonar.api.PropertyType;
import org.sonar.api.config.Configuration;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.pushapi.qualityprofile.QualityProfileChangeEventService;
import org.sonar.server.qualityprofile.builtin.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.IntegerTypeValidation;
import org.sonar.server.util.StringTypeValidation;
import org.sonar.server.util.TypeValidations;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Map.entry;
import static java.util.Map.of;
import static java.util.Map.ofEntries;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.db.rule.RuleTesting.newCustomRule;
import static org.sonar.server.qualityprofile.ActiveRuleInheritance.INHERITED;
import static org.sonar.server.qualityprofile.ActiveRuleInheritance.OVERRIDES;

class QProfileRuleImplIT {

  private System2 system2 = new AlwaysIncreasingSystem2();
  @RegisterExtension
  public DbTester db = DbTester.create(system2);
  @RegisterExtension
  public EsTester es = EsTester.create();
  @RegisterExtension
  public UserSessionRule userSession = UserSessionRule.standalone();
  private RuleIndex ruleIndex = new RuleIndex(es.client(), system2);
  private ActiveRuleIndexer activeRuleIndexer = new ActiveRuleIndexer(db.getDbClient(), es.client());
  private RuleIndexer ruleIndexer = new RuleIndexer(es.client(), db.getDbClient());
  private TypeValidations typeValidations = new TypeValidations(asList(new StringTypeValidation(), new IntegerTypeValidation()));
  private QualityProfileChangeEventService qualityProfileChangeEventService = mock(QualityProfileChangeEventService.class);
  private Configuration configuration = mock(Configuration.class);
  private final SonarQubeVersion sonarQubeVersion = new SonarQubeVersion(Version.create(10, 3));

  private RuleActivator ruleActivator = new RuleActivator(system2, db.getDbClient(), typeValidations, userSession, configuration,
    sonarQubeVersion);
  private QProfileRules underTest = new QProfileRulesImpl(db.getDbClient(), ruleActivator, ruleIndex, activeRuleIndexer,
    qualityProfileChangeEventService);

  @Test
  void system_activates_rule_without_parameters() {
    RuleDto rule = createRule();
    QProfileDto profile = createProfile(rule);
    RuleActivation activation = RuleActivation.create(rule.getUuid(), BLOCKER, null);
    List<ActiveRuleChange> changes = activate(profile, activation);

    assertThatRuleIsActivated(profile, rule, changes, BLOCKER, null, emptyMap());
    assertThatProfileIsUpdatedBySystem(profile);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), any(), eq(profile.getLanguage()));
  }

  @Test
  void user_activates_rule_without_parameters() {
    userSession.logIn();
    RuleDto rule = createRule();
    QProfileDto profile = createProfile(rule);
    RuleActivation activation = RuleActivation.create(rule.getUuid(), BLOCKER, null);
    List<ActiveRuleChange> changes = activate(profile, activation);

    assertThatRuleIsActivated(profile, rule, changes, BLOCKER, null, emptyMap());
    assertThatProfileIsUpdatedByUser(profile);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), any(), eq(profile.getLanguage()));
  }

  @Test
  void activate_rule_with_default_severity_and_parameters() {
    RuleDto rule = createRule();
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    RuleActivation activation = RuleActivation.create(rule.getUuid());
    List<ActiveRuleChange> changes = activate(profile, activation);

    assertThatRuleIsActivated(profile, rule, changes, rule.getSeverityString(), null, ofEntries(entry("min", "10")));
    assertThatProfileIsUpdatedBySystem(profile);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), any(), eq(profile.getLanguage()));
  }

  @Test
  void activate_rule_with_parameters() {
    RuleDto rule = createRule();
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    RuleActivation activation = RuleActivation.create(rule.getUuid(), null, of(ruleParam.getName(), "15"));
    List<ActiveRuleChange> changes = activate(profile, activation);

    assertThatRuleIsActivated(profile, rule, changes, rule.getSeverityString(), null, of("min", "15"));
    assertThatProfileIsUpdatedBySystem(profile);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), any(), eq(profile.getLanguage()));
  }

  @Test
  void activate_rule_with_default_severity() {
    RuleDto rule = createRule();
    QProfileDto profile = createProfile(rule);

    RuleActivation activation = RuleActivation.create(rule.getUuid());
    List<ActiveRuleChange> changes = activate(profile, activation);

    assertThatRuleIsActivated(profile, rule, changes, rule.getSeverityString(), null, emptyMap());
    assertThatProfileIsUpdatedBySystem(profile);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), any(), eq(profile.getLanguage()));
  }

  /**
   * SONAR-5841
   */
  @Test
  void activate_rule_with_empty_parameter_having_no_default_value() {
    RuleDto rule = createRule();
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    RuleActivation activation = RuleActivation.create(rule.getUuid(), null, of("min", ""));
    List<ActiveRuleChange> changes = activate(profile, activation);

    assertThatRuleIsActivated(profile, rule, changes, rule.getSeverityString(), null, of("min", "10"));
    assertThatProfileIsUpdatedBySystem(profile);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), any(), eq(profile.getLanguage()));
  }

  /**
   * //   * SONAR-5840
   * //
   */
  @Test
  void activate_rule_with_negative_integer_value_on_parameter_having_no_default_value() {
    RuleDto rule = createRule();
    RuleParamDto paramWithoutDefault = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue(null));
    RuleParamDto paramWithDefault = db.rules().insertRuleParam(rule, p -> p.setName("max").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    RuleActivation activation = RuleActivation.create(rule.getUuid(), null, of(paramWithoutDefault.getName(), "-10"));
    List<ActiveRuleChange> changes = activate(profile, activation);

    assertThatRuleIsActivated(profile, rule, changes, rule.getSeverityString(), null,
      of(paramWithoutDefault.getName(), "-10", paramWithDefault.getName(), paramWithDefault.getDefaultValue()));
    assertThatProfileIsUpdatedBySystem(profile);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), any(), eq(profile.getLanguage()));
  }

  @Test
  void activation_ignores_unsupported_parameters() {
    RuleDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule, p -> p.setName("max").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    RuleActivation activation = RuleActivation.create(rule.getUuid(), null, of("xxx", "yyy"));
    List<ActiveRuleChange> changes = activate(profile, activation);

    assertThatRuleIsActivated(profile, rule, changes, rule.getSeverityString(), null, of(param.getName(), param.getDefaultValue()));
    assertThatProfileIsUpdatedBySystem(profile);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), any(), eq(profile.getLanguage()));
  }

  @Test
  void update_an_already_activated_rule() {
    RuleDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule, p -> p.setName("max").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    // initial activation
    RuleActivation activation = RuleActivation.create(rule.getUuid(), MAJOR, null);
    List<ActiveRuleChange> changes = activate(profile, activation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));

    // update
    RuleActivation updateActivation = RuleActivation.create(rule.getUuid(), CRITICAL, of(param.getName(), "20"));
    changes = activate(profile, updateActivation);

    assertThatRuleIsUpdated(profile, rule, CRITICAL, null, of(param.getName(), "20"));
    assertThatProfileIsUpdatedBySystem(profile);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));
  }

  @Test
  void update_activation_with_parameter_without_default_value() {
    RuleDto rule = createRule();
    RuleParamDto paramWithoutDefault = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue(null));
    RuleParamDto paramWithDefault = db.rules().insertRuleParam(rule, p -> p.setName("max").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    // initial activation -> param "max" has a default value
    RuleActivation activation = RuleActivation.create(rule.getUuid());
    List<ActiveRuleChange> changes = activate(profile, activation);

    // update param "min", which has no default value
    RuleActivation updateActivation = RuleActivation.create(rule.getUuid(), MAJOR, of(paramWithoutDefault.getName(), "3"));
    changes = activate(profile, updateActivation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));

    assertThatRuleIsUpdated(profile, rule, MAJOR, null, of(paramWithDefault.getName(), "10", paramWithoutDefault.getName(), "3"));
    assertThatProfileIsUpdatedBySystem(profile);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));
  }

  @Test
  void reset_parameter_to_default_value() {
    RuleDto rule = createRule();
    RuleParamDto paramWithDefault = db.rules().insertRuleParam(rule, p -> p.setName("max").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    // initial activation -> param "max" has a default value
    RuleActivation activation = RuleActivation.create(rule.getUuid(), null, of(paramWithDefault.getName(), "20"));
    List<ActiveRuleChange> changes = activate(profile, activation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));

    // reset to default_value
    RuleActivation updateActivation = RuleActivation.create(rule.getUuid(), null, of(paramWithDefault.getName(), ""));
    changes = activate(profile, updateActivation);

    assertThatRuleIsUpdated(profile, rule, rule.getSeverityString(), null, of(paramWithDefault.getName(), "10"));
    assertThat(changes).hasSize(1);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));
  }

  @Test
  void update_activation_removes_parameter_without_default_value() {
    RuleDto rule = createRule();
    RuleParamDto paramWithoutDefault = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue(null));
    RuleParamDto paramWithDefault = db.rules().insertRuleParam(rule, p -> p.setName("max").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    // initial activation -> param "max" has a default value
    RuleActivation activation = RuleActivation.create(rule.getUuid(), null, of(paramWithoutDefault.getName(), "20"));
    List<ActiveRuleChange> changes = activate(profile, activation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));

    // remove parameter
    RuleActivation updateActivation = RuleActivation.create(rule.getUuid(), null, of(paramWithoutDefault.getName(), ""));
    changes = activate(profile, updateActivation);

    assertThatRuleIsUpdated(profile, rule, rule.getSeverityString(), null, of(paramWithDefault.getName(),
      paramWithDefault.getDefaultValue()));
    assertThat(changes).hasSize(1);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));
  }

  @Test
  void update_activation_with_new_parameter() {
    RuleDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule, p -> p.setName("max").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    // initial activation -> param "max" has a default value
    RuleActivation activation = RuleActivation.create(rule.getUuid());
    List<ActiveRuleChange> changes = activate(profile, activation);
    db.getDbClient().activeRuleDao().deleteParametersByRuleProfileUuids(db.getSession(), asList(profile.getRulesProfileUuid()));
    assertThatRuleIsActivated(profile, rule, changes, rule.getSeverityString(), null, emptyMap());
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));

    // contrary to activerule, the param is supposed to be inserted but not updated
    RuleActivation updateActivation = RuleActivation.create(rule.getUuid(), null, of(param.getName(), ""));
    changes = activate(profile, updateActivation);

    assertThatRuleIsUpdated(profile, rule, rule.getSeverityString(), null, of(param.getName(), param.getDefaultValue()));
    assertThat(changes).hasSize(1);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));
  }

  @Test
  void ignore_activation_without_changes() {
    RuleDto rule = createRule();
    QProfileDto profile = createProfile(rule);

    // initial activation
    RuleActivation activation = RuleActivation.create(rule.getUuid());
    List<ActiveRuleChange> changes = activate(profile, activation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));

    // update with exactly the same severity and params
    activation = RuleActivation.create(rule.getUuid());
    changes = activate(profile, activation);

    assertThat(changes).isEmpty();
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));
  }

  @Test
  void do_not_change_severity_and_params_if_unset_and_already_activated() {
    RuleDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule, p -> p.setName("max").setDefaultValue("10"));
    QProfileDto profile = createProfile(rule);

    // initial activation -> param "max" has a default value
    RuleActivation activation = RuleActivation.create(rule.getUuid(), BLOCKER, of(param.getName(), "20"));
    List<ActiveRuleChange> changes = activate(profile, activation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));


    // update without any severity or params => keep
    RuleActivation update = RuleActivation.create(rule.getUuid());
    changes = activate(profile, update);

    assertThat(changes).isEmpty();
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));
  }

  @Test
  void fail_to_activate_rule_if_profile_is_on_different_languages() {
    RuleDto rule = createJavaRule();
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage("js"));
    RuleActivation activation = RuleActivation.create(rule.getUuid());

    expectFailure("java rule " + rule.getKey() + " cannot be activated on js profile " + profile.getKee(), () -> activate(profile,
      activation));
    verifyNoInteractions(qualityProfileChangeEventService);
  }

  @Test
  void fail_to_activate_rule_if_rule_has_REMOVED_status() {
    RuleDto rule = db.rules().insert(r -> r.setStatus(RuleStatus.REMOVED));
    QProfileDto profile = createProfile(rule);
    RuleActivation activation = RuleActivation.create(rule.getUuid());

    expectFailure("Rule was removed: " + rule.getKey(), () -> activate(profile, activation));
    verifyNoInteractions(qualityProfileChangeEventService);
  }

  @Test
  void fail_to_activate_if_template() {
    RuleDto rule = db.rules().insert(r -> r.setIsTemplate(true));
    QProfileDto profile = createProfile(rule);
    RuleActivation activation = RuleActivation.create(rule.getUuid());

    expectFailure("Rule template can't be activated on a Quality profile: " + rule.getKey(), () -> activate(profile, activation));
    verifyNoInteractions(qualityProfileChangeEventService);
  }

  @Test
  void fail_to_activate_if_invalid_parameter() {
    RuleDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule, p -> p.setName("max").setDefaultValue("10").setType(PropertyType.INTEGER.name()));
    QProfileDto profile = createProfile(rule);

    RuleActivation activation = RuleActivation.create(rule.getUuid(), null, of(param.getName(), "foo"));
    expectFailure("Value 'foo' must be an integer.", () -> activate(profile, activation));
    verifyNoInteractions(qualityProfileChangeEventService);
  }

  @Test
  void ignore_parameters_when_activating_custom_rule() {
    RuleDto templateRule = db.rules().insert(r -> r.setIsTemplate(true));
    RuleParamDto templateParam = db.rules().insertRuleParam(templateRule, p -> p.setName("format"));
    RuleDto customRule = db.rules().insert(newCustomRule(templateRule));
    RuleParamDto customParam = db.rules().insertRuleParam(customRule, p -> p.setName("format").setDefaultValue("txt"));
    QProfileDto profile = createProfile(customRule);

    // initial activation
    RuleActivation activation = RuleActivation.create(customRule.getUuid(), MAJOR, emptyMap());
    List<ActiveRuleChange> changes = activate(profile, activation);
    assertThatRuleIsActivated(profile, customRule, null, MAJOR, null, of("format", "txt"));
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));

    // update -> parameter is not changed
    RuleActivation updateActivation = RuleActivation.create(customRule.getUuid(), BLOCKER, of("format", "xml"));
    changes = activate(profile, updateActivation);
    assertThatRuleIsActivated(profile, customRule, null, BLOCKER, null, of("format", "txt"));
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));
  }

  @Test
  void user_deactivates_a_rule() {
    userSession.logIn();
    RuleDto rule = createRule();
    QProfileDto profile = createProfile(rule);
    RuleActivation activation = RuleActivation.create(rule.getUuid());
    List<ActiveRuleChange> changes = activate(profile, activation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));

    changes = deactivate(profile, rule);
    verifyNoActiveRules();
    assertThatProfileIsUpdatedByUser(profile);
    assertThat(changes).hasSize(1);
    assertThat(changes.get(0).getType()).isEqualTo(ActiveRuleChange.Type.DEACTIVATED);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));
  }

  @Test
  void system_deactivates_a_rule() {
    RuleDto rule = createRule();
    QProfileDto profile = createProfile(rule);
    RuleActivation activation = RuleActivation.create(rule.getUuid());
    List<ActiveRuleChange> changes = activate(profile, activation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));

    changes = deactivate(profile, rule);
    verifyNoActiveRules();
    assertThatProfileIsUpdatedBySystem(profile);
    assertThatChangeIsDeactivation(changes, rule);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));
  }

  private void assertThatChangeIsDeactivation(List<ActiveRuleChange> changes, RuleDto rule) {
    assertThat(changes).hasSize(1);
    ActiveRuleChange change = changes.get(0);
    assertThat(change.getType()).isEqualTo(ActiveRuleChange.Type.DEACTIVATED);
    assertThat(change.getKey().getRuleKey()).isEqualTo(rule.getKey());
  }

  @Test
  void ignore_deactivation_if_rule_is_not_activated() {
    RuleDto rule = createRule();
    QProfileDto profile = createProfile(rule);

    List<ActiveRuleChange> changes = deactivate(profile, rule);
    verifyNoActiveRules();
    assertThat(changes).isEmpty();
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), any(), eq(profile.getLanguage()));
  }

  @Test
  void deactivate_rule_that_has_REMOVED_status() {
    RuleDto rule = createRule();
    QProfileDto profile = createProfile(rule);
    RuleActivation activation = RuleActivation.create(rule.getUuid());
    List<ActiveRuleChange> changes = activate(profile, activation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));

    rule.setStatus(RuleStatus.REMOVED);
    db.getDbClient().ruleDao().update(db.getSession(), rule);

    changes = deactivate(profile, rule);
    verifyNoActiveRules();
    assertThatChangeIsDeactivation(changes, rule);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(profile.getLanguage()));
  }

  @Test
  void activate_shouldPropagateActivationOnChildren() {
    RuleDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);
    QProfileDto grandChildProfile = createChildProfile(childProfile);

    List<ActiveRuleChange> changes = activate(childProfile, RuleActivation.create(rule.getUuid()));
    assertThatProfileHasNoActiveRules(parentProfile);
    assertThatRuleIsActivated(childProfile, rule, changes, rule.getSeverityString(), null, emptyMap());
    assertThatRuleIsActivated(grandChildProfile, rule, changes, rule.getSeverityString(), INHERITED, emptyMap());
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), any(), eq(childProfile.getLanguage()));
  }

  @Test
  void activate_whenChildProfileAlreadyActivatedRule_shouldNotStopPropagating() {
    RuleDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);
    QProfileDto childProfile2 = createChildProfile(childProfile);
    QProfileDto childProfile3 = createChildProfile(childProfile2);

    RuleActivation activation = RuleActivation.create(rule.getUuid(), MAJOR, emptyMap());

    // Rule already active on childProfile2
    List<ActiveRuleChange> changes = activate(childProfile2, activation);
    assertThatRuleIsActivated(childProfile2, rule, changes, rule.getSeverityString(), null, emptyMap());
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(parentProfile.getLanguage()));
    deactivate(childProfile3, rule);
    assertThatProfileHasNoActiveRules(childProfile3);

    changes = activate(parentProfile, activation);
    assertThatRuleIsActivated(parentProfile, rule, changes, rule.getSeverityString(), null, emptyMap());
    assertThatRuleIsActivated(childProfile, rule, changes, rule.getSeverityString(), INHERITED, emptyMap());
    assertThatRuleIsUpdated(childProfile2, rule, rule.getSeverityString(), INHERITED, emptyMap());
    assertThatRuleIsActivated(childProfile3, rule, changes, rule.getSeverityString(), INHERITED, emptyMap());
    assertThat(changes).hasSize(4);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(parentProfile.getLanguage()));
  }

  @Test
  void activate_whenChildAlreadyActivatedRuleWithOverriddenValues_shouldNotOverrideValues() {
    RuleDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);
    QProfileDto childProfile2 = createChildProfile(childProfile);
    QProfileDto childProfile3 = createChildProfile(childProfile2);

    List<ActiveRuleChange> changes = activate(childProfile2, RuleActivation.create(rule.getUuid(), CRITICAL, emptyMap()));
    assertThatRuleIsActivated(childProfile2, rule, changes, CRITICAL, null, emptyMap());
    assertThatRuleIsActivated(childProfile3, rule, changes, CRITICAL, INHERITED, emptyMap());
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(parentProfile.getLanguage()));

    changes = activate(parentProfile, RuleActivation.create(rule.getUuid(), MAJOR, emptyMap()));
    assertThatRuleIsActivated(parentProfile, rule, changes, MAJOR, null, emptyMap());
    assertThatRuleIsActivated(childProfile, rule, changes, MAJOR, INHERITED, emptyMap());
    assertThatRuleIsUpdated(childProfile2, rule, CRITICAL, OVERRIDES, emptyMap());
    // childProfile3 is neither activated nor updated, it keeps its inherited value from childProfile2
    assertThat(changes).hasSize(3);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(parentProfile.getLanguage()));
  }

  @Test
  void activate_whenParentHasRuleWithSameValues_shouldMarkInherited() {
    RuleDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    List<ActiveRuleChange> changes = activate(parentProfile, RuleActivation.create(rule.getUuid(), CRITICAL, emptyMap()));
    assertThatRuleIsActivated(parentProfile, rule, changes, CRITICAL, null, emptyMap());
    deactivate(childProfile, rule);
    assertThatProfileHasNoActiveRules(childProfile);

    changes = activate(childProfile, RuleActivation.create(rule.getUuid(), CRITICAL, emptyMap()));
    assertThatRuleIsActivated(childProfile, rule, changes, CRITICAL, INHERITED, emptyMap());
    assertThat(changes).hasSize(1);
  }

  @Test
  void activate_whenParentHasRuleWithDifferentValues_shouldMarkOverridden() {
    RuleDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    List<ActiveRuleChange> changes = activate(parentProfile, RuleActivation.create(rule.getUuid(), CRITICAL, emptyMap()));
    assertThatRuleIsActivated(parentProfile, rule, changes, CRITICAL, null, emptyMap());
    deactivate(childProfile, rule);
    assertThatProfileHasNoActiveRules(childProfile);

    changes = activate(childProfile, RuleActivation.create(rule.getUuid(), MAJOR, emptyMap()));
    assertThatRuleIsActivated(childProfile, rule, changes, MAJOR, OVERRIDES, emptyMap());
    assertThat(changes).hasSize(1);
  }

  @Test
  void update_on_child_profile_is_propagated_to_descendants() {
    RuleDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);
    QProfileDto grandChildProfile = createChildProfile(childProfile);

    System.out.println("ACTIVATE ON " + childProfile.getName());
    RuleActivation initialActivation = RuleActivation.create(rule.getUuid(), MAJOR, of(param.getName(), "foo"));
    List<ActiveRuleChange> changes = activate(childProfile, initialActivation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(childProfile.getLanguage()));

    System.out.println("---------------");
    System.out.println("ACTIVATE ON " + childProfile.getName());
    RuleActivation updateActivation = RuleActivation.create(rule.getUuid(), CRITICAL, of(param.getName(), "bar"));
    changes = activate(childProfile, updateActivation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(childProfile.getLanguage()));

    assertThatProfileHasNoActiveRules(parentProfile);
    assertThatRuleIsUpdated(childProfile, rule, CRITICAL, null, of(param.getName(), "bar"));
    assertThatRuleIsUpdated(grandChildProfile, rule, CRITICAL, INHERITED, of(param.getName(), "bar"));
    assertThat(changes).hasSize(2);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(childProfile.getLanguage()));
  }

  @Test
  void override_activation_of_inherited_profile() {
    RuleDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);
    QProfileDto grandChildProfile = createChildProfile(childProfile);

    RuleActivation initialActivation = RuleActivation.create(rule.getUuid(), MAJOR, of(param.getName(), "foo"));
    List<ActiveRuleChange> changes = activate(childProfile, initialActivation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(childProfile.getLanguage()));

    RuleActivation overrideActivation = RuleActivation.create(rule.getUuid(), CRITICAL, of(param.getName(), "bar"));
    changes = activate(grandChildProfile, overrideActivation);

    assertThatProfileHasNoActiveRules(parentProfile);
    assertThatRuleIsUpdated(childProfile, rule, MAJOR, null, of(param.getName(), "foo"));
    assertThatRuleIsUpdated(grandChildProfile, rule, CRITICAL, OVERRIDES, of(param.getName(), "bar"));
    assertThat(changes).hasSize(1);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(childProfile.getLanguage()));
  }

  @Test
  void updated_activation_on_parent_is_not_propagated_to_overridden_profiles() {
    RuleDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);
    QProfileDto grandChildProfile = createChildProfile(childProfile);

    RuleActivation initialActivation = RuleActivation.create(rule.getUuid(), MAJOR, of(param.getName(), "foo"));
    List<ActiveRuleChange> changes = activate(childProfile, initialActivation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(childProfile.getLanguage()));

    RuleActivation overrideActivation = RuleActivation.create(rule.getUuid(), CRITICAL, of(param.getName(), "bar"));
    changes = activate(grandChildProfile, overrideActivation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(grandChildProfile.getLanguage()));

    // update child --> do not touch grandChild
    RuleActivation updateActivation = RuleActivation.create(rule.getUuid(), BLOCKER, of(param.getName(), "baz"));
    changes = activate(childProfile, updateActivation);

    assertThatProfileHasNoActiveRules(parentProfile);
    assertThatRuleIsUpdated(childProfile, rule, BLOCKER, null, of(param.getName(), "baz"));
    assertThatRuleIsUpdated(grandChildProfile, rule, CRITICAL, OVERRIDES, of(param.getName(), "bar"));
    assertThat(changes).hasSize(1);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(childProfile.getLanguage()));
  }

  @Test
  void reset_on_parent_is_not_propagated_to_overridden_profiles() {
    RuleDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);
    QProfileDto grandChildProfile = createChildProfile(childProfile);

    RuleActivation initialActivation = RuleActivation.create(rule.getUuid(), MAJOR, of(param.getName(), "foo"));
    List<ActiveRuleChange> changes = activate(parentProfile, initialActivation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(parentProfile.getLanguage()));

    RuleActivation overrideActivation = RuleActivation.create(rule.getUuid(), CRITICAL, of(param.getName(), "bar"));
    changes = activate(grandChildProfile, overrideActivation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(grandChildProfile.getLanguage()));

    // reset parent --> touch child but not grandChild
    RuleActivation updateActivation = RuleActivation.createReset(rule.getUuid());
    changes = activate(parentProfile, updateActivation);

    Map<String, String> test = new HashMap<>();
    test.put(param.getName(), param.getDefaultValue());
    assertThatRuleIsUpdated(parentProfile, rule, rule.getSeverityString(), null, test);
    assertThatRuleIsUpdated(childProfile, rule, rule.getSeverityString(), INHERITED, of(param.getName(), param.getDefaultValue()));
    assertThatRuleIsUpdated(grandChildProfile, rule, CRITICAL, OVERRIDES, of(param.getName(), "bar"));
    assertThat(changes).hasSize(2);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(parentProfile.getLanguage()));
  }

  @Test
  void activate_whenRuleAlreadyActiveOnChildWithDifferentValues_shouldMarkOverridden() {
    RuleDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    RuleActivation childActivation = RuleActivation.create(rule.getUuid(), MAJOR, of(param.getName(), "foo"));
    List<ActiveRuleChange> changes = activate(childProfile, childActivation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(childProfile.getLanguage()));

    RuleActivation parentActivation = RuleActivation.create(rule.getUuid(), MAJOR, of(param.getName(), "bar"));
    changes = activate(parentProfile, parentActivation);

    assertThatRuleIsUpdated(parentProfile, rule, MAJOR, null, of(param.getName(), "bar"));
    assertThatRuleIsUpdated(childProfile, rule, MAJOR, OVERRIDES, of(param.getName(), "foo"));
    assertThat(changes).hasSize(2);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(parentProfile.getLanguage()));
  }

  @Test
  void activate_whenRuleAlreadyActiveOnChildWithSameValues_shouldMarkInherited() {
    RuleDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    RuleActivation childActivation = RuleActivation.create(rule.getUuid(), MAJOR, of(param.getName(), "foo"));
    List<ActiveRuleChange> changes = activate(childProfile, childActivation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(childProfile.getLanguage()));

    RuleActivation parentActivation = RuleActivation.create(rule.getUuid(), MAJOR, of(param.getName(), "foo"));
    changes = activate(parentProfile, parentActivation);

    assertThatRuleIsUpdated(parentProfile, rule, MAJOR, null, of(param.getName(), "foo"));
    assertThatRuleIsUpdated(childProfile, rule, MAJOR, INHERITED, of(param.getName(), "foo"));
    assertThat(changes).hasSize(2);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(parentProfile.getLanguage()));
  }

  @Test
  void activate_whenSettingValuesOnChildAndParentHasSameValues_shouldMarkInherited() {
    RuleDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    RuleActivation parentActivation = RuleActivation.create(rule.getUuid(), MAJOR, of(param.getName(), "foo"));
    List<ActiveRuleChange> changes = activate(parentProfile, parentActivation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(parentProfile.getLanguage()));

    RuleActivation overrideActivation = RuleActivation.create(rule.getUuid(), MAJOR, of(param.getName(), "foo"));
    changes = activate(childProfile, overrideActivation);

    assertThatRuleIsUpdated(childProfile, rule, MAJOR, INHERITED, of(param.getName(), "foo"));
    assertThat(changes).isEmpty();
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(childProfile.getLanguage()));
  }

  @Test
  void activate_whenSettingValuesOnChildAndParentHasDifferentValues_shouldMarkOverridden() {
    RuleDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    RuleActivation parentActivation = RuleActivation.create(rule.getUuid(), MAJOR, of(param.getName(), "foo"));
    List<ActiveRuleChange> changes = activate(parentProfile, parentActivation);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(parentProfile.getLanguage()));

    RuleActivation overrideActivation = RuleActivation.create(rule.getUuid(), CRITICAL, of(param.getName(), "bar"));
    changes = activate(childProfile, overrideActivation);

    assertThatRuleIsUpdated(childProfile, rule, CRITICAL, OVERRIDES, of(param.getName(), "bar"));
    assertThat(changes).hasSize(1);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(childProfile.getLanguage()));
  }

  @Test
  void deactivate_shouldPropagateDeactivationOnChildren() {
    RuleDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    RuleActivation activation = RuleActivation.create(rule.getUuid());
    List<ActiveRuleChange> changes = activate(parentProfile, activation);
    assertThatRuleIsActivated(parentProfile, rule, changes, rule.getSeverityString(), null, emptyMap());
    assertThatRuleIsActivated(childProfile, rule, changes, rule.getSeverityString(), INHERITED, emptyMap());
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(parentProfile.getLanguage()));

    changes = deactivate(parentProfile, rule);
    assertThatProfileHasNoActiveRules(parentProfile);
    assertThatProfileHasNoActiveRules(childProfile);
    assertThat(changes).hasSize(2);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(parentProfile.getLanguage()));
  }

  @Test
  void deactivate_whenChildAlreadyDeactivatedRule_shouldNotStopPropagating() {
    RuleDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);
    QProfileDto childProfile2 = createChildProfile(childProfile);
    QProfileDto childProfile3 = createChildProfile(childProfile2);

    RuleActivation activation = RuleActivation.create(rule.getUuid());

    // Rule active on parentProfile, childProfile1 and childProfile3 but not on childProfile2
    List<ActiveRuleChange> changes = activate(parentProfile, activation);
    assertThatRuleIsActivated(parentProfile, rule, changes, rule.getSeverityString(), null, emptyMap());
    assertThatRuleIsActivated(childProfile, rule, changes, rule.getSeverityString(), INHERITED, emptyMap());
    assertThatRuleIsActivated(childProfile2, rule, changes, rule.getSeverityString(), INHERITED, emptyMap());
    assertThatRuleIsActivated(childProfile3, rule, changes, rule.getSeverityString(), INHERITED, emptyMap());
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(parentProfile.getLanguage()));
    deactivate(childProfile2, rule);
    changes = activate(childProfile3, activation);
    assertThatProfileHasNoActiveRules(childProfile2);
    assertThatRuleIsActivated(childProfile3, rule, changes, rule.getSeverityString(), null, emptyMap());

    changes = deactivate(parentProfile, rule);
    assertThatProfileHasNoActiveRules(parentProfile);
    assertThatProfileHasNoActiveRules(childProfile);
    assertThatProfileHasNoActiveRules(childProfile2);
    assertThatProfileHasNoActiveRules(childProfile3);
    assertThat(changes).hasSize(3);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(parentProfile.getLanguage()));
  }

  @Test
  void deactivate_whenChildOverridesRule_shouldPropagateDeactivation() {
    RuleDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    RuleActivation activation = RuleActivation.create(rule.getUuid());
    List<ActiveRuleChange> changes = activate(parentProfile, activation);
    assertThatRuleIsActivated(parentProfile, rule, changes, rule.getSeverityString(), null, emptyMap());
    assertThatRuleIsActivated(childProfile, rule, changes, rule.getSeverityString(), INHERITED, emptyMap());
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(parentProfile.getLanguage()));

    activation = RuleActivation.create(rule.getUuid(), CRITICAL, null);
    changes = activate(childProfile, activation);
    assertThatRuleIsUpdated(childProfile, rule, CRITICAL, OVERRIDES, emptyMap());
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(childProfile.getLanguage()));

    changes = deactivate(parentProfile, rule);
    assertThatProfileHasNoActiveRules(parentProfile);
    assertThatProfileHasNoActiveRules(childProfile);
    assertThat(changes).hasSize(2);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(parentProfile.getLanguage()));
  }

  @Test
  void deactivate_whenRuleInherited_canBeDeactivated() {
    RuleDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    RuleActivation activation = RuleActivation.create(rule.getUuid());
    List<ActiveRuleChange> changes = activate(parentProfile, activation);
    assertThatRuleIsActivated(parentProfile, rule, changes, rule.getSeverityString(), null, emptyMap());
    assertThatRuleIsActivated(childProfile, rule, changes, rule.getSeverityString(), INHERITED, emptyMap());

    changes = deactivate(childProfile, rule);
    assertThatProfileHasNoActiveRules(childProfile);
    assertThat(changes).hasSize(1);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(parentProfile.getLanguage()));
  }

  @Test
  void deactivate_whenRuleInheritedAndPropertyDisabled_cannotBeDeactivated() {
    Mockito.when(configuration.getBoolean(CorePropertyDefinitions.ALLOW_DISABLE_INHERITED_RULES)).thenReturn(Optional.of(false));

    RuleDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    RuleActivation activation = RuleActivation.create(rule.getUuid());
    List<ActiveRuleChange> changes = activate(parentProfile, activation);
    assertThatRuleIsActivated(parentProfile, rule, changes, rule.getSeverityString(), null, emptyMap());
    assertThatRuleIsActivated(childProfile, rule, changes, rule.getSeverityString(), INHERITED, emptyMap());

    assertThatThrownBy(() -> deactivate(childProfile, rule))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("Cannot deactivate inherited rule");
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), any(), eq(parentProfile.getLanguage()));
  }

  @Test
  void reset_child_profile_do_not_change_parent() {
    RuleDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    RuleActivation activation = RuleActivation.create(rule.getUuid(), CRITICAL, null);
    List<ActiveRuleChange> changes = activate(parentProfile, activation);
    assertThatRuleIsActivated(parentProfile, rule, changes, CRITICAL, null, emptyMap());
    assertThatRuleIsActivated(childProfile, rule, changes, CRITICAL, INHERITED, emptyMap());
    assertThat(changes).hasSize(2);

    RuleActivation childActivation = RuleActivation.create(rule.getUuid(), BLOCKER, null);
    changes = activate(childProfile, childActivation);
    assertThatRuleIsUpdated(childProfile, rule, BLOCKER, OVERRIDES, emptyMap());
    assertThat(changes).hasSize(1);

    RuleActivation resetActivation = RuleActivation.createReset(rule.getUuid());
    changes = activate(childProfile, resetActivation);
    assertThatRuleIsUpdated(childProfile, rule, CRITICAL, INHERITED, emptyMap());
    assertThatRuleIsUpdated(parentProfile, rule, CRITICAL, null, emptyMap());
    assertThat(changes).hasSize(1);

  }

  @Test
  void reset_parent_is_not_propagated_when_child_overrides() {
    RuleDto rule = createRule();
    QProfileDto baseProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(baseProfile);
    QProfileDto grandChildProfile = createChildProfile(childProfile);

    RuleActivation activation = RuleActivation.create(rule.getUuid(), CRITICAL, null);
    List<ActiveRuleChange> changes = activate(baseProfile, activation);
    assertThatRuleIsActivated(baseProfile, rule, changes, CRITICAL, null, emptyMap());
    assertThatRuleIsActivated(childProfile, rule, changes, CRITICAL, INHERITED, emptyMap());
    assertThatRuleIsActivated(grandChildProfile, rule, changes, CRITICAL, INHERITED, emptyMap());
    assertThat(changes).hasSize(3);

    RuleActivation childActivation = RuleActivation.create(rule.getUuid(), BLOCKER, null);
    changes = activate(childProfile, childActivation);
    assertThatRuleIsUpdated(childProfile, rule, BLOCKER, OVERRIDES, emptyMap());
    assertThatRuleIsUpdated(grandChildProfile, rule, BLOCKER, INHERITED, emptyMap());
    assertThat(changes).hasSize(2);

    // Reset on parent do not change child nor grandchild
    RuleActivation resetActivation = RuleActivation.createReset(rule.getUuid());
    changes = activate(baseProfile, resetActivation);
    assertThatRuleIsUpdated(baseProfile, rule, rule.getSeverityString(), null, emptyMap());
    assertThatRuleIsUpdated(childProfile, rule, BLOCKER, OVERRIDES, emptyMap());
    assertThatRuleIsUpdated(grandChildProfile, rule, BLOCKER, INHERITED, emptyMap());
    assertThat(changes).hasSize(1);

    // Reset on child change grandchild
    resetActivation = RuleActivation.createReset(rule.getUuid());
    changes = activate(childProfile, resetActivation);
    assertThatRuleIsUpdated(baseProfile, rule, rule.getSeverityString(), null, emptyMap());
    assertThatRuleIsUpdated(childProfile, rule, rule.getSeverityString(), INHERITED, emptyMap());
    assertThatRuleIsUpdated(grandChildProfile, rule, rule.getSeverityString(), INHERITED, emptyMap());
    assertThat(changes).hasSize(2);
  }

  @Test
  void ignore_reset_if_not_activated() {
    RuleDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    createChildProfile(parentProfile);

    RuleActivation resetActivation = RuleActivation.createReset(rule.getUuid());
    List<ActiveRuleChange> changes = activate(parentProfile, resetActivation);
    verifyNoActiveRules();
    assertThat(changes).isEmpty();
  }

  @Test
  void bulk_activation() {
    int bulkSize = SearchOptions.MAX_PAGE_SIZE + 10 + new Random().nextInt(100);
    String language = secure().nextAlphanumeric(10);
    String repositoryKey = secure().nextAlphanumeric(10);
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(language));

    List<RuleDto> rules = new ArrayList<>();
    IntStream.rangeClosed(1, bulkSize).forEach(
      i -> rules.add(db.rules().insertRule(r -> r.setLanguage(language).setRepositoryKey(repositoryKey))));

    verifyNoActiveRules();
    ruleIndexer.indexAll();

    RuleQuery ruleQuery = new RuleQuery()
      .setRepositories(singletonList(repositoryKey));

    BulkChangeResult bulkChangeResult = underTest.bulkActivateAndCommit(db.getSession(), profile, ruleQuery, MINOR, true);

    assertThat(bulkChangeResult.countFailed()).isZero();
    assertThat(bulkChangeResult.countSucceeded()).isEqualTo(bulkSize);
    assertThat(bulkChangeResult.getChanges()).hasSize(bulkSize);
    assertThat(db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), profile)).hasSize(bulkSize);
    rules.forEach(r -> assertThatRuleIsActivated(profile, r, null, MINOR, true, null, emptyMap()));
  }

  @Test
  void bulk_deactivation() {
    int bulkSize = SearchOptions.MAX_PAGE_SIZE + 10 + new Random().nextInt(100);
    String language = secure().nextAlphanumeric(10);
    String repositoryKey = secure().nextAlphanumeric(10);
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(language));

    List<RuleDto> rules = new ArrayList<>();
    IntStream.rangeClosed(1, bulkSize).forEach(
      i -> rules.add(db.rules().insertRule(r -> r.setLanguage(language).setRepositoryKey(repositoryKey))));

    verifyNoActiveRules();
    ruleIndexer.indexAll();

    RuleQuery ruleQuery = new RuleQuery()
      .setRepositories(singletonList(repositoryKey));

    BulkChangeResult bulkChangeResult = underTest.bulkActivateAndCommit(db.getSession(), profile, ruleQuery, MINOR, null);

    assertThat(bulkChangeResult.countFailed()).isZero();
    assertThat(bulkChangeResult.countSucceeded()).isEqualTo(bulkSize);
    assertThat(bulkChangeResult.getChanges()).hasSize(bulkSize);
    assertThat(db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), profile)).hasSize(bulkSize);

    // Now deactivate all rules
    bulkChangeResult = underTest.bulkDeactivateAndCommit(db.getSession(), profile, ruleQuery);

    assertThat(bulkChangeResult.countFailed()).isZero();
    assertThat(bulkChangeResult.countSucceeded()).isEqualTo(bulkSize);
    assertThat(bulkChangeResult.getChanges()).hasSize(bulkSize);
    assertThat(db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), profile)).isEmpty();
    rules.forEach(r -> assertThatRuleIsNotPresent(profile, r));
  }

  @Test
  void bulkDeactivateAndCommit_whenRuleInherited_canBeDeactivated() {
    RuleDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    activate(parentProfile, RuleActivation.create(rule.getUuid()));
    assertThatRuleIsActivated(parentProfile, rule, null, rule.getSeverityString(), null, emptyMap());
    assertThatRuleIsActivated(childProfile, rule, null, rule.getSeverityString(), INHERITED, emptyMap());

    ruleIndexer.indexAll();

    RuleQuery ruleQuery = new RuleQuery()
      .setQProfile(childProfile);
    BulkChangeResult bulkChangeResult = underTest.bulkDeactivateAndCommit(db.getSession(), childProfile, ruleQuery);

    assertThat(bulkChangeResult.countFailed()).isZero();
    assertThat(bulkChangeResult.countSucceeded()).isOne();
    assertThat(bulkChangeResult.getChanges()).hasSize(1);
    assertThatProfileHasNoActiveRules(childProfile);
  }

  @Test
  void bulk_change_severity_and_prioritized_rule() {
    RuleDto rule1 = createJavaRule();
    RuleDto rule2 = createJavaRule();
    QProfileDto parentProfile = createProfile(rule1);
    QProfileDto childProfile = createChildProfile(parentProfile);
    QProfileDto grandchildProfile = createChildProfile(childProfile);

    activate(parentProfile, RuleActivation.create(rule1.getUuid()));
    activate(parentProfile, RuleActivation.create(rule2.getUuid()));

    ruleIndexer.indexAll();

    RuleQuery query = new RuleQuery()
      .setRuleKey(rule1.getRuleKey())
      .setQProfile(parentProfile);
    BulkChangeResult result = underTest.bulkActivateAndCommit(db.getSession(), parentProfile, query, "BLOCKER", true);

    assertThat(result.getChanges()).hasSize(3);
    assertThat(result.countSucceeded()).isOne();
    assertThat(result.countFailed()).isZero();

    // Rule1 must be activated with BLOCKER on all profiles
    assertThatRuleIsActivated(parentProfile, rule1, null, BLOCKER, true, null, emptyMap());
    assertThatRuleIsActivated(childProfile, rule1, null, BLOCKER, true, INHERITED, emptyMap());
    assertThatRuleIsActivated(grandchildProfile, rule1, null, BLOCKER, true, INHERITED, emptyMap());

    // Rule2 did not changed
    assertThatRuleIsActivated(parentProfile, rule2, null, rule2.getSeverityString(), null, emptyMap());
    assertThatRuleIsActivated(childProfile, rule2, null, rule2.getSeverityString(), INHERITED, emptyMap());
    assertThatRuleIsActivated(grandchildProfile, rule2, null, rule2.getSeverityString(), INHERITED, emptyMap());
  }

  @Test
  void delete_rule_from_all_profiles() {
    RuleDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);
    QProfileDto grandChildProfile = createChildProfile(childProfile);

    RuleActivation activation = RuleActivation.create(rule.getUuid(), CRITICAL, null);
    activate(parentProfile, activation);

    RuleActivation overrideActivation = RuleActivation.create(rule.getUuid(), BLOCKER, null);
    activate(grandChildProfile, overrideActivation);

    // Reset on parent do not change child nor grandchild
    List<ActiveRuleChange> changes = underTest.deleteRule(db.getSession(), rule);

    assertThatRuleIsNotPresent(parentProfile, rule);
    assertThatRuleIsNotPresent(childProfile, rule);
    assertThatRuleIsNotPresent(grandChildProfile, rule);
    assertThat(changes)
      .extracting(ActiveRuleChange::getType)
      .containsOnly(ActiveRuleChange.Type.DEACTIVATED)
      .hasSize(3);
  }

  @Test
  void activation_fails_when_profile_is_built_in() {
    RuleDto rule = createRule();
    QProfileDto builtInProfile = db.qualityProfiles().insert(p -> p.setLanguage(rule.getLanguage()).setIsBuiltIn(true));

    assertThatThrownBy(() -> {
      underTest.activateAndCommit(db.getSession(), builtInProfile, singleton(RuleActivation.create(rule.getUuid())));
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The built-in profile " + builtInProfile.getName() + " is read-only and can't be updated");
  }

  private void assertThatProfileHasNoActiveRules(QProfileDto profile) {
    List<OrgActiveRuleDto> activeRules = db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), profile);
    assertThat(activeRules).isEmpty();
  }

  private List<ActiveRuleChange> deactivate(QProfileDto profile, RuleDto rule) {
    return underTest.deactivateAndCommit(db.getSession(), profile, singleton(rule.getUuid()));
  }

  private List<ActiveRuleChange> activate(QProfileDto profile, RuleActivation activation) {
    return underTest.activateAndCommit(db.getSession(), profile, singleton(activation));
  }

  private QProfileDto createProfile(RuleDto rule) {
    return db.qualityProfiles().insert(p -> p.setLanguage(rule.getLanguage()));
  }

  private QProfileDto createChildProfile(QProfileDto parent) {
    return db.qualityProfiles().insert(p -> p
      .setLanguage(parent.getLanguage())
      .setParentKee(parent.getKee())
      .setName("Child of " + parent.getName()));
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

  private void assertThatRuleIsActivated(QProfileDto profile, RuleDto rule, @Nullable List<ActiveRuleChange> changes,
    String expectedSeverity, boolean expectedPrioritizedRule, @Nullable ActiveRuleInheritance expectedInheritance,
    Map<String, String> expectedParams) {
    OrgActiveRuleDto activeRule = db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), profile)
      .stream()
      .filter(ar -> ar.getRuleKey().equals(rule.getKey()))
      .findFirst()
      .orElseThrow(IllegalStateException::new);

    assertThat(activeRule.getSeverityString()).isEqualTo(expectedSeverity);
    assertThat(activeRule.isPrioritizedRule()).isEqualTo(expectedPrioritizedRule);
    assertThat(activeRule.getInheritance()).isEqualTo(expectedInheritance != null ? expectedInheritance.name() : null);

    List<ActiveRuleParamDto> params = db.getDbClient().activeRuleDao().selectParamsByActiveRuleUuid(db.getSession(), activeRule.getUuid());
    assertThat(params).hasSize(expectedParams.size());

    if (changes != null) {
      ActiveRuleChange change = changes.stream()
        .filter(c -> c.getActiveRule().getUuid().equals(activeRule.getUuid()))
        .findFirst().orElseThrow(IllegalStateException::new);
      assertThat(change.getInheritance()).isEqualTo(expectedInheritance);
      assertThat(change.getSeverity()).isEqualTo(expectedSeverity);
      assertThat(change.isPrioritizedRule()).isEqualTo(expectedPrioritizedRule);
      assertThat(change.getType()).isEqualTo(ActiveRuleChange.Type.ACTIVATED);
    }
  }

  private void assertThatRuleIsActivated(QProfileDto profile, RuleDto rule, @Nullable List<ActiveRuleChange> changes,
    String expectedSeverity, @Nullable ActiveRuleInheritance expectedInheritance, Map<String, String> expectedParams) {
    assertThatRuleIsActivated(profile, rule, changes, expectedSeverity, false, expectedInheritance, expectedParams);
  }

  private void assertThatRuleIsNotPresent(QProfileDto profile, RuleDto rule) {
    Optional<OrgActiveRuleDto> activeRule = db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), profile)
      .stream()
      .filter(ar -> ar.getRuleKey().equals(rule.getKey()))
      .findFirst();

    assertThat(activeRule).isEmpty();
  }

  private void assertThatRuleIsUpdated(QProfileDto profile, RuleDto rule,
    String expectedSeverity, @Nullable ActiveRuleInheritance expectedInheritance, Map<String, String> expectedParams) {
    OrgActiveRuleDto activeRule = db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), profile)
      .stream()
      .filter(ar -> ar.getRuleKey().equals(rule.getKey()))
      .findFirst()
      .orElseThrow(IllegalStateException::new);

    assertThat(activeRule.getSeverityString()).isEqualTo(expectedSeverity);
    assertThat(activeRule.getInheritance()).isEqualTo(expectedInheritance != null ? expectedInheritance.name() : null);

    List<ActiveRuleParamDto> params = db.getDbClient().activeRuleDao().selectParamsByActiveRuleUuid(db.getSession(), activeRule.getUuid());
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
    assertThat(db.countRowsOfTable(db.getSession(), "active_rules")).isZero();
  }

  private RuleDto createRule() {
    return db.rules().insert(r -> r.setSeverity(Severity.MAJOR));
  }

  private RuleDto createJavaRule() {
    return db.rules().insert(r -> r.setSeverity(Severity.MAJOR).setLanguage("java"));
  }
}
