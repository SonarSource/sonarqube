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
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupRoleDto;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.security.DefaultGroups.ANYONE;
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
    underTest.groupsCountByComponentIdAndPermission(dbSession, asList(123L, 456L, 789L), context -> result.add((CountByProjectAndPermissionDto) context.getResultObject()));

    assertThat(result).hasSize(3);
    assertThat(result).extracting("permission").containsOnly(ADMIN, USER);
    assertThat(result).extracting("componentId").containsOnly(123L, 456L);
    assertThat(result).extracting("count").containsOnly(4, 1);
  }

  @Test
  public void select_groups_by_query() {
    GroupDto group1 = groupDb.insertGroup(newGroupDto());
    GroupDto group2 = groupDb.insertGroup(newGroupDto());
    GroupDto group3 = groupDb.insertGroup(newGroupDto());
    permissionDb.addGlobalPermissionToGroup(SCAN_EXECUTION, null);

    List<String> groupNames = underTest.selectGroupNamesByPermissionQuery(dbSession, PermissionQuery.builder().build());
    assertThat(groupNames).containsOnly("Anyone", group1.getName(), group2.getName(), group3.getName());
  }

  @Test
  public void select_groups_by_query_is_ordered_by_group_names() {
    groupDb.insertGroup(newGroupDto().setName("Group-2"));
    groupDb.insertGroup(newGroupDto().setName("Group-3"));
    groupDb.insertGroup(newGroupDto().setName("Group-1"));
    permissionDb.addGlobalPermissionToGroup(SCAN_EXECUTION, null);

    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      PermissionQuery.builder().build())).containsExactly("Anyone", "Group-1", "Group-2", "Group-3");
  }

  @Test
  public void count_groups_by_query() {
    GroupDto group1 = groupDb.insertGroup(newGroupDto().setName("Group-1"));
    GroupDto group2 = groupDb.insertGroup(newGroupDto().setName("Group-2"));
    GroupDto group3 = groupDb.insertGroup(newGroupDto().setName("Group-3"));
    permissionDb.addGlobalPermissionToGroup(SCAN_EXECUTION, null);
    permissionDb.addGlobalPermissionToGroup(PROVISIONING, group1.getId());

    assertThat(underTest.countGroupsByPermissionQuery(dbSession,
      PermissionQuery.builder().build())).isEqualTo(4);
    assertThat(underTest.countGroupsByPermissionQuery(dbSession,
      PermissionQuery.builder().setPermission(PROVISIONING).build())).isEqualTo(1);
    assertThat(underTest.countGroupsByPermissionQuery(dbSession,
      PermissionQuery.builder().withPermissionOnly().build())).isEqualTo(2);
    assertThat(underTest.countGroupsByPermissionQuery(dbSession,
      PermissionQuery.builder().setSearchQuery("Group-").build())).isEqualTo(3);
    assertThat(underTest.countGroupsByPermissionQuery(dbSession,
      PermissionQuery.builder().setSearchQuery("Any").build())).isEqualTo(1);
  }

  @Test
  public void select_groups_by_query_with_global_permission() {
    GroupDto group1 = groupDb.insertGroup(newGroupDto().setName("Group-1"));
    GroupDto group2 = groupDb.insertGroup(newGroupDto().setName("Group-2"));
    GroupDto group3 = groupDb.insertGroup(newGroupDto().setName("Group-3"));

    ComponentDto project = componentDb.insertComponent(newProjectDto());

    permissionDb.addGlobalPermissionToGroup(SCAN_EXECUTION, null);
    permissionDb.addGlobalPermissionToGroup(PROVISIONING, null);
    permissionDb.addGlobalPermissionToGroup(SCAN_EXECUTION, group1.getId());
    permissionDb.addGlobalPermissionToGroup(SYSTEM_ADMIN, group3.getId());
    permissionDb.addProjectPermissionToGroup(UserRole.ADMIN, group2.getId(), project.getId());

    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      PermissionQuery.builder().setPermission(SCAN_EXECUTION).build())).containsExactly(ANYONE, "Group-1");

    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      PermissionQuery.builder().setPermission(SYSTEM_ADMIN).build())).containsExactly("Group-3");

    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      PermissionQuery.builder().setPermission(PROVISIONING).build())).containsExactly(ANYONE);
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
    permissionDb.addProjectPermissionToGroup(USER, null, project.getId());

    permissionDb.addProjectPermissionToGroup(SYSTEM_ADMIN, group1.getId(), anotherProject.getId());
    permissionDb.addProjectPermissionToGroup(SYSTEM_ADMIN, null, anotherProject.getId());
    permissionDb.addProjectPermissionToGroup(SCAN_EXECUTION, group3.getId(), anotherProject.getId());

    permissionDb.addGlobalPermissionToGroup(SCAN_EXECUTION, group2.getId());

    PermissionQuery.Builder builderOnComponent = PermissionQuery.builder().setComponentUuid(project.uuid());
    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      builderOnComponent.withPermissionOnly().build())).containsOnlyOnce(group1.getName());
    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      builderOnComponent.setPermission(SCAN_EXECUTION).build())).containsOnlyOnce(group1.getName());
    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      builderOnComponent.setPermission(USER).build())).containsOnlyOnce(ANYONE);
  }

  @Test
  public void select_groups_by_query_paginated() {
    IntStream.rangeClosed(0, 9).forEach(i -> groupDb.insertGroup(newGroupDto().setName(i + "-name")));

    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      PermissionQuery.builder().setPageIndex(2).setPageSize(3).build())).containsExactly("3-name", "4-name", "5-name");
  }

  @Test
  public void select_groups_by_query_with_search_query() {
    GroupDto group = groupDb.insertGroup(newGroupDto().setName("group-anyone"));
    groupDb.insertGroup(newGroupDto().setName("unknown"));
    permissionDb.addGlobalPermissionToGroup(SCAN_EXECUTION, group.getId());

    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      PermissionQuery.builder().setSearchQuery("any").build())).containsOnlyOnce(ANYONE, "group-anyone");
  }

  @Test
  public void select_groups_by_query_does_not_return_anyone_when_group_roles_is_empty() {
    GroupDto group = groupDb.insertGroup();

    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      PermissionQuery.builder().build()))
        .doesNotContain(ANYONE)
        .containsExactly(group.getName());
  }

  @Test
  public void select_group_permissions_by_group_names_on_global_permissions() {
    GroupDto group1 = groupDb.insertGroup(newGroupDto().setName("Group-1"));
    permissionDb.addGlobalPermissionToGroup(SCAN_EXECUTION, group1.getId());

    GroupDto group2 = groupDb.insertGroup(newGroupDto().setName("Group-2"));
    ComponentDto project = componentDb.insertComponent(newProjectDto());
    permissionDb.addProjectPermissionToGroup(UserRole.ADMIN, group2.getId(), project.getId());

    GroupDto group3 = groupDb.insertGroup(newGroupDto().setName("Group-3"));
    permissionDb.addGlobalPermissionToGroup(SYSTEM_ADMIN, group3.getId());

    // Anyone
    permissionDb.addGlobalPermissionToGroup(SCAN_EXECUTION, null);
    permissionDb.addGlobalPermissionToGroup(PROVISIONING, null);

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Group-1"), null))
      .extracting(GroupRoleDto::getGroupId, GroupRoleDto::getRole, GroupRoleDto::getResourceId)
      .containsOnly(tuple(group1.getId(), SCAN_EXECUTION, null));

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Group-2"), null)).isEmpty();

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Group-3"), null))
      .extracting(GroupRoleDto::getGroupId, GroupRoleDto::getRole, GroupRoleDto::getResourceId)
      .containsOnly(tuple(group3.getId(), SYSTEM_ADMIN, null));

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Anyone"), null))
      .extracting(GroupRoleDto::getGroupId, GroupRoleDto::getRole, GroupRoleDto::getResourceId)
      .containsOnly(
        tuple(0L, SCAN_EXECUTION, null),
        tuple(0L, PROVISIONING, null));

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Group-1", "Group-2", "Anyone"), null)).hasSize(3);
    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Unknown"), null)).isEmpty();
    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, Collections.emptyList(), null)).isEmpty();
  }

  @Test
  public void select_group_permissions_by_group_names_on_project_permissions() {
    GroupDto group1 = groupDb.insertGroup(newGroupDto().setName("Group-1"));
    permissionDb.addGlobalPermissionToGroup(PROVISIONING, group1.getId());

    GroupDto group2 = groupDb.insertGroup(newGroupDto().setName("Group-2"));
    ComponentDto project = componentDb.insertComponent(newProjectDto());
    permissionDb.addProjectPermissionToGroup(USER, group2.getId(), project.getId());

    GroupDto group3 = groupDb.insertGroup(newGroupDto().setName("Group-3"));
    permissionDb.addProjectPermissionToGroup(USER, group3.getId(), project.getId());

    // Anyone group
    permissionDb.addGlobalPermissionToGroup(SCAN_EXECUTION, null);
    permissionDb.addProjectPermissionToGroup(PROVISIONING, null, project.getId());

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Group-1"), project.getId())).isEmpty();

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Group-2"), project.getId()))
      .extracting(GroupRoleDto::getGroupId, GroupRoleDto::getRole, GroupRoleDto::getResourceId)
      .containsOnly(tuple(group2.getId(), USER, project.getId()));

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Group-3"), project.getId()))
      .extracting(GroupRoleDto::getGroupId, GroupRoleDto::getRole, GroupRoleDto::getResourceId)
      .containsOnly(tuple(group3.getId(), USER, project.getId()));

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Anyone"), project.getId()))
      .extracting(GroupRoleDto::getGroupId, GroupRoleDto::getRole, GroupRoleDto::getResourceId)
      .containsOnly(tuple(0L, PROVISIONING, project.getId()));

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Group-1", "Group-2", "Anyone"), project.getId())).hasSize(2);
    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Unknown"), project.getId())).isEmpty();
    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Group-1"), 123L)).isEmpty();
    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, Collections.emptyList(), project.getId())).isEmpty();
  }

}
