/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.config.Configuration;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileChangeQuery;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.db.rule.RuleChangeDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleImpactChangeDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.ActiveRuleInheritance;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.builtin.DescendantProfilesSupplier.Result;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.IntegerTypeValidation;
import org.sonar.server.util.StringTypeValidation;
import org.sonar.server.util.TypeValidations;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Map.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.sonar.api.issue.impact.SoftwareQuality.MAINTAINABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.RELIABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;
import static org.sonar.server.qualityprofile.ActiveRuleInheritance.INHERITED;
import static org.sonar.server.qualityprofile.ActiveRuleInheritance.OVERRIDES;

/**
 * Class org.sonar.server.qualityprofile.builtin.RuleActivator is mostly covered in
 * org.sonar.server.qualityprofile.builtin.BuiltInQProfileUpdateImplIT
 */
class RuleActivatorIT {
  @RegisterExtension
  public final DbTester db = DbTester.create();

  @RegisterExtension
  public final UserSessionRule userSession = UserSessionRule.standalone();

  private static final long NOW = 1_000;
  private static final long PAST = NOW - 100;
  private final System2 system2 = new TestSystem2().setNow(NOW);
  private final TypeValidations typeValidations = new TypeValidations(asList(new StringTypeValidation(), new IntegerTypeValidation()));
  private final SonarQubeVersion sonarQubeVersion = new SonarQubeVersion(Version.create(10, 3));
  private final RuleActivator underTest = new RuleActivator(system2, db.getDbClient(), UuidFactoryImpl.INSTANCE, typeValidations, userSession,
    mock(Configuration.class), sonarQubeVersion);

  @Test
  void reset_overridden_active_rule() {
    RuleDto rule = db.rules().insert(r -> r.setLanguage("xoo").setSeverity(Severity.BLOCKER));
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue("10"));

    QProfileDto parentProfile = db.qualityProfiles().insert(p -> p.setLanguage(rule.getLanguage()).setIsBuiltIn(true));
    ActiveRuleDto parentActiveRuleDto = activateRuleInDb(RulesProfileDto.from(parentProfile), rule,
      RulePriority.valueOf(Severity.BLOCKER), Map.of(MAINTAINABILITY, org.sonar.api.issue.impact.Severity.HIGH), false, null);
    ActiveRuleParamDto parentActiveRuleParam = activateRuleParamInDb(parentActiveRuleDto, ruleParam, "10");

    QProfileDto childProfile = createChildProfile(parentProfile);
    ActiveRuleDto childActiveRuleDto = activateRuleInDb(RulesProfileDto.from(childProfile), rule,
      RulePriority.valueOf(Severity.MINOR), Map.of(MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW), true, OVERRIDES);
    ActiveRuleParamDto childActiveRuleParam = activateRuleParamInDb(childActiveRuleDto, ruleParam, "15");

    DbSession session = db.getSession();
    RuleActivation resetRequest = RuleActivation.createReset(rule.getUuid());
    RuleActivationContext context = new RuleActivationContext.Builder()
      .setProfiles(asList(parentProfile, childProfile))
      .setBaseProfile(RulesProfileDto.from(childProfile))
      .setDate(NOW)
      .setDescendantProfilesSupplier((profiles, ruleUuids) -> new Result(emptyList(), emptyList(), emptyList()))
      .setRules(singletonList(rule))
      .setRuleParams(singletonList(ruleParam))
      .setActiveRules(asList(parentActiveRuleDto, childActiveRuleDto))
      .setActiveRuleParams(asList(parentActiveRuleParam, childActiveRuleParam))
      .build();

    List<ActiveRuleChange> result = underTest.activate(session, resetRequest, context);

    assertThat(result).hasSize(1);
    ActiveRuleChange activeRuleResult = result.get(0);
    assertThat(activeRuleResult.getParameters()).containsEntry("min", "10");
    assertThat(activeRuleResult.getSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(activeRuleResult.getNewImpacts()).isEqualTo(Map.of(MAINTAINABILITY, org.sonar.api.issue.impact.Severity.HIGH));
    assertThat(activeRuleResult.isPrioritizedRule()).isFalse();
    assertThat(activeRuleResult.getInheritance()).isEqualTo(ActiveRuleInheritance.INHERITED);
  }

  @Test
  void request_new_severity_and_prioritized_rule_and_param_for_child_rule() {
    RuleDto rule = db.rules().insert(r -> r.setLanguage("xoo").setSeverity(Severity.BLOCKER)
      .replaceAllDefaultImpacts(List.of(newImpactDto(MAINTAINABILITY, org.sonar.api.issue.impact.Severity.BLOCKER))));
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue("10"));

    QProfileDto parentProfile = db.qualityProfiles().insert(p -> p.setLanguage(rule.getLanguage()).setIsBuiltIn(true));
    ActiveRuleDto parentActiveRuleDto = activateRuleInDb(RulesProfileDto.from(parentProfile), rule,
      RulePriority.valueOf(Severity.BLOCKER), Map.of(MAINTAINABILITY, org.sonar.api.issue.impact.Severity.BLOCKER), null, null);
    ActiveRuleParamDto parentActiveRuleParam = activateRuleParamInDb(parentActiveRuleDto, ruleParam, "10");

    QProfileDto childProfile = createChildProfile(parentProfile);
    ActiveRuleDto childActiveRuleDto = activateRuleInDb(RulesProfileDto.from(childProfile), rule,
      RulePriority.valueOf(Severity.BLOCKER), Map.of(MAINTAINABILITY, org.sonar.api.issue.impact.Severity.BLOCKER), null, INHERITED);
    ActiveRuleParamDto childActiveRuleParam = activateRuleParamInDb(childActiveRuleDto, ruleParam, "10");

    DbSession session = db.getSession();
    RuleActivation resetRequest = RuleActivation.create(rule.getUuid(), Severity.MINOR, true, of("min", "15"));
    RuleActivationContext context = new RuleActivationContext.Builder()
      .setProfiles(asList(parentProfile, childProfile))
      .setBaseProfile(RulesProfileDto.from(childProfile))
      .setDate(NOW)
      .setDescendantProfilesSupplier((profiles, ruleUuids) -> new Result(emptyList(), emptyList(), emptyList()))
      .setRules(singletonList(rule))
      .setRuleParams(singletonList(ruleParam))
      .setActiveRules(asList(parentActiveRuleDto, childActiveRuleDto))
      .setActiveRuleParams(asList(parentActiveRuleParam, childActiveRuleParam))
      .build();

    List<ActiveRuleChange> result = underTest.activate(session, resetRequest, context);

    assertThat(result).hasSize(1);
    ActiveRuleChange activeRuleResult = result.get(0);
    assertThat(activeRuleResult.getParameters()).containsEntry("min", "15");
    assertThat(activeRuleResult.getSeverity()).isEqualTo(Severity.MINOR);
    assertThat(activeRuleResult.getNewImpacts()).containsExactlyEntriesOf(Map.of(MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW));
    assertThat(activeRuleResult.isPrioritizedRule()).isTrue();
    assertThat(activeRuleResult.getInheritance()).isEqualTo(OVERRIDES);
  }

  @Test
  void activate_whenOnlyOneImpactAndImpactDoesntMatchRuleType_shouldNotOverrideSeverity() {
    RuleDto rule = db.rules().insert(r -> r.setLanguage("xoo").setSeverity(Severity.BLOCKER)
      .replaceAllDefaultImpacts(List.of(newImpactDto(SECURITY, org.sonar.api.issue.impact.Severity.BLOCKER))));
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue("10"));

    QProfileDto parentProfile = db.qualityProfiles().insert(p -> p.setLanguage(rule.getLanguage()).setIsBuiltIn(true));
    ActiveRuleDto parentActiveRuleDto = activateRuleInDb(RulesProfileDto.from(parentProfile), rule,
      RulePriority.valueOf(Severity.BLOCKER), Map.of(SECURITY, org.sonar.api.issue.impact.Severity.BLOCKER), null, null);
    ActiveRuleParamDto parentActiveRuleParam = activateRuleParamInDb(parentActiveRuleDto, ruleParam, "10");

    QProfileDto childProfile = createChildProfile(parentProfile);
    ActiveRuleDto childActiveRuleDto = activateRuleInDb(RulesProfileDto.from(childProfile), rule,
      RulePriority.valueOf(Severity.BLOCKER), Map.of(SECURITY, org.sonar.api.issue.impact.Severity.BLOCKER), null, INHERITED);
    ActiveRuleParamDto childActiveRuleParam = activateRuleParamInDb(childActiveRuleDto, ruleParam, "10");

    DbSession session = db.getSession();
    RuleActivation request = RuleActivation.createOverrideImpacts(rule.getUuid(), Map.of(SECURITY, org.sonar.api.issue.impact.Severity.LOW), of("min", "15"));
    RuleActivationContext context = new RuleActivationContext.Builder()
      .setProfiles(asList(parentProfile, childProfile))
      .setBaseProfile(RulesProfileDto.from(childProfile))
      .setDate(NOW)
      .setDescendantProfilesSupplier((profiles, ruleUuids) -> new Result(emptyList(), emptyList(), emptyList()))
      .setRules(singletonList(rule))
      .setRuleParams(singletonList(ruleParam))
      .setActiveRules(asList(parentActiveRuleDto, childActiveRuleDto))
      .setActiveRuleParams(asList(parentActiveRuleParam, childActiveRuleParam))
      .build();

    List<ActiveRuleChange> result = underTest.activate(session, request, context);

    assertThat(result).hasSize(1);
    ActiveRuleChange activeRuleResult = result.get(0);
    assertThat(activeRuleResult.getSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(activeRuleResult.getNewImpacts()).containsExactlyEntriesOf(Map.of(SECURITY, org.sonar.api.issue.impact.Severity.LOW));
    assertThat(activeRuleResult.getInheritance()).isEqualTo(OVERRIDES);
  }

  @Test
  void activate_whenTwoImpactsAndImpactsDoesntMatchRuleType_shouldNotOverrideSeverity() {
    RuleDto rule = db.rules().insert(r -> r.setLanguage("xoo").setSeverity(Severity.BLOCKER)
      .replaceAllDefaultImpacts(
        List.of(newImpactDto(SECURITY, org.sonar.api.issue.impact.Severity.BLOCKER), new ImpactDto(RELIABILITY, org.sonar.api.issue.impact.Severity.BLOCKER))));
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue("10"));

    QProfileDto parentProfile = db.qualityProfiles().insert(p -> p.setLanguage(rule.getLanguage()).setIsBuiltIn(true));
    ActiveRuleDto parentActiveRuleDto = activateRuleInDb(RulesProfileDto.from(parentProfile), rule,
      RulePriority.valueOf(Severity.BLOCKER), Map.of(SECURITY, org.sonar.api.issue.impact.Severity.BLOCKER, RELIABILITY, org.sonar.api.issue.impact.Severity.BLOCKER), null, null);
    ActiveRuleParamDto parentActiveRuleParam = activateRuleParamInDb(parentActiveRuleDto, ruleParam, "10");

    QProfileDto childProfile = createChildProfile(parentProfile);
    ActiveRuleDto childActiveRuleDto = activateRuleInDb(RulesProfileDto.from(childProfile), rule,
      RulePriority.valueOf(Severity.BLOCKER), Map.of(SECURITY, org.sonar.api.issue.impact.Severity.BLOCKER, RELIABILITY, org.sonar.api.issue.impact.Severity.BLOCKER), null,
      INHERITED);
    ActiveRuleParamDto childActiveRuleParam = activateRuleParamInDb(childActiveRuleDto, ruleParam, "10");

    DbSession session = db.getSession();
    RuleActivation request = RuleActivation.createOverrideImpacts(rule.getUuid(),
      Map.of(SECURITY, org.sonar.api.issue.impact.Severity.LOW, RELIABILITY, org.sonar.api.issue.impact.Severity.LOW), of("min", "15"));
    RuleActivationContext context = new RuleActivationContext.Builder()
      .setProfiles(asList(parentProfile, childProfile))
      .setBaseProfile(RulesProfileDto.from(childProfile))
      .setDate(NOW)
      .setDescendantProfilesSupplier((profiles, ruleUuids) -> new Result(emptyList(), emptyList(), emptyList()))
      .setRules(singletonList(rule))
      .setRuleParams(singletonList(ruleParam))
      .setActiveRules(asList(parentActiveRuleDto, childActiveRuleDto))
      .setActiveRuleParams(asList(parentActiveRuleParam, childActiveRuleParam))
      .build();

    List<ActiveRuleChange> result = underTest.activate(session, request, context);

    assertThat(result).hasSize(1);
    ActiveRuleChange activeRuleResult = result.get(0);
    assertThat(activeRuleResult.getSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(activeRuleResult.getNewImpacts())
      .containsExactlyInAnyOrderEntriesOf(Map.of(SECURITY, org.sonar.api.issue.impact.Severity.LOW, RELIABILITY, org.sonar.api.issue.impact.Severity.LOW));
    assertThat(activeRuleResult.getInheritance()).isEqualTo(OVERRIDES);
  }

  private static ImpactDto newImpactDto(SoftwareQuality security, org.sonar.api.issue.impact.Severity severity) {
    return new ImpactDto().setSoftwareQuality(security).setSeverity(severity);
  }

  @Test
  void activate_whenImpactSeveritiesIsOverridden_shouldMapToRuleSeverity() {
    RuleDto rule = db.rules().insert(r -> r.setLanguage("xoo").setSeverity(Severity.BLOCKER)
      .replaceAllDefaultImpacts(List.of(newImpactDto(MAINTAINABILITY, org.sonar.api.issue.impact.Severity.BLOCKER))));
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue("10"));

    QProfileDto parentProfile = db.qualityProfiles().insert(p -> p.setLanguage(rule.getLanguage()).setIsBuiltIn(true));
    ActiveRuleDto parentActiveRuleDto = activateRuleInDb(RulesProfileDto.from(parentProfile), rule,
      RulePriority.valueOf(Severity.BLOCKER), Map.of(MAINTAINABILITY, org.sonar.api.issue.impact.Severity.BLOCKER), null, null);
    ActiveRuleParamDto parentActiveRuleParam = activateRuleParamInDb(parentActiveRuleDto, ruleParam, "10");

    QProfileDto childProfile = createChildProfile(parentProfile);
    ActiveRuleDto childActiveRuleDto = activateRuleInDb(RulesProfileDto.from(childProfile), rule,
      RulePriority.valueOf(Severity.BLOCKER), Map.of(MAINTAINABILITY, org.sonar.api.issue.impact.Severity.BLOCKER), null, INHERITED);
    ActiveRuleParamDto childActiveRuleParam = activateRuleParamInDb(childActiveRuleDto, ruleParam, "10");

    DbSession session = db.getSession();
    RuleActivation request = RuleActivation.createOverrideImpacts(rule.getUuid(), Map.of(MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW), of("min", "15"));
    RuleActivationContext context = new RuleActivationContext.Builder()
      .setProfiles(asList(parentProfile, childProfile))
      .setBaseProfile(RulesProfileDto.from(childProfile))
      .setDate(NOW)
      .setDescendantProfilesSupplier((profiles, ruleUuids) -> new Result(emptyList(), emptyList(), emptyList()))
      .setRules(singletonList(rule))
      .setRuleParams(singletonList(ruleParam))
      .setActiveRules(asList(parentActiveRuleDto, childActiveRuleDto))
      .setActiveRuleParams(asList(parentActiveRuleParam, childActiveRuleParam))
      .build();

    List<ActiveRuleChange> result = underTest.activate(session, request, context);

    assertThat(result).hasSize(1);
    ActiveRuleChange activeRuleResult = result.get(0);
    assertThat(activeRuleResult.getSeverity()).isEqualTo(Severity.MINOR);
    assertThat(activeRuleResult.getNewImpacts()).containsExactlyEntriesOf(Map.of(MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW));
    assertThat(activeRuleResult.getInheritance()).isEqualTo(OVERRIDES);

    List<QProfileChangeDto> qProfileChangeDtos = db.getDbClient().qProfileChangeDao().selectByQuery(session, new QProfileChangeQuery(childProfile.getKee()));
    assertThat(qProfileChangeDtos).hasSize(1);
    assertThat(qProfileChangeDtos.get(0).getChangeType()).isEqualTo("UPDATED");
    assertThat(qProfileChangeDtos.get(0).getDataAsMap()).containsEntry("severity", Severity.MINOR);
    RuleChangeDto ruleChange = qProfileChangeDtos.get(0).getRuleChange();
    RuleImpactChangeDto expected = new RuleImpactChangeDto(MAINTAINABILITY, MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW, org.sonar.api.issue.impact.Severity.BLOCKER);
    expected.setRuleChangeUuid(ruleChange.getUuid());
    assertThat(ruleChange.getRuleImpactChanges()).containsExactly(expected);
  }

  @Test
  void set_severity_and_param_for_child_rule_when_activating() {
    RuleDto rule = db.rules().insert(r -> r.setLanguage("xoo").setSeverity(Severity.BLOCKER));
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule, p -> p.setName("min").setDefaultValue("10"));

    QProfileDto parentProfile = db.qualityProfiles().insert(p -> p.setLanguage(rule.getLanguage()).setIsBuiltIn(true));

    QProfileDto childProfile = createChildProfile(parentProfile);

    DbSession session = db.getSession();
    RuleActivation resetRequest = RuleActivation.create(rule.getUuid());
    RuleActivationContext context = new RuleActivationContext.Builder()
      .setProfiles(asList(parentProfile, childProfile))
      .setBaseProfile(RulesProfileDto.from(childProfile))
      .setDate(NOW)
      .setDescendantProfilesSupplier((profiles, ruleUuids) -> new Result(emptyList(), emptyList(), emptyList()))
      .setRules(singletonList(rule))
      .setRuleParams(singletonList(ruleParam))
      .setActiveRules(emptyList())
      .setActiveRuleParams(emptyList())
      .build();

    List<ActiveRuleChange> result = underTest.activate(session, resetRequest, context);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getParameters()).containsEntry("min", "10");
    assertThat(result.get(0).getSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(result.get(0).getNewImpacts()).containsExactlyEntriesOf(Map.of(MAINTAINABILITY, org.sonar.api.issue.impact.Severity.HIGH));
    assertThat(result.get(0).getInheritance()).isNull();
  }

  @Test
  void fail_if_rule_language_doesnt_match_qp() {
    RuleDto rule = db.rules().insert(r -> r.setLanguage("xoo")
      .setRepositoryKey("repo")
      .setRuleKey("rule")
      .setSeverity(Severity.BLOCKER));
    QProfileDto qp = db.qualityProfiles().insert(p -> p.setLanguage("xoo2").setKee("qp").setIsBuiltIn(true));

    DbSession session = db.getSession();
    RuleActivation resetRequest = RuleActivation.create(rule.getUuid());
    RuleActivationContext context = new RuleActivationContext.Builder()
      .setProfiles(singletonList(qp))
      .setBaseProfile(RulesProfileDto.from(qp))
      .setDate(NOW)
      .setDescendantProfilesSupplier((profiles, ruleUuids) -> new Result(emptyList(), emptyList(), emptyList()))
      .setRules(singletonList(rule))
      .setRuleParams(emptyList())
      .setActiveRules(emptyList())
      .setActiveRuleParams(emptyList())
      .build();

    assertThrows("xoo rule repo:rule cannot be activated on xoo2 profile qp", BadRequestException.class,
      () -> underTest.activate(session, resetRequest, context));
  }

  @Test
  void testActivate_whenProfileIsNotBuiltInAndRuleUuidInPreviousBuiltinActiveRuleUuids_shouldContainNoChange() {
    QProfileDto builtinProfile = db.qualityProfiles().insert(p -> p.setIsBuiltIn(true).setLanguage("xoo"));
    QProfileDto customProfile = createChildProfile(builtinProfile);

    RuleDto rule = db.rules().insert(r -> r.setLanguage("xoo").setSeverity(Severity.CRITICAL));
    ActiveRuleDto builtinActiveRuleDto = activateRuleInDb(RulesProfileDto.from(builtinProfile), rule,
      RulePriority.valueOf(Severity.CRITICAL), Map.of(MAINTAINABILITY, org.sonar.api.issue.impact.Severity.HIGH), null, null);

    RuleActivationContext context = new RuleActivationContext.Builder()
      .setProfiles(List.of(builtinProfile, customProfile))
      .setBaseProfile(RulesProfileDto.from(builtinProfile))
      .setPreviousBuiltinActiveRuleUuids(Set.of(rule.getUuid()))
      .setDescendantProfilesSupplier((profiles, ruleUuids) -> new Result(emptyList(), emptyList(), emptyList()))
      .setRules(singletonList(rule))
      .setRuleParams(emptyList())
      .setActiveRules(List.of(builtinActiveRuleDto))
      .setActiveRuleParams(emptyList())
      .build();

    RuleActivation activation = RuleActivation.create(rule.getUuid());

    List<ActiveRuleChange> result = underTest.activate(db.getSession(), activation, context);
    assertThat(result).isEmpty();
  }

  @Test
  void testActivate_whenProfileIsNotBuiltInAndRuleUuidNotInPreviousBuiltinActiveRuleUuids_shouldContainActivation() {
    QProfileDto builtinProfile = db.qualityProfiles().insert(p -> p.setIsBuiltIn(true).setLanguage("xoo"));
    QProfileDto customProfile = createChildProfile(builtinProfile);

    RuleDto rule = db.rules().insert(r -> r.setLanguage("xoo").setSeverity(Severity.CRITICAL));
    ActiveRuleDto builtinActiveRuleDto = activateRuleInDb(RulesProfileDto.from(builtinProfile), rule,
      RulePriority.valueOf(Severity.CRITICAL), Map.of(MAINTAINABILITY, org.sonar.api.issue.impact.Severity.HIGH), null, null);

    RuleActivationContext context = new RuleActivationContext.Builder()
      .setProfiles(List.of(builtinProfile, customProfile))
      .setBaseProfile(RulesProfileDto.from(builtinProfile))
      .setPreviousBuiltinActiveRuleUuids(Set.of("another-rule-uuid"))
      .setDescendantProfilesSupplier((profiles, ruleUuids) -> new Result(emptyList(), emptyList(), emptyList()))
      .setRules(singletonList(rule))
      .setRuleParams(emptyList())
      .setActiveRules(List.of(builtinActiveRuleDto))
      .setActiveRuleParams(emptyList())
      .build();

    RuleActivation activation = RuleActivation.create(rule.getUuid());

    List<ActiveRuleChange> result = underTest.activate(db.getSession(), activation, context);
    assertThat(result)
      .hasSize(1)
      .extracting(ActiveRuleChange::getRuleUuid, ActiveRuleChange::getKey)
      .containsExactlyInAnyOrder(tuple(rule.getUuid(), ActiveRuleKey.of(customProfile, RuleKey.of(rule.getRepositoryKey(), rule.getRuleKey()))));
  }

  @Test
  void testActivate_whenProfileIsBuiltIn_shouldContainActivation() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setIsBuiltIn(true).setLanguage("xoo"));
    RuleDto rule = db.rules().insert(r -> r.setLanguage("xoo"));
    RuleActivationContext context = new RuleActivationContext.Builder()
      .setProfiles(List.of(profile))
      .setBaseProfile(RulesProfileDto.from(profile))
      .setPreviousBuiltinActiveRuleUuids(Set.of(rule.getUuid()))
      .setDescendantProfilesSupplier((profiles, ruleUuids) -> new Result(emptyList(), emptyList(), emptyList()))
      .setRules(singletonList(rule))
      .setRuleParams(emptyList())
      .setActiveRules(emptyList())
      .setActiveRuleParams(emptyList())
      .build();

    RuleActivation activation = RuleActivation.create(rule.getUuid());

    List<ActiveRuleChange> result = underTest.activate(db.getSession(), activation, context);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getRuleUuid()).isEqualTo(rule.getUuid());
  }

  private ActiveRuleDto activateRuleInDb(RulesProfileDto ruleProfile, RuleDto rule, RulePriority severity, Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts,
    @Nullable Boolean prioritizedRule, @Nullable ActiveRuleInheritance inheritance) {
    ActiveRuleDto dto = new ActiveRuleDto()
      .setKey(ActiveRuleKey.of(ruleProfile, RuleKey.of(rule.getRepositoryKey(), rule.getRuleKey())))
      .setProfileUuid(ruleProfile.getUuid())
      .setSeverity(severity.name())
      .setImpacts(impacts)
      .setPrioritizedRule(TRUE.equals(prioritizedRule))
      .setRuleUuid(rule.getUuid())
      .setInheritance(inheritance != null ? inheritance.name() : null)
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST);
    db.getDbClient().activeRuleDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  private ActiveRuleParamDto activateRuleParamInDb(ActiveRuleDto activeRuleDto, RuleParamDto ruleParamDto, String value) {
    ActiveRuleParamDto dto = new ActiveRuleParamDto()
      .setActiveRuleUuid(activeRuleDto.getUuid())
      .setRulesParameterUuid(ruleParamDto.getUuid())
      .setKey(ruleParamDto.getName())
      .setValue(value);
    db.getDbClient().activeRuleDao().insertParam(db.getSession(), activeRuleDto, dto);
    db.commit();
    return dto;
  }

  private QProfileDto createChildProfile(QProfileDto parent) {
    return db.qualityProfiles().insert(p -> p
      .setLanguage(parent.getLanguage())
      .setParentKee(parent.getKee())
      .setName("Child of " + parent.getName()))
      .setIsBuiltIn(false);
  }
}
