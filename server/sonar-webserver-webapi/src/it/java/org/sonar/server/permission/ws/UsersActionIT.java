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
package org.sonar.server.permission.ws;

import java.util.Set;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.api.web.UserRole;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.avatar.AvatarResolverImpl;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.permission.RequestValidator;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang.StringUtils.countMatches;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_USER_LOGIN;

public class UsersActionIT extends BasePermissionWsIT<UsersAction> {

  private final ResourceTypes resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private final PermissionService permissionService = new PermissionServiceImpl(resourceTypes);
  private final WsParameters wsParameters = new WsParameters(permissionService);
  private final RequestValidator requestValidator = new RequestValidator(permissionService);
  private final ManagedInstanceService managedInstanceService = mock(ManagedInstanceService.class);

  @Override
  protected UsersAction buildWsAction() {
    return new UsersAction(db.getDbClient(), userSession, newPermissionWsSupport(), new AvatarResolverImpl(), wsParameters, requestValidator, managedInstanceService);
  }

  @Test
  public void search_for_users_with_response_example() {
    UserDto user1 = db.users().insertUser(newUserDto().setLogin("admin").setName("Administrator").setEmail("admin@admin.com"));
    UserDto user2 = db.users().insertUser(newUserDto().setLogin("adam.west").setName("Adam West").setEmail("adamwest@adamwest.com"));
    UserDto user3 = db.users().insertUser(newUserDto().setLogin("george.orwell").setName("George Orwell").setEmail("george.orwell@1984.net"));
    db.users().insertGlobalPermissionOnUser(user1, GlobalPermission.ADMINISTER_QUALITY_PROFILES);
    db.users().insertGlobalPermissionOnUser(user1, GlobalPermission.ADMINISTER);
    db.users().insertGlobalPermissionOnUser(user1, GlobalPermission.ADMINISTER_QUALITY_GATES);
    db.users().insertGlobalPermissionOnUser(user3, GlobalPermission.SCAN);
    mockUsersAsManaged(user3.getUuid());

    loginAsAdmin();
    String result = newRequest().execute().getInput();

    assertJson(result).withStrictArrayOrder().isSimilarTo(getClass().getResource("users-example.json"));
  }

  @Test
  public void search_for_users_with_one_permission() {
    insertUsersHavingGlobalPermissions();

    loginAsAdmin();
    String result = newRequest().setParam("permission", "scan").execute().getInput();

    assertJson(result).withStrictArrayOrder().isSimilarTo(getClass().getResource("UsersActionIT/users.json"));
  }

  @Test
  public void search_for_users_with_permission_on_project() {
    // User has permission on project
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    UserDto user = db.users().insertUser(newUserDto());
    db.users().insertProjectPermissionOnUser(user, UserRole.ISSUE_ADMIN, project);

    // User has permission on another project
    ProjectDto anotherProject = db.components().insertPrivateProject().getProjectDto();
    UserDto userHavePermissionOnAnotherProject = db.users().insertUser(newUserDto());
    db.users().insertProjectPermissionOnUser(userHavePermissionOnAnotherProject, UserRole.ISSUE_ADMIN, anotherProject);

    // User has no permission
    UserDto withoutPermission = db.users().insertUser(newUserDto());

    userSession.logIn().addProjectPermission(GlobalPermission.ADMINISTER.getKey(), project);
    String result = newRequest()
      .setParam(PARAM_PERMISSION, UserRole.ISSUE_ADMIN)
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .execute()
      .getInput();

    assertThat(result).contains(user.getLogin())
      .doesNotContain(userHavePermissionOnAnotherProject.getLogin())
      .doesNotContain(withoutPermission.getLogin());
  }

  @Test
  public void search_also_for_users_without_permission_when_filtering_name() {
    // User with permission on project
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    UserDto user = db.users().insertUser(newUserDto("with-permission-login", "with-permission-name", "with-permission-email"));
    db.users().insertProjectPermissionOnUser(user, UserRole.ISSUE_ADMIN, project);

    // User without permission
    UserDto withoutPermission = db.users().insertUser(newUserDto("without-permission-login", "without-permission-name", "without-permission-email"));
    UserDto anotherUser = db.users().insertUser(newUserDto("another-user", "another-user", "another-user"));

    loginAsAdmin();
    String result = newRequest()
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(TEXT_QUERY, "with")
      .execute()
      .getInput();

    assertThat(result).contains(user.getLogin(), withoutPermission.getLogin()).doesNotContain(anotherUser.getLogin());
  }

  @Test
  public void search_also_for_users_without_permission_when_filtering_email() {
    // User with permission on project
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    UserDto user = db.users().insertUser(newUserDto("with-permission-login", "with-permission-name", "with-permission-email"));
    db.users().insertProjectPermissionOnUser(user, UserRole.ISSUE_ADMIN, project);

    // User without permission
    UserDto withoutPermission = db.users().insertUser(newUserDto("without-permission-login", "without-permission-name", "without-permission-email"));
    UserDto anotherUser = db.users().insertUser(newUserDto("another-user", "another-user", "another-user"));

    loginAsAdmin();
    String result = newRequest().setParam(PARAM_PROJECT_ID, project.getUuid()).setParam(TEXT_QUERY, "email").execute().getInput();

    assertThat(result).contains(user.getLogin(), withoutPermission.getLogin()).doesNotContain(anotherUser.getLogin());
  }

  @Test
  public void search_also_for_users_without_permission_when_filtering_login() {
    // User with permission on project
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    UserDto user = db.users().insertUser(newUserDto("with-permission-login", "with-permission-name", "with-permission-email"));
    db.users().insertProjectPermissionOnUser(user, UserRole.ISSUE_ADMIN, project);

    // User without permission
    UserDto withoutPermission = db.users().insertUser(newUserDto("without-permission-login", "without-permission-name", "without-permission-email"));
    UserDto anotherUser = db.users().insertUser(newUserDto("another-user", "another-user", "another-user"));

    loginAsAdmin();
    String result = newRequest().setParam(PARAM_PROJECT_ID, project.getUuid()).setParam(TEXT_QUERY, "login").execute().getInput();

    assertThat(result).contains(user.getLogin(), withoutPermission.getLogin()).doesNotContain(anotherUser.getLogin());
  }

  @Test
  public void search_for_users_with_query_as_a_parameter() {
    insertUsersHavingGlobalPermissions();

    loginAsAdmin();
    String result = newRequest()
      .setParam("permission", "scan")
      .setParam(TEXT_QUERY, "ame-1")
      .execute()
      .getInput();

    assertThat(result).contains("login-1")
      .doesNotContain("login-2")
      .doesNotContain("login-3");
  }

  @Test
  public void search_for_users_with_select_as_a_parameter() {
    insertUsersHavingGlobalPermissions();

    loginAsAdmin();
    String result = newRequest()
      .execute()
      .getInput();

    assertThat(result).contains("login-1", "login-2", "login-3");
  }

  @Test
  public void search_for_users_is_paginated() {
    for (int i = 9; i >= 0; i--) {
      UserDto user = db.users().insertUser(newUserDto().setName("user-" + i));
      db.users().insertGlobalPermissionOnUser(user, GlobalPermission.ADMINISTER);
      db.users().insertGlobalPermissionOnUser(user, GlobalPermission.ADMINISTER_QUALITY_GATES);
    }
    loginAsAdmin();

    assertJson(newRequest().setParam(PAGE, "1").setParam(PAGE_SIZE, "2").execute().getInput()).withStrictArrayOrder().isSimilarTo("{\n" +
      "  \"paging\": {\n" +
      "    \"pageIndex\": 1,\n" +
      "    \"pageSize\": 2,\n" +
      "    \"total\": 10\n" +
      "  },\n" +
      "  \"users\": [\n" +
      "    {\n" +
      "      \"name\": \"user-0\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"name\": \"user-1\"\n" +
      "    }\n" +
      "  ]\n" +
      "}");
    assertJson(newRequest().setParam(PAGE, "3").setParam(PAGE_SIZE, "4").execute().getInput()).withStrictArrayOrder().isSimilarTo("{\n" +
      "  \"paging\": {\n" +
      "    \"pageIndex\": 3,\n" +
      "    \"pageSize\": 4,\n" +
      "    \"total\": 10\n" +
      "  },\n" +
      "  \"users\": [\n" +
      "    {\n" +
      "      \"name\": \"user-8\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"name\": \"user-9\"\n" +
      "    }\n" +
      "  ]\n" +
      "}");
  }

  @Test
  public void return_more_than_20_permissions() {
    loginAsAdmin();
    for (int i = 0; i < 30; i++) {
      UserDto user = db.users().insertUser(newUserDto().setLogin("user-" + i));
      db.users().insertGlobalPermissionOnUser(user, GlobalPermission.SCAN);
      db.users().insertGlobalPermissionOnUser(user, GlobalPermission.PROVISION_PROJECTS);
    }

    String result = newRequest()
      .setParam(PAGE_SIZE, "100")
      .execute()
      .getInput();

    assertThat(countMatches(result, "scan")).isEqualTo(30);
  }

  @Test
  public void fail_if_project_permission_without_project() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_PERMISSION, UserRole.ISSUE_ADMIN)
        .setParam(Param.SELECTED, SelectionMode.ALL.value())
        .execute();
    })
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void fail_if_insufficient_privileges() {
    userSession.logIn("login");

    assertThatThrownBy(() -> {
      newRequest()
        .setParam("permission", GlobalPermission.ADMINISTER.getKey())
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_not_logged_in() {
    userSession.anonymous();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam("permission", GlobalPermission.ADMINISTER.getKey())
        .execute();
    })
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_if_project_uuid_and_project_key_are_provided() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    loginAsAdmin();

    assertThatThrownBy(() -> newRequest()
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .execute())
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Project id or project key can be provided, not both.");
  }

  @Test
  public void fail_if_search_query_is_too_short() {
    loginAsAdmin();

    assertThatThrownBy(() -> newRequest().setParam(TEXT_QUERY, "ab").execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'q' length (2) is shorter than the minimum authorized (3)");
  }

  @Test
  public void fail_when_using_branch_uuid() {
    UserDto user = db.users().insertUser(newUserDto());
    ProjectData project = db.components().insertPublicProject();
    BranchDto branch = db.components().insertProjectBranch(project.getProjectDto());
    db.users().insertProjectPermissionOnUser(user, UserRole.ISSUE_ADMIN, project.getProjectDto());
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project.getProjectDto());

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_PROJECT_ID, branch.getUuid())
        .setParam(PARAM_USER_LOGIN, user.getLogin())
        .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Entity not found");
  }

  private void insertUsersHavingGlobalPermissions() {
    UserDto user1 = db.users().insertUser(newUserDto("login-1", "name-1", "email-1"));
    UserDto user2 = db.users().insertUser(newUserDto("login-2", "name-2", "email-2"));
    UserDto user3 = db.users().insertUser(newUserDto("login-3", "name-3", "email-3"));
    db.users().insertGlobalPermissionOnUser(user1, GlobalPermission.SCAN);
    db.users().insertGlobalPermissionOnUser(user2, GlobalPermission.SCAN);
    db.users().insertGlobalPermissionOnUser(user3, GlobalPermission.ADMINISTER);
    mockUsersAsManaged(user1.getUuid());
  }

  private void mockUsersAsManaged(String... userUuids) {
    when(managedInstanceService.getUserUuidToManaged(any(), any())).thenAnswer(invocation ->
      {
        Set<?> allUsersUuids = invocation.getArgument(1, Set.class);
        return allUsersUuids.stream()
          .map(userUuid -> (String) userUuid)
          .collect(toMap(identity(), userUuid -> Set.of(userUuids).contains(userUuid)));
      }
    );
  }
}
