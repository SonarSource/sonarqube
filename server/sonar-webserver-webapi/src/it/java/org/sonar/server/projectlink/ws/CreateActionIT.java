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
package org.sonar.server.projectlink.ws;

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.PortfolioData;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.ProjectLinks;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_NAME;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_PROJECT_ID;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_PROJECT_KEY;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_URL;
import static org.sonar.test.JsonAssert.assertJson;

public class CreateActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final WsActionTester ws = new WsActionTester(new CreateAction(dbClient, userSession, TestComponentFinder.from(db), UuidFactoryFast.getInstance()));

  @Test
  public void example_with_key() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    logInAsProjectAdministrator(project);

    String result = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org")
      .execute().getInput();

    assertJson(result).ignoreFields("id").isSimilarTo(getClass().getResource("create-example.json"));
  }

  @Test
  public void example_with_id() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    logInAsProjectAdministrator(project);

    String result = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org")
      .execute().getInput();

    assertJson(result).ignoreFields("id").isSimilarTo(getClass().getResource("create-example.json"));
  }

  @Test
  public void require_project_admin() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    logInAsProjectAdministrator(project);

    createAndTest(project);
  }

  @Test
  public void with_long_name() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    logInAsProjectAdministrator(project);
    String longName = StringUtils.leftPad("", 60, "a");
    String expectedType = StringUtils.leftPad("", 20, "a");

    createAndTest(project, longName, "http://example.org", expectedType);
  }

  @Test
  public void fail_if_no_name() {
    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, "unknown")
      .setParam(PARAM_URL, "http://example.org");
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_long_name() {
    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, "unknown")
      .setParam(PARAM_NAME, StringUtils.leftPad("", 129, "*"))
      .setParam(PARAM_URL, "http://example.org");
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_no_url() {
    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, "unknown")
      .setParam(PARAM_NAME, "Custom");
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_long_url() {
    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, "unknown")
      .setParam(PARAM_NAME, "random")
      .setParam(PARAM_URL, StringUtils.leftPad("", 2049, "*"));
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_when_no_project() {
    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, "unknown")
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org");
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_if_anonymous() {
    userSession.anonymous();

    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());

    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org");
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_not_project_admin() {
    userSession.logIn();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();

    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org");
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_directory() {
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto directory = db.components().insertComponent(ComponentTesting.newDirectory(project.getMainBranchComponent(), "A/B"));
    failIfNotAProjectWithKey(project.getProjectDto(), directory);
    failIfNotAProjectWithUuid(project.getProjectDto(), directory);
  }

  @Test
  public void fail_if_file() {
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project.getMainBranchComponent()));
    failIfNotAProjectWithKey(project.getProjectDto(), file);
    failIfNotAProjectWithUuid(project.getProjectDto(), file);
  }

  @Test
  public void fail_if_view() {
    PortfolioData view = db.components().insertPrivatePortfolioData();

    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, view.getRootComponent());
    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org")
      .setParam(PARAM_PROJECT_KEY, view.getPortfolioDto().getKey());
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Project '" + view.getPortfolioDto().getKey() + "' not found");

    TestRequest testRequest1 = ws.newRequest()
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org")
      .setParam(PARAM_PROJECT_ID, view.getPortfolioDto().getUuid());
    assertThatThrownBy(testRequest1::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Project '" + view.getPortfolioDto().getUuid() + "' not found");

  }

  @Test
  public void fail_when_using_branch_db_uuid() {
    ProjectData project = db.components().insertPrivateProject();
    userSession.logIn().addProjectPermission(ProjectPermission.USER, project.getProjectDto());
    BranchDto branch = db.components().insertProjectBranch(project.getProjectDto());

    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_PROJECT_ID, branch.getUuid())
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org");
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining(format("Project '%s' not found", branch.getUuid()));
  }

  @Test
  public void define_create_action() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isTrue();
    assertThat(action.handler()).isNotNull();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(4);
  }

  private void failIfNotAProjectWithKey(ProjectDto project, ComponentDto component) {
    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);

    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org")
      .setParam(PARAM_PROJECT_KEY, component.getKey());
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Project '" + component.getKey() + "' not found");
  }

  private void failIfNotAProjectWithUuid(ProjectDto project, ComponentDto component) {
    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);

    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_NAME, "Custom")
      .setParam(PARAM_URL, "http://example.org")
      .setParam(PARAM_PROJECT_ID, component.uuid());
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Project '" + component.uuid() + "' not found");
  }

  private void createAndTest(ProjectDto project, String name, String url, String type) {
    ProjectLinks.CreateWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_NAME, name)
      .setParam(PARAM_URL, url)
      .executeProtobuf(ProjectLinks.CreateWsResponse.class);

    String newId = response.getLink().getId();

    ProjectLinkDto link = dbClient.projectLinkDao().selectByUuid(dbSession, newId);
    assertThat(link.getName()).isEqualTo(name);
    assertThat(link.getHref()).isEqualTo(url);
    assertThat(link.getType()).isEqualTo(type);
  }

  private void createAndTest(ProjectDto project) {
    createAndTest(project, "Custom", "http://example.org", "custom");
  }

  private void logInAsProjectAdministrator(ProjectDto project) {
    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);
  }
}
