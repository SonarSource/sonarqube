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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.PropertyType;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.NewBuiltInQualityProfile;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.core.util.UuidFactory;
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
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.rule.ServerRuleFinder;
import org.sonar.server.util.StringTypeValidation;
import org.sonar.server.util.TypeValidations;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.issue.impact.Severity.BLOCKER;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.api.issue.impact.Severity.INFO;
import static org.sonar.db.rule.RuleTesting.newRule;

class BuiltInQProfileInsertImplIT {

  @RegisterExtension
  public BuiltInQProfileRepositoryRule builtInQProfileRepository = new BuiltInQProfileRepositoryRule();
  @RegisterExtension
  public static DbTester db = DbTester.create();

  private final System2 system2 = new AlwaysIncreasingSystem2();
  private final UuidFactory uuidFactory = UuidFactoryImpl.INSTANCE;
  private final TypeValidations typeValidations = new TypeValidations(singletonList(new StringTypeValidation()));
  private final DbSession dbSession = db.getSession();
  private static final DbSession batchDbSession = db.getDbClient().openSession(true);
  private final ServerRuleFinder ruleFinder = new DefaultRuleFinder(db.getDbClient(), mock(RuleDescriptionFormatter.class));
  private final ActiveRuleIndexer activeRuleIndexer = mock(ActiveRuleIndexer.class);
  private final SonarQubeVersion sonarQubeVersion = new SonarQubeVersion(Version.create(10, 3));
  private final BuiltInQProfileInsertImpl underTest = new BuiltInQProfileInsertImpl(db.getDbClient(), ruleFinder, system2, uuidFactory, typeValidations, activeRuleIndexer,
    sonarQubeVersion);

  @AfterAll
  public static void tearDown() {
    batchDbSession.close();
  }

  @Test
  void insert_active_rules_and_changelog() {
    RuleDto rule1 = db.rules().insert(r -> r.setLanguage("xoo"));
    RuleDto rule2 = db.rules().insert(r -> r.setLanguage("xoo"));

    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile("the name", "xoo");

    newQp.activateRule(rule1.getRepositoryKey(), rule1.getRuleKey()).overrideSeverity(Severity.CRITICAL);
    newQp.activateRule(rule2.getRepositoryKey(), rule2.getRuleKey()).overrideSeverity(Severity.MAJOR);
    newQp.done();

    BuiltInQProfile builtIn = builtInQProfileRepository.create(context.profile("xoo", "the name"), rule1, rule2);
    call(builtIn);

    verifyTableSize("rules_profiles", 1);
    verifyTableSize("org_qprofiles", 1);
    verifyTableSize("active_rules", 2);
    verifyTableSize("active_rule_parameters", 0);
    verifyTableSize("qprofile_changes", 2);
    verifyTableSize("default_qprofiles", 0);

    QProfileDto profile = verifyProfileInDb(builtIn);
    verifyActiveRuleInDb(profile, rule1, Severity.CRITICAL);
    verifyActiveRuleInDb(profile, rule2, Severity.MAJOR);
  }

  @Test
  void insert_active_rules_with_impacts() {
    RuleDto rule1 = db.rules().insert(newRule()
      .replaceAllDefaultImpacts(Set.of(
        new ImpactDto(SoftwareQuality.SECURITY, INFO),
        new ImpactDto(SoftwareQuality.RELIABILITY, BLOCKER)))
      .setLanguage("xoo")
      .setSeverity(Severity.MINOR));
    RuleDto rule2 = db.rules().insert(newRule().setLanguage("xoo").setSeverity(Severity.MAJOR));

    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile("the name", "xoo");

    newQp.activateRule(rule1.getRepositoryKey(), rule1.getRuleKey());
    newQp.activateRule(rule2.getRepositoryKey(), rule2.getRuleKey());
    newQp.done();

    BuiltInQProfile builtIn = builtInQProfileRepository.create(context.profile("xoo", "the name"), rule1, rule2);
    call(builtIn);

    verifyTableSize("rules_profiles", 1);
    verifyTableSize("org_qprofiles", 1);
    verifyTableSize("active_rules", 2);
    verifyTableSize("active_rule_parameters", 0);
    verifyTableSize("qprofile_changes", 2);
    verifyTableSize("default_qprofiles", 0);

    QProfileDto profile = verifyProfileInDb(builtIn);
    verifyActiveRuleInDb(profile, rule1, Severity.MINOR, Map.of(SoftwareQuality.SECURITY, INFO, SoftwareQuality.RELIABILITY, BLOCKER));
    verifyActiveRuleInDb(profile, rule2, Severity.MAJOR, Map.of(SoftwareQuality.MAINTAINABILITY, HIGH));
  }

  @Test
  void insert_default_qp() {
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    context.createBuiltInQualityProfile("the name", "xoo")
      .setDefault(true)
      .done();

    BuiltInQProfile builtIn = builtInQProfileRepository.create(context.profile("xoo", "the name"));
    call(builtIn);

    verifyTableSize("rules_profiles", 1);
    verifyTableSize("org_qprofiles", 1);
    verifyTableSize("active_rules", 0);
    verifyTableSize("active_rule_parameters", 0);
    verifyTableSize("qprofile_changes", 0);
    verifyTableSize("default_qprofiles", 1);

    verifyProfileInDb(builtIn);
  }

  @Test
  void insert_active_rules_with_params() {
    RuleDto rule1 = db.rules().insert(r -> r.setLanguage("xoo"));
    RuleParamDto param1 = db.rules().insertRuleParam(rule1, p -> p.setType(PropertyType.STRING.name()));
    RuleParamDto param2 = db.rules().insertRuleParam(rule1, p -> p.setType(PropertyType.STRING.name()));

    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile("the name", "xoo");

    newQp.activateRule(rule1.getRepositoryKey(), rule1.getRuleKey()).overrideSeverity(Severity.CRITICAL);
    newQp.done();

    BuiltInQProfile builtIn = builtInQProfileRepository.create(context.profile("xoo", "the name"), rule1);
    call(builtIn);

    verifyTableSize("rules_profiles", 1);
    verifyTableSize("org_qprofiles", 1);
    verifyTableSize("active_rules", 1);
    verifyTableSize("active_rule_parameters", 2);
    verifyTableSize("qprofile_changes", 1);

    QProfileDto profile = verifyProfileInDb(builtIn);
    verifyActiveRuleInDb(profile, rule1, Severity.CRITICAL, param1, param2);
  }

  @Test
  void flag_profile_as_default_if_declared_as_default_by_api() {
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile("the name", "xoo").setDefault(true);
    newQp.done();

    BuiltInQProfile builtIn = builtInQProfileRepository.create(context.profile("xoo", "the name"));

    call(builtIn);

    QProfileDto profile = verifyProfileInDb(builtIn);
    QProfileDto defaultProfile = db.getDbClient().qualityProfileDao().selectDefaultProfile(dbSession, "xoo");
    assertThat(defaultProfile.getKee()).isEqualTo(profile.getKee());
  }

  @Test
  void existing_default_profile_must_not_be_changed() {
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile("the name", "xoo").setDefault(true);
    newQp.done();
    BuiltInQProfile builtIn = builtInQProfileRepository.create(context.profile("xoo", "the name"));

    QProfileDto currentDefault = db.qualityProfiles().insert(p -> p.setLanguage("xoo"));
    db.qualityProfiles().setAsDefault(currentDefault);

    call(builtIn);

    QProfileDto defaultProfile = db.getDbClient().qualityProfileDao().selectDefaultProfile(dbSession, "xoo");
    assertThat(defaultProfile.getKee()).isEqualTo(currentDefault.getKee());
    verifyTableSize("rules_profiles", 2);
  }

  @Test
  void dont_flag_profile_as_default_if_not_declared_as_default_by_api() {
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile("the name", "xoo").setDefault(false);
    newQp.done();
    BuiltInQProfile builtIn = builtInQProfileRepository.create(context.profile("xoo", "the name"));

    call(builtIn);

    QProfileDto defaultProfile = db.getDbClient().qualityProfileDao().selectDefaultProfile(dbSession, "xoo");
    assertThat(defaultProfile).isNull();
  }

  // TODO test lot of active_rules, params, orgas

  private void verifyActiveRuleInDb(QProfileDto profile, RuleDto rule, String expectedSeverity, RuleParamDto... paramDtos) {
    verifyActiveRuleInDb(profile, rule, expectedSeverity, null, paramDtos);
  }

  private void verifyActiveRuleInDb(QProfileDto profile, RuleDto rule, String expectedSeverity,
    @Nullable Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> expectedImpacts, RuleParamDto... paramDtos) {
    ActiveRuleDto activeRule = db.getDbClient().activeRuleDao().selectByKey(dbSession, ActiveRuleKey.of(profile, rule.getKey())).get();
    assertThat(activeRule.getUuid()).isNotNull();
    assertThat(activeRule.getInheritance()).isNull();
    assertThat(activeRule.doesOverride()).isFalse();
    assertThat(activeRule.getRuleUuid()).isEqualTo(rule.getUuid());
    assertThat(activeRule.getProfileUuid()).isEqualTo(profile.getRulesProfileUuid());
    assertThat(activeRule.getSeverityString()).isEqualTo(expectedSeverity);
    assertThat(activeRule.getImpacts()).isEqualTo(
      expectedImpacts != null ? expectedImpacts : Map.of(SoftwareQuality.MAINTAINABILITY, HIGH));
    assertThat(activeRule.getCreatedAt()).isPositive();
    assertThat(activeRule.getUpdatedAt()).isPositive();

    List<ActiveRuleParamDto> params = db.getDbClient().activeRuleDao().selectParamsByActiveRuleUuid(dbSession, activeRule.getUuid());
    assertThat(params).extracting(ActiveRuleParamDto::getKey).containsOnly(Arrays.stream(paramDtos).map(RuleParamDto::getName).toArray(String[]::new));

    QProfileChangeQuery changeQuery = new QProfileChangeQuery(profile.getKee());
    QProfileChangeDto change = db.getDbClient().qProfileChangeDao().selectByQuery(dbSession, changeQuery).stream()
      .filter(c -> c.getDataAsMap().get("ruleUuid").equals(rule.getUuid()))
      .findFirst()
      .get();
    assertThat(change.getChangeType()).isEqualTo(ActiveRuleChange.Type.ACTIVATED.name());
    assertThat(change.getCreatedAt()).isPositive();
    assertThat(change.getUuid()).isNotEmpty();
    assertThat(change.getUserUuid()).isNull();
    assertThat(change.getRulesProfileUuid()).isEqualTo(profile.getRulesProfileUuid());
    assertThat(change.getDataAsMap()).containsEntry("severity", expectedSeverity);
    assertThat(change.getSqVersion()).isEqualTo(sonarQubeVersion.toString());
  }

  private QProfileDto verifyProfileInDb(BuiltInQProfile builtIn) {
    QProfileDto profileOnOrg1 = db.getDbClient().qualityProfileDao().selectByNameAndLanguage(dbSession, builtIn.getName(), builtIn.getLanguage());
    assertThat(profileOnOrg1.getLanguage()).isEqualTo(builtIn.getLanguage());
    assertThat(profileOnOrg1.getName()).isEqualTo(builtIn.getName());
    assertThat(profileOnOrg1.getParentKee()).isNull();
    assertThat(profileOnOrg1.getLastUsed()).isNull();
    assertThat(profileOnOrg1.getUserUpdatedAt()).isNull();
    assertThat(profileOnOrg1.getRulesUpdatedAt()).isNotEmpty();
    assertThat(profileOnOrg1.getKee()).isNotEqualTo(profileOnOrg1.getRulesProfileUuid());
    assertThat(profileOnOrg1.getRulesProfileUuid()).isNotNull();
    return profileOnOrg1;
  }

  private void verifyTableSize(String table, int expectedSize) {
    assertThat(db.countRowsOfTable(dbSession, table)).as("table " + table).isEqualTo(expectedSize);
  }

  private void call(BuiltInQProfile builtIn) {
    underTest.create(dbSession, builtIn);
    dbSession.commit();
    batchDbSession.commit();
  }

}
