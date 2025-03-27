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
package org.sonar.db.permission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.security.DefaultGroups.ANYONE;
import static org.sonar.db.permission.PermissionQuery.DEFAULT_PAGE_SIZE;

class GroupPermissionDaoIT {

  private static final String ANYONE_UUID = "Anyone";
  private static final String MISSING_UUID = "-1";

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = db.getSession();
  private final GroupPermissionDao underTest = new GroupPermissionDao(new NoOpAuditPersister());

  @Test
  void group_count_by_permission_and_entity_id_on_private_projects() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project3 = db.components().insertPrivateProject().getProjectDto();

    db.users().insertEntityPermissionOnGroup(group1, ProjectPermission.ISSUE_ADMIN, project1);
    db.users().insertEntityPermissionOnGroup(group1, ProjectPermission.ADMIN, project2);
    db.users().insertEntityPermissionOnGroup(group2, ProjectPermission.ADMIN, project2);
    db.users().insertEntityPermissionOnGroup(group3, ProjectPermission.ADMIN, project2);
    db.users().insertEntityPermissionOnGroup(group1, ProjectPermission.USER, project2);
    db.users().insertEntityPermissionOnGroup(group1, ProjectPermission.USER, project3);

    final List<CountPerEntityPermission> result = new ArrayList<>();
    underTest.groupsCountByComponentUuidAndPermission(dbSession, asList(project2.getUuid(), project3.getUuid(), "789"),
      context -> result.add(context.getResultObject()));

    assertThat(result).hasSize(3);
    assertThat(result).extracting("permission").containsOnly(ProjectPermission.ADMIN.getKey(), ProjectPermission.USER.getKey());
    assertThat(result).extracting("entityUuid").containsOnly(project2.getUuid(), project3.getUuid());
    assertThat(result).extracting("count").containsOnly(3, 1);
  }

  @Test
  void group_count_by_permission_and_entity_id_on__projects() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();
    ProjectDto project1 = db.components().insertPublicProject().getProjectDto();
    ProjectDto project2 = db.components().insertPublicProject().getProjectDto();
    ProjectDto project3 = db.components().insertPublicProject().getProjectDto();

    db.users().insertEntityPermissionOnGroup(group1, "p1", project1);
    db.users().insertEntityPermissionOnGroup(group1, "p2", project2);
    db.users().insertEntityPermissionOnGroup(group2, "p2", project2);
    db.users().insertEntityPermissionOnGroup(group3, "p2", project2);
    // anyone group
    db.users().insertEntityPermissionOnAnyone("p2", project2);
    db.users().insertEntityPermissionOnGroup(group1, "p3", project2);
    db.users().insertEntityPermissionOnGroup(group1, "p3", project3);

    final List<CountPerEntityPermission> result = new ArrayList<>();
    underTest.groupsCountByComponentUuidAndPermission(dbSession, asList(project2.getUuid(), project3.getUuid(), "789"),
      context -> result.add(context.getResultObject()));

    assertThat(result).hasSize(3);
    assertThat(result).extracting("permission").containsOnly("p2", "p3");
    assertThat(result).extracting("entityUuid").containsOnly(project2.getUuid(), project3.getUuid());
    assertThat(result).extracting("count").containsOnly(4, 1);
  }

  @Test
  void selectGroupNamesByQuery_is_ordered_by_permissions_then_by_group_names() {
    GroupDto group2 = db.users().insertGroup("Group-2");
    GroupDto group3 = db.users().insertGroup("Group-3");
    GroupDto group1 = db.users().insertGroup("Group-1");
    db.users().insertPermissionOnAnyone(GlobalPermission.SCAN);
    db.users().insertPermissionOnGroup(group3, GlobalPermission.SCAN);

    assertThat(underTest.selectGroupNamesByQuery(dbSession, newQuery().build()))
      .containsExactly(ANYONE, group3.getName(), group1.getName(), group2.getName());
  }

  @Test
  void selectGroupNamesByQuery_is_ordered_by_permissions_then_by_group_when_many_groups_for_global_permissions() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    IntStream.rangeClosed(1, DEFAULT_PAGE_SIZE + 1).forEach(i -> {
      GroupDto group = db.users().insertGroup("Group-" + i);
      // Add permission on project to be sure projects are excluded
      db.users().insertEntityPermissionOnGroup(group, GlobalPermission.SCAN.getKey(), project);
    });
    String lastGroupName = "Group-" + (DEFAULT_PAGE_SIZE + 1);
    db.users().insertPermissionOnGroup(db.users().selectGroup(lastGroupName).orElseGet(() -> fail("group not found")),
      GlobalPermission.SCAN);

    assertThat(underTest.selectGroupNamesByQuery(dbSession, newQuery().build()))
      .hasSize(DEFAULT_PAGE_SIZE)
      .startsWith(ANYONE, lastGroupName, "Group-1");
  }

  @Test
  void selectGroupNamesByQuery_is_ordered_by_global_permissions_then_by_group_when_many_groups_for_entity_permissions() {
    IntStream.rangeClosed(1, DEFAULT_PAGE_SIZE + 1).forEach(i -> {
      GroupDto group = db.users().insertGroup("Group-" + i);
      // Add global permission to be sure they are excluded
      db.users().insertPermissionOnGroup(group, GlobalPermission.SCAN.getKey());
    });
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    String lastGroupName = "Group-" + (DEFAULT_PAGE_SIZE + 1);
    db.users().insertEntityPermissionOnGroup(db.users().selectGroup(lastGroupName).orElseGet(() -> fail("group not found")),
      GlobalPermission.SCAN.getKey(), project);

    assertThat(underTest.selectGroupNamesByQuery(dbSession, newQuery()
      .setEntity(project)
      .build()))
      .hasSize(DEFAULT_PAGE_SIZE)
      .startsWith(ANYONE, lastGroupName, "Group-1");
  }

  @Test
  void countGroupsByQuery() {
    GroupDto group1 = db.users().insertGroup("Group-1");
    db.users().insertGroup("Group-2");
    db.users().insertGroup("Group-3");
    db.users().insertPermissionOnAnyone(GlobalPermission.SCAN);
    db.users().insertPermissionOnGroup(group1, GlobalPermission.PROVISION_PROJECTS);

    assertThat(underTest.countGroupsByQuery(dbSession,
      newQuery().build())).isEqualTo(4);
    assertThat(underTest.countGroupsByQuery(dbSession,
      newQuery().setPermission(GlobalPermission.PROVISION_PROJECTS.getKey()).build())).isOne();
    assertThat(underTest.countGroupsByQuery(dbSession,
      newQuery().withAtLeastOnePermission().build())).isEqualTo(2);
    assertThat(underTest.countGroupsByQuery(dbSession,
      newQuery().setSearchQuery("Group-").build())).isEqualTo(3);
    assertThat(underTest.countGroupsByQuery(dbSession,
      newQuery().setSearchQuery("Any").build())).isOne();
  }

  @Test
  void selectGroupNamesByQuery_with_global_permission() {
    GroupDto group1 = db.users().insertGroup("Group-1");
    GroupDto group2 = db.users().insertGroup("Group-2");
    GroupDto group3 = db.users().insertGroup("Group-3");

    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    db.users().insertPermissionOnAnyone(GlobalPermission.SCAN);
    db.users().insertPermissionOnAnyone(GlobalPermission.PROVISION_PROJECTS);
    db.users().insertPermissionOnGroup(group1, GlobalPermission.SCAN);
    db.users().insertPermissionOnGroup(group3, GlobalPermission.ADMINISTER);
    db.users().insertEntityPermissionOnGroup(group2, ProjectPermission.ADMIN, project);

    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      newQuery().setPermission(GlobalPermission.SCAN.getKey()).build())).containsExactly(ANYONE, group1.getName());

    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      newQuery().setPermission(GlobalPermission.ADMINISTER.getKey()).build())).containsExactly(group3.getName());

    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      newQuery().setPermission(GlobalPermission.PROVISION_PROJECTS.getKey()).build())).containsExactly(ANYONE);
  }

  @Test
  void select_groups_by_query_with_project_permissions_on__projects() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();

    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    ProjectDto anotherProject = db.components().insertPublicProject().getProjectDto();

    db.users().insertEntityPermissionOnGroup(group1, "p1", project);
    db.users().insertEntityPermissionOnGroup(group1, "p2", project);
    db.users().insertEntityPermissionOnAnyone("p3", project);

    db.users().insertEntityPermissionOnGroup(group1, "p4", anotherProject);
    db.users().insertEntityPermissionOnAnyone("p4", anotherProject);
    db.users().insertEntityPermissionOnGroup(group3, "p1", anotherProject);
    db.users().insertPermissionOnGroup(group2, "p5");

    PermissionQuery.Builder builderOnComponent = newQuery()
      .setEntity(project);
    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      builderOnComponent.withAtLeastOnePermission().build())).containsOnlyOnce(group1.getName());
    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      builderOnComponent.setPermission("p1").build())).containsOnlyOnce(group1.getName());
    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      builderOnComponent.setPermission("p3").build())).containsOnlyOnce(ANYONE);
  }

  @Test
  void select_groups_by_query_with_project_permissions_on_private_projects() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();

    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectDto anotherProject = db.components().insertPrivateProject().getProjectDto();

    db.users().insertEntityPermissionOnGroup(group1, GlobalPermission.SCAN.getKey(), project);
    db.users().insertEntityPermissionOnGroup(group1, GlobalPermission.PROVISION_PROJECTS.getKey(), project);

    db.users().insertEntityPermissionOnGroup(group1, ProjectPermission.ADMIN, anotherProject);
    db.users().insertEntityPermissionOnGroup(group3, ProjectPermission.SCAN, anotherProject);
    db.users().insertPermissionOnGroup(group2, GlobalPermission.SCAN);

    PermissionQuery.Builder builderOnComponent = newQuery()
      .setEntity(project);
    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      builderOnComponent.withAtLeastOnePermission().build())).containsOnlyOnce(group1.getName());
    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      builderOnComponent.setPermission(GlobalPermission.SCAN.getKey()).build())).containsOnlyOnce(group1.getName());
    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      builderOnComponent.setPermission(ProjectPermission.USER).build())).isEmpty();
  }

  @Test
  void selectGroupNamesByQuery_is_paginated() {
    IntStream.rangeClosed(0, 9).forEach(i -> db.users().insertGroup(i + "-name"));

    List<String> groupNames = underTest.selectGroupNamesByQuery(dbSession,
      newQuery().setPageIndex(2).setPageSize(3).build());
    assertThat(groupNames).containsExactly("3-name", "4-name", "5-name");
  }

  @Test
  void selectGroupNamesByQuery_with_search_query() {
    GroupDto group = db.users().insertGroup("group-anyone");
    db.users().insertGroup("unknown");
    db.users().insertPermissionOnGroup(group, GlobalPermission.SCAN);

    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      newQuery().setSearchQuery("any").build())).containsOnlyOnce(ANYONE, group.getName());
  }

  @Test
  void selectGroupNamesByQuery_does_not_return_anyone_when_group_roles_is_empty() {
    GroupDto group = db.users().insertGroup();

    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      newQuery().build()))
      .doesNotContain(ANYONE)
      .containsExactly(group.getName());
  }

  @Test
  void selectByGroupUuids_on_global_permissions() {
    GroupDto group1 = db.users().insertGroup("Group-1");
    db.users().insertPermissionOnGroup(group1, GlobalPermission.SCAN);

    GroupDto group2 = db.users().insertGroup("Group-2");
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.users().insertEntityPermissionOnGroup(group2, ProjectPermission.ADMIN, project);

    GroupDto group3 = db.users().insertGroup("Group-3");
    db.users().insertPermissionOnGroup(group3, GlobalPermission.ADMINISTER);

    // Anyone
    db.users().insertPermissionOnAnyone(GlobalPermission.SCAN);
    db.users().insertPermissionOnAnyone(GlobalPermission.PROVISION_PROJECTS);

    assertThat(underTest.selectByGroupUuids(dbSession, List.of(group1.getUuid()), null))
      .extracting(GroupPermissionDto::getGroupUuid, GroupPermissionDto::getRole, GroupPermissionDto::getEntityUuid)
      .containsOnly(tuple(group1.getUuid(), GlobalPermission.SCAN.getKey(), null));

    assertThat(underTest.selectByGroupUuids(dbSession, List.of(group2.getUuid()), null)).isEmpty();

    assertThat(underTest.selectByGroupUuids(dbSession, List.of(group3.getUuid()), null))
      .extracting(GroupPermissionDto::getGroupUuid, GroupPermissionDto::getRole, GroupPermissionDto::getEntityUuid)
      .containsOnly(tuple(group3.getUuid(), GlobalPermission.ADMINISTER.getKey(), null));

    assertThat(underTest.selectByGroupUuids(dbSession, List.of(ANYONE_UUID), null))
      .extracting(GroupPermissionDto::getGroupUuid, GroupPermissionDto::getRole, GroupPermissionDto::getEntityUuid)
      .containsOnly(
        tuple(ANYONE_UUID, GlobalPermission.SCAN.getKey(), null),
        tuple(ANYONE_UUID, GlobalPermission.PROVISION_PROJECTS.getKey(), null));

    assertThat(underTest.selectByGroupUuids(dbSession, List.of(group1.getUuid(), group2.getUuid(), ANYONE_UUID), null)).hasSize(3);
    assertThat(underTest.selectByGroupUuids(dbSession, List.of(MISSING_UUID), null)).isEmpty();
    assertThat(underTest.selectByGroupUuids(dbSession, Collections.emptyList(), null)).isEmpty();
  }

  @Test
  void selectByGroupUuids_on__projects() {
    GroupDto group1 = db.users().insertGroup("Group-1");
    db.users().insertPermissionOnGroup(group1, "p1");

    GroupDto group2 = db.users().insertGroup("Group-2");
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    db.users().insertEntityPermissionOnGroup(group2, "p2", project);

    GroupDto group3 = db.users().insertGroup("Group-3");
    db.users().insertEntityPermissionOnGroup(group3, "p2", project);

    // Anyone group
    db.users().insertPermissionOnAnyone("p3");
    db.users().insertEntityPermissionOnAnyone("p4", project);

    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(group1.getUuid()), project.getUuid())).isEmpty();

    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(group2.getUuid()), project.getUuid()))
      .extracting(GroupPermissionDto::getGroupUuid, GroupPermissionDto::getRole, GroupPermissionDto::getEntityUuid)
      .containsOnly(tuple(group2.getUuid(), "p2", project.getUuid()));

    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(group3.getUuid()), project.getUuid()))
      .extracting(GroupPermissionDto::getGroupUuid, GroupPermissionDto::getRole, GroupPermissionDto::getEntityUuid)
      .containsOnly(tuple(group3.getUuid(), "p2", project.getUuid()));

    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(ANYONE_UUID), project.getUuid()))
      .extracting(GroupPermissionDto::getGroupUuid, GroupPermissionDto::getRole, GroupPermissionDto::getEntityUuid)
      .containsOnly(tuple(ANYONE_UUID, "p4", project.getUuid()));

    assertThat(underTest.selectByGroupUuids(dbSession, asList(group1.getUuid(), group2.getUuid(), ANYONE_UUID), project.getUuid())).hasSize(2);
    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(MISSING_UUID), project.getUuid())).isEmpty();
    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(group1.getUuid()), "123")).isEmpty();
    assertThat(underTest.selectByGroupUuids(dbSession, Collections.emptyList(), project.getUuid())).isEmpty();
  }

  @Test
  void selectByGroupUuids_on_private_projects() {
    GroupDto group1 = db.users().insertGroup("Group-1");
    db.users().insertPermissionOnGroup(group1, GlobalPermission.PROVISION_PROJECTS);

    GroupDto group2 = db.users().insertGroup("Group-2");
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    db.users().insertProjectPermissionOnGroup(group2, ProjectPermission.USER, project);

    GroupDto group3 = db.users().insertGroup("Group-3");
    db.users().insertProjectPermissionOnGroup(group3, ProjectPermission.USER, project);

    // Anyone group
    db.users().insertPermissionOnAnyone(GlobalPermission.SCAN);

    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(group1.getUuid()), project.uuid())).isEmpty();

    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(group2.getUuid()), project.uuid()))
      .extracting(GroupPermissionDto::getGroupUuid, GroupPermissionDto::getRole, GroupPermissionDto::getEntityUuid)
      .containsOnly(tuple(group2.getUuid(), ProjectPermission.USER.getKey(), project.uuid()));

    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(group3.getUuid()), project.uuid()))
      .extracting(GroupPermissionDto::getGroupUuid, GroupPermissionDto::getRole, GroupPermissionDto::getEntityUuid)
      .containsOnly(tuple(group3.getUuid(), ProjectPermission.USER.getKey(), project.uuid()));

    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(ANYONE_UUID), project.uuid()))
      .isEmpty();

    assertThat(underTest.selectByGroupUuids(dbSession, asList(group1.getUuid(), group2.getUuid(), ANYONE_UUID), project.uuid())).hasSize(1);
    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(MISSING_UUID), project.uuid())).isEmpty();
    assertThat(underTest.selectByGroupUuids(dbSession, singletonList(group1.getUuid()), "123")).isEmpty();
    assertThat(underTest.selectByGroupUuids(dbSession, Collections.emptyList(), project.uuid())).isEmpty();
  }

  @Test
  void selectGlobalPermissionsOfGroup() {
    GroupDto group1 = db.users().insertGroup("group1");
    GroupDto group2 = db.users().insertGroup("group2");
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();

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
  void selectEntityPermissionsOfGroup_on__project() {
    GroupDto group1 = db.users().insertGroup("group1");
    ComponentDto project1 = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPublicProject().getMainBranchComponent();

    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm4", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm5", project2);
    db.users().insertProjectPermissionOnAnyone("perm6", project1);

    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, group1.getUuid(), project1.uuid()))
      .containsOnly("perm3", "perm4");
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, group1.getUuid(), project2.uuid()))
      .containsOnly("perm5");
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, null, project1.uuid()))
      .containsOnly("perm6");
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, null, project2.uuid()))
      .isEmpty();
  }

  @Test
  void selectProjectKeysWithAnyonePermissions_on__project_none_found() {
    ComponentDto project1 = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPublicProject().getMainBranchComponent();
    GroupDto group = db.users().insertGroup();
    db.users().insertProjectPermissionOnGroup(group, "perm1", project1);
    db.users().insertProjectPermissionOnGroup(group, "perm1", project2);
    assertThat(underTest.selectProjectKeysWithAnyonePermissions(dbSession, 3)).isEmpty();
  }

  @Test
  void selectProjectKeysWithAnyonePermissions_on__project_ordered_by_kee() {
    ProjectDto project1 = db.components().insertPublicProject().getProjectDto();
    ProjectDto project2 = db.components().insertPublicProject().getProjectDto();
    ProjectDto project3 = db.components().insertPublicProject().getProjectDto();
    db.users().insertEntityPermissionOnAnyone("perm1", project1);
    db.users().insertEntityPermissionOnAnyone("perm1", project2);
    db.users().insertEntityPermissionOnAnyone("perm1", project3);

    TreeSet<String> sortedProjectKeys = new TreeSet<>(Set.of(project1.getKey(), project2.getKey(), project3.getKey()));
    assertThat(underTest.selectProjectKeysWithAnyonePermissions(dbSession, 3))
      .containsExactlyElementsOf(sortedProjectKeys);
  }

  @Test
  void selectProjectKeysWithAnyonePermissions_on__project_ordered_by_kee_max_5() {
    IntStream.rangeClosed(1, 9).forEach(i -> {
      ProjectDto project = db.components().insertPublicProject(p -> p.setKey("key-" + i)).getProjectDto();
      db.users().insertEntityPermissionOnAnyone("perm-" + i, project);
    });

    assertThat(underTest.selectProjectKeysWithAnyonePermissions(dbSession, 5))
      .containsExactly("key-1", "key-2", "key-3", "key-4", "key-5");
  }

  @Test
  void selectProjectKeysWithAnyonePermissions_on__projects_omit_blanket_anyone_group_permissions() {
    // Although saved in the same table (group_roles), this should not be included in the result as not assigned to single project.
    db.users().insertPermissionOnAnyone("perm-anyone");

    IntStream.rangeClosed(1, 9).forEach(i -> {
      ProjectDto project = db.components().insertPublicProject(p -> p.setKey("key-" + i)).getProjectDto();
      db.users().insertEntityPermissionOnAnyone("perm-" + i, project);
    });

    assertThat(underTest.selectProjectKeysWithAnyonePermissions(dbSession, 5))
      .containsExactly("key-1", "key-2", "key-3", "key-4", "key-5");
  }

  @Test
  void countEntitiesWithAnyonePermissions() {
    GroupDto group = db.users().insertGroup();
    IntStream.rangeClosed(1, 5).forEach(i -> {
      ComponentDto project = db.components().insertPublicProject(p -> p.setKey("key-" + i)).getMainBranchComponent();
      db.users().insertProjectPermissionOnAnyone("perm-" + i, project);
      db.users().insertProjectPermissionOnGroup(group, "perm-", project);
    });

    assertThat(underTest.countEntitiesWithAnyonePermissions(dbSession)).isEqualTo(5);
  }

  @Test
  void selectEntityPermissionsOfGroup_on_private_project() {
    GroupDto group1 = db.users().insertGroup("group1");
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();

    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm4", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm5", project2);

    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, group1.getUuid(), project1.uuid()))
      .containsOnly("perm3", "perm4");
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, group1.getUuid(), project2.uuid()))
      .containsOnly("perm5");
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, null, project1.uuid()))
      .isEmpty();
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, null, project2.uuid()))
      .isEmpty();
  }

  @Test
  void selectGroupUuidsWithPermissionOnEntityBut_returns_empty_if_project_does_not_exist() {
    ProjectData project = randomPublicOrPrivateProject();
    GroupDto group = db.users().insertGroup();
    db.users().insertProjectPermissionOnGroup(group, "foo", project.getMainBranchComponent());

    assertThat(underTest.selectGroupUuidsWithPermissionOnEntityBut(dbSession, "1234", ProjectPermission.USER))
      .isEmpty();
  }

  @Test
  void selectGroupUuidsWithPermissionOnEntityBut_returns_only_groups_of_project_which_do_not_have_permission() {
    ProjectDto project = randomPublicOrPrivateProject().getProjectDto();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertEntityPermissionOnGroup(group1, "p1", project);
    db.users().insertEntityPermissionOnGroup(group2, "p2", project);

    assertThat(underTest.selectGroupUuidsWithPermissionOnEntityBut(dbSession, project.getUuid(), "p2"))
      .containsOnly(group1.getUuid());
    assertThat(underTest.selectGroupUuidsWithPermissionOnEntityBut(dbSession, project.getUuid(), "p1"))
      .containsOnly(group2.getUuid());
    assertThat(underTest.selectGroupUuidsWithPermissionOnEntityBut(dbSession, project.getUuid(), "p3"))
      .containsOnly(group1.getUuid(), group2.getUuid());
  }

  @Test
  void selectGroupUuidsWithPermissionOnEntityBut_does_not_returns_group_AnyOne_of_project_when_it_does_not_have_permission() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertProjectPermissionOnGroup(group1, "p1", project);
    db.users().insertProjectPermissionOnGroup(group2, "p2", project);
    db.users().insertProjectPermissionOnAnyone("p2", project);

    assertThat(underTest.selectGroupUuidsWithPermissionOnEntityBut(dbSession, project.uuid(), "p2"))
      .containsOnly(group1.getUuid());
    assertThat(underTest.selectGroupUuidsWithPermissionOnEntityBut(dbSession, project.uuid(), "p1"))
      .containsOnly(group2.getUuid());
  }

  @Test
  void selectGroupUuidsWithPermissionOnEntityBut_does_not_return_groups_which_have_no_permission_at_all_on_specified_project() {
    ProjectData project = randomPublicOrPrivateProject();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertGroup();
    db.users().insertEntityPermissionOnGroup(group1, "p1", project.getProjectDto());
    db.users().insertEntityPermissionOnGroup(group2, "p2", project.getProjectDto());

    assertThat(underTest.selectGroupUuidsWithPermissionOnEntityBut(dbSession, project.projectUuid(), "p2"))
      .containsOnly(group1.getUuid());
    assertThat(underTest.selectGroupUuidsWithPermissionOnEntityBut(dbSession, project.projectUuid(), "p1"))
      .containsOnly(group2.getUuid());
  }

  @Test
  void deleteByRootEntityUuid_on_private_project() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    db.users().insertPermissionOnGroup(group1, "perm1");
    db.users().insertEntityPermissionOnGroup(group1, "perm2", project1);
    db.users().insertEntityPermissionOnGroup(group2, "perm3", project2);

    underTest.deleteByEntityUuid(dbSession, project1);
    dbSession.commit();

    assertThat(db.countSql("select count(uuid) from group_roles where entity_uuid ='" + project1.getUuid() + "'")).isZero();
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(2);
  }

  @Test
  void deleteByRootEntityUuid_on__project() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    ProjectDto project1 = db.components().insertPublicProject().getProjectDto();
    ProjectDto project2 = db.components().insertPublicProject().getProjectDto();
    db.users().insertPermissionOnGroup(group1, "perm1");
    db.users().insertEntityPermissionOnGroup(group1, "perm2", project1);
    db.users().insertEntityPermissionOnGroup(group2, "perm3", project2);
    db.users().insertEntityPermissionOnAnyone("perm4", project1);
    db.users().insertEntityPermissionOnAnyone("perm5", project2);

    underTest.deleteByEntityUuid(dbSession, project1);
    dbSession.commit();

    assertThat(db.countSql("select count(uuid) from group_roles where entity_uuid ='" + project1.getUuid() + "'")).isZero();
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(3);
  }

  @Test
  void delete_global_permission_from_group_on__project() {
    GroupDto group1 = db.users().insertGroup();
    ProjectDto project1 = db.components().insertPublicProject().getProjectDto();
    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertEntityPermissionOnGroup(group1, "perm3", project1);
    db.users().insertEntityPermissionOnAnyone("perm4", project1);

    underTest.delete(dbSession, "perm2", group1.getUuid(), group1.getName(), null);
    dbSession.commit();

    assertThatNoPermission("perm2");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(3);
  }

  @Test
  void delete_global_permission_from_group_on_private_project() {
    GroupDto group1 = db.users().insertGroup();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertEntityPermissionOnGroup(group1, "perm3", project1);

    underTest.delete(dbSession, "perm2", group1.getUuid(), group1.getName(), null);
    dbSession.commit();

    assertThatNoPermission("perm2");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(2);
  }

  @Test
  void delete_global_permission_from_anyone_on__project() {
    GroupDto group1 = db.users().insertGroup();
    ProjectDto project1 = db.components().insertPublicProject().getProjectDto();
    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertEntityPermissionOnGroup(group1, "perm3", project1);
    db.users().insertEntityPermissionOnAnyone("perm4", project1);

    underTest.delete(dbSession, "perm1", null, null, null);
    dbSession.commit();

    assertThatNoPermission("perm1");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(3);
  }

  @Test
  void delete_project_permission_from_group_on_private_project() {
    GroupDto group1 = db.users().insertGroup();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertEntityPermissionOnGroup(group1, "perm3", project1);

    underTest.delete(dbSession, "perm3", group1.getUuid(), group1.getName(), project1);
    dbSession.commit();

    assertThatNoPermission("perm3");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(2);
  }

  @Test
  void delete_project_permission_from_group_on__project() {
    GroupDto group1 = db.users().insertGroup();
    ProjectDto project1 = db.components().insertPublicProject().getProjectDto();
    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertEntityPermissionOnGroup(group1, "perm3", project1);
    db.users().insertEntityPermissionOnAnyone("perm4", project1);

    underTest.delete(dbSession, "perm3", group1.getUuid(), group1.getName(), project1);
    dbSession.commit();

    assertThatNoPermission("perm3");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(3);
  }

  @Test
  void delete_project_permission_from_anybody_on_private_project() {
    GroupDto group1 = db.users().insertGroup();
    ProjectDto project1 = db.components().insertPublicProject().getProjectDto();
    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertEntityPermissionOnGroup(group1, "perm3", project1);
    db.users().insertEntityPermissionOnAnyone("perm4", project1);

    underTest.delete(dbSession, "perm4", null, null, project1);
    dbSession.commit();

    assertThatNoPermission("perm4");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(3);
  }

  @Test
  void deleteByRootEntityAndGroupUuid_deletes_all_permissions_of_group_AnyOne_of_specified_component_if_groupUuid_is_null() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    GroupDto group = db.users().insertGroup();
    db.users().insertEntityPermissionOnAnyone("p1", project);
    db.users().insertEntityPermissionOnGroup(group, "p2", project);
    db.users().insertPermissionOnAnyone("p3");
    db.users().insertPermissionOnGroup(group, "p4");
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, null, project.getUuid()))
      .containsOnly("p1");
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, group.getUuid(), project.getUuid()))
      .containsOnly("p2");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, null))
      .containsOnly("p3");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group.getUuid()))
      .containsOnly("p4");

    int deletedCount = underTest.deleteByEntityAndGroupUuid(dbSession, null, project);

    assertThat(deletedCount).isOne();
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, null, project.getUuid()))
      .isEmpty();
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, group.getUuid(), project.getUuid()))
      .containsOnly("p2");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, null))
      .containsOnly("p3");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group.getUuid()))
      .containsOnly("p4");
  }

  @Test
  void deleteByRootEntityAndGroupUuid_deletes_all_permissions_of_specified_group_of_specified_component_if_groupUuid_is_non_null() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertEntityPermissionOnAnyone("p1", project);
    db.users().insertEntityPermissionOnGroup(group1, "p2", project);
    db.users().insertEntityPermissionOnGroup(group2, "p3", project);
    db.users().insertEntityPermissionOnGroup(group2, "p4", project);
    db.users().insertPermissionOnAnyone("p5");
    db.users().insertPermissionOnGroup(group1, "p6");
    db.users().insertPermissionOnGroup(group2, "p7");
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, null, project.getUuid()))
      .containsOnly("p1");
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, group1.getUuid(), project.getUuid()))
      .containsOnly("p2");
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, group2.getUuid(), project.getUuid()))
      .containsOnly("p3", "p4");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, null))
      .containsOnly("p5");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group1.getUuid()))
      .containsOnly("p6");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group2.getUuid()))
      .containsOnly("p7");

    int deletedCount = underTest.deleteByEntityAndGroupUuid(dbSession, group1.getUuid(), project);

    assertThat(deletedCount).isOne();
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, null, project.getUuid()))
      .containsOnly("p1");
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, group1.getUuid(), project.getUuid()))
      .isEmpty();
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, group2.getUuid(), project.getUuid()))
      .containsOnly("p3", "p4");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group1.getUuid()))
      .containsOnly("p6");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group2.getUuid()))
      .containsOnly("p7");

    deletedCount = underTest.deleteByEntityAndGroupUuid(dbSession, group2.getUuid(), project);

    assertThat(deletedCount).isEqualTo(2);
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, null, project.getUuid()))
      .containsOnly("p1");
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, group1.getUuid(), project.getUuid()))
      .isEmpty();
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, group2.getUuid(), project.getUuid()))
      .isEmpty();
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group1.getUuid()))
      .containsOnly("p6");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group2.getUuid()))
      .containsOnly("p7");
  }

  @Test
  void deleteByRootEntityAndGroupUuid_has_no_effect_if_component_does_not_exist() {
    GroupDto group = db.users().insertGroup();
    ProjectDto projectDto = new ProjectDto().setUuid("1234");

    assertThat(underTest.deleteByEntityAndGroupUuid(dbSession, null, projectDto)).isZero();
    assertThat(underTest.deleteByEntityAndGroupUuid(dbSession, group.getUuid(), projectDto)).isZero();
  }

  @Test
  void deleteByRootEntityAndGroupUuid_has_no_effect_if_component_has_no_group_permission_at_all() {
    ProjectData project = randomPublicOrPrivateProject();
    GroupDto group = db.users().insertGroup();

    assertThat(underTest.deleteByEntityAndGroupUuid(dbSession, null, project.getProjectDto())).isZero();
    assertThat(underTest.deleteByEntityAndGroupUuid(dbSession, group.getUuid(), project.getProjectDto())).isZero();
  }

  @Test
  void deleteByRootEntityAndGroupUuid_has_no_effect_if_group_does_not_exist() {
    ProjectData project = randomPublicOrPrivateProject();

    assertThat(underTest.deleteByEntityAndGroupUuid(dbSession, "5678", project.getProjectDto())).isZero();
  }

  @Test
  void deleteByRootEntityAndGroupUuid_has_no_effect_if_component_has_no_group_permission_for_group_AnyOne() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    GroupDto group1 = db.users().insertGroup();
    db.users().insertEntityPermissionOnGroup(group1, "p1", project);
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, null, project.getUuid()))
      .isEmpty();
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, group1.getUuid(), project.getUuid()))
      .containsOnly("p1");
    db.users().insertPermissionOnAnyone("p2");
    db.users().insertPermissionOnGroup(group1, "p3");

    int deletedCount = underTest.deleteByEntityAndGroupUuid(dbSession, null, project);

    assertThat(deletedCount).isZero();
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, null, project.getUuid()))
      .isEmpty();
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, group1.getUuid(), project.getUuid()))
      .containsOnly("p1");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, null))
      .containsOnly("p2");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group1.getUuid()))
      .containsOnly("p3");
  }

  @Test
  void deleteByRootEntityAndGroupUuid_has_no_effect_if_component_has_no_group_permission_for_specified_group() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertEntityPermissionOnGroup(group1, "p1", project);
    db.users().insertPermissionOnAnyone("p2");
    db.users().insertPermissionOnGroup(group1, "p3");

    int deletedCount = underTest.deleteByEntityAndGroupUuid(dbSession, group2.getUuid(), project);

    assertThat(deletedCount).isZero();
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, group1.getUuid(), project.getUuid()))
      .containsOnly("p1");
    assertThat(underTest.selectEntityPermissionsOfGroup(dbSession, group2.getUuid(), project.getUuid()))
      .isEmpty();
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, null))
      .containsOnly("p2");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, group1.getUuid()))
      .containsOnly("p3");
  }

  @Test
  void deleteByEntityAndPermission_deletes_all_rows_for_specified_role_of_specified_component() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    GroupDto group = db.users().insertGroup();
    Stream.of("p1", "p2").forEach(permission -> {
      db.users().insertPermissionOnAnyone(permission);
      db.users().insertPermissionOnGroup(group, permission);
      db.users().insertEntityPermissionOnGroup(group, permission, project);
      db.users().insertEntityPermissionOnAnyone(permission, project);
    });
    assertThat(getGlobalPermissionsForAnyone()).containsOnly("p1", "p2");
    assertThat(getGlobalPermissionsForGroup(group)).containsOnly("p1", "p2");
    assertThat(getProjectPermissionsForAnyOne(project.getUuid())).containsOnly("p1", "p2");
    assertThat(getProjectPermissionsForGroup(project.getUuid(), group)).containsOnly("p1", "p2");

    int deletedRows = underTest.deleteByEntityAndPermission(dbSession, "p1", project);

    assertThat(deletedRows).isEqualTo(2);
    assertThat(getGlobalPermissionsForAnyone()).containsOnly("p1", "p2");
    assertThat(getGlobalPermissionsForGroup(group)).containsOnly("p1", "p2");
    assertThat(getProjectPermissionsForAnyOne(project.getUuid())).containsOnly("p2");
    assertThat(getProjectPermissionsForGroup(project.getUuid(), group)).containsOnly("p2");

    deletedRows = underTest.deleteByEntityAndPermission(dbSession, "p2", project);

    assertThat(deletedRows).isEqualTo(2);
    assertThat(getGlobalPermissionsForAnyone()).containsOnly("p1", "p2");
    assertThat(getGlobalPermissionsForGroup(group)).containsOnly("p1", "p2");
    assertThat(getProjectPermissionsForAnyOne(project.getUuid())).isEmpty();
    assertThat(getProjectPermissionsForGroup(project.getUuid(), group)).isEmpty();
  }

  @Test
  void deleteByEntityAndPermission_has_no_effect_if_component_has_no_group_permission_at_all() {
    GroupDto group = db.users().insertGroup();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.users().insertPermissionOnAnyone("p1");
    db.users().insertPermissionOnGroup(group, "p1");

    assertThat(underTest.deleteByEntityAndPermission(dbSession, "p1", project)).isZero();

    assertThat(getGlobalPermissionsForAnyone()).containsOnly("p1");
    assertThat(getGlobalPermissionsForGroup(group)).containsOnly("p1");
    assertThat(getProjectPermissionsForAnyOne(project.getUuid())).isEmpty();
    assertThat(getProjectPermissionsForGroup(project.getUuid(), group)).isEmpty();
  }

  @Test
  void deleteByEntityAndPermission_has_no_effect_if_component_does_not_exist() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    GroupDto group = db.users().insertGroup();
    db.users().insertPermissionOnAnyone("p1");
    db.users().insertPermissionOnGroup(group, "p1");
    db.users().insertEntityPermissionOnGroup(group, "p1", project);
    db.users().insertEntityPermissionOnAnyone("p1", project);

    ProjectDto anotherProject = ComponentTesting.newProjectDto();

    assertThat(underTest.deleteByEntityAndPermission(dbSession, "p1", anotherProject)).isZero();

    assertThat(getGlobalPermissionsForAnyone()).containsOnly("p1");
    assertThat(getGlobalPermissionsForGroup(group)).containsOnly("p1");
    assertThat(getProjectPermissionsForAnyOne(project.getUuid())).containsOnly("p1");
    assertThat(getProjectPermissionsForGroup(project.getUuid(), group)).containsOnly("p1");
  }

  @Test
  void deleteByEntityAndPermission_has_no_effect_if_component_does_not_have_specified_permission() {
    GroupDto group = db.users().insertGroup();
    ProjectData project = randomPublicOrPrivateProject();
    db.users().insertPermissionOnAnyone("p1");
    db.users().insertPermissionOnGroup(group, "p1");

    assertThat(underTest.deleteByEntityAndPermission(dbSession, "p1", project.getProjectDto())).isZero();
  }

  @Test
  void selectGroupUuidsWithPermissionOnEntity_shouldReturnOnlyGroupsWithSpecifiedPermission() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectDto otherProject = db.components().insertPrivateProject().getProjectDto();
    String anyPermission = "any_permission";
    db.users().insertEntityPermissionOnGroup(group1, anyPermission, project);
    db.users().insertEntityPermissionOnGroup(group2, "otherPermission", project);
    db.users().insertEntityPermissionOnGroup(group3, anyPermission, otherProject);

    Set<String> results = underTest.selectGroupUuidsWithPermissionOnEntity(dbSession, project.getUuid(), anyPermission);

    assertThat(results).containsOnly(group1.getUuid());
  }

  @Test
  void selectGroupPermissionsOnEntity_whenPermissionsExist_returnsGroupPermissions() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();
    UserDto user = db.users().insertUser();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectDto otherProject = db.components().insertPrivateProject().getProjectDto();
    String permission1 = "permission_1";
    String permission2 = "permission_2";
    db.users().insertEntityPermissionOnGroup(group1, permission1, project);
    db.users().insertEntityPermissionOnGroup(group1, permission2, project);
    db.users().insertEntityPermissionOnGroup(group2, permission1, project);
    db.users().insertEntityPermissionOnGroup(group3, permission2, otherProject);
    db.users().insertProjectPermissionOnUser(user, permission1, project);

    List<GroupPermissionDto> results = underTest.selectGroupPermissionsOnEntity(dbSession, project.getUuid());

    assertThat(results)
      .extracting(
        GroupPermissionDto::getGroupUuid, GroupPermissionDto::getEntityUuid, GroupPermissionDto::getRole)
      .containsExactlyInAnyOrder(
        tuple(group1.getUuid(), project.getUuid(), permission1),
        tuple(group1.getUuid(), project.getUuid(), permission2),
        tuple(group2.getUuid(), project.getUuid(), permission1));
  }

  @Test
  void selectGroupPermissionsOnEntity_whenPermissionsDontExist_returnEmptyList() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    assertThat(underTest.selectGroupPermissionsOnEntity(dbSession, project.getUuid()))
      .isEmpty();
  }

  private Collection<String> getGlobalPermissionsForAnyone() {
    return getPermissions("group_uuid is null and entity_uuid is null");
  }

  private Collection<String> getGlobalPermissionsForGroup(GroupDto groupDto) {
    return getPermissions("group_uuid = '" + groupDto.getUuid() + "' and entity_uuid is null");
  }

  private Collection<String> getProjectPermissionsForAnyOne(String projectUuid) {
    return getPermissions("group_uuid is null and entity_uuid = '" + projectUuid + "'");
  }

  private Collection<String> getProjectPermissionsForGroup(String projectUuid, GroupDto group) {
    return getPermissions("group_uuid = '" + group.getUuid() + "' and entity_uuid = '" + projectUuid + "'");
  }

  private Collection<String> getPermissions(String whereClauses) {
    return db
      .select(dbSession, "select role from group_roles where " + whereClauses)
      .stream()
      .flatMap(map -> map.entrySet().stream())
      .map(entry -> (String) entry.getValue())
      .toList();
  }

  private ProjectData randomPublicOrPrivateProject() {
    return new Random().nextBoolean() ? db.components().insertPublicProject() : db.components().insertPrivateProject();
  }

  private PermissionQuery.Builder newQuery() {
    return PermissionQuery.builder();
  }

  private void assertThatNoPermission(String permission) {
    assertThat(db.countSql("select count(uuid) from group_roles where role='" + permission + "'")).isZero();
  }

}
