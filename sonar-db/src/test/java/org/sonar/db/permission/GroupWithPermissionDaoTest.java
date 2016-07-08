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
package org.sonar.db.permission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupRoleDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.user.GroupTesting.newGroupDto;

public class GroupWithPermissionDaoTest {

  private static final long COMPONENT_ID = 100L;

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  GroupDbTester groupDb = new GroupDbTester(db);
  PermissionDbTester permissionDb = new PermissionDbTester(db);
  ComponentDbTester componentDb = new ComponentDbTester(db);
  DbSession dbSession = db.getSession();

  PermissionDao underTest = new PermissionDao(db.myBatis());

  @Test
  public void select_groups_for_project_permission() {
    db.prepareDbUnit(getClass(), "groups_with_permissions.xml");

    OldPermissionQuery query = OldPermissionQuery.builder().permission("user").build();
    List<GroupWithPermissionDto> result = underTest.selectGroups(dbSession, query, COMPONENT_ID);
    assertThat(result).hasSize(4);

    GroupWithPermissionDto anyone = result.get(0);
    assertThat(anyone.getName()).isEqualTo("Anyone");
    assertThat(anyone.getDescription()).isNull();
    assertThat(anyone.getPermission()).isNotNull();

    GroupWithPermissionDto group1 = result.get(1);
    assertThat(group1.getName()).isEqualTo("sonar-administrators");
    assertThat(group1.getDescription()).isEqualTo("System administrators");
    assertThat(group1.getPermission()).isNotNull();

    GroupWithPermissionDto group2 = result.get(2);
    assertThat(group2.getName()).isEqualTo("sonar-reviewers");
    assertThat(group2.getDescription()).isEqualTo("Reviewers");
    assertThat(group2.getPermission()).isNull();

    GroupWithPermissionDto group3 = result.get(3);
    assertThat(group3.getName()).isEqualTo("sonar-users");
    assertThat(group3.getDescription()).isEqualTo("Any new users created will automatically join this group");
    assertThat(group3.getPermission()).isNotNull();
  }

  @Test
  public void anyone_group_is_not_returned_when_it_has_no_permission() {
    db.prepareDbUnit(getClass(), "groups_with_permissions.xml");

    // Anyone group has not the permission 'admin', so it's not returned
    OldPermissionQuery query = OldPermissionQuery.builder().permission("admin").build();
    List<GroupWithPermissionDto> result = underTest.selectGroups(dbSession, query, COMPONENT_ID);
    assertThat(result).hasSize(3);

    GroupWithPermissionDto group1 = result.get(0);
    assertThat(group1.getName()).isEqualTo("sonar-administrators");
    assertThat(group1.getPermission()).isNotNull();

    GroupWithPermissionDto group2 = result.get(1);
    assertThat(group2.getName()).isEqualTo("sonar-reviewers");
    assertThat(group2.getPermission()).isNull();

    GroupWithPermissionDto group3 = result.get(2);
    assertThat(group3.getName()).isEqualTo("sonar-users");
    assertThat(group3.getPermission()).isNull();
  }

  @Test
  public void select_groups_for_global_permission() {
    db.prepareDbUnit(getClass(), "groups_with_permissions.xml");

    OldPermissionQuery query = OldPermissionQuery.builder().permission("admin").build();
    List<GroupWithPermissionDto> result = underTest.selectGroups(dbSession, query, null);
    assertThat(result).hasSize(3);

    GroupWithPermissionDto group1 = result.get(0);
    assertThat(group1.getName()).isEqualTo("sonar-administrators");
    assertThat(group1.getPermission()).isNotNull();

    GroupWithPermissionDto group2 = result.get(1);
    assertThat(group2.getName()).isEqualTo("sonar-reviewers");
    assertThat(group2.getPermission()).isNull();

    GroupWithPermissionDto group3 = result.get(2);
    assertThat(group3.getName()).isEqualTo("sonar-users");
    assertThat(group3.getPermission()).isNull();
  }

  @Test
  public void search_by_groups_name() {
    db.prepareDbUnit(getClass(), "groups_with_permissions.xml");

    List<GroupWithPermissionDto> result = underTest.selectGroups(dbSession, OldPermissionQuery.builder().permission("user").search("aDMini").build(), COMPONENT_ID);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("sonar-administrators");

    result = underTest.selectGroups(dbSession, OldPermissionQuery.builder().permission("user").search("sonar").build(), COMPONENT_ID);
    assertThat(result).hasSize(3);
  }

  @Test
  public void search_groups_should_be_sorted_by_group_name() {
    db.prepareDbUnit(getClass(), "groups_with_permissions_should_be_sorted_by_group_name.xml");

    List<GroupWithPermissionDto> result = underTest.selectGroups(dbSession, OldPermissionQuery.builder().permission("user").build(), COMPONENT_ID);
    int count = underTest.countGroups(dbSession, "user", COMPONENT_ID);

    assertThat(result).hasSize(4);
    assertThat(count).isEqualTo(2);
    assertThat(result.get(0).getName()).isEqualTo("Anyone");
    assertThat(result.get(1).getName()).isEqualTo("sonar-administrators");
    assertThat(result.get(2).getName()).isEqualTo("sonar-reviewers");
    assertThat(result.get(3).getName()).isEqualTo("sonar-users");
  }

  @Test
  public void group_count_by_permission_and_component_id() {
    GroupDto group1 = groupDb.insertGroup();
    GroupDto group2 = groupDb.insertGroup();
    GroupDto group3 = groupDb.insertGroup();

    permissionDb.addProjectPermissionToGroup(ISSUE_ADMIN, group1.getId(), 42L);
    permissionDb.addProjectPermissionToGroup(ADMIN, group1.getId(), 123L);
    permissionDb.addProjectPermissionToGroup(ADMIN, group2.getId(), 123L);
    permissionDb.addProjectPermissionToGroup(ADMIN, group3.getId(), 123L);
    // anyone group
    permissionDb.addProjectPermissionToGroup(ADMIN, null, 123L);
    permissionDb.addProjectPermissionToGroup(USER, group1.getId(), 123L);
    permissionDb.addProjectPermissionToGroup(USER, group1.getId(), 456L);

    final List<CountByProjectAndPermissionDto> result = new ArrayList<>();
    underTest.groupsCountByComponentIdAndPermission(dbSession, Arrays.asList(123L, 456L, 789L), context -> result.add((CountByProjectAndPermissionDto) context.getResultObject()));

    assertThat(result).hasSize(3);
    assertThat(result).extracting("permission").containsOnly(ADMIN, USER);
    assertThat(result).extracting("componentId").containsOnly(123L, 456L);
    assertThat(result).extracting("count").containsOnly(4, 1);
  }

  @Test
  public void select_groups_by_query_ordered_by_group_names() {
    GroupDto group2 = groupDb.insertGroup(newGroupDto().setName("Group-2"));
    GroupDto group3 = groupDb.insertGroup(newGroupDto().setName("Group-3"));
    GroupDto group1 = groupDb.insertGroup(newGroupDto().setName("Group-1"));

    permissionDb.addGlobalPermissionToGroup(SCAN_EXECUTION, null);

    PermissionQuery.Builder dbQuery = PermissionQuery.builder();
    List<String> groupNames = selectGroupNames(dbQuery);
    int countNames = countGroupNames(dbQuery);
    List<GroupRoleDto> permissions = selectGroupPermissions(dbQuery);

    assertThat(groupNames).containsExactly("Anyone", "Group-1", "Group-2", "Group-3");
    assertThat(countNames).isEqualTo(4);
    assertThat(permissions).hasSize(4)
      .extracting(GroupRoleDto::getGroupId)
      .containsOnlyOnce(0L, group1.getId(), group2.getId(), group3.getId());
  }

  @Test
  public void select_groups_by_query_with_global_permission() {
    GroupDto group1 = groupDb.insertGroup(newGroupDto().setName("Group-1"));
    GroupDto group2 = groupDb.insertGroup(newGroupDto().setName("Group-2"));
    GroupDto group3 = groupDb.insertGroup(newGroupDto().setName("Group-3"));

    ComponentDto project = componentDb.insertComponent(newProjectDto());

    permissionDb.addGlobalPermissionToGroup(SCAN_EXECUTION, group1.getId());
    permissionDb.addGlobalPermissionToGroup(SCAN_EXECUTION, null);
    permissionDb.addGlobalPermissionToGroup(PROVISIONING, null);
    permissionDb.addProjectPermissionToGroup(UserRole.ADMIN, group2.getId(), project.getId());
    permissionDb.addGlobalPermissionToGroup(GlobalPermissions.SYSTEM_ADMIN, group3.getId());

    PermissionQuery.Builder dbQuery = PermissionQuery.builder()
      .withPermissionOnly()
      .setPermission(SCAN_EXECUTION);
    List<String> groupNames = selectGroupNames(dbQuery);
    int countNames = countGroupNames(dbQuery);
    List<GroupRoleDto> permissions = selectGroupPermissions(dbQuery);

    assertThat(groupNames).containsExactly(DefaultGroups.ANYONE, "Group-1");
    assertThat(countNames).isEqualTo(2);
    assertThat(permissions)
      .extracting(GroupRoleDto::getRole, GroupRoleDto::getGroupId, GroupRoleDto::getResourceId)
      .containsOnlyOnce(
        tuple(SCAN_EXECUTION, 0L, null),
        tuple(SCAN_EXECUTION, group1.getId(), null));
  }

  @Test
  public void select_groups_by_query_with_project_permissions() {
    GroupDto group1 = groupDb.insertGroup();
    GroupDto group2 = groupDb.insertGroup();
    GroupDto group3 = groupDb.insertGroup();

    ComponentDto project = componentDb.insertComponent(newProjectDto());
    ComponentDto anotherProject = componentDb.insertComponent(newProjectDto());

    permissionDb.addProjectPermissionToGroup(SCAN_EXECUTION, group1.getId(), project.getId());
    permissionDb.addProjectPermissionToGroup(PROVISIONING, group1.getId(), project.getId());
    permissionDb.addProjectPermissionToGroup(SYSTEM_ADMIN, group1.getId(), anotherProject.getId());
    permissionDb.addProjectPermissionToGroup(SYSTEM_ADMIN, null, anotherProject.getId());
    permissionDb.addGlobalPermissionToGroup(SCAN_EXECUTION, group2.getId());
    permissionDb.addProjectPermissionToGroup(SCAN_EXECUTION, group3.getId(), anotherProject.getId());

    PermissionQuery.Builder dbQuery = PermissionQuery.builder()
      .setComponentUuid(project.uuid())
      .withPermissionOnly();
    List<String> groupNames = selectGroupNames(dbQuery);
    int countNames = countGroupNames(dbQuery);
    List<GroupRoleDto> permissions = selectGroupPermissions(dbQuery);

    assertThat(groupNames).containsOnlyOnce(group1.getName());
    assertThat(countNames).isEqualTo(1);
    assertThat(permissions).hasSize(2)
      .extracting(GroupRoleDto::getRole, GroupRoleDto::getGroupId, GroupRoleDto::getResourceId)
      .containsOnlyOnce(
        tuple(SCAN_EXECUTION, group1.getId(), project.getId()),
        tuple(PROVISIONING, group1.getId(), project.getId()));
  }

  @Test
  public void select_groups_by_query_in_group_names() {
    groupDb.insertGroup(newGroupDto().setName("group-name-1"));
    groupDb.insertGroup(newGroupDto().setName("group-name-2"));
    groupDb.insertGroup(newGroupDto().setName("another-group-name"));

    PermissionQuery.Builder dbQuery = PermissionQuery.builder().setGroupNames(newArrayList("group-name-1", "group-name-2"));
    List<String> groupNames = selectGroupNames(dbQuery);
    int countNames = countGroupNames(dbQuery);
    List<GroupRoleDto> permissions = selectGroupPermissions(dbQuery);

    assertThat(groupNames).containsOnlyOnce("group-name-1", "group-name-2");
    assertThat(countNames).isEqualTo(2);
    assertThat(permissions).hasSize(2);
  }

  @Test
  public void select_groups_by_query_paginated() {
    IntStream.rangeClosed(0, 9).forEach(i -> groupDb.insertGroup(newGroupDto().setName(i + "-name")));

    PermissionQuery.Builder dbQuery = PermissionQuery.builder().setPageIndex(2).setPageSize(3);
    List<String> groupNames = selectGroupNames(dbQuery);
    int countNames = countGroupNames(dbQuery);

    assertThat(groupNames).containsExactly("3-name", "4-name", "5-name");
    assertThat(countNames).isEqualTo(10);
  }

  @Test
  public void select_groups_by_query_with_search_query() {
    GroupDto group = groupDb.insertGroup(newGroupDto().setName("group-anyone"));
    groupDb.insertGroup(newGroupDto().setName("unknown"));

    permissionDb.addGlobalPermissionToGroup(SCAN_EXECUTION, group.getId());

    PermissionQuery.Builder dbQuery = PermissionQuery.builder().setSearchQuery("any");
    List<String> groupNames = selectGroupNames(dbQuery);
    int countNames = countGroupNames(dbQuery);

    assertThat(groupNames).containsOnlyOnce(DefaultGroups.ANYONE, "group-anyone");
    assertThat(countNames).isEqualTo(2);
  }

  @Test
  public void select_groups_by_query_does_not_return_anyone_when_group_roles_is_empty() {
    GroupDto group = groupDb.insertGroup();

    PermissionQuery.Builder dbQuery = PermissionQuery.builder();
    List<String> groupNames = selectGroupNames(dbQuery);
    int countNames = countGroupNames(dbQuery);

    assertThat(groupNames)
      .doesNotContain(DefaultGroups.ANYONE)
      .containsExactly(group.getName());
    assertThat(countNames).isEqualTo(1);
  }

  private List<String> selectGroupNames(PermissionQuery.Builder dbQuery) {
    return underTest.selectGroupNamesByPermissionQuery(dbSession, dbQuery.build());
  }

  private int countGroupNames(PermissionQuery.Builder dbQuery) {
    return underTest.countGroupsByPermissionQuery(dbSession, dbQuery.build());
  }

  private List<GroupRoleDto> selectGroupPermissions(PermissionQuery.Builder dbQuery) {
    return underTest.selectGroupPermissionsByQuery(dbSession, dbQuery.build());
  }
}
