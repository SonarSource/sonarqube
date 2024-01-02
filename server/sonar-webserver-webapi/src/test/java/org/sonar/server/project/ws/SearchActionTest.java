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

import com.google.common.base.Joiner;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Projects.SearchWsResponse;
import org.sonarqube.ws.Projects.SearchWsResponse.Component;
import org.sonarqube.ws.client.component.ComponentsWsParameters;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newPortfolio;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ANALYZED_BEFORE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ON_PROVISIONED_ONLY;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECTS;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_VISIBILITY;

public class SearchActionTest {

  private static final String PROJECT_KEY_1 = "project1";
  private static final String PROJECT_KEY_2 = "project2";
  private static final String PROJECT_KEY_3 = "project3";

  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public final DbTester db = DbTester.create();

  private final WsActionTester ws = new WsActionTester(new SearchAction(db.getDbClient(), userSession));

  @Test
  public void search_by_key_query_with_partial_match_case_insensitive() {
    userSession.addPermission(ADMINISTER);
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto().setKey("project-_%-key"),
      ComponentTesting.newPrivateProjectDto().setKey("PROJECT-_%-KEY"),
      ComponentTesting.newPrivateProjectDto().setKey("project-key-without-escaped-characters"));

    SearchWsResponse response = call(SearchRequest.builder().setQuery("JeCt-_%-k").build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("project-_%-key", "PROJECT-_%-KEY");
  }

  @Test
  public void search_private_projects() {
    userSession.addPermission(ADMINISTER);
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto().setKey("private-key"),
      ComponentTesting.newPublicProjectDto().setKey("public-key"));

    SearchWsResponse response = call(SearchRequest.builder().setVisibility("private").build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("private-key");
  }

  @Test
  public void search_public_projects() {
    userSession.addPermission(ADMINISTER);
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto().setKey("private-key"),
      ComponentTesting.newPublicProjectDto().setKey("public-key"));

    SearchWsResponse response = call(SearchRequest.builder().setVisibility("public").build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("public-key");
  }

  @Test
  public void search_projects_when_no_qualifier_set() {
    userSession.addPermission(ADMINISTER);
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto().setKey(PROJECT_KEY_1),
      newPortfolio());

    SearchWsResponse response = call(SearchRequest.builder().build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(PROJECT_KEY_1);
  }

  @Test
  public void search_projects() {
    userSession.addPermission(ADMINISTER);
    ComponentDto project = ComponentTesting.newPrivateProjectDto().setKey(PROJECT_KEY_1);
    ComponentDto module = newModuleDto(project);
    ComponentDto directory = newDirectory(module, "dir");
    ComponentDto file = newFileDto(directory);
    db.components().insertComponents(
      project, module, directory, file,
      ComponentTesting.newPrivateProjectDto().setKey(PROJECT_KEY_2),
      newPortfolio());

    SearchWsResponse response = call(SearchRequest.builder().setQualifiers(singletonList("TRK")).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(PROJECT_KEY_1, PROJECT_KEY_2);
  }

  @Test
  public void search_views() {
    userSession.addPermission(ADMINISTER);
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto().setKey(PROJECT_KEY_1),
      newPortfolio().setKey("view1"));

    SearchWsResponse response = call(SearchRequest.builder().setQualifiers(singletonList("VW")).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("view1");
  }

  @Test
  public void search_projects_and_views() {
    userSession.addPermission(ADMINISTER);
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto().setKey(PROJECT_KEY_1),
      newPortfolio().setKey("view1"));

    SearchWsResponse response = call(SearchRequest.builder().setQualifiers(asList("TRK", "VW")).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(PROJECT_KEY_1, "view1");
  }

  @Test
  public void search_all() {
    userSession.addPermission(ADMINISTER);
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto().setKey(PROJECT_KEY_1),
      ComponentTesting.newPrivateProjectDto().setKey(PROJECT_KEY_2),
      ComponentTesting.newPrivateProjectDto().setKey(PROJECT_KEY_3));

    SearchWsResponse response = call(SearchRequest.builder().build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(PROJECT_KEY_1, PROJECT_KEY_2, PROJECT_KEY_3);
  }

  @Test
  public void search_for_old_projects() {
    userSession.addPermission(ADMINISTER);
    long aLongTimeAgo = 1_000_000_000L;
    long inBetween = 2_000_000_000L;
    long recentTime = 3_000_000_000L;

    ComponentDto oldProject = db.components().insertPublicProject();
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(oldProject).setCreatedAt(aLongTimeAgo));
    ComponentDto branch = db.components().insertProjectBranch(oldProject);
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(branch).setCreatedAt(inBetween));

    ComponentDto recentProject = db.components().insertPublicProject();
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(recentProject).setCreatedAt(recentTime));
    db.commit();

    SearchWsResponse result = call(SearchRequest.builder().setAnalyzedBefore(toStringAtUTC(new Date(recentTime + 1_000))).build());
    assertThat(result.getComponentsList()).extracting(Component::getKey, Component::getLastAnalysisDate)
      .containsExactlyInAnyOrder(tuple(oldProject.getKey(), formatDateTime(inBetween)), tuple(recentProject.getKey(), formatDateTime(recentTime)));

    result = call(SearchRequest.builder().setAnalyzedBefore(toStringAtUTC(new Date(recentTime))).build());
    assertThat(result.getComponentsList()).extracting(Component::getKey, Component::getLastAnalysisDate)
      .containsExactlyInAnyOrder(tuple(oldProject.getKey(), formatDateTime(inBetween)));

    result = call(SearchRequest.builder().setAnalyzedBefore(toStringAtUTC(new Date(aLongTimeAgo + 1_000L))).build());
    assertThat(result.getComponentsList()).isEmpty();
  }

  private static String toStringAtUTC(Date d) {
    OffsetDateTime offsetTime = d.toInstant().atOffset(ZoneOffset.UTC);
    return DateUtils.formatDateTime(offsetTime);
  }

  @Test
  public void does_not_return_branches_when_searching_by_key() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto branch = db.components().insertProjectBranch(project);
    userSession.addPermission(ADMINISTER);

    SearchWsResponse response = call(SearchRequest.builder().build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(project.getKey());
  }

  @Test
  public void result_is_paginated() {
    userSession.addPermission(ADMINISTER);
    List<ComponentDto> componentDtoList = new ArrayList<>();
    for (int i = 1; i <= 9; i++) {
      componentDtoList.add(newPrivateProjectDto("project-uuid-" + i).setKey("project-key-" + i).setName("Project Name " + i));
    }
    db.components().insertComponents(componentDtoList.toArray(new ComponentDto[] {}));

    SearchWsResponse response = call(SearchRequest.builder().setPage(2).setPageSize(3).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsExactly("project-key-4", "project-key-5", "project-key-6");
  }

  @Test
  public void provisioned_projects() {
    userSession.addPermission(ADMINISTER);
    ComponentDto provisionedProject = db.components().insertPrivateProject();
    ComponentDto analyzedProject = db.components().insertPrivateProject();
    db.components().insertSnapshot(newAnalysis(analyzedProject));

    SearchWsResponse response = call(SearchRequest.builder().setOnProvisionedOnly(true).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey)
      .containsExactlyInAnyOrder(provisionedProject.getKey())
      .doesNotContain(analyzedProject.getKey());
  }

  @Test
  public void search_by_component_keys() {
    userSession.addPermission(ADMINISTER);
    ComponentDto jdk = db.components().insertPrivateProject();
    ComponentDto sonarqube = db.components().insertPrivateProject();
    ComponentDto sonarlint = db.components().insertPrivateProject();

    SearchWsResponse result = call(SearchRequest.builder()
      .setProjects(Arrays.asList(jdk.getKey(), sonarqube.getKey()))
      .build());

    assertThat(result.getComponentsList()).extracting(Component::getKey)
      .containsExactlyInAnyOrder(jdk.getKey(), sonarqube.getKey())
      .doesNotContain(sonarlint.getKey());
  }

  @Test
  public void request_throws_IAE_if_more_than_1000_projects() {
    SearchRequest request = SearchRequest.builder()
      .setProjects(Collections.nCopies(1_001, "foo"))
      .build();
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'projects' can contains only 1000 values, got 1001");
  }

  @Test
  public void fail_when_not_system_admin() {
    userSession.addPermission(ADMINISTER_QUALITY_PROFILES);

    SearchRequest request = SearchRequest.builder().build();
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_on_invalid_qualifier() {
    userSession.addPermission(ADMINISTER_QUALITY_PROFILES);

    SearchRequest request = SearchRequest.builder().setQualifiers(singletonList("BRC")).build();
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of parameter 'qualifiers' (BRC) must be one of: [TRK, VW, APP]");
  }

  @Test
  public void definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.key()).isEqualTo("search");
    assertThat(action.isPost()).isFalse();
    assertThat(action.description()).isEqualTo("Search for projects or views to administrate them.<br>Requires 'Administer System' permission");
    assertThat(action.isInternal()).isFalse();
    assertThat(action.since()).isEqualTo("6.3");
    assertThat(action.handler()).isEqualTo(ws.getDef().handler());
    assertThat(action.params()).extracting(Param::key)
      .containsExactlyInAnyOrder("q", "qualifiers", "p", "ps", "visibility", "analyzedBefore", "onProvisionedOnly", "projects");
    assertThat(action.responseExample()).isEqualTo(getClass().getResource("search-example.json"));

    Param qParam = action.param("q");
    assertThat(qParam.isRequired()).isFalse();
    assertThat(qParam.description()).isEqualTo("Limit search to: " +
      "<ul>" +
      "<li>component names that contain the supplied string</li>" +
      "<li>component keys that contain the supplied string</li>" +
      "</ul>");

    Param qualifierParam = action.param("qualifiers");
    assertThat(qualifierParam.isRequired()).isFalse();
    assertThat(qualifierParam.description()).isEqualTo("Comma-separated list of component qualifiers. Filter the results with the specified qualifiers");
    assertThat(qualifierParam.possibleValues()).containsOnly("TRK", "VW", "APP");
    assertThat(qualifierParam.defaultValue()).isEqualTo("TRK");

    Param pParam = action.param("p");
    assertThat(pParam.isRequired()).isFalse();
    assertThat(pParam.defaultValue()).isEqualTo("1");
    assertThat(pParam.description()).isEqualTo("1-based page number");

    Param psParam = action.param("ps");
    assertThat(psParam.isRequired()).isFalse();
    assertThat(psParam.defaultValue()).isEqualTo("100");
    assertThat(psParam.description()).isEqualTo("Page size. Must be greater than 0 and less or equal than 500");

    Param visibilityParam = action.param("visibility");
    assertThat(visibilityParam.isRequired()).isFalse();
    assertThat(visibilityParam.description()).isEqualTo("Filter the projects that should be visible to everyone (public), or only specific user/groups (private).<br/>" +
      "If no visibility is specified, the default project visibility will be used.");

    Param lastAnalysisBefore = action.param("analyzedBefore");
    assertThat(lastAnalysisBefore.isRequired()).isFalse();
    assertThat(lastAnalysisBefore.since()).isEqualTo("6.6");

    Param onProvisionedOnly = action.param("onProvisionedOnly");
    assertThat(onProvisionedOnly.possibleValues()).containsExactlyInAnyOrder("true", "false", "yes", "no");
    assertThat(onProvisionedOnly.defaultValue()).isEqualTo("false");
    assertThat(onProvisionedOnly.since()).isEqualTo("6.6");
  }

  @Test
  public void json_example() {
    userSession.addPermission(ADMINISTER);
    ComponentDto publicProject = newPrivateProjectDto("project-uuid-1").setName("Project Name 1").setKey("project-key-1").setPrivate(false);
    ComponentDto privateProject = newPrivateProjectDto("project-uuid-2").setName("Project Name 1").setKey("project-key-2");
    db.components().insertComponents(
      publicProject,
      privateProject);
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(publicProject)
      .setCreatedAt(parseDateTime("2017-03-01T11:39:03+0300").getTime())
      .setRevision("cfb82f55c6ef32e61828c4cb3db2da12795fd767"));
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(privateProject)
      .setCreatedAt(parseDateTime("2017-03-02T15:21:47+0300").getTime())
      .setRevision("7be96a94ac0c95a61ee6ee0ef9c6f808d386a355"));
    db.commit();

    String response = ws.newRequest()
      .setMediaType(MediaTypes.JSON)
      .execute().getInput();

    assertJson(response).isSimilarTo(ws.getDef().responseExampleAsString());
    assertJson(ws.getDef().responseExampleAsString()).isSimilarTo(response);
  }

  private SearchWsResponse call(SearchRequest wsRequest) {
    TestRequest request = ws.newRequest();
    List<String> qualifiers = wsRequest.getQualifiers();
    if (!qualifiers.isEmpty()) {
      request.setParam(ComponentsWsParameters.PARAM_QUALIFIERS, Joiner.on(",").join(qualifiers));
    }
    ofNullable(wsRequest.getQuery()).ifPresent(query -> request.setParam(TEXT_QUERY, query));
    ofNullable(wsRequest.getPage()).ifPresent(page -> request.setParam(PAGE, String.valueOf(page)));
    ofNullable(wsRequest.getPageSize()).ifPresent(pageSize -> request.setParam(PAGE_SIZE, String.valueOf(pageSize)));
    ofNullable(wsRequest.getVisibility()).ifPresent(v -> request.setParam(PARAM_VISIBILITY, v));
    ofNullable(wsRequest.getAnalyzedBefore()).ifPresent(d -> request.setParam(PARAM_ANALYZED_BEFORE, d));
    ofNullable(wsRequest.getProjects()).ifPresent(l1 -> request.setParam(PARAM_PROJECTS, String.join(",", l1)));
    request.setParam(PARAM_ON_PROVISIONED_ONLY, String.valueOf(wsRequest.isOnProvisionedOnly()));
    return request.executeProtobuf(SearchWsResponse.class);
  }

}
