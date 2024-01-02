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
package org.sonar.server.project.ws;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbTester;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.web.UserRole.SCAN;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.Projects.SearchMyScannableProjectsResponse;
import static org.sonarqube.ws.Projects.SearchMyScannableProjectsResponse.Project;

public class SearchMyScannableProjectsActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create();

  private final WsActionTester ws = new WsActionTester(
    new SearchMyScannableProjectsAction(db.getDbClient(), new ProjectFinder(db.getDbClient(), userSession)));

  @Test
  public void projects_filtered_by_query() {
    ProjectDto project1 = db.components().insertPrivateProject(p -> p.setKey("project:one").setName("Projet Un")).getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject(p -> p.setKey("project:two").setName("Projet Deux")).getProjectDto();
    ProjectDto project3 = db.components().insertPrivateProject(p -> p.setKey("project:three").setName("Project Three")).getProjectDto();
    ProjectDto project4 = db.components().insertPublicProject(p -> p.setKey("project:four").setName("Project Four")).getProjectDto();
    userSession.addProjectPermission(SCAN, project1, project2, project3, project4);

    List<Project> result = ws.newRequest()
      .setParam(TEXT_QUERY, "project")
      .executeProtobuf(SearchMyScannableProjectsResponse.class)
      .getProjectsList();

    assertThat(result)
      .extracting(Project::getKey, Project::getName)
      .containsExactlyInAnyOrder(
        tuple("project:three", "Project Three"),
        tuple("project:four", "Project Four"));
  }

  @Test
  public void projects_not_filtered_by_empty_query() {
    ProjectDto project1 = db.components().insertPrivateProject(p -> p.setKey("project:one").setName("Projet Un")).getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject(p -> p.setKey("project:two").setName("Projet Deux")).getProjectDto();
    ProjectDto project3 = db.components().insertPrivateProject(p -> p.setKey("project:three").setName("Project Three")).getProjectDto();
    ProjectDto project4 = db.components().insertPublicProject(p -> p.setKey("project:four").setName("Project Four")).getProjectDto();
    userSession.addProjectPermission(SCAN, project1, project2, project3, project4);

    List<Project> result = ws.newRequest()
      .setParam(TEXT_QUERY, "")
      .executeProtobuf(SearchMyScannableProjectsResponse.class)
      .getProjectsList();

    assertThat(result)
      .extracting(Project::getKey, Project::getName)
      .containsExactlyInAnyOrder(
        tuple("project:one", "Projet Un"),
        tuple("project:two", "Projet Deux"),
        tuple("project:three", "Project Three"),
        tuple("project:four", "Project Four"));
  }

  @Test
  public void projects_filtered_by_scan_permission() {
    db.components().insertPrivateProject(p -> p.setKey("project:one").setName("Projet Un")).getProjectDto();
    db.components().insertPrivateProject(p -> p.setKey("project:two").setName("Projet Deux")).getProjectDto();
    db.components().insertPrivateProject(p -> p.setKey("project:three").setName("Project Three")).getProjectDto();
    db.components().insertPublicProject(p -> p.setKey("project:four").setName("Project Four")).getProjectDto();

    List<Project> result = ws.newRequest()
      .executeProtobuf(SearchMyScannableProjectsResponse.class)
      .getProjectsList();

    assertThat(result).isEmpty();
  }

  @Test
  public void projects_filtered_for_anonymous_user() {
    ProjectDto project1 = db.components().insertPrivateProject(p -> p.setKey("project:one").setName("Projet Un")).getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject(p -> p.setKey("project:two").setName("Projet Deux")).getProjectDto();
    ProjectDto project3 = db.components().insertPrivateProject(p -> p.setKey("project:three").setName("Project Three")).getProjectDto();
    ProjectDto project4 = db.components().insertPublicProject(p -> p.setKey("project:four").setName("Project Four")).getProjectDto();
    userSession.addProjectPermission(SCAN, project1, project2, project3, project4);

    WsActionTester ws = new WsActionTester(
      new SearchMyScannableProjectsAction(db.getDbClient(), new ProjectFinder(db.getDbClient(), userSession.anonymous())));

    List<Project> result = ws.newRequest()
      .executeProtobuf(SearchMyScannableProjectsResponse.class)
      .getProjectsList();

    assertThat(result).isEmpty();
  }

  @Test
  public void projects_not_filtered_due_to_global_scan_permission() {
    db.components().insertPrivateProject(p -> p.setKey("project:one").setName("Projet Un")).getProjectDto();
    db.components().insertPrivateProject(p -> p.setKey("project:two").setName("Projet Deux")).getProjectDto();
    db.components().insertPrivateProject(p -> p.setKey("project:three").setName("Project Three")).getProjectDto();
    db.components().insertPublicProject(p -> p.setKey("project:four").setName("Project Four")).getProjectDto();
    userSession.addPermission(GlobalPermission.SCAN);

    List<Project> result = ws.newRequest()
      .executeProtobuf(SearchMyScannableProjectsResponse.class)
      .getProjectsList();

    assertThat(result)
      .extracting(Project::getKey, Project::getName)
      .containsExactlyInAnyOrder(
        tuple("project:one", "Projet Un"),
        tuple("project:two", "Projet Deux"),
        tuple("project:three", "Project Three"),
        tuple("project:four", "Project Four"));
  }

  @Test
  public void json_example() {
    ProjectDto project1 = db.components().insertPrivateProject(p -> p.setKey("project-key-1").setName("Project 1")).getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject(p -> p.setKey("project-key-2").setName("Project 2")).getProjectDto();
    ProjectDto project3 = db.components().insertPublicProject(p -> p.setKey("public-project-without-scan-permissions")
      .setName("Public Project with Scan Permissions")).getProjectDto();
    userSession.addProjectPermission(SCAN, project1, project2);
    userSession.registerProjects(project3);

    String result = ws.newRequest()
      .execute().getInput();

    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
    assertJson(ws.getDef().responseExampleAsString()).isSimilarTo(result);
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("search_my_scannable_projects");
    assertThat(definition.since()).isEqualTo("9.5");
    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.isPost()).isFalse();
    assertThat(definition.responseExampleAsString()).isNotEmpty();

    assertThat(definition.params())
      .extracting(Param::key, Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("q", false));
  }

}
