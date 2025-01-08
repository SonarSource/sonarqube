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
package org.sonar.server.qualitygate.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.ai.code.assurance.AiCodeAssuranceEntitlement;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.server.ws.WebService.SelectionMode.ALL;
import static org.sonar.api.server.ws.WebService.SelectionMode.DESELECTED;
import static org.sonar.api.server.ws.WebService.SelectionMode.SELECTED;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_NAME;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PAGE;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PAGE_SIZE;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SELECTED;

class SearchActionIT {

  @RegisterExtension
  UserSessionRule userSession = UserSessionRule.standalone();

  @RegisterExtension
  DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final AiCodeAssuranceEntitlement entitlement = mock(AiCodeAssuranceEntitlement.class);
  private final SearchAction underTest = new SearchAction(dbClient, userSession,
    new QualityGatesWsSupport(dbClient, userSession, TestComponentFinder.from(db)), entitlement);
  private final WsActionTester ws = new WsActionTester(underTest);

  @BeforeEach
  void setUp() {
    when(entitlement.isEnabled()).thenReturn(true);
  }

  @Test
  void search_projects_of_a_quality_gate() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDtoByMainBranch(project), qualityGate);

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, valueOf(qualityGate.getName()))
      .executeProtobuf(SearchResponse.class);

    assertThat(response.getResultsList())
      .extracting(Result::getKey, Result::getName)
      .containsExactlyInAnyOrder(tuple(project.getKey(), project.name()));
  }

  @Test
  void return_empty_association() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, valueOf(qualityGate.getName()))
      .executeProtobuf(SearchResponse.class);

    assertThat(response.getResultsList()).isEmpty();
  }

  @Test
  void return_all_projects() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto unassociatedProject = db.components().insertPublicProject().getProjectDto();
    ProjectDto associatedProject = db.components().insertPublicProject().getProjectDto();
    db.qualityGates().associateProjectToQualityGate(associatedProject, qualityGate);

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, valueOf(qualityGate.getName()))
      .setParam(PARAM_SELECTED, ALL.value())
      .executeProtobuf(SearchResponse.class);

    assertThat(response.getResultsList())
      .extracting(Result::getName, Result::getKey, Result::getSelected)
      .containsExactlyInAnyOrder(
        tuple(associatedProject.getName(), associatedProject.getKey(), true),
        tuple(unassociatedProject.getName(), unassociatedProject.getKey(), false));
  }

  @Test
  void return_only_associated_project() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto associatedProject = db.components().insertPublicProject().getProjectDto();
    ProjectDto unassociatedProject = db.components().insertPublicProject().getProjectDto();
    db.qualityGates().associateProjectToQualityGate(associatedProject, qualityGate);

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, valueOf(qualityGate.getName()))
      .setParam(PARAM_SELECTED, SELECTED.value())
      .executeProtobuf(SearchResponse.class);

    assertThat(response.getResultsList())
      .extracting(Result::getName, Result::getSelected)
      .containsExactlyInAnyOrder(tuple(associatedProject.getName(), true))
      .doesNotContain(tuple(unassociatedProject.getName(), false));
  }

  @Test
  void return_only_unassociated_project() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto associatedProject = db.components().insertPublicProject().getProjectDto();
    ProjectDto unassociatedProject = db.components().insertPublicProject().getProjectDto();
    db.qualityGates().associateProjectToQualityGate(associatedProject, qualityGate);

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, valueOf(qualityGate.getName()))
      .setParam(PARAM_SELECTED, DESELECTED.value())
      .executeProtobuf(SearchResponse.class);

    assertThat(response.getResultsList())
      .extracting(Result::getName, Result::getSelected)
      .containsExactlyInAnyOrder(tuple(unassociatedProject.getName(), false))
      .doesNotContain(tuple(associatedProject.getName(), true));
  }

  @Test
  void return_only_authorized_projects() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    UserDto user = db.users().insertUser();
    // User can only see project1 1
    db.users().insertProjectPermissionOnUser(user, USER, project1);
    userSession.logIn(user);

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, valueOf(qualityGate.getName()))
      .setParam(PARAM_SELECTED, ALL.value())
      .executeProtobuf(SearchResponse.class);

    assertThat(response.getResultsList())
      .extracting(Result::getName)
      .containsExactlyInAnyOrder(project1.name())
      .doesNotContain(project2.name());
  }

  @Test
  void test_paging() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project1 = db.components().insertPublicProject(dto -> dto.setName("proj_1")).getProjectDto();
    ProjectDto project2 = db.components().insertPublicProject(dto -> dto.setName("proj_2")).getProjectDto();
    ProjectDto project3 = db.components().insertPublicProject(dto -> dto.setName("proj_3")).getProjectDto();
    db.qualityGates().associateProjectToQualityGate(project1, qualityGate);

    // Return partial result on first page
    assertThat(ws.newRequest()
      .setParam(PARAM_GATE_NAME, valueOf(qualityGate.getName()))
      .setParam(PARAM_SELECTED, ALL.value())
      .setParam(PARAM_PAGE, "1")
      .setParam(PARAM_PAGE_SIZE, "1")
      .executeProtobuf(SearchResponse.class)
      .getResultsList())
      .extracting(Result::getName)
      .containsExactlyInAnyOrder(project1.getName());

    // Return partial result on second page
    assertThat(ws.newRequest()
      .setParam(PARAM_GATE_NAME, valueOf(qualityGate.getName()))
      .setParam(PARAM_SELECTED, ALL.value())
      .setParam(PARAM_PAGE, "2")
      .setParam(PARAM_PAGE_SIZE, "1")
      .executeProtobuf(SearchResponse.class)
      .getResultsList())
      .extracting(Result::getName)
      .containsExactlyInAnyOrder(project2.getName());

    // Return partial result on first page
    assertThat(ws.newRequest()
      .setParam(PARAM_GATE_NAME, valueOf(qualityGate.getName()))
      .setParam(PARAM_SELECTED, ALL.value())
      .setParam(PARAM_PAGE, "1")
      .setParam(PARAM_PAGE_SIZE, "2")
      .executeProtobuf(SearchResponse.class)
      .getResultsList())
      .extracting(Result::getName)
      .containsExactlyInAnyOrder(project1.getName(), project2.getName());

    // Return all result on first page
    assertThat(ws.newRequest()
      .setParam(PARAM_GATE_NAME, valueOf(qualityGate.getName()))
      .setParam(PARAM_SELECTED, ALL.value())
      .setParam(PARAM_PAGE, "1")
      .setParam(PARAM_PAGE_SIZE, "3")
      .executeProtobuf(SearchResponse.class)
      .getResultsList())
      .extracting(Result::getName)
      .containsExactlyInAnyOrder(project1.getName(), project2.getName(), project3.getName());

    // Return no result as page index is off limit
    assertThat(ws.newRequest()
      .setParam(PARAM_GATE_NAME, valueOf(qualityGate.getName()))
      .setParam(PARAM_SELECTED, ALL.value())
      .setParam(PARAM_PAGE, "3")
      .setParam(PARAM_PAGE_SIZE, "3")
      .executeProtobuf(SearchResponse.class)
      .getResultsList())
      .extracting(Result::getName)
      .isEmpty();
  }

  @Test
  void test_pagination_on_many_pages() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    for (int i = 0; i < 20; i++) {
      ProjectDto project = db.components().insertPublicProject().getProjectDto();
      db.qualityGates().associateProjectToQualityGate(project, qualityGate);
    }
    userSession.addPermission(ADMINISTER_QUALITY_GATES);

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, valueOf(qualityGate.getName()))
      .setParam(PARAM_PAGE_SIZE, valueOf(5))
      .setParam(PARAM_PAGE, valueOf(2))
      .executeProtobuf(SearchResponse.class);

    assertThat(response)
      .extracting(
        searchResponse -> searchResponse.getPaging().getPageIndex(),
        searchResponse -> searchResponse.getPaging().getPageSize(),
        searchResponse -> searchResponse.getPaging().getTotal())
      .contains(2, 5, 20);
  }

  @Test
  void test_pagination_on_one_page() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    for (int i = 0; i < 20; i++) {
      ProjectDto project = db.components().insertPublicProject().getProjectDto();
      db.qualityGates().associateProjectToQualityGate(project, qualityGate);
    }
    userSession.addPermission(ADMINISTER_QUALITY_GATES);

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, valueOf(qualityGate.getName()))
      .setParam(PARAM_PAGE_SIZE, valueOf(100))
      .setParam(PARAM_PAGE, valueOf(1))
      .executeProtobuf(SearchResponse.class);

    assertThat(response)
      .extracting(
        searchResponse -> searchResponse.getPaging().getPageIndex(),
        searchResponse -> searchResponse.getPaging().getPageSize(),
        searchResponse -> searchResponse.getPaging().getTotal())
      .contains(1, 100, 20);
  }

  @Test
  void fail_on_unknown_quality_gate() {
    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_GATE_NAME, "unknown")
      .executeProtobuf(SearchResponse.class))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("No quality gate has been found for name unknown");
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void return_ai_code_assurance(boolean containsAiCode) {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project = db.components().insertPublicProject(componentDto -> componentDto.setName("proj1"),
      projectDto -> projectDto.setContainsAiCode(containsAiCode)).getProjectDto();
    db.qualityGates().associateProjectToQualityGate(project, qualityGate);

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, valueOf(qualityGate.getName()))
      .setParam(PARAM_SELECTED, ALL.value())
      .executeProtobuf(SearchResponse.class);

    assertThat(response.getResultsList())
      .extracting(Result::getName, Result::getKey, Result::getContainsAiCode)
      .containsExactlyInAnyOrder(
        tuple(project.getName(), project.getKey(), containsAiCode));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void contains_ai_code_is_false_for_community_edition(boolean containsAiCode) {
    when(entitlement.isEnabled()).thenReturn(false);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project = db.components().insertPublicProject(componentDto -> componentDto.setName("proj1"),
      projectDto -> projectDto.setContainsAiCode(containsAiCode)).getProjectDto();
    db.qualityGates().associateProjectToQualityGate(project, qualityGate);

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, valueOf(qualityGate.getName()))
      .setParam(PARAM_SELECTED, ALL.value())
      .executeProtobuf(SearchResponse.class);

    assertThat(response.getResultsList())
      .extracting(Result::getName, Result::getKey, Result::getContainsAiCode)
      .containsExactlyInAnyOrder(
        tuple(project.getName(), project.getKey(), false));
  }

  @Test
  void definition() {
    WebService.Action action = ws.getDef();

    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isFalse();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("gateName", true),
        tuple("query", false),
        tuple("selected", false),
        tuple("page", false),
        tuple("pageSize", false));
  }

}
