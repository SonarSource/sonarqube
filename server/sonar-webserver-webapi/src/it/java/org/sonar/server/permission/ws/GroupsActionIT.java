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
package org.sonar.server.permission.ws;

import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.component.ComponentTypes;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.permission.ProjectPermission.ISSUE_ADMIN;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;

public class GroupsActionIT extends BasePermissionWsIT<GroupsAction> {

  private GroupDto group1;
  private GroupDto group2;
  private final ComponentTypes componentTypes = new ComponentTypesRule().setRootQualifiers(ComponentQualifiers.PROJECT);
  private final PermissionService permissionService = new PermissionServiceImpl(componentTypes);
  private final WsParameters wsParameters = new WsParameters(permissionService);
  private final ManagedInstanceService managedInstanceService = mock(ManagedInstanceService.class);

  @Override
  protected GroupsAction buildWsAction() {
    return new GroupsAction(
      db.getDbClient(),
      userSession,
      newPermissionWsSupport(), wsParameters, managedInstanceService);
  }

  @Before
  public void setUp() {
    group1 = db.users().insertGroup("group-1-name");
    group2 = db.users().insertGroup("group-2-name");
    GroupDto group3 = db.users().insertGroup("group-3-name");
    db.users().insertPermissionOnGroup(group1, GlobalPermission.SCAN);
    db.users().insertPermissionOnGroup(group2, GlobalPermission.SCAN);
    db.users().insertPermissionOnGroup(group3, GlobalPermission.ADMINISTER);
    db.users().insertPermissionOnAnyone(GlobalPermission.SCAN);
    db.commit();
  }

  @Test
  public void verify_definition() {
    Action wsDef = wsTester.getDef();

    assertThat(wsDef.isInternal()).isTrue();
    assertThat(wsDef.since()).isEqualTo("5.2");
    assertThat(wsDef.isPost()).isFalse();
    assertThat(wsDef.changelog()).extracting(Change::getVersion, Change::getDescription).containsExactlyInAnyOrder(
      tuple("10.0", "Response includes 'managed' field."),
      tuple("8.4", "Field 'id' in the response is deprecated. Format changes from integer to string."),
      tuple("7.4", "The response list is returning all groups even those without permissions, the groups with permission are at the top of the list."));
  }

  @Test
  public void search_for_groups_with_one_permission() {
    loginAsAdmin();

    String json = newRequest()
      .setParam(PARAM_PERMISSION, GlobalPermission.SCAN.getKey())
      .execute()
      .getInput();
    assertJson(json).isSimilarTo("{\n" +
      "  \"paging\": {\n" +
      "    \"pageIndex\": 1,\n" +
      "    \"pageSize\": 20,\n" +
      "    \"total\": 3\n" +
      "  },\n" +
      "  \"groups\": [\n" +
      "    {\n" +
      "      \"name\": \"Anyone\",\n" +
      "      \"permissions\": [\n" +
      "        \"scan\"\n" +
      "      ]\n" +
      "    },\n" +
      "    {\n" +
      "      \"name\": \"group-1-name\",\n" +
      "      \"description\": \"" + group1.getDescription() + "\",\n" +
      "      \"permissions\": [\n" +
      "        \"scan\"\n" +
      "      ],\n" +
      "      \"managed\": false\n" +
      "    },\n" +
      "    {\n" +
      "      \"name\": \"group-2-name\",\n" +
      "      \"description\": \"" + group2.getDescription() + "\",\n" +
      "      \"permissions\": [\n" +
      "        \"scan\"\n" +
      "      ],\n" +
      "      \"managed\": false\n" +
      "    }\n" +
      "  ]\n" +
      "}\n");
  }

  @Test
  public void search_with_selection() {
    loginAsAdmin();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, GlobalPermission.SCAN.getKey())
      .execute()
      .getInput();

    assertThat(result).containsSubsequence(DefaultGroups.ANYONE, "group-1", "group-2");
  }

  @Test
  public void search_groups_with_pagination() {
    loginAsAdmin();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, GlobalPermission.SCAN.getKey())
      .setParam(PAGE_SIZE, "1")
      .setParam(PAGE, "3")
      .execute()
      .getInput();

    assertThat(result).contains("group-2")
      .doesNotContain("group-1")
      .doesNotContain("group-3");
  }

  @Test
  public void search_groups_with_query() {
    loginAsAdmin();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, GlobalPermission.SCAN.getKey())
      .setParam(TEXT_QUERY, "group-")
      .execute()
      .getInput();

    assertThat(result)
      .contains("group-1", "group-2")
      .doesNotContain(DefaultGroups.ANYONE);
  }

  @Test
  public void search_groups_with_project_permissions() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    GroupDto group = db.users().insertGroup("project-group-name");
    db.users().insertEntityPermissionOnGroup(group, ISSUE_ADMIN, project);

    ProjectDto anotherProject = db.components().insertPrivateProject().getProjectDto();
    GroupDto anotherGroup = db.users().insertGroup("another-project-group-name");
    db.users().insertEntityPermissionOnGroup(anotherGroup, ISSUE_ADMIN, anotherProject);

    GroupDto groupWithoutPermission = db.users().insertGroup("group-without-permission");

    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);
    String result = newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN.getKey())
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .execute()
      .getInput();

    assertThat(result).contains(group.getName())
      .doesNotContain(anotherGroup.getName())
      .doesNotContain(groupWithoutPermission.getName());
  }

  @Test
  public void return_also_groups_without_permission_when_search_query() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    GroupDto group = db.users().insertGroup("group-with-permission");
    db.users().insertEntityPermissionOnGroup(group, ISSUE_ADMIN, project);

    GroupDto groupWithoutPermission = db.users().insertGroup("group-without-permission");
    GroupDto anotherGroup = db.users().insertGroup("another-group");

    loginAsAdmin();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN.getKey())
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(TEXT_QUERY, "group-with")
      .execute()
      .getInput();

    assertThat(result).contains(group.getName())
      .doesNotContain(groupWithoutPermission.getName())
      .doesNotContain(anotherGroup.getName());
  }

  @Test
  public void return_only_groups_with_permission_when_no_search_query() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    GroupDto group = db.users().insertGroup("project-group-name");
    db.users().insertEntityPermissionOnGroup(group, ISSUE_ADMIN, project);

    GroupDto groupWithoutPermission = db.users().insertGroup("group-without-permission");

    loginAsAdmin();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN.getKey())
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .execute()
      .getInput();

    assertThat(result).contains(group.getName()).doesNotContain(groupWithoutPermission.getName());
  }

  @Test
  public void return_anyone_group_when_search_query_and_no_param_permission() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    GroupDto group = db.users().insertGroup("group-with-permission");
    db.users().insertEntityPermissionOnGroup(group, ISSUE_ADMIN, project);

    loginAsAdmin();
    String result = newRequest()
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(TEXT_QUERY, "nyo")
      .execute()
      .getInput();

    assertThat(result).contains("Anyone");
  }

  @Test
  public void search_groups_on_views() {
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto("view-uuid");
    GroupDto group = db.users().insertGroup("project-group-name");
    db.users().insertEntityPermissionOnGroup(group, ISSUE_ADMIN, portfolio);

    loginAsAdmin();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN.getKey())
      .setParam(PARAM_PROJECT_ID, "view-uuid")
      .execute()
      .getInput();

    assertThat(result).contains("project-group-name")
      .doesNotContain("group-1")
      .doesNotContain("group-2")
      .doesNotContain("group-3");
  }

  @Test
  public void return_isManaged() {
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto("view-uuid");
    GroupDto managedGroup = db.users().insertGroup("managed-group");
    GroupDto localGroup = db.users().insertGroup("local-group");
    db.users().insertEntityPermissionOnGroup(managedGroup, ISSUE_ADMIN, portfolio);
    db.users().insertEntityPermissionOnGroup(localGroup, ISSUE_ADMIN, portfolio);
    mockGroupsAsManaged(managedGroup.getUuid());

    loginAsAdmin();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN.getKey())
      .setParam(PARAM_PROJECT_ID, "view-uuid")
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("""
      {
        "paging": {
          "pageIndex": 1,
          "pageSize": 20,
          "total": 2
        },
        "groups": [
          {
            "name": "local-group",
            "managed": false
          },
          {
            "name": "managed-group",
            "managed": true
          }
        ]
      }""");
  }

  @Test
  public void fail_if_not_logged_in() {
    assertThatThrownBy(() -> {
      userSession.anonymous();

      newRequest()
        .setParam(PARAM_PERMISSION, GlobalPermission.SCAN.getKey())
        .execute();
    })
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_if_insufficient_privileges() {
    assertThatThrownBy(() -> {
      userSession.logIn("login");
      newRequest()
        .setParam(PARAM_PERMISSION, GlobalPermission.SCAN.getKey())
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_project_uuid_and_project_key_are_provided() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();

    assertThatThrownBy(() -> {
      loginAsAdmin();
      newRequest()
        .setParam(PARAM_PERMISSION, GlobalPermission.SCAN.getKey())
        .setParam(PARAM_PROJECT_ID, project.uuid())
        .setParam(PARAM_PROJECT_KEY, project.getKey())
        .execute();
    })
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void fail_when_using_branch_uuid() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project);
    GroupDto group = db.users().insertGroup();
    db.users().insertProjectPermissionOnGroup(group, ISSUE_ADMIN, project);
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_PERMISSION, ISSUE_ADMIN.getKey())
        .setParam(PARAM_PROJECT_ID, branch.uuid())
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Entity not found");
  }

  private void mockGroupsAsManaged(String... groupUuids) {
    when(managedInstanceService.getGroupUuidToManaged(any(), any())).thenAnswer(invocation -> {
      Set<?> allGroupUuids = invocation.getArgument(1, Set.class);
      return allGroupUuids.stream()
        .map(groupUuid -> (String) groupUuid)
        .collect(toMap(identity(), userUuid -> Set.of(groupUuids).contains(userUuid)));
    });
  }
}
