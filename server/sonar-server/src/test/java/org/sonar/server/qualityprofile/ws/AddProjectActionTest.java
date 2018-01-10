/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.qualityprofile.ws;

import java.net.HttpURLConnection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

public class AddProjectActionTest {

  private static final String LANGUAGE_1 = "xoo";
  private static final String LANGUAGE_2 = "foo";

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient = db.getDbClient();
  private Languages languages = LanguageTesting.newLanguages(LANGUAGE_1, LANGUAGE_2);
  private QProfileWsSupport wsSupport = new QProfileWsSupport(dbClient, userSession, TestDefaultOrganizationProvider.from(db));
  private AddProjectAction underTest = new AddProjectAction(dbClient, userSession, languages, TestComponentFinder.from(db), wsSupport);
  private WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void definition() {
    WebService.Action definition = tester.getDef();
    assertThat(definition.since()).isEqualTo("5.2");
    assertThat(definition.isPost()).isTrue();

    // parameters
    assertThat(definition.params()).extracting(WebService.Param::key)
      .containsExactlyInAnyOrder("key", "qualityProfile", "project", "language", "projectUuid", "organization");
    WebService.Param profile = definition.param("key");
    assertThat(profile.deprecatedKey()).isEqualTo("profileKey");
    assertThat(profile.deprecatedSince()).isEqualTo("6.6");
    WebService.Param languageParam = definition.param("language");
    assertThat(languageParam.possibleValues()).containsOnly(LANGUAGE_1, LANGUAGE_2);
    assertThat(languageParam.exampleValue()).isNull();
    WebService.Param project = definition.param("project");
    assertThat(project.deprecatedKey()).isEqualTo("projectKey");
    WebService.Param projectUuid = definition.param("projectUuid");
    assertThat(projectUuid.deprecatedSince()).isEqualTo("6.5");
    WebService.Param organizationParam = definition.param("organization");
    assertThat(organizationParam.since()).isEqualTo("6.4");
    assertThat(organizationParam.isInternal()).isTrue();
  }

  @Test
  public void add_project_on_profile_of_default_organization() {
    logInAsProfileAdmin(db.getDefaultOrganization());
    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization());

    TestResponse response = call(project, profile);
    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);

    assertProjectIsAssociatedToProfile(project, profile);
  }

  @Test
  public void add_project_on_profile_of_specified_organization() {
    OrganizationDto org1 = db.organizations().insert();
    logInAsProfileAdmin(org1);
    ComponentDto project = db.components().insertPrivateProject(org1);
    QProfileDto profile = db.qualityProfiles().insert(org1, p -> p.setLanguage(LANGUAGE_1));

    TestResponse response = call(org1, project, profile);
    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);

    assertProjectIsAssociatedToProfile(project, profile);
  }

  @Test
  public void as_qprofile_editor() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization, qp -> qp.setLanguage(LANGUAGE_1));
    db.qualityProfiles().addUserPermission(qualityProfile, user);
    ComponentDto project = db.components().insertPrivateProject(organization);
    userSession.logIn(user);

    call(organization, project, qualityProfile);

    assertProjectIsAssociatedToProfile(project, qualityProfile);
  }

  @Test
  public void fail_if_profile_and_project_are_in_different_organizations() {
    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();
    logInAsProfileAdmin(org2);
    ComponentDto project = db.components().insertPrivateProject(org1);
    QProfileDto profileInOrg2 = db.qualityProfiles().insert(org2, p -> p.setLanguage(LANGUAGE_1));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Project and quality profile must have the same organization");

    call(org2, project, profileInOrg2);
  }

  @Test
  public void fail_if_profile_is_not_found_in_specified_organization() {
    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();
    logInAsProfileAdmin(org1);
    ComponentDto project = db.components().insertPrivateProject(org1);
    QProfileDto profileInOrg2 = db.qualityProfiles().insert(org2, p -> p.setLanguage(LANGUAGE_1));

    expectedException.expect(NotFoundException.class);
    expectedException
      .expectMessage("Quality Profile for language '" + LANGUAGE_1 + "' and name '" + profileInOrg2.getName() + "' does not exist in organization '" + org1.getKey() + "'");

    call(org1, project, profileInOrg2);
  }

  @Test
  public void change_association_in_default_organization() {
    logInAsProfileAdmin(db.getDefaultOrganization());

    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());
    // two profiles on same language
    QProfileDto profile1 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(LANGUAGE_1));
    QProfileDto profile2 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(LANGUAGE_1));
    db.qualityProfiles().associateWithProject(project, profile1);

    call(project, profile2);

    assertProjectIsNotAssociatedToProfile(project, profile1);
    assertProjectIsAssociatedToProfile(project, profile2);
  }

  @Test
  public void changing_association_does_not_change_other_language_associations() {
    logInAsProfileAdmin(db.getDefaultOrganization());
    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());
    QProfileDto profile1Language1 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(LANGUAGE_1));
    QProfileDto profile2Language2 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(LANGUAGE_2));
    QProfileDto profile3Language1 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(LANGUAGE_1));
    db.qualityProfiles().associateWithProject(project, profile1Language1, profile2Language2);

    call(project, profile3Language1);

    assertProjectIsAssociatedToProfile(project, profile3Language1);
    assertProjectIsAssociatedToProfile(project, profile2Language2);
  }

  @Test
  public void project_administrator_can_change_profile() {
    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization());
    userSession.logIn(db.users().insertUser()).addProjectPermission(UserRole.ADMIN, project);

    call(project, profile);

    assertProjectIsAssociatedToProfile(project, profile);
  }

  @Test
  public void throw_ForbiddenException_if_not_project_nor_organization_administrator() {
    userSession.logIn(db.users().insertUser());
    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization());

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    call(project, profile);
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    userSession.anonymous();
    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization());

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    call(project, profile);
  }

  @Test
  public void throw_NotFoundException_if_project_does_not_exist() {
    logInAsProfileAdmin(db.getDefaultOrganization());
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component id 'unknown' not found");

    tester.newRequest()
      .setParam("projectUuid", "unknown")
      .setParam("profileKey", profile.getKee())
      .execute();
  }

  @Test
  public void throw_NotFoundException_if_profile_does_not_exist() {
    logInAsProfileAdmin(db.getDefaultOrganization());
    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Quality Profile with key 'unknown' does not exist");

    tester.newRequest()
      .setParam("projectUuid", project.uuid())
      .setParam("profileKey", "unknown")
      .execute();
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn(db.users().insertUser()).addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    QProfileDto profile = db.qualityProfiles().insert(organization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component key '%s' not found", branch.getDbKey()));

    tester.newRequest()
      .setParam("project", branch.getDbKey())
      .setParam("profileKey", profile.getKee())
      .execute();
  }

  @Test
  public void fail_when_using_branch_uuid() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn(db.users().insertUser()).addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    QProfileDto profile = db.qualityProfiles().insert(organization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component id '%s' not found", branch.uuid()));

    tester.newRequest()
      .setParam("projectUuid", branch.uuid())
      .setParam("profileKey", profile.getKee())
      .execute();
  }

  private void assertProjectIsAssociatedToProfile(ComponentDto project, QProfileDto profile) {
    QProfileDto loaded = dbClient.qualityProfileDao().selectAssociatedToProjectAndLanguage(db.getSession(), project, profile.getLanguage());
    assertThat(loaded.getKee()).isEqualTo(profile.getKee());
  }

  private void assertProjectIsNotAssociatedToProfile(ComponentDto project, QProfileDto profile) {
    QProfileDto loaded = dbClient.qualityProfileDao().selectAssociatedToProjectAndLanguage(db.getSession(), project, profile.getLanguage());
    assertThat(loaded == null || !loaded.getKee().equals(profile.getKee())).isTrue();
  }

  private void logInAsProfileAdmin(OrganizationDto organization) {
    userSession.logIn(db.users().insertUser()).addPermission(ADMINISTER_QUALITY_PROFILES, organization);
  }

  private TestResponse call(ComponentDto project, QProfileDto qualityProfile) {
    TestRequest request = tester.newRequest()
      .setParam("projectUuid", project.uuid())
      .setParam("key", qualityProfile.getKee());
    return request.execute();
  }

  private TestResponse call(OrganizationDto organization, ComponentDto project, QProfileDto qualityProfile) {
    TestRequest request = tester.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("projectUuid", project.uuid())
      .setParam("language", qualityProfile.getLanguage())
      .setParam("qualityProfile", qualityProfile.getName());
    return request.execute();
  }
}
