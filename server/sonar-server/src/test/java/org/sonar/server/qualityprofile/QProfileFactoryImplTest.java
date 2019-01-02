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

import java.util.Collections;
import java.util.Set;
import org.assertj.core.api.AbstractObjectAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.OrgQProfileDto;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class QProfileFactoryImplTest {

  private System2 system2 = new AlwaysIncreasingSystem2();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(system2);

  private DbSession dbSession = db.getSession();
  private ActiveRuleIndexer activeRuleIndexer = mock(ActiveRuleIndexer.class);
  private QProfileFactory underTest = new QProfileFactoryImpl(db.getDbClient(), new SequenceUuidFactory(), system2, activeRuleIndexer);
  private RuleDefinitionDto rule;
  private RuleParamDto ruleParam;

  @Before
  public void setUp() {
    rule = db.rules().insert();
    ruleParam = db.rules().insertRuleParam(rule);
  }

  @Test
  public void checkAndCreateCustom() {
    OrganizationDto organization = db.organizations().insert();

    QProfileDto profile = underTest.checkAndCreateCustom(dbSession, organization, new QProfileName("xoo", "P1"));

    assertThat(profile.getOrganizationUuid()).isEqualTo(organization.getUuid());
    assertThat(profile.getKee()).isNotEmpty();
    assertThat(profile.getName()).isEqualTo("P1");
    assertThat(profile.getLanguage()).isEqualTo("xoo");
    assertThat(profile.getId()).isNotNull();
    assertThat(profile.isBuiltIn()).isFalse();

    QProfileDto reloaded = db.getDbClient().qualityProfileDao().selectByNameAndLanguage(dbSession, organization, profile.getName(), profile.getLanguage());
    assertEqual(profile, reloaded);
    assertThat(db.getDbClient().qualityProfileDao().selectOrderedByOrganizationUuid(dbSession, organization)).extracting(QProfileDto::getKee).containsExactly(profile.getKee());
  }

  @Test
  public void checkAndCreateCustom_throws_BadRequestException_if_name_null() {
    QProfileName name = new QProfileName("xoo", null);
    OrganizationDto organization = db.organizations().insert();

    expectBadRequestException("quality_profiles.profile_name_cant_be_blank");

    underTest.checkAndCreateCustom(dbSession, organization, name);
  }

  @Test
  public void checkAndCreateCustom_throws_BadRequestException_if_name_empty() {
    QProfileName name = new QProfileName("xoo", "");
    OrganizationDto organization = db.organizations().insert();

    expectBadRequestException("quality_profiles.profile_name_cant_be_blank");

    underTest.checkAndCreateCustom(dbSession, organization, name);
  }

  @Test
  public void checkAndCreateCustom_throws_BadRequestException_if_already_exists() {
    QProfileName name = new QProfileName("xoo", "P1");
    OrganizationDto organization = db.organizations().insert();

    underTest.checkAndCreateCustom(dbSession, organization, name);
    dbSession.commit();

    expectBadRequestException("Quality profile already exists: xoo/P1");

    underTest.checkAndCreateCustom(dbSession, organization, name);
  }

  @Test
  public void delete_custom_profiles() {
    OrganizationDto org = db.organizations().insert();
    QProfileDto profile1 = createCustomProfile(org);
    QProfileDto profile2 = createCustomProfile(org);
    QProfileDto profile3 = createCustomProfile(org);

    underTest.delete(dbSession, asList(profile1, profile2));

    verifyCallActiveRuleIndexerDelete(profile1.getKee(), profile2.getKee());
    assertThatCustomProfileDoesNotExist(profile1);
    assertThatCustomProfileDoesNotExist(profile2);
    assertThatCustomProfileExists(profile3);
  }

  @Test
  public void delete_removes_custom_profile_marked_as_default() {
    OrganizationDto org = db.organizations().insert();
    QProfileDto profile = createCustomProfile(org);
    db.qualityProfiles().setAsDefault(profile);

    underTest.delete(dbSession, asList(profile));

    assertThatCustomProfileDoesNotExist(profile);
  }

  @Test
  public void delete_removes_custom_profile_from_project_associations() {
    OrganizationDto org = db.organizations().insert();
    QProfileDto profile = createCustomProfile(org);
    ComponentDto project = db.components().insertPrivateProject(org);
    db.qualityProfiles().associateWithProject(project, profile);

    underTest.delete(dbSession, asList(profile));

    assertThatCustomProfileDoesNotExist(profile);
  }

  @Test
  public void delete_builtin_profile() {
    RulesProfileDto builtInProfile = createBuiltInProfile();
    OrganizationDto org = db.organizations().insert();
    QProfileDto profile = associateBuiltInProfileToOrganization(builtInProfile, org);

    underTest.delete(dbSession, asList(profile));

    verifyNoCallsActiveRuleIndexerDelete();

    // remove only from org_qprofiles
    assertThat(db.getDbClient().qualityProfileDao().selectOrderedByOrganizationUuid(dbSession, org)).isEmpty();

    assertThatRulesProfileExists(builtInProfile);
  }

  @Test
  public void delete_builtin_profile_associated_to_project() {
    RulesProfileDto builtInProfile = createBuiltInProfile();
    OrganizationDto org = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(org);
    QProfileDto profile = associateBuiltInProfileToOrganization(builtInProfile, org);
    db.qualityProfiles().associateWithProject(project, profile);
    assertThat(db.getDbClient().qualityProfileDao().selectAssociatedToProjectAndLanguage(dbSession, project, profile.getLanguage())).isNotNull();

    underTest.delete(dbSession, asList(profile));

    verifyNoCallsActiveRuleIndexerDelete();

    // remove only from org_qprofiles and project_qprofiles
    assertThat(db.getDbClient().qualityProfileDao().selectOrderedByOrganizationUuid(dbSession, org)).isEmpty();
    assertThat(db.getDbClient().qualityProfileDao().selectAssociatedToProjectAndLanguage(dbSession, project, profile.getLanguage())).isNull();
    assertThatRulesProfileExists(builtInProfile);
  }

  @Test
  public void delete_builtin_profile_marked_as_default_on_organization() {
    RulesProfileDto builtInProfile = createBuiltInProfile();
    OrganizationDto org = db.organizations().insert();
    QProfileDto profile = associateBuiltInProfileToOrganization(builtInProfile, org);
    db.qualityProfiles().setAsDefault(profile);

    underTest.delete(dbSession, asList(profile));

    verifyNoCallsActiveRuleIndexerDelete();

    // remove only from org_qprofiles and default_qprofiles
    assertThat(db.getDbClient().qualityProfileDao().selectOrderedByOrganizationUuid(dbSession, org)).isEmpty();
    assertThat(db.getDbClient().qualityProfileDao().selectDefaultProfile(dbSession, org, profile.getLanguage())).isNull();
    assertThatRulesProfileExists(builtInProfile);
  }

  @Test
  public void delete_accepts_empty_list_of_keys() {
    OrganizationDto org = db.organizations().insert();
    QProfileDto profile = createCustomProfile(org);

    underTest.delete(dbSession, Collections.emptyList());

    verifyZeroInteractions(activeRuleIndexer);
    assertQualityProfileFromDb(profile).isNotNull();
  }

  @Test
  public void delete_removes_qprofile_edit_permissions() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization);
    UserDto user = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, user);
    GroupDto group = db.users().insertGroup(organization);
    db.qualityProfiles().addGroupPermission(profile, group);

    underTest.delete(dbSession, asList(profile));

    assertThat(db.countRowsOfTable(dbSession, "qprofile_edit_users")).isZero();
    assertThat(db.countRowsOfTable(dbSession, "qprofile_edit_groups")).isZero();
  }

  private QProfileDto createCustomProfile(OrganizationDto org) {
    QProfileDto profile = db.qualityProfiles().insert(org, p -> p.setLanguage("xoo").setIsBuiltIn(false));
    ActiveRuleDto activeRuleDto = db.qualityProfiles().activateRule(profile, rule);

    ActiveRuleParamDto activeRuleParam = new ActiveRuleParamDto()
      .setRulesParameterId(ruleParam.getId())
      .setKey("foo")
      .setValue("bar");
    db.getDbClient().activeRuleDao().insertParam(dbSession, activeRuleDto, activeRuleParam);

    db.getDbClient().qProfileChangeDao().insert(dbSession, new QProfileChangeDto()
      .setChangeType(ActiveRuleChange.Type.ACTIVATED.name())
      .setRulesProfileUuid(profile.getRulesProfileUuid()));
    db.commit();
    return profile;
  }

  private RulesProfileDto createBuiltInProfile() {
    RulesProfileDto rulesProfileDto = new RulesProfileDto()
      .setIsBuiltIn(true)
      .setKee(Uuids.createFast())
      .setLanguage("xoo")
      .setName("Sonar way");
    db.getDbClient().qualityProfileDao().insert(dbSession, rulesProfileDto);
    ActiveRuleDto activeRuleDto = new ActiveRuleDto()
      .setProfileId(rulesProfileDto.getId())
      .setRuleId(rule.getId())
      .setSeverity(Severity.BLOCKER);
    db.getDbClient().activeRuleDao().insert(dbSession, activeRuleDto);

    ActiveRuleParamDto activeRuleParam = new ActiveRuleParamDto()
      .setRulesParameterId(ruleParam.getId())
      .setKey("foo")
      .setValue("bar");
    db.getDbClient().activeRuleDao().insertParam(dbSession, activeRuleDto, activeRuleParam);

    db.getDbClient().qProfileChangeDao().insert(dbSession, new QProfileChangeDto()
      .setChangeType(ActiveRuleChange.Type.ACTIVATED.name())
      .setRulesProfileUuid(rulesProfileDto.getKee()));

    db.commit();
    return rulesProfileDto;
  }

  private QProfileDto associateBuiltInProfileToOrganization(RulesProfileDto rulesProfile, OrganizationDto organization) {
    OrgQProfileDto orgQProfileDto = new OrgQProfileDto()
      .setUuid(Uuids.createFast())
      .setRulesProfileUuid(rulesProfile.getKee())
      .setOrganizationUuid(organization.getUuid());
    db.getDbClient().qualityProfileDao().insert(dbSession, orgQProfileDto);
    db.commit();
    return QProfileDto.from(orgQProfileDto, rulesProfile);
  }

  private AbstractObjectAssert<?, QProfileDto> assertQualityProfileFromDb(QProfileDto profile) {
    return assertThat(db.getDbClient().qualityProfileDao().selectByUuid(dbSession, profile.getKee()));
  }

  private void verifyNoCallsActiveRuleIndexerDelete() {
    verify(activeRuleIndexer, never()).commitDeletionOfProfiles(any(DbSession.class), anyCollection());
  }

  private void verifyCallActiveRuleIndexerDelete(String... expectedRuleProfileUuids) {
    Class<Set<QProfileDto>> setClass = (Class<Set<QProfileDto>>) (Class) Set.class;
    ArgumentCaptor<Set<QProfileDto>> setCaptor = ArgumentCaptor.forClass(setClass);
    verify(activeRuleIndexer).commitDeletionOfProfiles(any(DbSession.class), setCaptor.capture());

    assertThat(setCaptor.getValue())
      .extracting(QProfileDto::getKee)
      .containsExactlyInAnyOrder(expectedRuleProfileUuids);
  }

  private void assertThatRulesProfileExists(RulesProfileDto rulesProfile) {
    assertThat(db.getDbClient().qualityProfileDao().selectBuiltInRuleProfiles(dbSession))
      .extracting(RulesProfileDto::getKee)
      .containsExactly(rulesProfile.getKee());
    assertThat(db.countRowsOfTable(dbSession, "active_rules")).isGreaterThan(0);
    assertThat(db.countRowsOfTable(dbSession, "active_rule_parameters")).isGreaterThan(0);
    assertThat(db.countRowsOfTable(dbSession, "qprofile_changes")).isGreaterThan(0);
  }

  private void assertThatCustomProfileDoesNotExist(QProfileDto profile) {
    assertThat(db.countSql(dbSession, "select count(*) from org_qprofiles where uuid = '" + profile.getKee() + "'")).isEqualTo(0);
    assertThat(db.countSql(dbSession, "select count(*) from project_qprofiles where profile_key = '" + profile.getKee() + "'")).isEqualTo(0);
    assertThat(db.countSql(dbSession, "select count(*) from default_qprofiles where qprofile_uuid = '" + profile.getKee() + "'")).isEqualTo(0);
    assertThat(db.countSql(dbSession, "select count(*) from rules_profiles where kee = '" + profile.getRulesProfileUuid() + "'")).isEqualTo(0);
    assertThat(db.countSql(dbSession, "select count(*) from active_rules where profile_id = " + profile.getId())).isEqualTo(0);
    assertThat(db.countSql(dbSession, "select count(*) from qprofile_changes where rules_profile_uuid = '" + profile.getRulesProfileUuid() + "'")).isEqualTo(0);
    // TODO active_rule_parameters
  }

  private void assertThatCustomProfileExists(QProfileDto profile) {
    assertThat(db.countSql(dbSession, "select count(*) from org_qprofiles where uuid = '" + profile.getKee() + "'")).isGreaterThan(0);
    //assertThat(db.countSql(dbSession, "select count(*) from project_qprofiles where profile_key = '" + profile.getKee() + "'")).isGreaterThan(0);
    //assertThat(db.countSql(dbSession, "select count(*) from default_qprofiles where qprofile_uuid = '" + profile.getKee() + "'")).isGreaterThan(0);
    assertThat(db.countSql(dbSession, "select count(*) from rules_profiles where kee = '" + profile.getRulesProfileUuid() + "'")).isEqualTo(1);
    assertThat(db.countSql(dbSession, "select count(*) from active_rules where profile_id = " + profile.getId())).isGreaterThan(0);
    assertThat(db.countSql(dbSession, "select count(*) from qprofile_changes where rules_profile_uuid = '" + profile.getRulesProfileUuid() + "'")).isGreaterThan(0);
    // TODO active_rule_parameters
  }

  private static void assertEqual(QProfileDto p1, QProfileDto p2) {
    assertThat(p2.getOrganizationUuid()).isEqualTo(p1.getOrganizationUuid());
    assertThat(p2.getName()).isEqualTo(p1.getName());
    assertThat(p2.getKee()).startsWith(p1.getKee());
    assertThat(p2.getLanguage()).isEqualTo(p1.getLanguage());
    assertThat(p2.getId()).isEqualTo(p1.getId());
    assertThat(p2.getParentKee()).isEqualTo(p1.getParentKee());
  }

  private void expectBadRequestException(String message) {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(message);
  }
}
