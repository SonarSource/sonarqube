/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.ProjectLinks.Link;
import org.sonarqube.ws.ProjectLinks.SearchWsResponse;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_PROJECT_ID;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_PROJECT_KEY;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = db.getDbClient();
  private final WsActionTester ws = new WsActionTester(new SearchAction(dbClient, userSession, TestComponentFinder.from(db)));

  @Test
  public void example() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.projectLinks().insertProvidedLink(project, l -> l.setUuid("1").setType("homepage").setName("Homepage").setHref("http://example.org"));
    db.projectLinks().insertCustomLink(project, l -> l.setUuid("2").setType("custom").setName("Custom").setHref("http://example.org/custom"));
    logInAsProjectAdministrator(project);

    String result = ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("search-example.json"));
  }

  @Test
  public void request_by_project_id() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto link = db.projectLinks().insertCustomLink(project);
    logInAsProjectAdministrator(project);

    SearchWsResponse response = callByUuid(project.getUuid());

    assertThat(response.getLinksList())
      .extracting(Link::getId, Link::getName)
      .containsExactlyInAnyOrder(tuple(link.getUuid(), link.getName()));
  }

  @Test
  public void request_by_project_key() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto link = db.projectLinks().insertCustomLink(project);
    logInAsProjectAdministrator(project);

    SearchWsResponse response = callByKey(project.getKey());

    assertThat(response.getLinksList())
      .extracting(Link::getId, Link::getName)
      .containsExactlyInAnyOrder(tuple(link.getUuid(), link.getName()));
  }

  @Test
  public void response_fields() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto homepageLink = db.projectLinks().insertProvidedLink(project);
    ProjectLinkDto customLink = db.projectLinks().insertCustomLink(project);
    logInAsProjectAdministrator(project);

    SearchWsResponse response = callByKey(project.getKey());

    assertThat(response.getLinksList()).extracting(Link::getId, Link::getName, Link::getType, Link::getUrl)
      .containsExactlyInAnyOrder(
        tuple(homepageLink.getUuid(), "", homepageLink.getType(), homepageLink.getHref()),
        tuple(customLink.getUuid(), customLink.getName(), customLink.getType(), customLink.getHref()));
  }

  @Test
  public void several_projects() {
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto link1 = db.projectLinks().insertCustomLink(project1);
    ProjectLinkDto link2 = db.projectLinks().insertCustomLink(project2);
    userSession.addProjectPermission(USER, project1);
    userSession.addProjectPermission(USER, project2);

    SearchWsResponse response = callByKey(project1.getKey());

    assertThat(response.getLinksList())
      .extracting(Link::getId, Link::getName)
      .containsExactlyInAnyOrder(tuple(link1.getUuid(), link1.getName()));
  }

  @Test
  public void request_does_not_fail_when_link_has_no_name() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto link = db.projectLinks().insertProvidedLink(project);
    logInAsProjectAdministrator(project);

    SearchWsResponse response = callByKey(project.getKey());

    assertThat(response.getLinksList())
      .extracting(Link::getId, Link::hasName)
      .containsExactlyInAnyOrder(tuple(link.getUuid(), false));
  }

  @Test
  public void project_administrator_can_search_for_links() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto link = db.projectLinks().insertCustomLink(project);
    logInAsProjectAdministrator(project);

    SearchWsResponse response = callByKey(project.getKey());

    assertThat(response.getLinksList())
      .extracting(Link::getId, Link::getName)
      .containsExactlyInAnyOrder(tuple(link.getUuid(), link.getName()));
  }

  @Test
  public void project_user_can_search_for_links() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto link = db.projectLinks().insertCustomLink(project);
    userSession.logIn().addProjectPermission(USER, project);

    SearchWsResponse response = callByKey(project.getKey());

    assertThat(response.getLinksList())
      .extracting(Link::getId, Link::getName)
      .containsExactlyInAnyOrder(tuple(link.getUuid(), link.getName()));
  }

  @Test
  public void fail_when_no_project() {
    assertThatThrownBy(() -> callByKey("unknown"))
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_if_directory() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(ComponentTesting.newDirectory(project, "A/B"));
    failIfNotAProjectWithKey(project, directory);
    failIfNotAProjectWithUuid(project, directory);
  }

  @Test
  public void fail_if_file() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    failIfNotAProjectWithKey(project, file);
    failIfNotAProjectWithUuid(project, file);
  }

  @Test
  public void fail_if_view() {
    ComponentDto view = db.components().insertPrivatePortfolio();
    failIfNotAProjectWithKey(view, view);
    failIfNotAProjectWithUuid(view, view);
  }

  @Test
  public void fail_if_insufficient_privileges() {
    userSession.anonymous();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();

    assertThatThrownBy(() -> callByKey(project.getKey()))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_when_both_id_and_key_are_provided() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    logInAsProjectAdministrator(project);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .execute())
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_when_no_id_nor_key_are_provided() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .execute())
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_when_using_branch_db_uuid() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.logIn().addProjectPermission(USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_PROJECT_ID, branch.uuid())
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining(format("Project '%s' not found", branch.uuid()));
  }

  @Test
  public void define_search_action() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isNotNull();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(2);
  }

  private SearchWsResponse callByKey(String projectKey) {
    return ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, projectKey)
      .executeProtobuf(SearchWsResponse.class);
  }

  private SearchWsResponse callByUuid(String projectUuid) {
    return ws.newRequest()
      .setParam(PARAM_PROJECT_ID, projectUuid)
      .executeProtobuf(SearchWsResponse.class);
  }

  private void logInAsProjectAdministrator(ProjectDto project) {
    userSession.logIn().addProjectPermission(ADMIN, project);
  }

  private void failIfNotAProjectWithKey(ComponentDto root, ComponentDto component) {
    userSession.logIn().addProjectPermission(USER, root);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, component.getKey())
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Project '" + component.getKey() + "' not found");
  }

  private void failIfNotAProjectWithUuid(ComponentDto root, ComponentDto component) {
    userSession.logIn().addProjectPermission(USER, root);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_PROJECT_ID, component.uuid())
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Project '" + component.uuid() + "' not found");
  }
}
