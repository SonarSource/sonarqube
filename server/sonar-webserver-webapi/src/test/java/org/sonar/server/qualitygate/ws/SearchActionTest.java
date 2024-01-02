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
package org.sonar.server.qualitygate.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates.SearchResponse;
import org.sonarqube.ws.Qualitygates.SearchResponse.Result;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.sonar.api.server.ws.WebService.SelectionMode.ALL;
import static org.sonar.api.server.ws.WebService.SelectionMode.DESELECTED;
import static org.sonar.api.server.ws.WebService.SelectionMode.SELECTED;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PAGE;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PAGE_SIZE;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SELECTED;

public class SearchActionTest {


  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final SearchAction underTest = new SearchAction(dbClient, userSession,
    new QualityGatesWsSupport(dbClient, userSession, TestComponentFinder.from(db)));
  private final WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void search_projects_of_a_quality_gate() {
    ComponentDto project = db.components().insertPublicProject();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDto(project), qualityGate);

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_ID, valueOf(qualityGate.getUuid()))
      .executeProtobuf(SearchResponse.class);

    assertThat(response.getResultsList())
      .extracting(Result::getKey, Result::getName)
      .containsExactlyInAnyOrder(tuple(project.getKey(), project.name()));
  }

  @Test
  public void return_empty_association() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_ID, valueOf(qualityGate.getUuid()))
      .executeProtobuf(SearchResponse.class);

    assertThat(response.getResultsList()).isEmpty();
  }

  @Test
  public void return_all_projects() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto unassociatedProject = db.components().insertPublicProjectDto();
    ProjectDto associatedProject = db.components().insertPublicProjectDto();
    db.qualityGates().associateProjectToQualityGate(associatedProject, qualityGate);

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_ID, valueOf(qualityGate.getUuid()))
      .setParam(PARAM_SELECTED, ALL.value())
      .executeProtobuf(SearchResponse.class);

    assertThat(response.getResultsList())
      .extracting(Result::getName, Result::getKey, Result::getSelected)
      .containsExactlyInAnyOrder(
        tuple(associatedProject.getName(), associatedProject.getKey(), true),
        tuple(unassociatedProject.getName(), unassociatedProject.getKey(), false));
  }

  @Test
  public void return_only_associated_project() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto associatedProject = db.components().insertPublicProjectDto();
    ProjectDto unassociatedProject = db.components().insertPublicProjectDto();
    db.qualityGates().associateProjectToQualityGate(associatedProject, qualityGate);

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_ID, valueOf(qualityGate.getUuid()))
      .setParam(PARAM_SELECTED, SELECTED.value())
      .executeProtobuf(SearchResponse.class);

    assertThat(response.getResultsList())
      .extracting(Result::getName, Result::getSelected)
      .containsExactlyInAnyOrder(tuple(associatedProject.getName(), true))
      .doesNotContain(tuple(unassociatedProject.getName(), false));
  }

  @Test
  public void return_only_unassociated_project() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto associatedProject = db.components().insertPublicProjectDto();
    ProjectDto unassociatedProject = db.components().insertPublicProjectDto();
    db.qualityGates().associateProjectToQualityGate(associatedProject, qualityGate);

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_ID, valueOf(qualityGate.getUuid()))
      .setParam(PARAM_SELECTED, DESELECTED.value())
      .executeProtobuf(SearchResponse.class);

    assertThat(response.getResultsList())
      .extracting(Result::getName, Result::getSelected)
      .containsExactlyInAnyOrder(tuple(unassociatedProject.getName(), false))
      .doesNotContain(tuple(associatedProject.getName(), true));
  }

  @Test
  public void return_only_authorized_projects() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    // User can only see project1 1
    db.users().insertProjectPermissionOnUser(user, USER, project1);
    userSession.logIn(user);

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_ID, valueOf(qualityGate.getUuid()))
      .setParam(PARAM_SELECTED, ALL.value())
      .executeProtobuf(SearchResponse.class);

    assertThat(response.getResultsList())
      .extracting(Result::getName)
      .containsExactlyInAnyOrder(project1.name())
      .doesNotContain(project2.name());
  }

  @Test
  public void test_paging() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project1 = db.components().insertPublicProjectDto();
    ProjectDto project2 = db.components().insertPublicProjectDto();
    ProjectDto project3 = db.components().insertPublicProjectDto();
    db.qualityGates().associateProjectToQualityGate(project1, qualityGate);

    // Return partial result on first page
    assertThat(ws.newRequest()
      .setParam(PARAM_GATE_ID, valueOf(qualityGate.getUuid()))
      .setParam(PARAM_SELECTED, ALL.value())
      .setParam(PARAM_PAGE, "1")
      .setParam(PARAM_PAGE_SIZE, "1")
      .executeProtobuf(SearchResponse.class)
      .getResultsList())
      .extracting(Result::getName)
      .containsExactlyInAnyOrder(project1.getName());

    // Return partial result on second page
    assertThat(ws.newRequest()
      .setParam(PARAM_GATE_ID, valueOf(qualityGate.getUuid()))
      .setParam(PARAM_SELECTED, ALL.value())
      .setParam(PARAM_PAGE, "2")
      .setParam(PARAM_PAGE_SIZE, "1")
      .executeProtobuf(SearchResponse.class)
      .getResultsList())
      .extracting(Result::getName)
      .containsExactlyInAnyOrder(project2.getName());

    // Return partial result on first page
    assertThat(ws.newRequest()
      .setParam(PARAM_GATE_ID, valueOf(qualityGate.getUuid()))
      .setParam(PARAM_SELECTED, ALL.value())
      .setParam(PARAM_PAGE, "1")
      .setParam(PARAM_PAGE_SIZE, "2")
      .executeProtobuf(SearchResponse.class)
      .getResultsList())
      .extracting(Result::getName)
      .containsExactlyInAnyOrder(project1.getName(), project2.getName());

    // Return all result on first page
    assertThat(ws.newRequest()
      .setParam(PARAM_GATE_ID, valueOf(qualityGate.getUuid()))
      .setParam(PARAM_SELECTED, ALL.value())
      .setParam(PARAM_PAGE, "1")
      .setParam(PARAM_PAGE_SIZE, "3")
      .executeProtobuf(SearchResponse.class)
      .getResultsList())
      .extracting(Result::getName)
      .containsExactlyInAnyOrder(project1.getName(), project2.getName(), project3.getName());

    // Return no result as page index is off limit
    assertThat(ws.newRequest()
      .setParam(PARAM_GATE_ID, valueOf(qualityGate.getUuid()))
      .setParam(PARAM_SELECTED, ALL.value())
      .setParam(PARAM_PAGE, "3")
      .setParam(PARAM_PAGE_SIZE, "3")
      .executeProtobuf(SearchResponse.class)
      .getResultsList())
      .extracting(Result::getName)
      .isEmpty();
  }

  @Test
  public void test_pagination_on_many_pages() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    for (int i = 0; i < 20; i++) {
      ProjectDto project = db.components().insertPublicProjectDto();
      db.qualityGates().associateProjectToQualityGate(project, qualityGate);
    }
    userSession.addPermission(ADMINISTER_QUALITY_GATES);

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_ID, valueOf(qualityGate.getUuid()))
      .setParam(PARAM_PAGE_SIZE, valueOf(5))
      .setParam(PARAM_PAGE, valueOf(2))
      .executeProtobuf(SearchResponse.class);

    assertThat(response)
      .extracting(SearchResponse::getMore,
        searchResponse -> searchResponse.getPaging().getPageIndex(),
        searchResponse -> searchResponse.getPaging().getPageSize(),
        searchResponse -> searchResponse.getPaging().getTotal())
      .contains(true, 2, 5, 20);
  }

  @Test
  public void test_pagination_on_one_page() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    for (int i = 0; i < 20; i++) {
      ProjectDto project = db.components().insertPublicProjectDto();
      db.qualityGates().associateProjectToQualityGate(project, qualityGate);
    }
    userSession.addPermission(ADMINISTER_QUALITY_GATES);

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_ID, valueOf(qualityGate.getUuid()))
      .setParam(PARAM_PAGE_SIZE, valueOf(100))
      .setParam(PARAM_PAGE, valueOf(1))
      .executeProtobuf(SearchResponse.class);

    assertThat(response)
      .extracting(SearchResponse::getMore,
        searchResponse -> searchResponse.getPaging().getPageIndex(),
        searchResponse -> searchResponse.getPaging().getPageSize(),
        searchResponse -> searchResponse.getPaging().getTotal())
      .contains(false, 1, 100, 20);
  }

  @Test
  public void fail_on_unknown_quality_gate() {
    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_GATE_ID, "42")
      .executeProtobuf(SearchResponse.class))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("No quality gate has been found for id 42");
  }

  @Test
  public void definition() {
    WebService.Action action = ws.getDef();

    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isFalse();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("gateId", false),
        tuple("gateName", false),
        tuple("query", false),
        tuple("selected", false),
        tuple("page", false),
        tuple("pageSize", false));
  }

}
