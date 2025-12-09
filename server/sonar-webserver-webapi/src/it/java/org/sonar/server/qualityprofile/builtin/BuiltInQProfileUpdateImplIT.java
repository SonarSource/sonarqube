/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.qualityprofile.builtin;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.config.Configuration;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.NewBuiltInQualityProfile;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbTester;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.pushapi.qualityprofile.QualityProfileChangeEventService;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.ActiveRuleInheritance;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.IntegerTypeValidation;
import org.sonar.server.util.StringTypeValidation;
import org.sonar.server.util.TypeValidations;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sonar.api.rules.RulePriority.BLOCKER;
import static org.sonar.api.rules.RulePriority.CRITICAL;
import static org.sonar.api.rules.RulePriority.MAJOR;
import static org.sonar.api.rules.RulePriority.MINOR;
import static org.sonar.db.qualityprofile.QualityProfileTesting.newRuleProfileDto;
import static org.sonar.server.qualityprofile.ActiveRuleInheritance.INHERITED;

class BuiltInQProfileUpdateImplIT {

  private static final long NOW = 1_000;
  private static final long PAST = NOW - 100;

  @RegisterExtension
  public BuiltInQProfileRepositoryRule builtInProfileRepository = new BuiltInQProfileRepositoryRule();
  @RegisterExtension
  public DbTester db = DbTester.create();
  @RegisterExtension
  public UserSessionRule userSession = UserSessionRule.standalone();
  private System2 system2 = new TestSystem2().setNow(NOW);
  private ActiveRuleIndexer activeRuleIndexer = mock(ActiveRuleIndexer.class);
  private TypeValidations typeValidations = new TypeValidations(asList(new StringTypeValidation(), new IntegerTypeValidation()));
  private QualityProfileChangeEventService qualityProfileChangeEventService = mock(QualityProfileChangeEventService.class);
  private final SonarQubeVersion sonarQubeVersion = new SonarQubeVersion(Version.create(10, 3));
  private RuleActivator ruleActivator = new RuleActivator(system2, db.getDbClient(), UuidFactoryImpl.INSTANCE, typeValidations, userSession, mock(Configuration.class),
    sonarQubeVersion);

  private BuiltInQProfileUpdateImpl underTest = new BuiltInQProfileUpdateImpl(db.getDbClient(), ruleActivator, activeRuleIndexer,
    qualityProfileChangeEventService);

  private RulesProfileDto persistedProfile;

  @BeforeEach
  void setUp() {
    persistedProfile = newRuleProfileDto(rp -> rp
      .setIsBuiltIn(true)
      .setLanguage("xoo")
      .setRulesUpdatedAt(null));
    db.getDbClient().qualityProfileDao().insert(db.getSession(), persistedProfile);
    db.commit();
  }

  @Test
  void activate_new_rules() {
    RuleDto rule1 = db.rules()
      .insert(r -> r.setLanguage("xoo").replaceAllDefaultImpacts(List.of(newImpactDto(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.HIGH),
        newImpactDto(SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.LOW))));
    RuleDto rule2 = db.rules().insert(r -> r.setLanguage("xoo"));
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile("Sonar way", "xoo");
    newQp.activateRule(rule1.getRepositoryKey(), rule1.getRuleKey()).overrideSeverity(Severity.CRITICAL);
    newQp.activateRule(rule2.getRepositoryKey(), rule2.getRuleKey()).overrideSeverity(Severity.MAJOR);
    newQp.done();
    BuiltInQProfile builtIn = builtInProfileRepository.create(context.profile("xoo", "Sonar way"), rule1, rule2);

    List<ActiveRuleChange> changes = underTest.update(db.getSession(), builtIn, persistedProfile);

    List<ActiveRuleDto> activeRules = db.getDbClient().activeRuleDao().selectByRuleProfile(db.getSession(), persistedProfile);
    assertThat(activeRules).hasSize(2);
    assertThatRuleIsNewlyActivated(activeRules, rule1, CRITICAL, Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.HIGH,
      SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.LOW));
    assertThatRuleIsNewlyActivated(activeRules, rule2, MAJOR, Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.MEDIUM));
    assertThatProfileIsMarkedAsUpdated(persistedProfile);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(persistedProfile.getLanguage()));
  }

  private static ImpactDto newImpactDto(SoftwareQuality maintainability, org.sonar.api.issue.impact.Severity high) {
    return new ImpactDto().setSoftwareQuality(maintainability).setSeverity(high);
  }

  @Test
  void already_activated_rule_is_updated_in_case_of_differences() {
    RuleDto rule = db.rules().insert(
      r -> r.setLanguage("xoo")
        .replaceAllDefaultImpacts(List.of(new ImpactDto().setSoftwareQuality(SoftwareQuality.MAINTAINABILITY).setSeverity(org.sonar.api.issue.impact.Severity.HIGH))));
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile("Sonar way", "xoo");
    newQp.activateRule(rule.getRepositoryKey(), rule.getRuleKey()).overrideSeverity(Severity.BLOCKER);
    newQp.done();
    BuiltInQProfile builtIn = builtInProfileRepository.create(context.profile("xoo", "Sonar way"), rule);

    activateRuleInDb(persistedProfile, rule, BLOCKER, Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.HIGH));

    List<ActiveRuleChange> changes = underTest.update(db.getSession(), builtIn, persistedProfile);

    List<ActiveRuleDto> activeRules = db.getDbClient().activeRuleDao().selectByRuleProfile(db.getSession(), persistedProfile);
    assertThat(activeRules).hasSize(1);
    assertThatRuleIsUpdated(activeRules, rule, BLOCKER, Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.BLOCKER));
    assertThatProfileIsMarkedAsUpdated(persistedProfile);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(persistedProfile.getLanguage()));
  }

  @Test
  void already_activated_rule_is_not_touched_if_no_differences() {
    RuleDto rule = db.rules().insert(r -> r.setLanguage("xoo"));
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile("Sonar way", "xoo");
    newQp.activateRule(rule.getRepositoryKey(), rule.getRuleKey()).overrideSeverity(Severity.CRITICAL);
    newQp.done();
    BuiltInQProfile builtIn = builtInProfileRepository.create(context.profile("xoo", "Sonar way"), rule);

    activateRuleInDb(persistedProfile, rule, CRITICAL, Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.HIGH));

    underTest.update(db.getSession(), builtIn, persistedProfile);

    List<ActiveRuleDto> activeRules = db.getDbClient().activeRuleDao().selectByRuleProfile(db.getSession(), persistedProfile);
    assertThat(activeRules).hasSize(1);
    assertThatRuleIsUntouched(activeRules, rule, CRITICAL);
    assertThatProfileIsNotMarkedAsUpdated(persistedProfile);
    verifyNoInteractions(qualityProfileChangeEventService);
  }

  @Test
  void deactivate_rule_that_is_not_in_built_in_definition_anymore() {
    RuleDto rule1 = db.rules().insert(r -> r.setLanguage("xoo"));
    RuleDto rule2 = db.rules().insert(r -> r.setLanguage("xoo"));
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile("Sonar way", "xoo");
    newQp.activateRule(rule2.getRepositoryKey(), rule2.getRuleKey()).overrideSeverity(Severity.MAJOR);
    newQp.done();
    BuiltInQProfile builtIn = builtInProfileRepository.create(context.profile("xoo", "Sonar way"), rule1, rule2);

    // built-in definition contains only rule2
    // so rule1 must be deactivated
    activateRuleInDb(persistedProfile, rule1, CRITICAL, Map.of());

    List<ActiveRuleChange> changes = underTest.update(db.getSession(), builtIn, persistedProfile);

    List<ActiveRuleDto> activeRules = db.getDbClient().activeRuleDao().selectByRuleProfile(db.getSession(), persistedProfile);
    assertThat(activeRules).hasSize(1);
    assertThatRuleIsDeactivated(activeRules, rule1);
    assertThatProfileIsMarkedAsUpdated(persistedProfile);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(persistedProfile.getLanguage()));
  }

  @Test
  void activate_deactivate_and_update_three_rules_at_the_same_time() {
    RuleDto rule1 = db.rules().insert(r -> r.setLanguage("xoo"));
    RuleDto rule2 = db.rules().insert(r -> r.setLanguage("xoo"));
    RuleDto rule3 = db.rules().insert(r -> r.setLanguage("xoo"));

    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile("Sonar way", "xoo");
    newQp.activateRule(rule1.getRepositoryKey(), rule1.getRuleKey()).overrideSeverity(Severity.CRITICAL);
    newQp.activateRule(rule2.getRepositoryKey(), rule2.getRuleKey()).overrideSeverity(Severity.MAJOR);
    newQp.done();
    BuiltInQProfile builtIn = builtInProfileRepository.create(context.profile("xoo", "Sonar way"), rule1, rule2);

    // rule1 must be updated (blocker to critical)
    // rule2 must be activated
    // rule3 must be deactivated
    activateRuleInDb(persistedProfile, rule1, BLOCKER, Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.HIGH));
    activateRuleInDb(persistedProfile, rule3, BLOCKER, Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.HIGH));

    List<ActiveRuleChange> changes = underTest.update(db.getSession(), builtIn, persistedProfile);

    List<ActiveRuleDto> activeRules = db.getDbClient().activeRuleDao().selectByRuleProfile(db.getSession(), persistedProfile);
    assertThat(activeRules).hasSize(2);
    assertThatRuleIsUpdated(activeRules, rule1, CRITICAL, Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.HIGH));
    assertThatRuleIsNewlyActivated(activeRules, rule2, MAJOR, Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.MEDIUM));
    assertThatRuleIsDeactivated(activeRules, rule3);
    assertThatProfileIsMarkedAsUpdated(persistedProfile);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(persistedProfile.getLanguage()));
  }

  // SONAR-10473
  @Test
  void activate_rule_on_built_in_profile_resets_severity_to_default_if_not_overridden() {
    RuleDto rule = db.rules().insert(r -> r.setSeverity(Severity.MAJOR).setLanguage("xoo"));

    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile("Sonar way", "xoo");
    newQp.activateRule(rule.getRepositoryKey(), rule.getRuleKey());
    newQp.done();
    BuiltInQProfile builtIn = builtInProfileRepository.create(context.profile("xoo", "Sonar way"), rule);
    underTest.update(db.getSession(), builtIn, persistedProfile);

    List<ActiveRuleDto> activeRules = db.getDbClient().activeRuleDao().selectByRuleProfile(db.getSession(), persistedProfile);
    assertThatRuleIsNewlyActivated(activeRules, rule, MAJOR, Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.HIGH));

    // emulate an upgrade of analyzer that changes the default severity of the rule
    rule.setSeverity(Severity.MINOR);
    rule.replaceAllDefaultImpacts(List.of(newImpactDto(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW)));
    db.rules().update(rule);

    List<ActiveRuleChange> changes = underTest.update(db.getSession(), builtIn, persistedProfile);
    activeRules = db.getDbClient().activeRuleDao().selectByRuleProfile(db.getSession(), persistedProfile);
    assertThatRuleIsNewlyActivated(activeRules, rule, MINOR, Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW));
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(persistedProfile.getLanguage()));
  }

  @Test
  void activate_rule_on_built_in_profile_resets_params_to_default_if_not_overridden() {
    RuleDto rule = db.rules().insert(r -> r.setLanguage("xoo"));
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue("10"));

    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile("Sonar way", rule.getLanguage());
    newQp.activateRule(rule.getRepositoryKey(), rule.getRuleKey());
    newQp.done();
    BuiltInQProfile builtIn = builtInProfileRepository.create(context.profile(newQp.language(), newQp.name()), rule);
    List<ActiveRuleChange> changes = underTest.update(db.getSession(), builtIn, persistedProfile);

    List<ActiveRuleDto> activeRules = db.getDbClient().activeRuleDao().selectByRuleProfile(db.getSession(), persistedProfile);
    assertThat(activeRules).hasSize(1);
    assertThatRuleHasParams(db, activeRules.get(0), tuple("min", "10"));
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(persistedProfile.getLanguage()));

    // emulate an upgrade of analyzer that changes the default value of parameter min
    ruleParam.setDefaultValue("20");
    db.getDbClient().ruleDao().updateRuleParam(db.getSession(), rule, ruleParam);

    changes = underTest.update(db.getSession(), builtIn, persistedProfile);
    activeRules = db.getDbClient().activeRuleDao().selectByRuleProfile(db.getSession(), persistedProfile);
    assertThat(activeRules).hasSize(1);
    assertThatRuleHasParams(db, activeRules.get(0), tuple("min", "20"));
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(persistedProfile.getLanguage()));
  }

  @Test
  void propagate_activation_to_descendant_profiles() {
    RuleDto rule = db.rules().insert(r -> r.setLanguage("xoo"));

    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(rule.getLanguage()).setIsBuiltIn(true));
    QProfileDto childProfile = createChildProfile(profile);
    QProfileDto grandchildProfile = createChildProfile(childProfile);

    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile(profile.getName(), profile.getLanguage());
    newQp.activateRule(rule.getRepositoryKey(), rule.getRuleKey());
    newQp.done();
    BuiltInQProfile builtIn = builtInProfileRepository.create(context.profile(profile.getLanguage(), profile.getName()), rule);
    List<ActiveRuleChange> changes = underTest.update(db.getSession(), builtIn, RulesProfileDto.from(profile));

    assertThat(changes).hasSize(3);
    assertThatRuleIsActivated(profile, rule, changes, rule.getSeverityString(), null, emptyMap());
    assertThatRuleIsActivated(childProfile, rule, changes, rule.getSeverityString(), INHERITED, emptyMap());
    assertThatRuleIsActivated(grandchildProfile, rule, changes, rule.getSeverityString(), INHERITED, emptyMap());
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(persistedProfile.getLanguage()));
  }

  @Test
  void deactivated_rule_should_not_be_activated_in_descendant_profiles() {
    RuleDto rule1 = db.rules().insert(r -> r.setLanguage("xoo"));
    RuleDto rule2 = db.rules().insert(r -> r.setLanguage("xoo"));

    QProfileDto parentProfile = db.qualityProfiles().insert(p -> p.setLanguage("xoo").setIsBuiltIn(true));
    activateRuleInDb(RulesProfileDto.from(parentProfile), rule1, RulePriority.valueOf(Severity.MINOR),
      Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW));
    activateRuleInDb(RulesProfileDto.from(parentProfile), rule2, RulePriority.valueOf(Severity.MINOR),
      Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW));

    // child profile has only rule1 activated, to simulate rule2 is deactivated
    QProfileDto childProfile = createChildProfile(parentProfile);
    activateRuleInDb(RulesProfileDto.from(childProfile), rule1, RulePriority.valueOf(Severity.MINOR),
      Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW), INHERITED);

    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile(parentProfile.getName(), parentProfile.getLanguage());
    newQp.activateRule(rule1.getRepositoryKey(), rule1.getRuleKey());
    newQp.activateRule(rule2.getRepositoryKey(), rule2.getRuleKey());
    newQp.done();
    BuiltInQProfile builtIn = builtInProfileRepository.create(context.profile(parentProfile.getLanguage(), parentProfile.getName()), rule1, rule2);
    List<ActiveRuleChange> changes = underTest.update(db.getSession(), builtIn, RulesProfileDto.from(parentProfile));

    assertThat(changes).hasSize(3);

    List<ActiveRuleDto> parentActiveRules = db.getDbClient().activeRuleDao().selectByRuleProfile(db.getSession(), RulesProfileDto.from(parentProfile));
    assertThatRuleIsUpdated(parentActiveRules, rule1, RulePriority.valueOfInt(rule1.getSeverity()), rule1.getDefaultImpactsMap());
    assertThatRuleIsUpdated(parentActiveRules, rule2, RulePriority.valueOfInt(rule2.getSeverity()), rule2.getDefaultImpactsMap());

    List<ActiveRuleDto> childActiveRules = db.getDbClient().activeRuleDao().selectByRuleProfile(db.getSession(), RulesProfileDto.from(childProfile));
    assertThatRuleIsUpdated(childActiveRules, rule1, RulePriority.valueOfInt(rule1.getSeverity()), rule1.getDefaultImpactsMap(), INHERITED);
  }

  // SONAR-14559
  @Test
  void propagate_rule_update_to_descendant_active_rule() {
    RuleDto rule = db.rules().insert(r -> r.setLanguage("xoo").setSeverity(Severity.BLOCKER)
      .replaceAllDefaultImpacts(List.of(newImpactDto(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.BLOCKER))));

    QProfileDto parentProfile = db.qualityProfiles().insert(p -> p.setLanguage(rule.getLanguage()).setIsBuiltIn(true));
    activateRuleInDb(RulesProfileDto.from(parentProfile), rule, RulePriority.valueOf(Severity.MINOR),
      Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW));

    QProfileDto childProfile = createChildProfile(parentProfile);
    activateRuleInDb(RulesProfileDto.from(childProfile), rule, RulePriority.valueOf(Severity.MINOR),
      Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW), INHERITED);

    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile(parentProfile.getName(), parentProfile.getLanguage());
    newQp.activateRule(rule.getRepositoryKey(), rule.getRuleKey());
    newQp.done();
    BuiltInQProfile builtIn = builtInProfileRepository.create(context.profile(parentProfile.getLanguage(), parentProfile.getName()), rule);
    List<ActiveRuleChange> changes = underTest.update(db.getSession(), builtIn, RulesProfileDto.from(parentProfile));

    assertThat(changes).hasSize(2);

    List<ActiveRuleDto> parentActiveRules = db.getDbClient().activeRuleDao().selectByRuleProfile(db.getSession(), RulesProfileDto.from(parentProfile));
    assertThatRuleIsUpdated(parentActiveRules, rule, RulePriority.BLOCKER, Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.BLOCKER));

    List<ActiveRuleDto> childActiveRules = db.getDbClient().activeRuleDao().selectByRuleProfile(db.getSession(), RulesProfileDto.from(childProfile));
    assertThatRuleIsUpdated(childActiveRules, rule, RulePriority.BLOCKER, Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.BLOCKER), INHERITED);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(persistedProfile.getLanguage()));
  }

  @Test
  void propagate_rule_param_update_to_descendant_active_rule_params() {
    RuleDto rule = db.rules().insert(r -> r.setLanguage("xoo").setSeverity(Severity.BLOCKER));
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue("10"));

    QProfileDto parentProfile = db.qualityProfiles().insert(p -> p.setLanguage(rule.getLanguage()).setIsBuiltIn(true));
    ActiveRuleDto parentActiveRuleDto = activateRuleInDb(RulesProfileDto.from(parentProfile), rule,
      RulePriority.valueOf(Severity.MINOR), Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.HIGH));
    activateRuleParamInDb(parentActiveRuleDto, ruleParam, "20");

    QProfileDto childProfile = createChildProfile(parentProfile);
    ActiveRuleDto childActiveRuleDto = activateRuleInDb(RulesProfileDto.from(childProfile), rule,
      RulePriority.valueOf(Severity.MINOR), Map.of(), INHERITED);
    activateRuleParamInDb(childActiveRuleDto, ruleParam, "20");

    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile(parentProfile.getName(), parentProfile.getLanguage());
    newQp.activateRule(rule.getRepositoryKey(), rule.getRuleKey());
    newQp.done();
    BuiltInQProfile builtIn = builtInProfileRepository.create(context.profile(parentProfile.getLanguage(), parentProfile.getName()), rule);
    List<ActiveRuleChange> changes = underTest.update(db.getSession(), builtIn, RulesProfileDto.from(parentProfile));

    assertThat(changes).hasSize(2);

    assertThatRuleHasParams(db, parentActiveRuleDto, tuple("min", "10"));
    assertThatRuleHasParams(db, childActiveRuleDto, tuple("min", "10"));
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(persistedProfile.getLanguage()));
  }

  @Test
  void propagate_deactivation_to_descendant_profiles() {
    RuleDto rule = db.rules().insert(r -> r.setLanguage("xoo"));

    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(rule.getLanguage()).setIsBuiltIn(true));
    QProfileDto childProfile = createChildProfile(profile);
    QProfileDto grandChildProfile = createChildProfile(childProfile);

    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile(profile.getName(), profile.getLanguage());
    newQp.activateRule(rule.getRepositoryKey(), rule.getRuleKey());
    newQp.done();

    // first run to activate the rule
    BuiltInQProfile builtIn = builtInProfileRepository.create(context.profile(profile.getLanguage(), profile.getName()), rule);
    List<ActiveRuleChange> changes = underTest.update(db.getSession(), builtIn, RulesProfileDto.from(profile));
    assertThat(changes).hasSize(3).extracting(ActiveRuleChange::getType).containsOnly(ActiveRuleChange.Type.ACTIVATED);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(persistedProfile.getLanguage()));

    // second run to deactivate the rule
    context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile updatedQp = context.createBuiltInQualityProfile(profile.getName(), profile.getLanguage());
    updatedQp.done();
    builtIn = builtInProfileRepository.create(context.profile(profile.getLanguage(), profile.getName()), rule);
    changes = underTest.update(db.getSession(), builtIn, RulesProfileDto.from(profile));
    assertThat(changes).hasSize(3).extracting(ActiveRuleChange::getType).containsOnly(ActiveRuleChange.Type.DEACTIVATED);
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), eq(changes), eq(persistedProfile.getLanguage()));

    assertThatRuleIsDeactivated(profile, rule);
    assertThatRuleIsDeactivated(childProfile, rule);
    assertThatRuleIsDeactivated(grandChildProfile, rule);
  }

  private QProfileDto createChildProfile(QProfileDto parent) {
    return db.qualityProfiles().insert(p -> p
      .setLanguage(parent.getLanguage())
      .setParentKee(parent.getKee())
      .setName("Child of " + parent.getName()))
      .setIsBuiltIn(false);
  }

  private void assertThatRuleIsActivated(QProfileDto profile, RuleDto rule, @Nullable List<ActiveRuleChange> changes,
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

    if (changes != null) {
      ActiveRuleChange change = changes.stream()
        .filter(c -> c.getActiveRule().getUuid().equals(activeRule.getUuid()))
        .findFirst().orElseThrow(IllegalStateException::new);
      assertThat(change.getInheritance()).isEqualTo(expectedInheritance);
      assertThat(change.getSeverity()).isEqualTo(expectedSeverity);
      assertThat(change.getType()).isEqualTo(ActiveRuleChange.Type.ACTIVATED);
    }
  }

  private static void assertThatRuleHasParams(DbTester db, ActiveRuleDto activeRule, Tuple... expectedParams) {
    assertThat(db.getDbClient().activeRuleDao().selectParamsByActiveRuleUuid(db.getSession(), activeRule.getUuid()))
      .extracting(ActiveRuleParamDto::getKey, ActiveRuleParamDto::getValue)
      .containsExactlyInAnyOrder(expectedParams);
  }

  private static void assertThatRuleIsNewlyActivated(List<ActiveRuleDto> activeRules, RuleDto rule, RulePriority severity,
    Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts) {
    ActiveRuleDto activeRule = findRule(activeRules, rule).get();

    assertThat(activeRule.getInheritance()).isNull();
    assertThat(activeRule.getSeverityString()).isEqualTo(severity.name());
    assertThat(activeRule.getImpacts()).isEqualTo(impacts);
    assertThat(activeRule.getCreatedAt()).isEqualTo(NOW);
    assertThat(activeRule.getUpdatedAt()).isEqualTo(NOW);
  }

  private static void assertThatRuleIsUpdated(List<ActiveRuleDto> activeRules, RuleDto rule, RulePriority severity,
    Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts, @Nullable ActiveRuleInheritance expectedInheritance) {
    ActiveRuleDto activeRule = findRule(activeRules, rule).get();

    if (expectedInheritance != null) {
      assertThat(activeRule.getInheritance()).isEqualTo(expectedInheritance.name());
    } else {
      assertThat(activeRule.getInheritance()).isNull();
    }
    assertThat(activeRule.getSeverityString()).isEqualTo(severity.name());
    assertThat(activeRule.getImpacts()).isEqualTo(impacts);
    assertThat(activeRule.getCreatedAt()).isEqualTo(PAST);
    assertThat(activeRule.getUpdatedAt()).isEqualTo(NOW);
  }

  private static void assertThatRuleIsUpdated(List<ActiveRuleDto> activeRules, RuleDto rule, RulePriority severity,
    Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts) {
    assertThatRuleIsUpdated(activeRules, rule, severity, impacts, null);
  }

  private static void assertThatRuleIsUntouched(List<ActiveRuleDto> activeRules, RuleDto rule, RulePriority severity) {
    ActiveRuleDto activeRule = findRule(activeRules, rule).get();

    assertThat(activeRule.getInheritance()).isNull();
    assertThat(activeRule.getSeverityString()).isEqualTo(severity.name());
    assertThat(activeRule.getCreatedAt()).isEqualTo(PAST);
    assertThat(activeRule.getUpdatedAt()).isEqualTo(PAST);
  }

  private void assertThatRuleIsDeactivated(QProfileDto profile, RuleDto rule) {
    Collection<ActiveRuleDto> activeRules = db.getDbClient().activeRuleDao().selectByRulesAndRuleProfileUuids(
      db.getSession(), singletonList(rule.getUuid()), singletonList(profile.getRulesProfileUuid()));
    assertThat(activeRules).isEmpty();
  }

  private static void assertThatRuleIsDeactivated(List<ActiveRuleDto> activeRules, RuleDto rule) {
    assertThat(findRule(activeRules, rule)).isEmpty();
  }

  private void assertThatProfileIsMarkedAsUpdated(RulesProfileDto dto) {
    RulesProfileDto reloaded = db.getDbClient().qualityProfileDao().selectBuiltInRuleProfiles(db.getSession())
      .stream()
      .filter(p -> p.getUuid().equals(dto.getUuid()))
      .findFirst()
      .get();
    assertThat(reloaded.getRulesUpdatedAt()).isNotEmpty();
  }

  private void assertThatProfileIsNotMarkedAsUpdated(RulesProfileDto dto) {
    RulesProfileDto reloaded = db.getDbClient().qualityProfileDao().selectBuiltInRuleProfiles(db.getSession())
      .stream()
      .filter(p -> p.getUuid().equals(dto.getUuid()))
      .findFirst()
      .get();
    assertThat(reloaded.getRulesUpdatedAt()).isNull();
  }

  private static Optional<ActiveRuleDto> findRule(List<ActiveRuleDto> activeRules, RuleDto rule) {
    return activeRules.stream()
      .filter(ar -> ar.getRuleKey().equals(rule.getKey()))
      .findFirst();
  }

  private ActiveRuleDto activateRuleInDb(RulesProfileDto profile, RuleDto rule, RulePriority severity, Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts) {
    return activateRuleInDb(profile, rule, severity, impacts, null);
  }

  private ActiveRuleDto activateRuleInDb(RulesProfileDto ruleProfile, RuleDto rule, RulePriority severity, Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts,
    @Nullable ActiveRuleInheritance inheritance) {
    ActiveRuleDto dto = new ActiveRuleDto()
      .setKey(ActiveRuleKey.of(ruleProfile, RuleKey.of(rule.getRepositoryKey(), rule.getRuleKey())))
      .setProfileUuid(ruleProfile.getUuid())
      .setSeverity(severity.name())
      .setImpacts(impacts)
      .setRuleUuid(rule.getUuid())
      .setInheritance(inheritance != null ? inheritance.name() : null)
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST);
    db.getDbClient().activeRuleDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  private void activateRuleParamInDb(ActiveRuleDto activeRuleDto, RuleParamDto ruleParamDto, String value) {
    ActiveRuleParamDto dto = new ActiveRuleParamDto()
      .setActiveRuleUuid(activeRuleDto.getUuid())
      .setRulesParameterUuid(ruleParamDto.getUuid())
      .setKey(ruleParamDto.getName())
      .setValue(value);
    db.getDbClient().activeRuleDao().insertParam(db.getSession(), activeRuleDto, dto);
    db.commit();
  }
}
