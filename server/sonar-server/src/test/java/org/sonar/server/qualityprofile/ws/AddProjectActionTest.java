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
import org.sonar.db.qualityprofile.QualityProfileDto;
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
  public void test_definition() {
    WebService.Action definition = tester.getDef();
    assertThat(definition.since()).isEqualTo("5.2");
    assertThat(definition.isPost()).isTrue();

    // parameters
    assertThat(definition.params()).extracting(WebService.Param::key).containsOnly("profileKey", "profileName", "projectKey", "language", "projectUuid", "organization");
    WebService.Param languageParam = definition.param("language");
    assertThat(languageParam.possibleValues()).containsOnly(LANGUAGE_1, LANGUAGE_2);
    assertThat(languageParam.exampleValue()).isNull();
    WebService.Param organizationParam = definition.param("organization");
    assertThat(organizationParam.since()).isEqualTo("6.4");
    assertThat(organizationParam.isInternal()).isTrue();
  }

  @Test
  public void add_project_on_profile_of_default_organization() {
    logInAsProfileAdmin(db.getDefaultOrganization());
    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());
    QualityProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization());

    TestResponse response = call(project, profile);
    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);

    assertProjectIsAssociatedToProfile(project, profile);
  }

  @Test
  public void add_project_on_profile_of_specified_organization() {
    OrganizationDto org1 = db.organizations().insert();
    logInAsProfileAdmin(org1);
    ComponentDto project = db.components().insertPrivateProject(org1);
    QualityProfileDto profile = db.qualityProfiles().insert(org1, p -> p.setLanguage(LANGUAGE_1));

    TestResponse response = call(org1, project, profile);
    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);

    assertProjectIsAssociatedToProfile(project, profile);
  }

  @Test
  public void throw_IAE_if_profile_and_project_are_in_different_organizations() {
    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();
    logInAsProfileAdmin(org1);
    ComponentDto project = db.components().insertPrivateProject(org1);
    QualityProfileDto profileInOrg2 = db.qualityProfiles().insert(org2, p -> p.setLanguage(LANGUAGE_1));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Project and Quality profile must have same organization");

    call(org2, project, profileInOrg2);

    assertProjectIsNotAssociatedToProfile(project, profileInOrg2);
  }

  @Test
  public void throw_NotFoundException_if_profile_is_not_found_in_specified_organization() {
    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();
    logInAsProfileAdmin(org1);
    ComponentDto project = db.components().insertPrivateProject(org1);
    QualityProfileDto profileInOrg2 = db.qualityProfiles().insert(org2, p -> p.setLanguage(LANGUAGE_1));

    expectedException.expect(NotFoundException.class);
    expectedException
      .expectMessage("Quality Profile for language '" + LANGUAGE_1 + "' and name '" + profileInOrg2.getName() + "' does not exist in organization '" + org1.getKey() + "'");

    call(org1, project, profileInOrg2);

    assertProjectIsNotAssociatedToProfile(project, profileInOrg2);
  }

  @Test
  public void change_association_in_default_organization() throws Exception {
    logInAsProfileAdmin(db.getDefaultOrganization());

    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());
    // two profiles on same language
    QualityProfileDto profile1 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(LANGUAGE_1));
    QualityProfileDto profile2 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(LANGUAGE_1));
    db.qualityProfiles().associateProjectWithQualityProfile(project, profile1);

    call(project, profile2);

    assertProjectIsNotAssociatedToProfile(project, profile1);
    assertProjectIsAssociatedToProfile(project, profile2);
  }

  @Test
  public void changing_association_does_not_change_other_language_associations() throws Exception {
    logInAsProfileAdmin(db.getDefaultOrganization());
    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());
    QualityProfileDto profile1Language1 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(LANGUAGE_1));
    QualityProfileDto profile2Language2 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(LANGUAGE_2));
    QualityProfileDto profile3Language1 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(LANGUAGE_1));
    db.qualityProfiles().associateProjectWithQualityProfile(project, profile1Language1, profile2Language2);

    call(project, profile3Language1);

    assertProjectIsAssociatedToProfile(project, profile3Language1);
    assertProjectIsAssociatedToProfile(project, profile2Language2);
  }

  @Test
  public void project_administrator_can_change_profile() throws Exception {
    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());
    QualityProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization());
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    call(project, profile);

    assertProjectIsAssociatedToProfile(project, profile);
  }

  @Test
  public void throw_ForbiddenException_if_not_project_nor_organization_administrator() {
    userSession.logIn();
    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());
    QualityProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization());

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    call(project, profile);
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    userSession.anonymous();
    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());
    QualityProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization());

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    call(project, profile);
  }

  @Test
  public void throw_NotFoundException_if_project_does_not_exist() {
    logInAsProfileAdmin(db.getDefaultOrganization());
    QualityProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component id 'unknown' not found");

    tester.newRequest()
      .setParam("projectUuid", "unknown")
      .setParam("profileKey", profile.getKey())
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

  private void assertProjectIsAssociatedToProfile(ComponentDto project, QualityProfileDto profile) {
    QualityProfileDto loaded = dbClient.qualityProfileDao().selectByProjectAndLanguage(db.getSession(), project.getKey(), profile.getLanguage());
    assertThat(loaded.getKey()).isEqualTo(profile.getKey());
  }

  private void assertProjectIsNotAssociatedToProfile(ComponentDto project, QualityProfileDto profile) {
    QualityProfileDto loaded = dbClient.qualityProfileDao().selectByProjectAndLanguage(db.getSession(), project.getKey(), profile.getLanguage());
    assertThat(loaded == null || !loaded.getKey().equals(profile.getKey())).isTrue();
  }

  private void logInAsProfileAdmin(OrganizationDto organization) {
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);
  }

  private TestResponse call(ComponentDto project, QualityProfileDto qualityProfile) {
    TestRequest request = tester.newRequest()
      .setParam("projectUuid", project.uuid())
      .setParam("profileKey", qualityProfile.getKey());
    return request.execute();
  }

  private TestResponse call(OrganizationDto organization, ComponentDto project, QualityProfileDto qualityProfile) {
    TestRequest request = tester.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("projectUuid", project.uuid())
      .setParam("language", qualityProfile.getLanguage())
      .setParam("profileName", qualityProfile.getName());
    return request.execute();
  }
}
