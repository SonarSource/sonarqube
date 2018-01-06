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
package org.sonar.server.projectlink.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentLinkDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_ID;

public class DeleteActionTest {

  private static final String PROJECT_KEY = KEY_PROJECT_EXAMPLE_001;
  private static final String PROJECT_UUID = UUID_EXAMPLE_01;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private ComponentDbTester componentDb = new ComponentDbTester(db);

  private WsActionTester ws;

  private DeleteAction underTest;

  @Before
  public void setUp() {
    underTest = new DeleteAction(dbClient, userSession);
    ws = new WsActionTester(underTest);
  }

  @Test
  public void no_response() {
    ComponentDto project = insertProject();
    ComponentLinkDto link = insertCustomLink(project.uuid());
    logInAsProjectAdministrator(project);

    TestResponse response = deleteLink(link.getId());

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(response.getInput()).isEmpty();
  }

  @Test
  public void actual_removal() {
    ComponentDto project = insertProject();
    ComponentLinkDto link = insertCustomLink(project.uuid());
    long id = link.getId();
    logInAsProjectAdministrator(project);

    deleteLink(id);
    assertLinkIsDeleted(id);
  }

  @Test
  public void keep_links_of_another_project() {
    ComponentDto project1 = insertProject();
    ComponentDto project2 = insertProject("another", "abcd");
    ComponentLinkDto customLink1 = insertCustomLink(project1.uuid());
    ComponentLinkDto customLink2 = insertCustomLink(project2.uuid());
    Long id1 = customLink1.getId();
    Long id2 = customLink2.getId();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project1, project2);

    deleteLink(id1);
    assertLinkIsDeleted(id1);
    assertLinkIsNotDeleted(id2);
  }

  @Test
  public void fail_when_delete_provided_link() {
    ComponentDto project = insertProject();
    ComponentLinkDto link = insertHomepageLink(project.uuid());
    logInAsProjectAdministrator(project);

    expectedException.expect(BadRequestException.class);

    deleteLink(link.getId());
  }

  @Test
  public void fail_when_no_link() {
    expectedException.expect(NotFoundException.class);

    deleteLink("175");
  }

  @Test
  public void fail_if_anonymous() {
    userSession.anonymous();

    ComponentDto project = insertProject();
    ComponentLinkDto link = insertCustomLink(project.uuid());

    expectedException.expect(ForbiddenException.class);

    deleteLink(link.getId());
  }

  @Test
  public void fail_if_not_project_admin() {
    userSession.logIn();

    ComponentDto project = insertProject();
    ComponentLinkDto link = insertCustomLink(project.uuid());

    expectedException.expect(ForbiddenException.class);
    deleteLink(link.getId());
  }

  private ComponentDto insertProject(String projectKey, String projectUuid) {
    return componentDb.insertComponent(new ComponentDto()
      .setOrganizationUuid("org1")
      .setUuid(projectUuid)
      .setDbKey(projectKey)
      .setUuidPath("")
      .setRootUuid("")
      .setProjectUuid(""));
  }

  private ComponentDto insertProject() {
    return insertProject(PROJECT_KEY, PROJECT_UUID);
  }

  private void insertLink(ComponentLinkDto linkDto) {
    dbClient.componentLinkDao().insert(dbSession, linkDto);
    dbSession.commit();
  }

  private ComponentLinkDto insertHomepageLink(String projectUuid) {
    ComponentLinkDto link = new ComponentLinkDto()
      .setComponentUuid(projectUuid)
      .setName("Homepage")
      .setType("homepage")
      .setHref("http://example.org");
    insertLink(link);
    return link;
  }

  private ComponentLinkDto insertCustomLink(String projectUuid) {
    ComponentLinkDto link = new ComponentLinkDto()
      .setComponentUuid(projectUuid)
      .setName("Custom")
      .setHref("http://example.org/custom");
    insertLink(link);
    return link;
  }

  private TestResponse deleteLink(String id) {
    return ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_ID, id)
      .execute();
  }

  private TestResponse deleteLink(Long id) {
    return deleteLink(String.valueOf(id));
  }

  private void assertLinkIsDeleted(Long id) {
    assertThat(dbClient.componentLinkDao().selectById(dbSession, id)).isNull();
  }

  private void assertLinkIsNotDeleted(Long id) {
    assertThat(dbClient.componentLinkDao().selectById(dbSession, id)).isNotNull();
  }

  private void logInAsProjectAdministrator(ComponentDto project) {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
  }
}
