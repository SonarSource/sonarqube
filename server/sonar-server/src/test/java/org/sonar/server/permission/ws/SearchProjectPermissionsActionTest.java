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
package org.sonar.server.permission.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.web.UserRole;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.i18n.I18nRule;
import org.sonarqube.ws.Permissions;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_QUALIFIER;

public class SearchProjectPermissionsActionTest extends BasePermissionWsTest<SearchProjectPermissionsAction> {

  private ComponentDbTester componentDb = new ComponentDbTester(db);
  private I18nRule i18n = new I18nRule();

  @Before
  public void setUp() {
    i18n.setProjectPermissions();
    userSession.logIn().setSystemAdministrator();
  }

  @Override
  protected SearchProjectPermissionsAction buildWsAction() {
    i18n.setProjectPermissions();
    ResourceTypesRule rootResourceTypes = newRootResourceTypes();
    PermissionWsSupport wsSupport = newPermissionWsSupport();
    return new SearchProjectPermissionsAction(db.getDbClient(), userSession, i18n, rootResourceTypes, wsSupport);
  }

  @Test
  public void search_project_permissions_counts_0_users_and_0_groups_on_public_project_without_any_specified_permission_in_DB() {
    ComponentDto project = db.components().insertPublicProject();

    String result = newRequest().execute().getInput();

    assertJson(result)
      .ignoreFields("permissions")
      .isSimilarTo("{" +
        "  \"paging\": {" +
        "    \"pageIndex\": 1," +
        "    \"pageSize\": 25," +
        "    \"total\": 1" +
        "  }," +
        "  \"projects\": [" +
        "    {" +
        "      \"id\": \"" + project.uuid() + "\"," +
        "      \"key\": \"" + project.getDbKey() + "\"," +
        "      \"name\": \"" + project.name() + "\"," +
        "      \"qualifier\": \"TRK\"," +
        "      \"permissions\": []" +
        "    }" +
        "  ]" +
        "}");
  }

  @Test
  public void search_project_permissions_counts_0_users_and_0_groups_on_private_project_without_any_specified_permission_in_DB() {
    ComponentDto project = db.components().insertPrivateProject();

    String result = newRequest().execute().getInput();

    assertJson(result)
      .ignoreFields("permissions")
      .isSimilarTo("{" +
        "  \"paging\": {" +
        "    \"pageIndex\": 1," +
        "    \"pageSize\": 25," +
        "    \"total\": 1" +
        "  }," +
        "  \"projects\": [" +
        "    {" +
        "      \"id\": \"" + project.uuid() + "\"," +
        "      \"key\": \"" + project.getDbKey() + "\"," +
        "      \"name\": \"" + project.name() + "\"," +
        "      \"qualifier\": \"TRK\"," +
        "      \"permissions\": []" +
        "    }" +
        "  ]" +
        "}");
  }

  @Test
  public void search_project_permissions() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();

    ComponentDto jdk7 = insertJdk7();
    ComponentDto project2 = insertClang();
    ComponentDto view = insertView();
    insertProjectInView(jdk7, view);

    db.users().insertProjectPermissionOnUser(user1, UserRole.ISSUE_ADMIN, jdk7);
    db.users().insertProjectPermissionOnUser(user1, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnUser(user2, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnUser(user3, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnUser(user1, UserRole.ISSUE_ADMIN, project2);
    db.users().insertProjectPermissionOnUser(user1, UserRole.ISSUE_ADMIN, view);
    // global permission
    db.users().insertPermissionOnUser(user1, ADMINISTER);

    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();

    db.users().insertProjectPermissionOnAnyone(UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnGroup(group1, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnGroup(group2, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnGroup(group3, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnGroup(group2, UserRole.ADMIN, view);

    db.commit();

    String result = newRequest().execute().getInput();

    assertJson(result)
      .ignoreFields("permissions")
      .isSimilarTo(getClass().getResource("search_project_permissions-example.json"));
  }

  @Test
  public void empty_result() {
    String result = newRequest().execute().getInput();

    assertJson(result)
      .ignoreFields("permissions")
      .isSimilarTo(getClass().getResource("SearchProjectPermissionsActionTest/empty.json"));
  }

  @Test
  public void search_project_permissions_with_project_permission() {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid"));
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    String result = newRequest()
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .execute().getInput();

    assertThat(result).contains("project-uuid");
  }

  @Test
  public void has_projects_ordered_by_name() {
    OrganizationDto organizationDto = db.organizations().insert();
    for (int i = 9; i >= 1; i--) {
      db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto)
        .setName("project-name-" + i));
    }

    String result = newRequest()
      .setParam(PAGE, "1")
      .setParam(PAGE_SIZE, "3")
      .execute().getInput();

    assertThat(result)
      .contains("project-name-1", "project-name-2", "project-name-3")
      .doesNotContain("project-name-4");
  }

  @Test
  public void search_by_query_on_name() {
    componentDb.insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setName("project-name"));
    componentDb.insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setName("another-name"));

    String result = newRequest()
      .setParam(TEXT_QUERY, "project")
      .execute().getInput();

    assertThat(result).contains("project-name")
      .doesNotContain("another-name");
  }

  @Test
  public void search_by_query_on_key_must_match_exactly() {
    OrganizationDto organizationDto = db.organizations().insert();
    componentDb.insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(organizationDto).setDbKey("project-key"));
    componentDb.insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(organizationDto).setDbKey("another-key"));

    String result = newRequest()
      .setParam(TEXT_QUERY, "project-key")
      .execute()
      .getInput();

    assertThat(result).contains("project-key")
      .doesNotContain("another-key");
  }

  @Test
  public void handle_more_than_1000_projects() {
    for (int i = 1; i <= 1001; i++) {
      componentDb.insertProjectAndSnapshot(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid-" + i));
    }

    String result = newRequest()
      .setParam(TEXT_QUERY, "project")
      .setParam(PAGE_SIZE, "1001")
      .execute()
      .getInput();

    assertThat(result).contains("project-uuid-1", "project-uuid-999", "project-uuid-1001");
  }

  @Test
  public void filter_by_qualifier() {
    OrganizationDto organizationDto = db.organizations().insert();
    db.components().insertComponent(newView(organizationDto, "view-uuid"));
    db.components().insertComponent(newPrivateProjectDto(organizationDto, "project-uuid"));

    Permissions.SearchProjectPermissionsWsResponse result = newRequest()
      .setParam(PARAM_QUALIFIER, Qualifiers.PROJECT)
      .executeProtobuf(Permissions.SearchProjectPermissionsWsResponse.class);

    assertThat(result.getProjectsList())
      .extracting("id")
      .contains("project-uuid")
      .doesNotContain("view-uuid");
  }

  @Test
  public void fail_if_not_logged_in() {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    newRequest().execute();
  }

  @Test
  public void fail_if_not_admin() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequest().execute();
  }

  @Test
  public void display_all_project_permissions() {
    String result = newRequest().execute().getInput();

    assertJson(result)
      .ignoreFields("permissions")
      .isSimilarTo(getClass().getResource("SearchProjectPermissionsActionTest/display_all_project_permissions.json"));
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Project key '%s' not found", branch.getDbKey()));

    newRequest()
      .setParam(PARAM_PROJECT_KEY, branch.getDbKey())
      .execute();
  }

  private ComponentDto insertView() {
    return db.components().insertComponent(newView(db.getDefaultOrganization())
      .setUuid("752d8bfd-420c-4a83-a4e5-8ab19b13c8fc")
      .setName("Java")
      .setDbKey("Java"));
  }

  private ComponentDto insertProjectInView(ComponentDto project, ComponentDto view) {
    return db.components().insertComponent(newProjectCopy("project-in-view-uuid", project, view));
  }

  private ComponentDto insertClang() {
    return db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid-2")
      .setName("Clang")
      .setDbKey("clang")
      .setUuid("ce4c03d6-430f-40a9-b777-ad877c00aa4d"));
  }

  private ComponentDto insertJdk7() {
    return db.components().insertComponent(ComponentTesting.newPublicProjectDto(db.getDefaultOrganization())
      .setName("JDK 7")
      .setDbKey("net.java.openjdk:jdk7")
      .setUuid("0bd7b1e7-91d6-439e-a607-4a3a9aad3c6a"));
  }
}
