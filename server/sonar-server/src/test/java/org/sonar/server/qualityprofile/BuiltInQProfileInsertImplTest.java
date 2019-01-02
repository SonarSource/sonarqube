/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.NewBuiltInQualityProfile;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileChangeQuery;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.util.TypeValidations;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class BuiltInQProfileInsertImplTest {

  @Rule
  public BuiltInQProfileRepositoryRule builtInQProfileRepository = new BuiltInQProfileRepositoryRule();
  @Rule
  public DbTester db = DbTester.create().setDisableDefaultOrganization(true);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private System2 system2 = new AlwaysIncreasingSystem2();
  private UuidFactory uuidFactory = new SequenceUuidFactory();
  private TypeValidations typeValidations = new TypeValidations(emptyList());
  private DbSession dbSession = db.getSession();
  private DbSession batchDbSession = db.getDbClient().openSession(true);
  private ActiveRuleIndexer activeRuleIndexer = mock(ActiveRuleIndexer.class);
  private BuiltInQProfileInsertImpl underTest = new BuiltInQProfileInsertImpl(db.getDbClient(), system2, uuidFactory, typeValidations, activeRuleIndexer);

  @After
  public void tearDown() {
    batchDbSession.close();
  }

  @Test
  public void insert_single_row_in_RULES_PROFILES_and_reference_it_in_ORG_QPROFILES() {
    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();
    BuiltInQProfile builtIn = builtInQProfileRepository.create(LanguageTesting.newLanguage("xoo"), "the name", false);

    call(builtIn);

    verifyTableSize("org_qprofiles", 2);
    verifyTableSize("rules_profiles", 1);
    verifyTableSize("active_rules", 0);
    verifyTableSize("active_rule_parameters", 0);
    verifyTableSize("qprofile_changes", 0);
    verifyTableSize("project_qprofiles", 0);

    QProfileDto profileOnOrg1 = verifyProfileInDb(org1, builtIn);
    QProfileDto profileOnOrg2 = verifyProfileInDb(org2, builtIn);

    // same row in table rules_profiles is used
    assertThat(profileOnOrg1.getKee()).isNotEqualTo(profileOnOrg2.getKee());
    assertThat(profileOnOrg1.getRulesProfileUuid()).isEqualTo(profileOnOrg2.getRulesProfileUuid());
    assertThat(profileOnOrg1.getId()).isEqualTo(profileOnOrg2.getId());
  }

  @Test
  public void insert_active_rules_and_changelog() {
    OrganizationDto org = db.organizations().insert();
    RuleDefinitionDto rule1 = db.rules().insert(r -> r.setLanguage("xoo"));
    RuleDefinitionDto rule2 = db.rules().insert(r -> r.setLanguage("xoo"));

    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile("the name", "xoo");

    newQp.activateRule(rule1.getRepositoryKey(), rule1.getRuleKey()).overrideSeverity(Severity.CRITICAL);
    newQp.activateRule(rule2.getRepositoryKey(), rule2.getRuleKey()).overrideSeverity(Severity.MAJOR);
    newQp.done();

    BuiltInQProfile builtIn = builtInQProfileRepository.create(context.profile("xoo", "the name"), rule1, rule2);
    call(builtIn);

    verifyTableSize("rules_profiles", 1);
    verifyTableSize("active_rules", 2);
    verifyTableSize("active_rule_parameters", 0);
    verifyTableSize("qprofile_changes", 2);

    QProfileDto profile = verifyProfileInDb(org, builtIn);
    verifyActiveRuleInDb(profile, rule1, Severity.CRITICAL);
    verifyActiveRuleInDb(profile, rule2, Severity.MAJOR);
  }

  @Test
  public void flag_profile_as_default_on_organization_if_declared_as_default_by_api() {
    OrganizationDto org = db.organizations().insert();
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile("the name", "xoo").setDefault(true);
    newQp.done();

    BuiltInQProfile builtIn = builtInQProfileRepository.create(context.profile("xoo", "the name"));

    call(builtIn);

    QProfileDto profile = verifyProfileInDb(org, builtIn);
    QProfileDto defaultProfile = db.getDbClient().qualityProfileDao().selectDefaultProfile(dbSession, org, "xoo");
    assertThat(defaultProfile.getKee()).isEqualTo(profile.getKee());
  }

  @Test
  public void existing_default_profile_in_organization_must_not_be_changed() {
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile("the name", "xoo").setDefault(true);
    newQp.done();
    BuiltInQProfile builtIn = builtInQProfileRepository.create(context.profile("xoo", "the name"));

    OrganizationDto org = db.organizations().insert();
    QProfileDto currentDefault = db.qualityProfiles().insert(org, p -> p.setLanguage("xoo"));
    db.qualityProfiles().setAsDefault(currentDefault);

    call(builtIn);

    QProfileDto defaultProfile = db.getDbClient().qualityProfileDao().selectDefaultProfile(dbSession, org, "xoo");
    assertThat(defaultProfile.getKee()).isEqualTo(currentDefault.getKee());
  }

  @Test
  public void dont_flag_profile_as_default_on_organization_if_not_declared_as_default_by_api() {
    OrganizationDto org = db.organizations().insert();
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile("the name", "xoo").setDefault(false);
    newQp.done();
    BuiltInQProfile builtIn = builtInQProfileRepository.create(context.profile("xoo", "the name"));

    call(builtIn);

    QProfileDto defaultProfile = db.getDbClient().qualityProfileDao().selectDefaultProfile(dbSession, org, "xoo");
    assertThat(defaultProfile).isNull();
  }

  // TODO test params
  // TODO test lot of active_rules, params, orgas

  private void verifyActiveRuleInDb(QProfileDto profile, RuleDefinitionDto rule, String expectedSeverity) {
    ActiveRuleDto activeRule = db.getDbClient().activeRuleDao().selectByKey(dbSession, ActiveRuleKey.of(profile, rule.getKey())).get();
    assertThat(activeRule.getId()).isPositive();
    assertThat(activeRule.getInheritance()).isNull();
    assertThat(activeRule.doesOverride()).isFalse();
    assertThat(activeRule.getRuleId()).isEqualTo(rule.getId());
    assertThat(activeRule.getProfileId()).isEqualTo(profile.getId());
    assertThat(activeRule.getSeverityString()).isEqualTo(expectedSeverity);
    assertThat(activeRule.getCreatedAt()).isPositive();
    assertThat(activeRule.getUpdatedAt()).isPositive();

    List<ActiveRuleParamDto> params = db.getDbClient().activeRuleDao().selectParamsByActiveRuleId(dbSession, activeRule.getId());
    assertThat(params).isEmpty();

    QProfileChangeQuery changeQuery = new QProfileChangeQuery(profile.getKee());
    QProfileChangeDto change = db.getDbClient().qProfileChangeDao().selectByQuery(dbSession, changeQuery).stream()
      .filter(c -> c.getDataAsMap().get("ruleId").equals(String.valueOf(rule.getId())))
      .findFirst()
      .get();
    assertThat(change.getChangeType()).isEqualTo(ActiveRuleChange.Type.ACTIVATED.name());
    assertThat(change.getCreatedAt()).isPositive();
    assertThat(change.getUuid()).isNotEmpty();
    assertThat(change.getUserUuid()).isNull();
    assertThat(change.getRulesProfileUuid()).isEqualTo(profile.getRulesProfileUuid());
    assertThat(change.getDataAsMap().get("severity")).isEqualTo(expectedSeverity);
  }

  private QProfileDto verifyProfileInDb(OrganizationDto organization, BuiltInQProfile builtIn) {
    QProfileDto profileOnOrg1 = db.getDbClient().qualityProfileDao().selectByNameAndLanguage(dbSession, organization, builtIn.getName(), builtIn.getLanguage());
    assertThat(profileOnOrg1.getLanguage()).isEqualTo(builtIn.getLanguage());
    assertThat(profileOnOrg1.getName()).isEqualTo(builtIn.getName());
    assertThat(profileOnOrg1.getOrganizationUuid()).isEqualTo(organization.getUuid());
    assertThat(profileOnOrg1.getParentKee()).isNull();
    assertThat(profileOnOrg1.getLastUsed()).isNull();
    assertThat(profileOnOrg1.getUserUpdatedAt()).isNull();
    assertThat(profileOnOrg1.getRulesUpdatedAt()).isNotEmpty();
    assertThat(profileOnOrg1.getKee()).isNotEqualTo(profileOnOrg1.getRulesProfileUuid());
    assertThat(profileOnOrg1.getId()).isNotNull();
    return profileOnOrg1;
  }

  private void verifyTableSize(String table, int expectedSize) {
    assertThat(db.countRowsOfTable(dbSession, table)).as("table " + table).isEqualTo(expectedSize);
  }

  private void call(BuiltInQProfile builtIn) {
    underTest.create(dbSession, batchDbSession, builtIn);
    dbSession.commit();
    batchDbSession.commit();
  }

}
