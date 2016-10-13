/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.permission.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.ws.WsTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsPermissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.component.ComponentTesting.newDeveloper;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.CONTROLLER;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_QUALIFIER;

public class SearchProjectPermissionsActionTest extends BasePermissionWsTest<SearchProjectPermissionsAction> {

  private ComponentDbTester componentDb = new ComponentDbTester(db);
  private I18nRule i18n = new I18nRule();

  @Before
  public void setUp() {
    i18n.setProjectPermissions();
    userSession.login().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }

  @Override
  protected SearchProjectPermissionsAction buildWsAction() {
    i18n.setProjectPermissions();
    ResourceTypesRule rootResourceTypes = newRootResourceTypes();
    PermissionWsSupport wsSupport = newPermissionWsSupport();
    SearchProjectPermissionsDataLoader dataLoader = new SearchProjectPermissionsDataLoader(db.getDbClient(), wsSupport, rootResourceTypes);
    return new SearchProjectPermissionsAction(db.getDbClient(), userSession, i18n, rootResourceTypes, dataLoader, wsSupport);
  }

  @Test
  public void search_project_permissions() throws Exception {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();

    ComponentDto jdk7 = insertJdk7();
    ComponentDto project2 = insertClang();
    ComponentDto dev = insertDeveloper();
    ComponentDto view = insertView();
    insertProjectInView(jdk7, view);

    db.users().insertProjectPermissionOnUser(user1, UserRole.ISSUE_ADMIN, jdk7);
    db.users().insertProjectPermissionOnUser(user1, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnUser(user2, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnUser(user3, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnUser(user1, UserRole.ISSUE_ADMIN, project2);
    db.users().insertProjectPermissionOnUser(user1, UserRole.ISSUE_ADMIN, dev);
    db.users().insertProjectPermissionOnUser(user1, UserRole.ISSUE_ADMIN, view);
    // global permission
    db.users().insertPermissionOnUser(user1, GlobalPermissions.SYSTEM_ADMIN);

    GroupDto group1 = db.users().insertGroup(newGroupDto());
    GroupDto group2 = db.users().insertGroup(newGroupDto());
    GroupDto group3 = db.users().insertGroup(newGroupDto());

    db.users().insertProjectPermissionOnAnyone(db.getDefaultOrganization(), UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnGroup(group1, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnGroup(group2, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnGroup(group3, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnGroup(group2, UserRole.ADMIN, dev);
    db.users().insertProjectPermissionOnGroup(group2, UserRole.ADMIN, view);

    db.commit();

    String result = newRequest().execute().outputAsString();

    assertJson(result)
      .ignoreFields("permissions")
      .isSimilarTo(getClass().getResource("search_project_permissions-example.json"));
  }

  @Test
  public void empty_result() throws Exception {
    String result = newRequest().execute().outputAsString();

    assertJson(result)
      .ignoreFields("permissions")
      .isSimilarTo(getClass().getResource("SearchProjectPermissionsActionTest/empty.json"));
  }

  @Test
  public void search_project_permissions_with_project_permission() throws Exception {
    userSession.login().addProjectUuidPermissions(UserRole.ADMIN, "project-uuid");
    db.components().insertComponent(newProjectDto("project-uuid"));

    String result = newRequest()
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .execute().outputAsString();

    assertThat(result).contains("project-uuid");
  }

  @Test
  public void has_projects_ordered_by_name() throws Exception {
    for (int i = 9; i >= 1; i--) {
      db.components().insertComponent(newProjectDto()
        .setName("project-name-" + i));
    }

    String result = newRequest()
      .setParam(PAGE, "1")
      .setParam(PAGE_SIZE, "3")
      .execute().outputAsString();

    assertThat(result)
      .contains("project-name-1", "project-name-2", "project-name-3")
      .doesNotContain("project-name-4");
  }

  @Test
  public void search_by_query_on_name() throws Exception {
    componentDb.insertProjectAndSnapshot(newProjectDto().setName("project-name"));
    componentDb.insertProjectAndSnapshot(newProjectDto().setName("another-name"));
    componentDb.indexAllComponents();

    String result = newRequest()
      .setParam(TEXT_QUERY, "project")
      .execute().outputAsString();

    assertThat(result).contains("project-name")
      .doesNotContain("another-name");
  }

  @Test
  public void search_by_query_on_key_must_match_exactly() throws Exception {
    componentDb.insertProjectAndSnapshot(newProjectDto().setKey("project-key"));
    componentDb.insertProjectAndSnapshot(newProjectDto().setKey("another-key"));
    componentDb.indexAllComponents();

    String result = newRequest()
      .setParam(TEXT_QUERY, "project-key")
      .execute()
      .outputAsString();

    assertThat(result).contains("project-key")
      .doesNotContain("another-key");
  }

  @Test
  public void handle_more_than_1000_projects() throws Exception {
    for (int i = 1; i <= 1001; i++) {
      componentDb.insertProjectAndSnapshot(newProjectDto("project-uuid-" + i));
    }
    componentDb.indexAllComponents();

    String result = newRequest()
      .setParam(TEXT_QUERY, "project")
      .setParam(PAGE_SIZE, "1001")
      .execute()
      .outputAsString();

    assertThat(result).contains("project-uuid-1", "project-uuid-999", "project-uuid-1001");
  }

  @Test
  public void filter_by_qualifier() throws Exception {
    db.components().insertComponent(newView("view-uuid"));
    db.components().insertComponent(newDeveloper("developer-name"));
    db.components().insertComponent(newProjectDto("project-uuid"));

    byte[] wsResponse = newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_QUALIFIER, Qualifiers.PROJECT)
      .execute()
      .output();
    WsPermissions.SearchProjectPermissionsWsResponse result = WsPermissions.SearchProjectPermissionsWsResponse.parseFrom(wsResponse);

    assertThat(result.getProjectsList())
      .extracting("id")
      .contains("project-uuid")
      .doesNotContain("view-uuid")
      .doesNotContain("developer-name");
  }

  @Test
  public void fail_if_not_logged_in() throws Exception {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    newRequest().execute();
  }

  @Test
  public void fail_if_not_admin() throws Exception {
    userSession.login();

    expectedException.expect(ForbiddenException.class);

    newRequest().execute();
  }

  @Test
  public void display_all_project_permissions() throws Exception {
    String result = newRequest().execute().outputAsString();

    assertJson(result)
      .ignoreFields("permissions")
      .isSimilarTo(getClass().getResource("SearchProjectPermissionsActionTest/display_all_project_permissions.json"));
  }

  private ComponentDto insertView() {
    return db.components().insertComponent(newView()
      .setUuid("752d8bfd-420c-4a83-a4e5-8ab19b13c8fc")
      .setName("Java")
      .setKey("Java"));
  }

  private ComponentDto insertProjectInView(ComponentDto project, ComponentDto view) {
    return db.components().insertComponent(newProjectCopy("project-in-view-uuid", project, view));
  }

  private ComponentDto insertDeveloper() {
    return db.components().insertComponent(newDeveloper("Simon Brandhof")
      .setUuid("4e607bf9-7ed0-484a-946d-d58ba7dab2fb")
      .setKey("simon-brandhof"));
  }

  private ComponentDto insertClang() {
    return db.components().insertComponent(newProjectDto("project-uuid-2")
      .setName("Clang")
      .setKey("clang")
      .setUuid("ce4c03d6-430f-40a9-b777-ad877c00aa4d"));
  }

  private ComponentDto insertJdk7() {
    return db.components().insertComponent(newProjectDto("project-uuid-1")
      .setName("JDK 7")
      .setKey("net.java.openjdk:jdk7")
      .setUuid("0bd7b1e7-91d6-439e-a607-4a3a9aad3c6a"));
  }

  private WsTester.TestRequest newRequest() {
    return wsTester.newPostRequest(CONTROLLER, "search_project_permissions");
  }
}
