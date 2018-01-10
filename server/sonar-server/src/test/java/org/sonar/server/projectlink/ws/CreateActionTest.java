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

import java.io.IOException;
import java.util.Random;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentLinkDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.ProjectLinks;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_NAME;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_PROJECT_ID;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_PROJECT_KEY;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_URL;

public class CreateActionTest {

  private final String PROJECT_KEY = KEY_PROJECT_EXAMPLE_001;
  private final String PROJECT_UUID = UUID_EXAMPLE_01;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private WsActionTester ws;

  private CreateAction underTest;

  @Before
  public void setUp() {
    ComponentFinder componentFinder = TestComponentFinder.from(db);
    underTest = new CreateAction(dbClient, userSession, componentFinder);
    ws = new WsActionTester(underTest);
  }

  @Test
  public void example_with_key() {
    ComponentDto project = insertProject();
    logInAsProjectAdministrator(project);

    String result = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_PROJECT_KEY, project.getDbKey())
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org")
      .execute().getInput();

    assertJson(result).ignoreFields("id").isSimilarTo(getClass().getResource("create-example.json"));
  }

  @Test
  public void example_with_id() {
    ComponentDto project = insertProject();
    logInAsProjectAdministrator(project);

    String result = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org")
      .execute().getInput();

    assertJson(result).ignoreFields("id").isSimilarTo(getClass().getResource("create-example.json"));
  }

  @Test
  public void require_project_admin() throws IOException {
    ComponentDto project = insertProject();
    logInAsProjectAdministrator(project);
    createAndTest(project);
  }

  @Test
  public void with_long_name() throws IOException {
    ComponentDto project = insertProject();
    logInAsProjectAdministrator(project);

    String longName = StringUtils.leftPad("", 60, "a");
    String expectedType = StringUtils.leftPad("", 20, "a");
    createAndTest(project, longName, "http://example.org", expectedType);
  }

  @Test
  public void fail_if_no_name() {
    expectedException.expect(IllegalArgumentException.class);

    ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, "unknown")
      .setParam(PARAM_URL, "http://example.org")
      .execute();
  }

  @Test
  public void fail_if_long_name() {
    expectedException.expect(IllegalArgumentException.class);
    ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, "unknown")
      .setParam(PARAM_NAME, StringUtils.leftPad("", 129, "*"))
      .setParam(PARAM_URL, "http://example.org")
      .execute();
  }

  @Test
  public void fail_if_no_url() {
    expectedException.expect(IllegalArgumentException.class);
    ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, "unknown")
      .setParam(PARAM_NAME, "Custom")
      .execute();
  }

  @Test
  public void fail_if_long_url() {
    expectedException.expect(IllegalArgumentException.class);
    ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, "unknown")
      .setParam(PARAM_NAME, "random")
      .setParam(PARAM_URL, StringUtils.leftPad("", 2049, "*"))
      .execute();
  }

  @Test
  public void fail_when_no_project() {
    expectedException.expect(NotFoundException.class);
    ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, "unknown")
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org")
      .execute();
  }

  @Test
  public void fail_if_anonymous() {
    userSession.anonymous();
    insertProject();

    expectedException.expect(ForbiddenException.class);
    ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, PROJECT_KEY)
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org")
      .execute();
  }

  @Test
  public void fail_if_not_project_admin() {
    userSession.logIn();
    insertProject();

    expectedException.expect(ForbiddenException.class);
    ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, PROJECT_KEY)
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org")
      .execute();
  }

  @Test
  public void fail_if_module() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(ComponentTesting.newModuleDto(project));
    failIfNotAProject(project, module);
  }

  @Test
  public void fail_if_directory() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto directory = db.components().insertComponent(ComponentTesting.newDirectory(project, "A/B"));
    failIfNotAProject(project, directory);
  }

  @Test
  public void fail_if_file() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    failIfNotAProject(project, file);
  }

  @Test
  public void fail_if_subview() {
    ComponentDto view = db.components().insertView();
    ComponentDto subview = db.components().insertComponent(ComponentTesting.newSubView(view));
    failIfNotAProject(view, subview);
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Project key '%s' not found", branch.getDbKey()));

    ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, branch.getDbKey())
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org")
      .execute();
  }

  @Test
  public void fail_when_using_branch_db_uuid() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Project id '%s' not found", branch.uuid()));

    ws.newRequest()
      .setParam(PARAM_PROJECT_ID, branch.uuid())
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org")
      .execute();
  }

  private void failIfNotAProject(ComponentDto root, ComponentDto component) {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, root);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Component '" + component.getDbKey() + "' (id: " + component.uuid() + ") must be a project");

    TestRequest testRequest = ws.newRequest();
    if (new Random().nextBoolean()) {
      testRequest.setParam(PARAM_PROJECT_KEY, component.getDbKey());
    } else {
      testRequest.setParam(PARAM_PROJECT_ID, component.uuid());
    }
    testRequest
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org")
      .execute();
  }

  private ComponentDto insertProject() {
    OrganizationDto org = db.organizations().insert();
    return db.components().insertComponent(
      ComponentTesting.newPrivateProjectDto(org, PROJECT_UUID).setDbKey(PROJECT_KEY));
  }

  private void createAndTest(ComponentDto project, String name, String url, String type) {
    ProjectLinks.CreateWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_PROJECT_KEY, project.getDbKey())
      .setParam(PARAM_NAME, name)
      .setParam(PARAM_URL, url)
      .executeProtobuf(ProjectLinks.CreateWsResponse.class);

    long newId = Long.valueOf(response.getLink().getId());

    ComponentLinkDto link = dbClient.componentLinkDao().selectById(dbSession, newId);
    assertThat(link.getName()).isEqualTo(name);
    assertThat(link.getHref()).isEqualTo(url);
    assertThat(link.getType()).isEqualTo(type);
  }

  private void createAndTest(ComponentDto project) throws IOException {
    createAndTest(project, "Custom", "http://example.org", "custom");
  }

  private void logInAsProjectAdministrator(ComponentDto project) {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
  }
}
