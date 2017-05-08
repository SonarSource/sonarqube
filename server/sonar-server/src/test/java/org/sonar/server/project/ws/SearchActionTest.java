/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
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
import org.sonarqube.ws.WsProjects.SearchWsResponse;
import org.sonarqube.ws.WsProjects.SearchWsResponse.Component;
import org.sonarqube.ws.client.component.ComponentsWsParameters;
import org.sonarqube.ws.client.project.SearchWsRequest;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ORGANIZATION;
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
    new SearchAction(db.getDbClient(), userSession, defaultOrganizationProvider, new ProjectsWsSupport(db.getDbClient(), mock(BillingValidationsProxy.class))));

  @Test
  public void search_by_key_query() throws IOException {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setKey("project-_%-key"),
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setKey("project-key-without-escaped-characters"));

    SearchWsResponse response = call(SearchWsRequest.builder().setQuery("project-_%-key").build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("project-_%-key");
  }

  @Test
  public void search_private_projects() {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setKey("private-key"),
      ComponentTesting.newPublicProjectDto(db.getDefaultOrganization()).setKey("public-key"));

    SearchWsResponse response = call(SearchWsRequest.builder().setVisibility("private").build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("private-key");
  }

  @Test
  public void search_public_projects() {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setKey("private-key"),
      ComponentTesting.newPublicProjectDto(db.getDefaultOrganization()).setKey("public-key"));

    SearchWsResponse response = call(SearchWsRequest.builder().setVisibility("public").build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("public-key");
  }

  @Test
  public void search_projects_when_no_qualifier_set() throws IOException {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setKey(PROJECT_KEY_1),
      newView(db.getDefaultOrganization()));

    SearchWsResponse response = call(SearchWsRequest.builder().build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(PROJECT_KEY_1);
  }

  @Test
  public void search_projects() throws IOException {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setKey(PROJECT_KEY_1);
    ComponentDto module = newModuleDto(project);
    ComponentDto directory = newDirectory(module, "dir");
    ComponentDto file = newFileDto(directory);
    db.components().insertComponents(
      project, module, directory, file,
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setKey(PROJECT_KEY_2),
      newView(db.getDefaultOrganization()));

    SearchWsResponse response = call(SearchWsRequest.builder().setQualifiers(singletonList("TRK")).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(PROJECT_KEY_1, PROJECT_KEY_2);
  }

  @Test
  public void search_views() throws IOException {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setKey(PROJECT_KEY_1),
      newView(db.getDefaultOrganization()).setKey("view1"));

    SearchWsResponse response = call(SearchWsRequest.builder().setQualifiers(singletonList("VW")).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("view1");
  }

  @Test
  public void search_projects_and_views() throws IOException {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setKey(PROJECT_KEY_1),
      newView(db.getDefaultOrganization()).setKey("view1"));

    SearchWsResponse response = call(SearchWsRequest.builder().setQualifiers(asList("TRK", "VW")).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(PROJECT_KEY_1, "view1");
  }

  @Test
  public void search_on_default_organization_when_no_organization_set() throws IOException {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    OrganizationDto otherOrganization = db.organizations().insert();
    db.components().insertComponents(
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setKey(PROJECT_KEY_1),
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setKey(PROJECT_KEY_2),
      ComponentTesting.newPrivateProjectDto(otherOrganization).setKey(PROJECT_KEY_3));

    SearchWsResponse response = call(SearchWsRequest.builder().build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(PROJECT_KEY_1, PROJECT_KEY_2);
  }

  @Test
  public void search_for_projects_on_given_organization() throws IOException {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    userSession.addPermission(ADMINISTER, organization1);
    ComponentDto project1 = ComponentTesting.newPrivateProjectDto(organization1);
    ComponentDto project2 = ComponentTesting.newPrivateProjectDto(organization1);
    ComponentDto project3 = ComponentTesting.newPrivateProjectDto(organization2);
    db.components().insertComponents(project1, project2, project3);

    SearchWsResponse response = call(SearchWsRequest.builder().setOrganization(organization1.getKey()).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(project1.key(), project2.key());
  }

  @Test
  public void result_is_paginated() throws IOException {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    List<ComponentDto> componentDtoList = new ArrayList<>();
    for (int i = 1; i <= 9; i++) {
      componentDtoList.add(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid-" + i).setKey("project-key-" + i).setName("Project Name " + i));
    }
    db.components().insertComponents(componentDtoList.toArray(new ComponentDto[] {}));

    SearchWsResponse response = call(SearchWsRequest.builder().setPage(2).setPageSize(3).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsExactly("project-key-4", "project-key-5", "project-key-6");
  }

  @Test
  public void fail_when_not_system_admin() throws Exception {
    userSession.addPermission(ADMINISTER_QUALITY_PROFILES, db.getDefaultOrganization());
    expectedException.expect(ForbiddenException.class);

    call(SearchWsRequest.builder().build());
  }

  @Test
  public void fail_on_unknown_organization() throws Exception {
    expectedException.expect(NotFoundException.class);

    call(SearchWsRequest.builder().setOrganization("unknown").build());
  }

  @Test
  public void fail_on_invalid_qualifier() throws Exception {
    userSession.addPermission(ADMINISTER_QUALITY_PROFILES, db.getDefaultOrganization());
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value of parameter 'qualifiers' (BRC) must be one of: [TRK, VW]");

    call(SearchWsRequest.builder().setQualifiers(singletonList("BRC")).build());
  }

  @Test
  public void verify_define() {
    WebService.Action action = ws.getDef();
    assertThat(action.key()).isEqualTo("search");
    assertThat(action.isPost()).isFalse();
    assertThat(action.description()).isEqualTo("Search for projects or views.<br>Requires 'System Administrator' permission");
    assertThat(action.isInternal()).isTrue();
    assertThat(action.since()).isEqualTo("6.3");
    assertThat(action.handler()).isEqualTo(ws.getDef().handler());
    assertThat(action.params()).hasSize(6);
    assertThat(action.responseExample()).isEqualTo(getClass().getResource("search-example.json"));

    WebService.Param organization = action.param("organization");
    Assertions.assertThat(organization.description()).isEqualTo("The key of the organization");
    Assertions.assertThat(organization.isInternal()).isTrue();
    Assertions.assertThat(organization.isRequired()).isFalse();
    Assertions.assertThat(organization.since()).isEqualTo("6.3");

    WebService.Param qParam = action.param("q");
    assertThat(qParam.isRequired()).isFalse();
    assertThat(qParam.description()).isEqualTo("Limit search to component names or component keys that contain the supplied string.");

    WebService.Param qualifierParam = action.param("qualifiers");
    assertThat(qualifierParam.isRequired()).isFalse();
    assertThat(qualifierParam.description()).isEqualTo("Comma-separated list of component qualifiers. Filter the results with the specified qualifiers");
    assertThat(qualifierParam.possibleValues()).containsOnly("TRK", "VW");
    assertThat(qualifierParam.defaultValue()).isEqualTo("TRK");

    WebService.Param pParam = action.param("p");
    assertThat(pParam.isRequired()).isFalse();
    assertThat(pParam.defaultValue()).isEqualTo("1");
    assertThat(pParam.description()).isEqualTo("1-based page number");

    WebService.Param psParam = action.param("ps");
    assertThat(psParam.isRequired()).isFalse();
    assertThat(psParam.defaultValue()).isEqualTo("100");
    assertThat(psParam.description()).isEqualTo("Page size. Must be greater than 0 and less than 500");

    WebService.Param visibilityParam = action.param("visibility");
    assertThat(visibilityParam.isRequired()).isFalse();
    assertThat(visibilityParam.description()).isEqualTo("Filter the projects that should be visible to everyone (public), or only specific user/groups (private).<br/>" +
      "If no visibility is specified, the default project visibility of the organization will be used.");
  }

  @Test
  public void verify_response_example() throws URISyntaxException, IOException {
    OrganizationDto organization = db.organizations().insertForKey("my-org-1");
    userSession.addPermission(ADMINISTER, organization);
    db.components().insertComponents(
      newPrivateProjectDto(organization, "project-uuid-1").setName("Project Name 1").setKey("project-key-1").setPrivate(false),
      newPrivateProjectDto(organization, "project-uuid-2").setName("Project Name 1").setKey("project-key-2"));

    String response = ws.newRequest()
      .setMediaType(MediaTypes.JSON)
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute().getInput();
    assertJson(response).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  private SearchWsResponse call(SearchWsRequest wsRequest) {
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
    return request.executeProtobuf(SearchWsResponse.class);
  }

}
