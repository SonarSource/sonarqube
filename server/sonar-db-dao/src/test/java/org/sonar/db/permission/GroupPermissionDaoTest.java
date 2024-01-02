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
package org.sonar.db.permission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.security.DefaultGroups.ANYONE;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.db.permission.PermissionQuery.DEFAULT_PAGE_SIZE;

public class GroupPermissionDaoTest {

  private static final String ANYONE_UUID = "Anyone";
  private static final String MISSING_UUID = "-1";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = db.getSession();
  private final GroupPermissionDao underTest = new GroupPermissionDao(new NoOpAuditPersister());

  @Test
  public void group_count_by_permission_and_component_id_on_private_projects() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto project3 = db.components().insertPrivateProject();

    db.users().insertProjectPermissionOnGroup(group1, ISSUE_ADMIN, project1);
    db.users().insertProjectPermissionOnGroup(group1, ADMIN, project2);
    db.users().insertProjectPermissionOnGroup(group2, ADMIN, project2);
    db.users().insertProjectPermissionOnGroup(group3, ADMIN, project2);
    db.users().insertProjectPermissionOnGroup(group1, USER, project2);
    db.users().insertProjectPermissionOnGroup(group1, USER, project3);

    final List<CountPerProjectPermission> result = new ArrayList<>();
    underTest.groupsCountByComponentUuidAndPermission(dbSession, asList(project2.uuid(), project3.uuid(), "789"),
      context -> result.add(context.getResultObject()));

    assertThat(result).hasSize(3);
    assertThat(result).extracting("permission").containsOnly(ADMIN, USER);
    assertThat(result).extracting("componentUuid").containsOnly(project2.uuid(), project3.uuid());
    assertThat(result).extracting("count").containsOnly(3, 1);
  }

  @Test
  public void group_count_by_permission_and_component_id_on_public_projects() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();
    ComponentDto project1 = db.components().insertPublicProject();
    ComponentDto project2 = db.components().insertPublicProject();
    ComponentDto project3 = db.components().insertPublicProject();

    db.users().insertProjectPermissionOnGroup(group1, "p1", project1);
    db.users().insertProjectPermissionOnGroup(group1, "p2", project2);
    db.users().insertProjectPermissionOnGroup(group2, "p2", project2);
    db.users().insertProjectPermissionOnGroup(group3, "p2", project2);
    // anyone group
    db.users().insertProjectPermissionOnAnyone("p2", project2);
    db.users().insertProjectPermissionOnGroup(group1, "p3", project2);
    db.users().insertProjectPermissionOnGroup(group1, "p3", project3);

    final List<CountPerProjectPermission> result = new ArrayList<>();
    underTest.groupsCountByComponentUuidAndPermission(dbSession, asList(project2.uuid(), project3.uuid(), "789"),
      context -> result.add(context.getResultObject()));

    assertThat(result).hasSize(3);
    assertThat(result).extracting("permission").containsOnly("p2", "p3");
    assertThat(result).extracting("componentUuid").containsOnly(project2.uuid(), project3.uuid());
    assertThat(result).extracting("count").containsOnly(4, 1);
  }

  @Test
  public void selectGroupNamesByQuery_is_ordered_by_permissions_then_by_group_names() {
    GroupDto group2 = db.users().insertGroup("Group-2");
    GroupDto group3 = db.users().insertGroup("Group-3");
    GroupDto group1 = db.users().insertGroup("Group-1");
    db.users().insertPermissionOnAnyone(SCAN);
    db.users().insertPermissionOnGroup(group3, SCAN);

    assertThat(underTest.selectGroupNamesByQuery(dbSession, newQuery().build()))
      .containsExactly(ANYONE, group3.getName(), group1.getName(), group2.getName());
  }

  @Test
  public void selectGroupNamesByQuery_is_ordered_by_permissions_then_by_group_when_many_groups_for_global_permissions() {
    ComponentDto project = db.components().insertPrivateProject();
    IntStream.rangeClosed(1, DEFAULT_PAGE_SIZE + 1).forEach(i -> {
      GroupDto group = db.users().insertGroup("Group-" + i);
      // Add permission on project to be sure projects are excluded
      db.users().insertProjectPermissionOnGroup(group, SCAN.getKey(), project);
    });
    String lastGroupName = "Group-" + (DEFAULT_PAGE_SIZE + 1);
    db.users().insertPermissionOnGroup(db.users().selectGroup(lastGroupName).get(), SCAN);

    assertThat(underTest.selectGroupNamesByQuery(dbSession, newQuery().build()))
      .hasSize(DEFAULT_PAGE_SIZE)
      .startsWith(ANYONE, lastGroupName, "Group-1");
  }

  @Test
  public void selectGroupNamesByQuery_is_ordered_by_global_permissions_then_by_group_when_many_groups_for_project_permissions() {
    IntStream.rangeClosed(1, DEFAULT_PAGE_SIZE + 1).forEach(i -> {
      GroupDto group = db.users().insertGroup("Group-" + i);
      // Add global permission to be sure they are excluded
      db.users().insertPermissionOnGroup(group, SCAN.getKey());
    });
    ComponentDto project = db.components().insertPrivateProject();
    String lastGroupName = "Group-" + (DEFAULT_PAGE_SIZE + 1);
    db.users().insertProjectPermissionOnGroup(db.users().selectGroup(lastGroupName).get(), SCAN.getKey(), project);

    assertThat(underTest.selectGroupNamesByQuery(dbSession, newQuery()
      .setComponent(project)
      .build()))
      .hasSize(DEFAULT_PAGE_SIZE)
      .startsWith(ANYONE, lastGroupName, "Group-1");
  }

  @Test
  public void countGroupsByQuery() {
    GroupDto group1 = db.users().insertGroup("Group-1");
    db.users().insertGroup("Group-2");
    db.users().insertGroup("Group-3");
    db.users().insertPermissionOnAnyone(SCAN);
    db.users().insertPermissionOnGroup(group1, PROVISION_PROJECTS);

    assertThat(underTest.countGroupsByQuery(dbSession,
      newQuery().build())).isEqualTo(4);
    assertThat(underTest.countGroupsByQuery(dbSession,
      newQuery().setPermission(PROVISION_PROJECTS.getKey()).build())).isOne();
    assertThat(underTest.countGroupsByQuery(dbSession,
      newQuery().withAtLeastOnePermission().build())).isEqualTo(2);
    assertThat(underTest.countGroupsByQuery(dbSession,
      newQuery().setSearchQuery("Group-").build())).isEqualTo(3);
    assertThat(underTest.countGroupsByQuery(dbSession,
      newQuery().setSearchQuery("Any").build())).isOne();
  }

  @Test
  public void selectGroupNamesByQuery_with_global_permission() {
    GroupDto group1 = db.users().insertGroup("Group-1");
    GroupDto group2 = db.users().insertGroup("Group-2");
    GroupDto group3 = db.users().insertGroup("Group-3");

    ComponentDto project = db.components().insertPrivateProject();

    db.users().insertPermissionOnAnyone(SCAN);
    db.users().insertPermissionOnAnyone(PROVISION_PROJECTS);
    db.users().insertPermissionOnGroup(group1, SCAN);
    db.users().insertPermissionOnGroup(group3, ADMINISTER);
    db.users().insertProjectPermissionOnGroup(group2, UserRole.ADMIN, project);

    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      newQuery().setPermission(SCAN.getKey()).build())).containsExactly(ANYONE, group1.getName());

    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      newQuery().setPermission(ADMINISTER.getKey()).build())).containsExactly(group3.getName());

    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      newQuery().setPermission(PROVISION_PROJECTS.getKey()).build())).containsExactly(ANYONE);
  }

  @Test
  public void select_groups_by_query_with_project_permissions_on_public_projects() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();

    ComponentDto project = db.components().insertPublicProject();
    ComponentDto anotherProject = db.components().insertPublicProject();

    db.users().insertProjectPermissionOnGroup(group1, "p1", project);
    db.users().insertProjectPermissionOnGroup(group1, "p2", project);
    db.users().insertProjectPermissionOnAnyone("p3", project);

    db.users().insertProjectPermissionOnGroup(group1, "p4", anotherProject);
    db.users().insertProjectPermissionOnAnyone("p4", anotherProject);
    db.users().insertProjectPermissionOnGroup(group3, "p1", anotherProject);
    db.users().insertPermissionOnGroup(group2, "p5");

    PermissionQuery.Builder builderOnComponent = newQuery()
      .setComponent(project);
    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      builderOnComponent.withAtLeastOnePermission().build())).containsOnlyOnce(group1.getName());
    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      builderOnComponent.setPermission("p1").build())).containsOnlyOnce(group1.getName());
    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      builderOnComponent.setPermission("p3").build())).containsOnlyOnce(ANYONE);
  }

  @Test
  public void select_groups_by_query_with_project_permissions_on_private_projects() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();

    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto anotherProject = db.components().insertPrivateProject();

    db.users().insertProjectPermissionOnGroup(group1, SCAN.getKey(), project);
    db.users().insertProjectPermissionOnGroup(group1, PROVISION_PROJECTS.getKey(), project);

    db.users().insertProjectPermissionOnGroup(group1, ADMIN, anotherProject);
    db.users().insertProjectPermissionOnGroup(group3, UserRole.SCAN, anotherProject);
    db.users().insertPermissionOnGroup(group2, SCAN);

    PermissionQuery.Builder builderOnComponent = newQuery()
      .setComponent(project);
    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      builderOnComponent.withAtLeastOnePermission().build())).containsOnlyOnce(group1.getName());
    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      builderOnComponent.setPermission(SCAN_EXECUTION).build())).containsOnlyOnce(group1.getName());
    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      builderOnComponent.setPermission(USER).build())).isEmpty();
  }

  @Test
  public void selectGroupNamesByQuery_is_paginated() {
    IntStream.rangeClosed(0, 9).forEach(i -> db.users().insertGroup(i + "-name"));

    List<String> groupNames = underTest.selectGroupNamesByQuery(dbSession,
      newQuery().setPageIndex(2).setPageSize(3).build());
    assertThat(groupNames).containsExactly("3-name", "4-name", "5-name");
  }

  @Test
  public void selectGroupNamesByQuery_with_search_query() {
    GroupDto group = db.users().insertGroup("group-anyone");
    db.users().insertGroup("unknown");
    db.users().insertPermissionOnGroup(group, SCAN);

    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      newQuery().setSearchQuery("any").build())).containsOnlyOnce(ANYONE, group.getName());
  }

  @Test
  public void selectGroupNamesByQuery_does_not_return_anyone_when_group_roles_is_empty() {
    GroupDto group = db.users().insertGroup();

    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      newQuery().build()))
      .doesNotContain(ANYONE)
      .containsExactly(group.getName());
  }

  @Test
  public void selectByGroupUuids_on_global_permissions() {
    GroupDto group1 = db.users().insertGroup("Group-1");
    db.users().insertPermissionOnGroup(group1, SCAN);

    GroupDto group2 = db.users().insertGroup("Group-2");
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnGroup(group2, UserRole.ADMIN, project);

    GroupDto group3 = db.users().insertGroup("Group-3");
    db.users().insertPermissionOnGroup(group3, ADMINISTER);

    // Anyone
    db.users().insertPermissionOnAnyone(SCAN);
    db.users().insertPermissionOnAnyone(PROVISION_PROJECTS);

    assertThat(underTest.selectByGroupUuids(dbSession, asList(group1.getUuid()), null))
      .extracting(GroupPermissionDto::getGroupUuid, GroupPermissionDto::getRole, GroupPermissionDto::getComponentUuid)
      .containsOnly(tuple(group1.getUuid(), SCAN_EXECUTION, null));

    assertThat(underTest.selectByGroupUuids(dbSession, asList(group2.getUuid()), null)).isEmpty();

    assertThat(underTest.selectByGroupUuids(dbSession, asList(group3.getUuid()), null))
      .extracting(GroupPermissionDto::getGroupUuid, GroupPermissionDto::getRole, GroupPermissionDto::getComponentUuid)
      .containsOnly(tuple(group3.getUuid(), ADMINISTER.getKey(), null));

    assertThat(underTest.selectByGroupUuids(dbSession, asList(ANYONE_UUID), null))
      .extracting(GroupPermissionDto::getGroupUuid, GroupPermissionDto::getRole, GroupPermissionDto::getComponentUuid)
      .containsOnly(
        tuple(ANYONE_UUID, SCAN.getKey(), null),
        tuple(ANYONE_UUID, PROVISION_PROJECTS.getKey(), null));

    assertThat(underTest.selectByGroupUuids(dbSession, asList(group1.getUuid(), group2.getUuid(), ANYONE_UUID), null)).hasSize(3);
    assertThat(underTest.selectByGroupUuids(dbSession, asList(MISSING_UUID), null)).isEmpty();
    assertThat(underTest.selectByGroupUuids(dbSession, Collections.emptyList(), null)).isEmpty();
  }

  @Test
  public void selectByGroupUuids_on_public_projects() {
    GroupDto group1 = db.users().insertGroup("Group-1");
    db.users().insertPermissionOnGroup(group1, "p1");

    GroupDto group2 = db.users().insertGroup("Group-2");
    ComponentDto project = db.components().insertPublicProject();
    db.users().insertProjectPermissionOnGroup(group2, "p2", project);

    GroupDto group3 = db.users().insertGroup("Group-3");
    db.users().insertProjectPermissionOnGroup(group3, "p2", project);

    // Anyone group
    db.users().insertPermissionOnAnyone("p3");
    db.users().insertProjectPermissionOnAnyone("p4", project);

    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(group1.getUuid()), project.uuid())).isEmpty();

    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(group2.getUuid()), project.uuid()))
      .extracting(GroupPermissionDto::getGroupUuid, GroupPermissionDto::getRole, GroupPermissionDto::getComponentUuid)
      .containsOnly(tuple(group2.getUuid(), "p2", project.uuid()));

    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(group3.getUuid()), project.uuid()))
      .extracting(GroupPermissionDto::getGroupUuid, GroupPermissionDto::getRole, GroupPermissionDto::getComponentUuid)
      .containsOnly(tuple(group3.getUuid(), "p2", project.uuid()));

    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(ANYONE_UUID), project.uuid()))
      .extracting(GroupPermissionDto::getGroupUuid, GroupPermissionDto::getRole, GroupPermissionDto::getComponentUuid)
      .containsOnly(tuple(ANYONE_UUID, "p4", project.uuid()));

    assertThat(underTest.selectByGroupUuids(dbSession, asList(group1.getUuid(), group2.getUuid(), ANYONE_UUID), project.uuid())).hasSize(2);
    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(MISSING_UUID), project.uuid())).isEmpty();
    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(group1.getUuid()), "123")).isEmpty();
    assertThat(underTest.selectByGroupUuids(dbSession, Collections.emptyList(), project.uuid())).isEmpty();
  }

  @Test
  public void selectByGroupUuids_on_private_projects() {
    GroupDto group1 = db.users().insertGroup("Group-1");
    db.users().insertPermissionOnGroup(group1, PROVISION_PROJECTS);

    GroupDto group2 = db.users().insertGroup("Group-2");
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnGroup(group2, USER, project);

    GroupDto group3 = db.users().insertGroup("Group-3");
    db.users().insertProjectPermissionOnGroup(group3, USER, project);

    // Anyone group
    db.users().insertPermissionOnAnyone(SCAN);

    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(group1.getUuid()), project.uuid())).isEmpty();

    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(group2.getUuid()), project.uuid()))
      .extracting(GroupPermissionDto::getGroupUuid, GroupPermissionDto::getRole, GroupPermissionDto::getComponentUuid)
      .containsOnly(tuple(group2.getUuid(), USER, project.uuid()));

    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(group3.getUuid()), project.uuid()))
      .extracting(GroupPermissionDto::getGroupUuid, GroupPermissionDto::getRole, GroupPermissionDto::getComponentUuid)
      .containsOnly(tuple(group3.getUuid(), USER, project.uuid()));

    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(ANYONE_UUID), project.uuid()))
      .isEmpty();

    assertThat(underTest.selectByGroupUuids(dbSession, asList(group1.getUuid(), group2.getUuid(), ANYONE_UUID), project.uuid())).hasSize(1);
    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(MISSING_UUID), project.uuid())).isEmpty();
    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(group1.getUuid()), "123")).isEmpty();
    assertThat(underTest.selectByGroupUuids(dbSession, Collections.emptyList(), project.uuid())).isEmpty();
  }

  @Test
  public void selectGlobalPermissionsOfGroup() {
    GroupDto group1 = db.users().insertGroup("group1");
    GroupDto group2 = db.users().insertGroup("group2");
    ComponentDto project = db.components().insertPublicProject();

    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertPermissionOnGroup(group1, "perm3");
    db.users().insertPermissionOnGroup(group2, "perm4");
    db.users().insertProjectPermissionOnGroup(group1, "perm5", project);
    db.users().insertProjectPermissionOnAnyone("perm6", project);

    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group1.getUuid())).containsOnly("perm2", "perm3");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group2.getUuid())).containsOnly("perm4");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, null)).containsOnly("perm1");
  }

  @Test
  public void selectProjectPermissionsOfGroup_on_public_project() {
    GroupDto group1 = db.users().insertGroup("group1");
    ComponentDto project1 = db.components().insertPublicProject();
    ComponentDto project2 = db.components().insertPublicProject();

    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm4", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm5", project2);
    db.users().insertProjectPermissionOnAnyone("perm6", project1);

    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, group1.getUuid(), project1.uuid()))
      .containsOnly("perm3", "perm4");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, group1.getUuid(), project2.uuid()))
      .containsOnly("perm5");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, null, project1.uuid()))
      .containsOnly("perm6");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, null, project2.uuid()))
      .isEmpty();
  }

  @Test
  public void selectProjectKeysWithAnyonePermissions_on_public_project_none_found() {
    ComponentDto project1 = db.components().insertPublicProject();
    ComponentDto project2 = db.components().insertPublicProject();
    GroupDto group = db.users().insertGroup();
    db.users().insertProjectPermissionOnGroup(group, "perm1", project1);
    db.users().insertProjectPermissionOnGroup(group, "perm1", project2);
    assertThat(underTest.selectProjectKeysWithAnyonePermissions(dbSession, 3)).isEmpty();
  }

  @Test
  public void selectProjectKeysWithAnyonePermissions_on_public_project_ordered_by_kee() {
    ComponentDto project1 = db.components().insertPublicProject();
    ComponentDto project2 = db.components().insertPublicProject();
    ComponentDto project3 = db.components().insertPublicProject();
    db.users().insertProjectPermissionOnAnyone("perm1", project1);
    db.users().insertProjectPermissionOnAnyone("perm1", project2);
    db.users().insertProjectPermissionOnAnyone("perm1", project3);
    assertThat(underTest.selectProjectKeysWithAnyonePermissions(dbSession, 3))
      .containsExactly(project1.getKey(), project2.getKey(), project3.getKey());
  }

  @Test
  public void selectProjectKeysWithAnyonePermissions_on_public_project_ordered_by_kee_max_5() {
    IntStream.rangeClosed(1, 9).forEach(i -> {
      ComponentDto project = db.components().insertPublicProject(p -> p.setKey("key-" + i));
      db.users().insertProjectPermissionOnAnyone("perm-" + i, project);
    });

    assertThat(underTest.selectProjectKeysWithAnyonePermissions(dbSession, 5))
      .containsExactly("key-1", "key-2", "key-3", "key-4", "key-5");
  }

  @Test
  public void selectProjectKeysWithAnyonePermissions_on_public_projects_omit_blanket_anyone_group_permissions() {
    // Although saved in the same table (group_roles), this should not be included in the result as not assigned to single project.
    db.users().insertPermissionOnAnyone("perm-anyone");

    IntStream.rangeClosed(1, 9).forEach(i -> {
      ComponentDto project = db.components().insertPublicProject(p -> p.setKey("key-" + i));
      db.users().insertProjectPermissionOnAnyone("perm-" + i, project);
    });

    assertThat(underTest.selectProjectKeysWithAnyonePermissions(dbSession, 5))
      .containsExactly("key-1", "key-2", "key-3", "key-4", "key-5");
  }

  @Test
  public void countProjectsWithAnyonePermissions() {
    GroupDto group = db.users().insertGroup();
    IntStream.rangeClosed(1, 5).forEach(i -> {
      ComponentDto project = db.components().insertPublicProject(p -> p.setKey("key-" + i));
      db.users().insertProjectPermissionOnAnyone("perm-" + i, project);
      db.users().insertProjectPermissionOnGroup(group, "perm-", project);
    });

    assertThat(underTest.countProjectsWithAnyonePermissions(dbSession)).isEqualTo(5);
  }

  @Test
  public void selectProjectPermissionsOfGroup_on_private_project() {
    GroupDto group1 = db.users().insertGroup("group1");
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();

    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm4", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm5", project2);

    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, group1.getUuid(), project1.uuid()))
      .containsOnly("perm3", "perm4");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, group1.getUuid(), project2.uuid()))
      .containsOnly("perm5");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, null, project1.uuid()))
      .isEmpty();
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, null, project2.uuid()))
      .isEmpty();
  }

  @Test
  public void selectAllPermissionsByGroupUuid_on_public_project() {
    GroupDto group1 = db.users().insertGroup("group1");
    ComponentDto project1 = db.components().insertPublicProject();
    ComponentDto project2 = db.components().insertPublicProject();
    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm4", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm5", project2);
    db.users().insertProjectPermissionOnAnyone("perm6", project1);

    List<GroupPermissionDto> result = new ArrayList<>();
    underTest.selectAllPermissionsByGroupUuid(dbSession, group1.getUuid(), context -> result.add(context.getResultObject()));
    assertThat(result).extracting(GroupPermissionDto::getComponentUuid, GroupPermissionDto::getRole).containsOnly(
      tuple(null, "perm2"),
      tuple(project1.uuid(), "perm3"), tuple(project1.uuid(), "perm4"), tuple(project2.uuid(), "perm5"));
  }

  @Test
  public void selectAllPermissionsByGroupUuid_on_private_project() {
    GroupDto group1 = db.users().insertGroup("group1");
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm4", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm5", project2);

    List<GroupPermissionDto> result = new ArrayList<>();
    underTest.selectAllPermissionsByGroupUuid(dbSession, group1.getUuid(), context -> result.add(context.getResultObject()));
    assertThat(result).extracting(GroupPermissionDto::getComponentUuid, GroupPermissionDto::getRole).containsOnly(
      tuple(null, "perm2"),
      tuple(project1.uuid(), "perm3"), tuple(project1.uuid(), "perm4"), tuple(project2.uuid(), "perm5"));
  }

  @Test
  public void selectGroupUuidsWithPermissionOnProjectBut_returns_empty_if_project_does_not_exist() {
    ComponentDto project = randomPublicOrPrivateProject();
    GroupDto group = db.users().insertGroup();
    db.users().insertProjectPermissionOnGroup(group, "foo", project);

    assertThat(underTest.selectGroupUuidsWithPermissionOnProjectBut(dbSession, "1234", UserRole.USER))
      .isEmpty();
  }

  @Test
  public void selectGroupUuidsWithPermissionOnProjectBut_returns_only_groups_of_project_which_do_not_have_permission() {
    ComponentDto project = randomPublicOrPrivateProject();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertProjectPermissionOnGroup(group1, "p1", project);
    db.users().insertProjectPermissionOnGroup(group2, "p2", project);

    assertThat(underTest.selectGroupUuidsWithPermissionOnProjectBut(dbSession, project.uuid(), "p2"))
      .containsOnly(group1.getUuid());
    assertThat(underTest.selectGroupUuidsWithPermissionOnProjectBut(dbSession, project.uuid(), "p1"))
      .containsOnly(group2.getUuid());
    assertThat(underTest.selectGroupUuidsWithPermissionOnProjectBut(dbSession, project.uuid(), "p3"))
      .containsOnly(group1.getUuid(), group2.getUuid());
  }

  @Test
  public void selectGroupUuidsWithPermissionOnProjectBut_does_not_returns_group_AnyOne_of_project_when_it_does_not_have_permission() {
    ComponentDto project = db.components().insertPublicProject();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertProjectPermissionOnGroup(group1, "p1", project);
    db.users().insertProjectPermissionOnGroup(group2, "p2", project);
    db.users().insertProjectPermissionOnAnyone("p2", project);

    assertThat(underTest.selectGroupUuidsWithPermissionOnProjectBut(dbSession, project.uuid(), "p2"))
      .containsOnly(group1.getUuid());
    assertThat(underTest.selectGroupUuidsWithPermissionOnProjectBut(dbSession, project.uuid(), "p1"))
      .containsOnly(group2.getUuid());
  }

  @Test
  public void selectGroupUuidsWithPermissionOnProjectBut_does_not_return_groups_which_have_no_permission_at_all_on_specified_project() {
    ComponentDto project = randomPublicOrPrivateProject();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();
    db.users().insertProjectPermissionOnGroup(group1, "p1", project);
    db.users().insertProjectPermissionOnGroup(group2, "p2", project);

    assertThat(underTest.selectGroupUuidsWithPermissionOnProjectBut(dbSession, project.uuid(), "p2"))
      .containsOnly(group1.getUuid());
    assertThat(underTest.selectGroupUuidsWithPermissionOnProjectBut(dbSession, project.uuid(), "p1"))
      .containsOnly(group2.getUuid());
  }

  @Test
  public void deleteByRootComponentUuid_on_private_project() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    db.users().insertPermissionOnGroup(group1, "perm1");
    db.users().insertProjectPermissionOnGroup(group1, "perm2", project1);
    db.users().insertProjectPermissionOnGroup(group2, "perm3", project2);

    underTest.deleteByRootComponentUuid(dbSession, project1);
    dbSession.commit();

    assertThat(db.countSql("select count(uuid) from group_roles where component_uuid='" + project1.uuid() + "'")).isZero();
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(2);
  }

  @Test
  public void deleteByRootComponentUuid_on_public_project() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    ComponentDto project1 = db.components().insertPublicProject();
    ComponentDto project2 = db.components().insertPublicProject();
    db.users().insertPermissionOnGroup(group1, "perm1");
    db.users().insertProjectPermissionOnGroup(group1, "perm2", project1);
    db.users().insertProjectPermissionOnGroup(group2, "perm3", project2);
    db.users().insertProjectPermissionOnAnyone("perm4", project1);
    db.users().insertProjectPermissionOnAnyone("perm5", project2);

    underTest.deleteByRootComponentUuid(dbSession, project1);
    dbSession.commit();

    assertThat(db.countSql("select count(uuid) from group_roles where component_uuid='" + project1.uuid() + "'")).isZero();
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(3);
  }

  @Test
  public void delete_global_permission_from_group_on_public_project() {
    GroupDto group1 = db.users().insertGroup();
    ComponentDto project1 = db.components().insertPublicProject();
    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnAnyone("perm4", project1);

    underTest.delete(dbSession, "perm2", group1.getUuid(), group1.getName(), null, project1);
    dbSession.commit();

    assertThatNoPermission("perm2");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(3);
  }

  @Test
  public void delete_global_permission_from_group_on_private_project() {
    GroupDto group1 = db.users().insertGroup();
    ComponentDto project1 = db.components().insertPrivateProject();
    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);

    underTest.delete(dbSession, "perm2", group1.getUuid(), group1.getName(), null, project1);
    dbSession.commit();

    assertThatNoPermission("perm2");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(2);
  }

  @Test
  public void delete_global_permission_from_anyone_on_public_project() {
    GroupDto group1 = db.users().insertGroup();
    ComponentDto project1 = db.components().insertPublicProject();
    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnAnyone("perm4", project1);

    underTest.delete(dbSession, "perm1", null, null, null, project1);
    dbSession.commit();

    assertThatNoPermission("perm1");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(3);
  }

  @Test
  public void delete_project_permission_from_group_on_private_project() {
    GroupDto group1 = db.users().insertGroup();
    ComponentDto project1 = db.components().insertPrivateProject();
    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);

    underTest.delete(dbSession, "perm3", group1.getUuid(), group1.getName(), project1.uuid(), project1);
    dbSession.commit();

    assertThatNoPermission("perm3");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(2);
  }

  @Test
  public void delete_project_permission_from_group_on_public_project() {
    GroupDto group1 = db.users().insertGroup();
    ComponentDto project1 = db.components().insertPublicProject();
    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnAnyone("perm4", project1);

    underTest.delete(dbSession, "perm3", group1.getUuid(), group1.getName(), project1.uuid(), project1);
    dbSession.commit();

    assertThatNoPermission("perm3");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(3);
  }

  @Test
  public void delete_project_permission_from_anybody_on_private_project() {
    GroupDto group1 = db.users().insertGroup();
    ComponentDto project1 = db.components().insertPublicProject();
    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnAnyone("perm4", project1);

    underTest.delete(dbSession, "perm4", null, null, project1.uuid(), project1);
    dbSession.commit();

    assertThatNoPermission("perm4");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(3);
  }

  @Test
  public void deleteByRootComponentUuidAndGroupUuid_deletes_all_permissions_of_group_AnyOne_of_specified_component_if_groupUuid_is_null() {
    ComponentDto project = db.components().insertPublicProject();
    GroupDto group = db.users().insertGroup();
    db.users().insertProjectPermissionOnAnyone("p1", project);
    db.users().insertProjectPermissionOnGroup(group, "p2", project);
    db.users().insertPermissionOnAnyone("p3");
    db.users().insertPermissionOnGroup(group, "p4");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, null, project.uuid()))
      .containsOnly("p1");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, group.getUuid(), project.uuid()))
      .containsOnly("p2");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, null))
      .containsOnly("p3");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group.getUuid()))
      .containsOnly("p4");

    int deletedCount = underTest.deleteByRootComponentUuidAndGroupUuid(dbSession, null, project);

    assertThat(deletedCount).isOne();
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, null, project.uuid()))
      .isEmpty();
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, group.getUuid(), project.uuid()))
      .containsOnly("p2");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, null))
      .containsOnly("p3");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group.getUuid()))
      .containsOnly("p4");
  }

  @Test
  public void deleteByRootComponentUuidAndGroupUuid_deletes_all_permissions_of_specified_group_of_specified_component_if_groupUuid_is_non_null() {
    ComponentDto project = db.components().insertPublicProject();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertProjectPermissionOnAnyone("p1", project);
    db.users().insertProjectPermissionOnGroup(group1, "p2", project);
    db.users().insertProjectPermissionOnGroup(group2, "p3", project);
    db.users().insertProjectPermissionOnGroup(group2, "p4", project);
    db.users().insertPermissionOnAnyone("p5");
    db.users().insertPermissionOnGroup(group1, "p6");
    db.users().insertPermissionOnGroup(group2, "p7");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, null, project.uuid()))
      .containsOnly("p1");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, group1.getUuid(), project.uuid()))
      .containsOnly("p2");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, group2.getUuid(), project.uuid()))
      .containsOnly("p3", "p4");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, null))
      .containsOnly("p5");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group1.getUuid()))
      .containsOnly("p6");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group2.getUuid()))
      .containsOnly("p7");

    int deletedCount = underTest.deleteByRootComponentUuidAndGroupUuid(dbSession, group1.getUuid(), project);

    assertThat(deletedCount).isOne();
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, null, project.uuid()))
      .containsOnly("p1");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, group1.getUuid(), project.uuid()))
      .isEmpty();
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, group2.getUuid(), project.uuid()))
      .containsOnly("p3", "p4");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group1.getUuid()))
      .containsOnly("p6");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group2.getUuid()))
      .containsOnly("p7");

    deletedCount = underTest.deleteByRootComponentUuidAndGroupUuid(dbSession, group2.getUuid(), project);

    assertThat(deletedCount).isEqualTo(2);
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, null, project.uuid()))
      .containsOnly("p1");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, group1.getUuid(), project.uuid()))
      .isEmpty();
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, group2.getUuid(), project.uuid()))
      .isEmpty();
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group1.getUuid()))
      .containsOnly("p6");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group2.getUuid()))
      .containsOnly("p7");
  }

  @Test
  public void deleteByRootComponentUuidAndGroupUuid_has_no_effect_if_component_does_not_exist() {
    GroupDto group = db.users().insertGroup();
    ComponentDto component = new ComponentDto().setUuid("1234");

    assertThat(underTest.deleteByRootComponentUuidAndGroupUuid(dbSession,  null, component)).isZero();
    assertThat(underTest.deleteByRootComponentUuidAndGroupUuid(dbSession, group.getUuid(), component)).isZero();
  }

  @Test
  public void deleteByRootComponentUuidAndGroupUuid_has_no_effect_if_component_has_no_group_permission_at_all() {
    ComponentDto project = randomPublicOrPrivateProject();
    GroupDto group = db.users().insertGroup();

    assertThat(underTest.deleteByRootComponentUuidAndGroupUuid(dbSession,  null, project)).isZero();
    assertThat(underTest.deleteByRootComponentUuidAndGroupUuid(dbSession, group.getUuid(), project)).isZero();
  }

  @Test
  public void deleteByRootComponentUuidAndGroupUuid_has_no_effect_if_group_does_not_exist() {
    ComponentDto project = randomPublicOrPrivateProject();

    assertThat(underTest.deleteByRootComponentUuidAndGroupUuid(dbSession, "5678", project)).isZero();
  }

  @Test
  public void deleteByRootComponentUuidAndGroupUuid_has_no_effect_if_component_has_no_group_permission_for_group_AnyOne() {
    ComponentDto project = db.components().insertPrivateProject();
    GroupDto group1 = db.users().insertGroup();
    db.users().insertProjectPermissionOnGroup(group1, "p1", project);
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, null, project.uuid()))
      .isEmpty();
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, group1.getUuid(), project.uuid()))
      .containsOnly("p1");
    db.users().insertPermissionOnAnyone("p2");
    db.users().insertPermissionOnGroup(group1, "p3");

    int deletedCount = underTest.deleteByRootComponentUuidAndGroupUuid(dbSession, null, project);

    assertThat(deletedCount).isZero();
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, null, project.uuid()))
      .isEmpty();
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, group1.getUuid(), project.uuid()))
      .containsOnly("p1");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, null))
      .containsOnly("p2");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group1.getUuid()))
      .containsOnly("p3");
  }

  @Test
  public void deleteByRootComponentUuidAndGroupUuid_has_no_effect_if_component_has_no_group_permission_for_specified_group() {
    ComponentDto project = db.components().insertPrivateProject();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertProjectPermissionOnGroup(group1, "p1", project);
    db.users().insertPermissionOnAnyone("p2");
    db.users().insertPermissionOnGroup(group1, "p3");

    int deletedCount = underTest.deleteByRootComponentUuidAndGroupUuid(dbSession, group2.getUuid(), project);

    assertThat(deletedCount).isZero();
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, group1.getUuid(), project.uuid()))
      .containsOnly("p1");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, group2.getUuid(), project.uuid()))
      .isEmpty();
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, null))
      .containsOnly("p2");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group1.getUuid()))
      .containsOnly("p3");
  }

  @Test
  public void deleteByRootComponentUuidAndPermission_deletes_all_rows_for_specified_role_of_specified_component() {
    ComponentDto project = db.components().insertPublicProject();
    GroupDto group = db.users().insertGroup();
    Stream.of("p1", "p2").forEach(permission -> {
      db.users().insertPermissionOnAnyone(permission);
      db.users().insertPermissionOnGroup(group, permission);
      db.users().insertProjectPermissionOnGroup(group, permission, project);
      db.users().insertProjectPermissionOnAnyone(permission, project);
    });
    assertThat(getGlobalPermissionsForAnyone()).containsOnly("p1", "p2");
    assertThat(getGlobalPermissionsForGroup(group)).containsOnly("p1", "p2");
    assertThat(getProjectPermissionsForAnyOne(project)).containsOnly("p1", "p2");
    assertThat(getProjectPermissionsForGroup(project, group)).containsOnly("p1", "p2");

    int deletedRows = underTest.deleteByRootComponentUuidAndPermission(dbSession, "p1", project);

    assertThat(deletedRows).isEqualTo(2);
    assertThat(getGlobalPermissionsForAnyone()).containsOnly("p1", "p2");
    assertThat(getGlobalPermissionsForGroup(group)).containsOnly("p1", "p2");
    assertThat(getProjectPermissionsForAnyOne(project)).containsOnly("p2");
    assertThat(getProjectPermissionsForGroup(project, group)).containsOnly("p2");

    deletedRows = underTest.deleteByRootComponentUuidAndPermission(dbSession,  "p2", project);

    assertThat(deletedRows).isEqualTo(2);
    assertThat(getGlobalPermissionsForAnyone()).containsOnly("p1", "p2");
    assertThat(getGlobalPermissionsForGroup(group)).containsOnly("p1", "p2");
    assertThat(getProjectPermissionsForAnyOne(project)).isEmpty();
    assertThat(getProjectPermissionsForGroup(project, group)).isEmpty();
  }

  @Test
  public void deleteByRootComponentUuidAndPermission_has_no_effect_if_component_has_no_group_permission_at_all() {
    GroupDto group = db.users().insertGroup();
    ComponentDto project = randomPublicOrPrivateProject();
    db.users().insertPermissionOnAnyone("p1");
    db.users().insertPermissionOnGroup(group, "p1");

    assertThat(underTest.deleteByRootComponentUuidAndPermission(dbSession, "p1", project)).isZero();

    assertThat(getGlobalPermissionsForAnyone()).containsOnly("p1");
    assertThat(getGlobalPermissionsForGroup(group)).containsOnly("p1");
    assertThat(getProjectPermissionsForAnyOne(project)).isEmpty();
    assertThat(getProjectPermissionsForGroup(project, group)).isEmpty();
  }

  @Test
  public void deleteByRootComponentUuidAndPermission_has_no_effect_if_component_does_not_exist() {
    ComponentDto project = db.components().insertPublicProject();
    GroupDto group = db.users().insertGroup();
    db.users().insertPermissionOnAnyone("p1");
    db.users().insertPermissionOnGroup(group, "p1");
    db.users().insertProjectPermissionOnGroup(group, "p1", project);
    db.users().insertProjectPermissionOnAnyone("p1", project);

    ComponentDto anotherProject = new ComponentDto().setUuid("1324");

    assertThat(underTest.deleteByRootComponentUuidAndPermission(dbSession, "p1", anotherProject)).isZero();

    assertThat(getGlobalPermissionsForAnyone()).containsOnly("p1");
    assertThat(getGlobalPermissionsForGroup(group)).containsOnly("p1");
    assertThat(getProjectPermissionsForAnyOne(project)).containsOnly("p1");
    assertThat(getProjectPermissionsForGroup(project, group)).containsOnly("p1");
  }

  @Test
  public void deleteByRootComponentUuidAndPermission_has_no_effect_if_component_does_not_have_specified_permission() {
    GroupDto group = db.users().insertGroup();
    ComponentDto project = randomPublicOrPrivateProject();
    db.users().insertPermissionOnAnyone("p1");
    db.users().insertPermissionOnGroup(group, "p1");

    assertThat(underTest.deleteByRootComponentUuidAndPermission(dbSession, "p1", project)).isZero();
  }

  private Collection<String> getGlobalPermissionsForAnyone() {
    return getPermissions("group_uuid is null and component_uuid is null");
  }

  private Collection<String> getGlobalPermissionsForGroup(GroupDto groupDto) {
    return getPermissions("group_uuid = '" + groupDto.getUuid() + "' and component_uuid is null");
  }

  private Collection<String> getProjectPermissionsForAnyOne(ComponentDto project) {
    return getPermissions("group_uuid is null and component_uuid = '" + project.uuid() + "'");
  }

  private Collection<String> getProjectPermissionsForGroup(ComponentDto project, GroupDto group) {
    return getPermissions("group_uuid = '" + group.getUuid() + "' and component_uuid = '" + project.uuid() + "'");
  }

  private Collection<String> getPermissions(String whereClauses) {
    return db
      .select(dbSession, "select role from group_roles where " + whereClauses)
      .stream()
      .flatMap(map -> map.entrySet().stream())
      .map(entry -> (String) entry.getValue())
      .collect(MoreCollectors.toList());
  }

  private ComponentDto randomPublicOrPrivateProject() {
    return new Random().nextBoolean() ? db.components().insertPublicProject() : db.components().insertPrivateProject();
  }

  private PermissionQuery.Builder newQuery() {
    return PermissionQuery.builder();
  }

  private void assertThatNoPermission(String permission) {
    assertThat(db.countSql("select count(uuid) from group_roles where role='" + permission + "'")).isZero();
  }

}
