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

import java.util.Collection;
import java.util.Collections;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.OrgQProfileDto;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.builtin.QProfileName;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class QProfileFactoryImplIT {

  private System2 system2 = new AlwaysIncreasingSystem2();

  @Rule
  public DbTester db = DbTester.create(system2);

  private DbSession dbSession = db.getSession();
  private ActiveRuleIndexer activeRuleIndexer = mock(ActiveRuleIndexer.class);
  private QProfileFactory underTest = new QProfileFactoryImpl(db.getDbClient(), new SequenceUuidFactory(), system2, activeRuleIndexer);
  private RuleDto rule;
  private RuleParamDto ruleParam;

  @Before
  public void setUp() {
    rule = db.rules().insert();
    ruleParam = db.rules().insertRuleParam(rule);
  }

  @Test
  public void checkAndCreateCustom() {
    QProfileDto profile = underTest.checkAndCreateCustom(dbSession, new QProfileName("xoo", "P1"));

    assertThat(profile.getKee()).isNotEmpty();
    assertThat(profile.getName()).isEqualTo("P1");
    assertThat(profile.getLanguage()).isEqualTo("xoo");
    assertThat(profile.getRulesProfileUuid()).isNotNull();
    assertThat(profile.isBuiltIn()).isFalse();

    QProfileDto reloaded = db.getDbClient().qualityProfileDao().selectByNameAndLanguage(dbSession, profile.getName(), profile.getLanguage());
    assertEqual(profile, reloaded);
    assertThat(db.getDbClient().qualityProfileDao().selectAll(dbSession)).extracting(QProfileDto::getKee).containsExactly(profile.getKee());
  }

  @Test
  public void checkAndCreateCustom_throws_BadRequestException_if_name_null() {
    QProfileName name = new QProfileName("xoo", null);

    expectBadRequestException(() -> underTest.checkAndCreateCustom(dbSession, name), "quality_profiles.profile_name_cant_be_blank");
  }

  @Test
  public void checkAndCreateCustom_throws_BadRequestException_if_name_empty() {
    QProfileName name = new QProfileName("xoo", "");

    expectBadRequestException(() -> underTest.checkAndCreateCustom(dbSession, name), "quality_profiles.profile_name_cant_be_blank");
  }

  @Test
  public void checkAndCreateCustom_throws_BadRequestException_if_already_exists() {
    QProfileName name = new QProfileName("xoo", "P1");

    underTest.checkAndCreateCustom(dbSession, name);
    dbSession.commit();

    expectBadRequestException(() -> underTest.checkAndCreateCustom(dbSession, name), "Quality profile already exists: xoo/P1");
  }

  @Test
  public void delete_custom_profiles() {
    QProfileDto profile1 = createCustomProfile();
    QProfileDto profile2 = createCustomProfile();
    QProfileDto profile3 = createCustomProfile();

    underTest.delete(dbSession, asList(profile1, profile2));

    verifyCallActiveRuleIndexerDelete(profile1.getKee(), profile2.getKee());
    assertThatCustomProfileDoesNotExist(profile1);
    assertThatCustomProfileDoesNotExist(profile2);
    assertThatCustomProfileExists(profile3);
  }

  @Test
  public void delete_removes_custom_profile_marked_as_default() {
    QProfileDto profile = createCustomProfile();
    db.qualityProfiles().setAsDefault(profile);

    underTest.delete(dbSession, asList(profile));

    assertThatCustomProfileDoesNotExist(profile);
  }

  @Test
  public void delete_removes_custom_profile_from_project_associations() {
    QProfileDto profile = createCustomProfile();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.qualityProfiles().associateWithProject(project, profile);

    underTest.delete(dbSession, asList(profile));

    assertThatCustomProfileDoesNotExist(profile);
  }

  @Test
  public void delete_builtin_profile() {
    RulesProfileDto builtInProfile = createBuiltInProfile();
    QProfileDto profile = associateBuiltInProfile(builtInProfile);

    underTest.delete(dbSession, asList(profile));

    verifyNoCallsActiveRuleIndexerDelete();

    // remove only from org_qprofiles
    assertThat(db.getDbClient().qualityProfileDao().selectAll(dbSession)).isEmpty();

    assertThatRulesProfileExists(builtInProfile);
  }

  @Test
  public void delete_builtin_profile_associated_to_project() {
    RulesProfileDto builtInProfile = createBuiltInProfile();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    QProfileDto profile = associateBuiltInProfile(builtInProfile);
    db.qualityProfiles().associateWithProject(project, profile);
    assertThat(db.getDbClient().qualityProfileDao().selectAssociatedToProjectAndLanguage(dbSession, project, profile.getLanguage())).isNotNull();

    underTest.delete(dbSession, asList(profile));

    verifyNoCallsActiveRuleIndexerDelete();

    // remove only from org_qprofiles and project_qprofiles
    assertThat(db.getDbClient().qualityProfileDao().selectAll(dbSession)).isEmpty();
    assertThat(db.getDbClient().qualityProfileDao().selectAssociatedToProjectAndLanguage(dbSession, project, profile.getLanguage())).isNull();
    assertThatRulesProfileExists(builtInProfile);
  }

  @Test
  public void delete_builtin_profile_marked_as_default() {
    RulesProfileDto builtInProfile = createBuiltInProfile();
    QProfileDto profile = associateBuiltInProfile(builtInProfile);
    db.qualityProfiles().setAsDefault(profile);

    underTest.delete(dbSession, asList(profile));

    verifyNoCallsActiveRuleIndexerDelete();

    // remove only from org_qprofiles and default_qprofiles
    assertThat(db.getDbClient().qualityProfileDao().selectAll(dbSession)).isEmpty();
    assertThat(db.getDbClient().qualityProfileDao().selectDefaultProfile(dbSession, profile.getLanguage())).isNull();
    assertThatRulesProfileExists(builtInProfile);
  }

  @Test
  public void delete_accepts_empty_list_of_keys() {
    QProfileDto profile = createCustomProfile();

    underTest.delete(dbSession, Collections.emptyList());

    verifyNoInteractions(activeRuleIndexer);
    assertQualityProfileFromDb(profile).isNotNull();
  }

  @Test
  public void delete_removes_qprofile_edit_permissions() {
    QProfileDto profile = db.qualityProfiles().insert();
    UserDto user = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, user);
    GroupDto group = db.users().insertGroup();
    db.qualityProfiles().addGroupPermission(profile, group);

    underTest.delete(dbSession, asList(profile));

    assertThat(db.countRowsOfTable(dbSession, "qprofile_edit_users")).isZero();
    assertThat(db.countRowsOfTable(dbSession, "qprofile_edit_groups")).isZero();
  }

  private QProfileDto createCustomProfile() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage("xoo").setIsBuiltIn(false));
    ActiveRuleDto activeRuleDto = db.qualityProfiles().activateRule(profile, rule);

    ActiveRuleParamDto activeRuleParam = new ActiveRuleParamDto()
      .setRulesParameterUuid(ruleParam.getUuid())
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
      .setUuid(Uuids.createFast())
      .setLanguage("xoo")
      .setName("Sonar way");
    db.getDbClient().qualityProfileDao().insert(dbSession, rulesProfileDto);
    ActiveRuleDto activeRuleDto = new ActiveRuleDto()
      .setProfileUuid(rulesProfileDto.getUuid())
      .setRuleUuid(rule.getUuid())
      .setSeverity(Severity.BLOCKER);
    db.getDbClient().activeRuleDao().insert(dbSession, activeRuleDto);

    ActiveRuleParamDto activeRuleParam = new ActiveRuleParamDto()
      .setRulesParameterUuid(ruleParam.getUuid())
      .setKey("foo")
      .setValue("bar");
    db.getDbClient().activeRuleDao().insertParam(dbSession, activeRuleDto, activeRuleParam);

    db.getDbClient().qProfileChangeDao().insert(dbSession, new QProfileChangeDto()
      .setChangeType(ActiveRuleChange.Type.ACTIVATED.name())
      .setRulesProfileUuid(rulesProfileDto.getUuid()));

    db.commit();
    return rulesProfileDto;
  }

  private QProfileDto associateBuiltInProfile(RulesProfileDto rulesProfile) {
    OrgQProfileDto orgQProfileDto = new OrgQProfileDto()
      .setUuid(Uuids.createFast())
      .setRulesProfileUuid(rulesProfile.getUuid());

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
    ArgumentCaptor<Collection<QProfileDto>> collectionCaptor = ArgumentCaptor.forClass(Collection.class);
    verify(activeRuleIndexer).commitDeletionOfProfiles(any(DbSession.class), collectionCaptor.capture());

    assertThat(collectionCaptor.getValue())
      .extracting(QProfileDto::getKee)
      .containsExactlyInAnyOrder(expectedRuleProfileUuids);
  }

  private void assertThatRulesProfileExists(RulesProfileDto rulesProfile) {
    assertThat(db.getDbClient().qualityProfileDao().selectBuiltInRuleProfiles(dbSession))
      .extracting(RulesProfileDto::getUuid)
      .containsExactly(rulesProfile.getUuid());
    assertThat(db.countRowsOfTable(dbSession, "active_rules")).isPositive();
    assertThat(db.countRowsOfTable(dbSession, "active_rule_parameters")).isPositive();
    assertThat(db.countRowsOfTable(dbSession, "qprofile_changes")).isPositive();
  }

  private void assertThatCustomProfileDoesNotExist(QProfileDto profile) {
    assertThat(db.countSql(dbSession, "select count(*) from org_qprofiles where uuid = '" + profile.getKee() + "'")).isZero();
    assertThat(db.countSql(dbSession, "select count(*) from project_qprofiles where profile_key = '" + profile.getKee() + "'")).isZero();
    assertThat(db.countSql(dbSession, "select count(*) from default_qprofiles where qprofile_uuid = '" + profile.getKee() + "'")).isZero();
    assertThat(db.countSql(dbSession, "select count(*) from rules_profiles where uuid = '" + profile.getRulesProfileUuid() + "'")).isZero();
    assertThat(db.countSql(dbSession, "select count(*) from active_rules where profile_uuid = '" + profile.getRulesProfileUuid() + "'")).isZero();
    assertThat(db.countSql(dbSession, "select count(*) from qprofile_changes where rules_profile_uuid = '" + profile.getRulesProfileUuid() + "'")).isZero();
    // TODO active_rule_parameters
  }

  private void assertThatCustomProfileExists(QProfileDto profile) {
    assertThat(db.countSql(dbSession, "select count(*) from org_qprofiles where uuid = '" + profile.getKee() + "'")).isPositive();
    // assertThat(db.countSql(dbSession, "select count(*) from project_qprofiles where profile_key = '" + profile.getKee() +
    // "'")).isPositive();
    // assertThat(db.countSql(dbSession, "select count(*) from default_qprofiles where qprofile_uuid = '" + profile.getKee() +
    // "'")).isPositive();
    assertThat(db.countSql(dbSession, "select count(*) from rules_profiles where uuid = '" + profile.getRulesProfileUuid() + "'")).isOne();
    assertThat(db.countSql(dbSession, "select count(*) from active_rules where profile_uuid = '" + profile.getRulesProfileUuid() + "'")).isPositive();
    assertThat(db.countSql(dbSession, "select count(*) from qprofile_changes where rules_profile_uuid = '" + profile.getRulesProfileUuid() + "'")).isPositive();
    // TODO active_rule_parameters
  }

  private static void assertEqual(QProfileDto p1, QProfileDto p2) {
    assertThat(p2.getName()).isEqualTo(p1.getName());
    assertThat(p2.getKee()).startsWith(p1.getKee());
    assertThat(p2.getLanguage()).isEqualTo(p1.getLanguage());
    assertThat(p2.getRulesProfileUuid()).isEqualTo(p1.getRulesProfileUuid());
    assertThat(p2.getParentKee()).isEqualTo(p1.getParentKee());
  }

  private void expectBadRequestException(ThrowingCallable callback, String message) {
    assertThatThrownBy(callback)
      .isInstanceOf(BadRequestException.class)
      .hasMessage(message);
  }
}
