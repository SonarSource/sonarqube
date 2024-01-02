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
package org.sonar.server.projectlink.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_ID;

public class DeleteActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final WsActionTester ws = new WsActionTester(new DeleteAction(dbClient, userSession, TestComponentFinder.from(db)));

  @Test
  public void no_response() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto link = db.projectLinks().insertCustomLink(project);
    logInAsProjectAdministrator(project);

    TestResponse response = deleteLink(link);

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(response.getInput()).isEmpty();
  }

  @Test
  public void remove_custom_link() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto link = db.projectLinks().insertCustomLink(project);
    logInAsProjectAdministrator(project);

    deleteLink(link);

    assertLinkIsDeleted(link.getUuid());
  }

  @Test
  public void delete_whenGlobalAdminPermission_shouldDeleteLink() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto link = db.projectLinks().insertCustomLink(project);
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER);

    deleteLink(link);

    assertLinkIsDeleted(link.getUuid());
  }

  @Test
  public void keep_links_of_another_project() {
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto customLink1 = db.projectLinks().insertCustomLink(project1);
    ProjectLinkDto customLink2 = db.projectLinks().insertCustomLink(project2);
    userSession.logIn().addProjectPermission(ADMIN, project1, project2);

    deleteLink(customLink1);

    assertLinkIsDeleted(customLink1.getUuid());
    assertLinkIsNotDeleted(customLink2.getUuid());
  }

  @Test
  public void fail_when_delete_provided_link() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto link = db.projectLinks().insertProvidedLink(project);
    logInAsProjectAdministrator(project);

    assertThatThrownBy(() -> deleteLink(link))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("Provided link cannot be deleted");
  }

  @Test
  public void fail_on_unknown_link() {
    TestRequest testRequest = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_ID, "UNKNOWN");
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_if_anonymous() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto link = db.projectLinks().insertCustomLink(project);
    userSession.anonymous();

    assertThatThrownBy(() -> deleteLink(link))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_not_project_admin() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto link = db.projectLinks().insertCustomLink(project);
    userSession.logIn();

    assertThatThrownBy(() -> deleteLink(link))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void define_delete_action() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isTrue();
    assertThat(action.handler()).isNotNull();
    assertThat(action.responseExample()).isNull();
    assertThat(action.params()).hasSize(1);
  }

  private TestResponse deleteLink(ProjectLinkDto link) {
    return ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_ID, link.getUuid())
      .execute();
  }

  private void assertLinkIsDeleted(String uuid) {
    assertThat(dbClient.projectLinkDao().selectByUuid(dbSession, uuid)).isNull();
  }

  private void assertLinkIsNotDeleted(String uuid) {
    assertThat(dbClient.projectLinkDao().selectByUuid(dbSession, uuid)).isNotNull();
  }

  private void logInAsProjectAdministrator(ProjectDto project) {
    userSession.logIn().addProjectPermission(ADMIN, project);
  }
}
