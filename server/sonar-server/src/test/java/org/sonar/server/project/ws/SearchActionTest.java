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
package org.sonar.server.project.ws;

import com.google.common.base.Joiner;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.BillingValidationsProxy;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Projects.SearchWsResponse;
import org.sonarqube.ws.Projects.SearchWsResponse.Component;
import org.sonarqube.ws.client.component.ComponentsWsParameters;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.utils.DateUtils.formatDate;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ANALYZED_BEFORE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ON_PROVISIONED_ONLY;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECTS;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECT_IDS;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_VISIBILITY;

public class SearchActionTest {

  private static final String PROJECT_KEY_1 = "project1";
  private static final String PROJECT_KEY_2 = "project2";
  private static final String PROJECT_KEY_3 = "project3";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create();

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);

  private WsActionTester ws = new WsActionTester(
    new SearchAction(db.getDbClient(), userSession, new ProjectsWsSupport(db.getDbClient(), defaultOrganizationProvider, mock(BillingValidationsProxy.class))));

  @Test
  public void search_by_key_query_with_partial_match_case_insensitive() {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setDbKey("project-_%-key"),
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setDbKey("PROJECT-_%-KEY"),
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setDbKey("project-key-without-escaped-characters"));

    SearchWsResponse response = call(SearchRequest.builder().setQuery("JeCt-_%-k").build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("project-_%-key", "PROJECT-_%-KEY");
  }

  @Test
  public void search_private_projects() {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setDbKey("private-key"),
      ComponentTesting.newPublicProjectDto(db.getDefaultOrganization()).setDbKey("public-key"));

    SearchWsResponse response = call(SearchRequest.builder().setVisibility("private").build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("private-key");
  }

  @Test
  public void search_public_projects() {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setDbKey("private-key"),
      ComponentTesting.newPublicProjectDto(db.getDefaultOrganization()).setDbKey("public-key"));

    SearchWsResponse response = call(SearchRequest.builder().setVisibility("public").build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("public-key");
  }

  @Test
  public void search_projects_when_no_qualifier_set() {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setDbKey(PROJECT_KEY_1),
      newView(db.getDefaultOrganization()));

    SearchWsResponse response = call(SearchRequest.builder().build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(PROJECT_KEY_1);
  }

  @Test
  public void search_projects() {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setDbKey(PROJECT_KEY_1);
    ComponentDto module = newModuleDto(project);
    ComponentDto directory = newDirectory(module, "dir");
    ComponentDto file = newFileDto(directory);
    db.components().insertComponents(
      project, module, directory, file,
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setDbKey(PROJECT_KEY_2),
      newView(db.getDefaultOrganization()));

    SearchWsResponse response = call(SearchRequest.builder().setQualifiers(singletonList("TRK")).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(PROJECT_KEY_1, PROJECT_KEY_2);
  }

  @Test
  public void search_views() {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setDbKey(PROJECT_KEY_1),
      newView(db.getDefaultOrganization()).setDbKey("view1"));

    SearchWsResponse response = call(SearchRequest.builder().setQualifiers(singletonList("VW")).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("view1");
  }

  @Test
  public void search_projects_and_views() {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setDbKey(PROJECT_KEY_1),
      newView(db.getDefaultOrganization()).setDbKey("view1"));

    SearchWsResponse response = call(SearchRequest.builder().setQualifiers(asList("TRK", "VW")).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(PROJECT_KEY_1, "view1");
  }

  @Test
  public void search_on_default_organization_when_no_organization_set() {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    OrganizationDto otherOrganization = db.organizations().insert();
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setDbKey(PROJECT_KEY_1),
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setDbKey(PROJECT_KEY_2),
      ComponentTesting.newPrivateProjectDto(otherOrganization).setDbKey(PROJECT_KEY_3));

    SearchWsResponse response = call(SearchRequest.builder().build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(PROJECT_KEY_1, PROJECT_KEY_2);
  }

  @Test
  public void search_for_projects_on_given_organization() {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    userSession.addPermission(ADMINISTER, organization1);
    ComponentDto project1 = ComponentTesting.newPrivateProjectDto(organization1);
    ComponentDto project2 = ComponentTesting.newPrivateProjectDto(organization1);
    ComponentDto project3 = ComponentTesting.newPrivateProjectDto(organization2);
    db.components().insertComponents(project1, project2, project3);

    SearchWsResponse response = call(SearchRequest.builder().setOrganization(organization1.getKey()).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(project1.getDbKey(), project2.getDbKey());
  }

  @Test
  public void search_for_old_projects() {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    long aLongTimeAgo = 1_000_000_000L;
    long recentTime = 3_000_000_000L;
    ComponentDto oldProject = db.components().insertPublicProject();
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(oldProject).setCreatedAt(aLongTimeAgo));
    ComponentDto recentProject = db.components().insertPublicProject();
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(recentProject).setCreatedAt(recentTime));
    db.commit();

    SearchWsResponse result = call(SearchRequest.builder().setAnalyzedBefore(formatDate(new Date(recentTime))).build());

    assertThat(result.getComponentsList()).extracting(Component::getKey)
      .containsExactlyInAnyOrder(oldProject.getKey())
      .doesNotContain(recentProject.getKey());
  }

  @Test
  public void does_not_return_branches_when_searching_by_key() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());

    SearchWsResponse response = call(SearchRequest.builder().build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(project.getDbKey());
  }

  @Test
  public void result_is_paginated() {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    List<ComponentDto> componentDtoList = new ArrayList<>();
    for (int i = 1; i <= 9; i++) {
      componentDtoList.add(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid-" + i).setDbKey("project-key-" + i).setName("Project Name " + i));
    }
    db.components().insertComponents(componentDtoList.toArray(new ComponentDto[] {}));

    SearchWsResponse response = call(SearchRequest.builder().setPage(2).setPageSize(3).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsExactly("project-key-4", "project-key-5", "project-key-6");
  }

  @Test
  public void provisioned_projects() {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
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
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
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
  public void search_by_component_uuids() {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    ComponentDto jdk = db.components().insertPrivateProject();
    ComponentDto sonarqube = db.components().insertPrivateProject();
    ComponentDto sonarlint = db.components().insertPrivateProject();

    SearchWsResponse result = call(SearchRequest.builder()
      .setProjectIds(Arrays.asList(jdk.uuid(), sonarqube.uuid()))
      .build());

    assertThat(result.getComponentsList()).extracting(Component::getKey)
      .containsExactlyInAnyOrder(jdk.getKey(), sonarqube.getKey())
      .doesNotContain(sonarlint.getKey());
  }

  @Test
  public void fail_when_not_system_admin() {
    userSession.addPermission(ADMINISTER_QUALITY_PROFILES, db.getDefaultOrganization());
    expectedException.expect(ForbiddenException.class);

    call(SearchRequest.builder().build());
  }

  @Test
  public void fail_on_unknown_organization() {
    expectedException.expect(NotFoundException.class);

    call(SearchRequest.builder().setOrganization("unknown").build());
  }

  @Test
  public void fail_on_invalid_qualifier() {
    userSession.addPermission(ADMINISTER_QUALITY_PROFILES, db.getDefaultOrganization());
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value of parameter 'qualifiers' (BRC) must be one of: [TRK, VW, APP]");

    call(SearchRequest.builder().setQualifiers(singletonList("BRC")).build());
  }

  @Test
  public void definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.key()).isEqualTo("search");
    assertThat(action.isPost()).isFalse();
    assertThat(action.description()).isEqualTo("Search for projects or views to administrate them.<br>Requires 'System Administrator' permission");
    assertThat(action.isInternal()).isFalse();
    assertThat(action.since()).isEqualTo("6.3");
    assertThat(action.handler()).isEqualTo(ws.getDef().handler());
    assertThat(action.params()).extracting(Param::key)
      .containsExactlyInAnyOrder("organization", "q", "qualifiers", "p", "ps", "visibility", "analyzedBefore", "onProvisionedOnly", "projects", "projectIds");
    assertThat(action.responseExample()).isEqualTo(getClass().getResource("search-example.json"));

    Param organization = action.param("organization");
    Assertions.assertThat(organization.description()).isEqualTo("The key of the organization");
    Assertions.assertThat(organization.isInternal()).isTrue();
    Assertions.assertThat(organization.isRequired()).isFalse();
    Assertions.assertThat(organization.since()).isEqualTo("6.3");

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
    assertThat(psParam.description()).isEqualTo("Page size. Must be greater than 0 and less than 500");

    Param visibilityParam = action.param("visibility");
    assertThat(visibilityParam.isRequired()).isFalse();
    assertThat(visibilityParam.description()).isEqualTo("Filter the projects that should be visible to everyone (public), or only specific user/groups (private).<br/>" +
      "If no visibility is specified, the default project visibility of the organization will be used.");

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
    OrganizationDto organization = db.organizations().insertForKey("my-org-1");
    userSession.addPermission(ADMINISTER, organization);
    ComponentDto publicProject = newPrivateProjectDto(organization, "project-uuid-1").setName("Project Name 1").setDbKey("project-key-1").setPrivate(false);
    ComponentDto privateProject = newPrivateProjectDto(organization, "project-uuid-2").setName("Project Name 1").setDbKey("project-key-2");
    db.components().insertComponents(
      publicProject,
      privateProject);
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(publicProject).setCreatedAt(parseDateTime("2017-03-01T11:39:03+0300").getTime()));
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(privateProject).setCreatedAt(parseDateTime("2017-03-02T15:21:47+0300").getTime()));
    db.commit();

    String response = ws.newRequest()
      .setMediaType(MediaTypes.JSON)
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute().getInput();

    assertJson(response).isSimilarTo(ws.getDef().responseExampleAsString());
    assertJson(ws.getDef().responseExampleAsString()).isSimilarTo(response);
  }

  private SearchWsResponse call(SearchRequest wsRequest) {
    TestRequest request = ws.newRequest();
    setNullable(wsRequest.getOrganization(), organization -> request.setParam(PARAM_ORGANIZATION, organization));
    List<String> qualifiers = wsRequest.getQualifiers();
    if (!qualifiers.isEmpty()) {
      request.setParam(ComponentsWsParameters.PARAM_QUALIFIERS, Joiner.on(",").join(qualifiers));
    }
    setNullable(wsRequest.getQuery(), query -> request.setParam(TEXT_QUERY, query));
    setNullable(wsRequest.getPage(), page -> request.setParam(PAGE, String.valueOf(page)));
    setNullable(wsRequest.getPageSize(), pageSize -> request.setParam(PAGE_SIZE, String.valueOf(pageSize)));
    setNullable(wsRequest.getVisibility(), v -> request.setParam(PARAM_VISIBILITY, v));
    setNullable(wsRequest.getAnalyzedBefore(), d -> request.setParam(PARAM_ANALYZED_BEFORE, d));
    setNullable(wsRequest.getProjects(), l -> request.setParam(PARAM_PROJECTS, String.join(",", l)));
    setNullable(wsRequest.getProjectIds(), l -> request.setParam(PARAM_PROJECT_IDS, String.join(",", l)));
    request.setParam(PARAM_ON_PROVISIONED_ONLY, String.valueOf(wsRequest.isOnProvisionedOnly()));
    return request.executeProtobuf(SearchWsResponse.class);
  }

}
