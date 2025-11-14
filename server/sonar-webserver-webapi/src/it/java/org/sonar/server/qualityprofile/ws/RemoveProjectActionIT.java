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
package org.sonar.server.qualityprofile.ws;

import java.net.HttpURLConnection;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.resources.Languages;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.pushapi.qualityprofile.QualityProfileChangeEventService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;

public class RemoveProjectActionIT {
  private static final String LANGUAGE_1 = "xoo";
  private static final String LANGUAGE_2 = "foo";

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final DbClient dbClient = db.getDbClient();
  private final Languages languages = LanguageTesting.newLanguages(LANGUAGE_1, LANGUAGE_2);
  private final QProfileWsSupport wsSupport = new QProfileWsSupport(dbClient, userSession);

  private final QualityProfileChangeEventService qualityProfileChangeEventService = Mockito.mock(QualityProfileChangeEventService.class);
  private final RemoveProjectAction underTest = new RemoveProjectAction(dbClient, userSession, languages,
    new ComponentFinder(dbClient, new ComponentTypesRule().setRootQualifiers(ComponentQualifiers.PROJECT)), wsSupport, qualityProfileChangeEventService);
  private final WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.since()).isEqualTo("5.2");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.key()).isEqualTo("remove_project");

    assertThat(definition.params()).extracting(WebService.Param::key).containsOnly("qualityProfile", "project", "language");
    WebService.Param languageParam = definition.param("language");
    assertThat(languageParam.possibleValues()).containsOnly(LANGUAGE_1, LANGUAGE_2);
    assertThat(languageParam.exampleValue()).isNull();
    assertThat(languageParam.deprecatedSince()).isNullOrEmpty();
    WebService.Param profileName = definition.param("qualityProfile");
    assertThat(profileName.deprecatedSince()).isNullOrEmpty();
  }

  @Test
  public void remove_profile_from_project() {
    logInAsProfileAdmin();

    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    QProfileDto profileLang1 = db.qualityProfiles().insert(p -> p.setLanguage(LANGUAGE_1));
    QProfileDto profileLang2 = db.qualityProfiles().insert(p -> p.setLanguage(LANGUAGE_2));
    db.qualityProfiles().associateWithProject(project, profileLang1);
    db.qualityProfiles().associateWithProject(project, profileLang2);

    TestResponse response = call(project, profileLang1);
    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);

    assertProjectIsNotAssociatedToProfile(project, profileLang1);
    assertProjectIsAssociatedToProfile(project, profileLang2);
    verify(qualityProfileChangeEventService).publishRuleActivationToSonarLintClients(project, null, profileLang1);
  }

  @Test
  public void removal_does_not_fail_if_profile_is_not_associated_to_project() {
    logInAsProfileAdmin();

    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    QProfileDto profile = db.qualityProfiles().insert(qp -> qp.setLanguage("xoo"));

    TestResponse response = call(project, profile);
    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);

    assertProjectIsNotAssociatedToProfile(project, profile);
    verify(qualityProfileChangeEventService).publishRuleActivationToSonarLintClients(project, null, profile);
  }

  @Test
  public void project_administrator_can_remove_profile() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    QProfileDto profile = db.qualityProfiles().insert(qp -> qp.setLanguage("xoo"));
    db.qualityProfiles().associateWithProject(project, profile);
    userSession.logIn(db.users().insertUser()).addProjectPermission(ProjectPermission.ADMIN, project);

    call(project, profile);

    assertProjectIsNotAssociatedToProfile(project, profile);
    verify(qualityProfileChangeEventService).publishRuleActivationToSonarLintClients(project, null, profile);
  }

  @Test
  public void as_qprofile_editor_and_global_admin() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(LANGUAGE_1));
    db.qualityProfiles().associateWithProject(project, profile);
    UserDto user = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, user);
    userSession.logIn(user).addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    call(project, profile);

    assertProjectIsNotAssociatedToProfile(project, profile);
    verify(qualityProfileChangeEventService).publishRuleActivationToSonarLintClients(project, null, profile);
  }

  @Test
  public void as_qprofile_editor_fail_if_not_project_nor_global_admin() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(LANGUAGE_1));
    db.qualityProfiles().associateWithProject(project, profile);
    UserDto user = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, user);
    userSession.logIn(user);

    assertThatThrownBy(() -> call(project, profile))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_if_not_enough_permissions() {
    userSession.logIn(db.users().insertUser());
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    QProfileDto profile = db.qualityProfiles().insert(qp -> qp.setLanguage("xoo"));

    assertThatThrownBy(() -> call(project, profile))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_if_not_logged_in() {
    userSession.anonymous();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    QProfileDto profile = db.qualityProfiles().insert();

    assertThatThrownBy(() -> call(project, profile))
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }

  @Test
  public void fail_if_project_does_not_exist() {
    logInAsProfileAdmin();
    QProfileDto profile = db.qualityProfiles().insert();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("project", "unknown")
        .setParam("profileKey", profile.getKee())
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project 'unknown' not found");
  }

  @Test
  public void fail_if_profile_does_not_exist() {
    logInAsProfileAdmin();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("project", project.getKey())
        .setParam("language", "xoo")
        .setParam("qualityProfile", "unknown")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Quality Profile for language 'xoo' and name 'unknown' does not exist");
  }

  private void assertProjectIsAssociatedToProfile(ProjectDto project, QProfileDto profile) {
    QProfileDto loaded = dbClient.qualityProfileDao().selectAssociatedToProjectAndLanguage(db.getSession(), project, profile.getLanguage());
    assertThat(loaded.getKee()).isEqualTo(profile.getKee());
  }

  private void assertProjectIsNotAssociatedToProfile(ProjectDto project, QProfileDto profile) {
    QProfileDto loaded = dbClient.qualityProfileDao().selectAssociatedToProjectAndLanguage(db.getSession(), project, profile.getLanguage());
    assertThat(loaded == null || !loaded.getKee().equals(profile.getKee())).isTrue();
  }

  private void logInAsProfileAdmin() {
    userSession.logIn(db.users().insertUser()).addPermission(ADMINISTER_QUALITY_PROFILES);
  }

  private TestResponse call(ProjectDto project, QProfileDto qualityProfile) {
    TestRequest request = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("language", qualityProfile.getLanguage())
      .setParam("qualityProfile", qualityProfile.getName());
    return request.execute();
  }
}
