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
package org.sonar.server.projectlink.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_ID;

public class DeleteActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private WsActionTester ws = new WsActionTester(new DeleteAction(dbClient, userSession));

  @Test
  public void no_response() {
    ComponentDto project = db.components().insertPrivateProject();
    ProjectLinkDto link = db.componentLinks().insertCustomLink(project);
    logInAsProjectAdministrator(project);

    TestResponse response = deleteLink(link);

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(response.getInput()).isEmpty();
  }

  @Test
  public void remove_custom_link() {
    ComponentDto project = db.components().insertPrivateProject();
    ProjectLinkDto link = db.componentLinks().insertCustomLink(project);
    logInAsProjectAdministrator(project);

    deleteLink(link);

    assertLinkIsDeleted(link.getUuid());
  }

  @Test
  public void keep_links_of_another_project() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    ProjectLinkDto customLink1 = db.componentLinks().insertCustomLink(project1);
    ProjectLinkDto customLink2 = db.componentLinks().insertCustomLink(project2);
    userSession.logIn().addProjectPermission(ADMIN, project1, project2);

    deleteLink(customLink1);

    assertLinkIsDeleted(customLink1.getUuid());
    assertLinkIsNotDeleted(customLink2.getUuid());
  }

  @Test
  public void fail_when_delete_provided_link() {
    ComponentDto project = db.components().insertPrivateProject();
    ProjectLinkDto link = db.componentLinks().insertProvidedLink(project);
    logInAsProjectAdministrator(project);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Provided link cannot be deleted");

    deleteLink(link);
  }

  @Test
  public void fail_on_unknown_link() {
    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_ID, "UNKNOWN")
      .execute();
  }

  @Test
  public void fail_if_anonymous() {
    ComponentDto project = db.components().insertPrivateProject();
    ProjectLinkDto link = db.componentLinks().insertCustomLink(project);
    userSession.anonymous();

    expectedException.expect(ForbiddenException.class);

    deleteLink(link);
  }

  @Test
  public void fail_if_not_project_admin() {
    ComponentDto project = db.components().insertPrivateProject();
    ProjectLinkDto link = db.componentLinks().insertCustomLink(project);
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    deleteLink(link);
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

  private void logInAsProjectAdministrator(ComponentDto project) {
    userSession.logIn().addProjectPermission(ADMIN, project);
  }
}
