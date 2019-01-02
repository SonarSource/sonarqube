/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
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
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.OrganizationPermission.SCAN;

public class GroupPermissionDaoTest {

  private static final int ANYONE_ID = 0;
  private static final int MISSING_ID = -1;

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbSession dbSession = db.getSession();
  private GroupPermissionDao underTest = new GroupPermissionDao();
  private String defaultOrganizationUuid;

  @Before
  public void setUp() throws Exception {
    defaultOrganizationUuid = db.getDefaultOrganization().getUuid();
  }

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
    underTest.groupsCountByComponentIdAndPermission(dbSession, asList(project2.getId(), project3.getId(), 789L),
      context -> result.add((CountPerProjectPermission) context.getResultObject()));

    assertThat(result).hasSize(3);
    assertThat(result).extracting("permission").containsOnly(ADMIN, USER);
    assertThat(result).extracting("componentId").containsOnly(project2.getId(), project3.getId());
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
    underTest.groupsCountByComponentIdAndPermission(dbSession, asList(project2.getId(), project3.getId(), 789L),
      context -> result.add((CountPerProjectPermission) context.getResultObject()));

    assertThat(result).hasSize(3);
    assertThat(result).extracting("permission").containsOnly("p2", "p3");
    assertThat(result).extracting("componentId").containsOnly(project2.getId(), project3.getId());
    assertThat(result).extracting("count").containsOnly(4, 1);
  }

  @Test
  public void selectGroupNamesByQuery_is_ordered_by_permissions_then_by_group_names() {
    OrganizationDto organizationDto = db.organizations().insert();
    GroupDto group2 = db.users().insertGroup(organizationDto, "Group-2");
    GroupDto group3 = db.users().insertGroup(organizationDto, "Group-3");
    GroupDto group1 = db.users().insertGroup(organizationDto, "Group-1");
    db.users().insertPermissionOnAnyone(organizationDto, SCAN);
    db.users().insertPermissionOnGroup(group3, SCAN);

    assertThat(underTest.selectGroupNamesByQuery(dbSession, newQuery().setOrganizationUuid(organizationDto.getUuid()).build()))
      .containsExactly(ANYONE, group3.getName(), group1.getName(), group2.getName());
  }

  @Test
  public void countGroupsByQuery() {
    OrganizationDto organizationDto = db.getDefaultOrganization();
    GroupDto group1 = db.users().insertGroup(organizationDto, "Group-1");
    db.users().insertGroup(organizationDto, "Group-2");
    db.users().insertGroup(organizationDto, "Group-3");
    db.users().insertPermissionOnAnyone(organizationDto, SCAN);
    db.users().insertPermissionOnGroup(group1, PROVISION_PROJECTS);

    assertThat(underTest.countGroupsByQuery(dbSession,
      newQuery().build())).isEqualTo(4);
    assertThat(underTest.countGroupsByQuery(dbSession,
      newQuery().setPermission(PROVISION_PROJECTS.getKey()).build())).isEqualTo(1);
    assertThat(underTest.countGroupsByQuery(dbSession,
      newQuery().withAtLeastOnePermission().build())).isEqualTo(2);
    assertThat(underTest.countGroupsByQuery(dbSession,
      newQuery().setSearchQuery("Group-").build())).isEqualTo(3);
    assertThat(underTest.countGroupsByQuery(dbSession,
      newQuery().setSearchQuery("Any").build())).isEqualTo(1);
  }

  @Test
  public void selectGroupNamesByQuery_with_global_permission() {
    OrganizationDto organizationDto = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(organizationDto, "Group-1");
    GroupDto group2 = db.users().insertGroup(organizationDto, "Group-2");
    GroupDto group3 = db.users().insertGroup(organizationDto, "Group-3");

    ComponentDto project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto));

    db.users().insertPermissionOnAnyone(organizationDto, SCAN);
    db.users().insertPermissionOnAnyone(organizationDto, PROVISION_PROJECTS);
    db.users().insertPermissionOnGroup(group1, SCAN);
    db.users().insertPermissionOnGroup(group3, ADMINISTER);
    db.users().insertProjectPermissionOnGroup(group2, UserRole.ADMIN, project);

    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      newQuery().setOrganizationUuid(organizationDto.getUuid()).setPermission(SCAN.getKey()).build())).containsExactly(ANYONE, group1.getName());

    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      newQuery().setOrganizationUuid(organizationDto.getUuid()).setPermission(ADMINISTER.getKey()).build())).containsExactly(group3.getName());

    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      newQuery().setOrganizationUuid(organizationDto.getUuid()).setPermission(PROVISION_PROJECTS.getKey()).build())).containsExactly(ANYONE);
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

    PermissionQuery.Builder builderOnComponent = newQuery().setComponentUuid(project.uuid());
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

    PermissionQuery.Builder builderOnComponent = newQuery().setComponentUuid(project.uuid());
    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      builderOnComponent.withAtLeastOnePermission().build())).containsOnlyOnce(group1.getName());
    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      builderOnComponent.setPermission(SCAN_EXECUTION).build())).containsOnlyOnce(group1.getName());
    assertThat(underTest.selectGroupNamesByQuery(dbSession,
      builderOnComponent.setPermission(USER).build())).isEmpty();
  }

  @Test
  public void selectGroupNamesByQuery_is_paginated() {
    IntStream.rangeClosed(0, 9).forEach(i -> db.users().insertGroup(db.getDefaultOrganization(), i + "-name"));

    List<String> groupNames = underTest.selectGroupNamesByQuery(dbSession,
      newQuery().setPageIndex(2).setPageSize(3).build());
    assertThat(groupNames).containsExactly("3-name", "4-name", "5-name");
  }

  @Test
  public void selectGroupNamesByQuery_with_search_query() {
    GroupDto group = db.users().insertGroup(db.getDefaultOrganization(), "group-anyone");
    db.users().insertGroup(db.getDefaultOrganization(), "unknown");
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
  public void selectByGroupIds_on_global_permissions() {
    OrganizationDto organizationDto = db.organizations().insert();

    GroupDto group1 = db.users().insertGroup(organizationDto, "Group-1");
    db.users().insertPermissionOnGroup(group1, SCAN);

    GroupDto group2 = db.users().insertGroup(organizationDto, "Group-2");
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto));
    db.users().insertProjectPermissionOnGroup(group2, UserRole.ADMIN, project);

    GroupDto group3 = db.users().insertGroup(organizationDto, "Group-3");
    db.users().insertPermissionOnGroup(group3, ADMINISTER);

    // Anyone
    db.users().insertPermissionOnAnyone(organizationDto, SCAN);
    db.users().insertPermissionOnAnyone(organizationDto, PROVISION_PROJECTS);

    assertThat(underTest.selectByGroupIds(dbSession, organizationDto.getUuid(), asList(group1.getId()), null))
      .extracting(GroupPermissionDto::getGroupId, GroupPermissionDto::getRole, GroupPermissionDto::getResourceId)
      .containsOnly(tuple(group1.getId(), SCAN_EXECUTION, null));

    assertThat(underTest.selectByGroupIds(dbSession, organizationDto.getUuid(), asList(group2.getId()), null)).isEmpty();

    assertThat(underTest.selectByGroupIds(dbSession, organizationDto.getUuid(), asList(group3.getId()), null))
      .extracting(GroupPermissionDto::getGroupId, GroupPermissionDto::getRole, GroupPermissionDto::getResourceId)
      .containsOnly(tuple(group3.getId(), ADMINISTER.getKey(), null));

    assertThat(underTest.selectByGroupIds(dbSession, organizationDto.getUuid(), asList(ANYONE_ID), null))
      .extracting(GroupPermissionDto::getGroupId, GroupPermissionDto::getRole, GroupPermissionDto::getResourceId)
      .containsOnly(
        tuple(0, SCAN.getKey(), null),
        tuple(0, PROVISION_PROJECTS.getKey(), null));

    assertThat(underTest.selectByGroupIds(dbSession, organizationDto.getUuid(), asList(group1.getId(), group2.getId(), ANYONE_ID), null)).hasSize(3);
    assertThat(underTest.selectByGroupIds(dbSession, organizationDto.getUuid(), asList(MISSING_ID), null)).isEmpty();
    assertThat(underTest.selectByGroupIds(dbSession, organizationDto.getUuid(), Collections.emptyList(), null)).isEmpty();
  }

  @Test
  public void selectByGroupIds_on_public_projects() {
    OrganizationDto org = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org, "Group-1");
    db.users().insertPermissionOnGroup(group1, "p1");

    GroupDto group2 = db.users().insertGroup(org, "Group-2");
    ComponentDto project = db.components().insertPublicProject(org);
    db.users().insertProjectPermissionOnGroup(group2, "p2", project);

    GroupDto group3 = db.users().insertGroup(org, "Group-3");
    db.users().insertProjectPermissionOnGroup(group3, "p2", project);

    // Anyone group
    db.users().insertPermissionOnAnyone(org, "p3");
    db.users().insertProjectPermissionOnAnyone("p4", project);

    assertThat(underTest.selectByGroupIds(dbSession, defaultOrganizationUuid, singletonList(group1.getId()), project.getId())).isEmpty();

    assertThat(underTest.selectByGroupIds(dbSession, org.getUuid(), singletonList(group2.getId()), project.getId()))
      .extracting(GroupPermissionDto::getGroupId, GroupPermissionDto::getRole, GroupPermissionDto::getResourceId)
      .containsOnly(tuple(group2.getId(), "p2", project.getId()));

    assertThat(underTest.selectByGroupIds(dbSession, org.getUuid(), singletonList(group3.getId()), project.getId()))
      .extracting(GroupPermissionDto::getGroupId, GroupPermissionDto::getRole, GroupPermissionDto::getResourceId)
      .containsOnly(tuple(group3.getId(), "p2", project.getId()));

    assertThat(underTest.selectByGroupIds(dbSession, org.getUuid(), singletonList(ANYONE_ID), project.getId()))
      .extracting(GroupPermissionDto::getGroupId, GroupPermissionDto::getRole, GroupPermissionDto::getResourceId)
      .containsOnly(tuple(0, "p4", project.getId()));

    assertThat(underTest.selectByGroupIds(dbSession, org.getUuid(), asList(group1.getId(), group2.getId(), ANYONE_ID), project.getId())).hasSize(2);
    assertThat(underTest.selectByGroupIds(dbSession, org.getUuid(), singletonList(MISSING_ID), project.getId())).isEmpty();
    assertThat(underTest.selectByGroupIds(dbSession, org.getUuid(), singletonList(group1.getId()), 123L)).isEmpty();
    assertThat(underTest.selectByGroupIds(dbSession, org.getUuid(), Collections.emptyList(), project.getId())).isEmpty();
  }

  @Test
  public void selectByGroupIds_on_private_projects() {
    OrganizationDto org = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org, "Group-1");
    db.users().insertPermissionOnGroup(group1, PROVISION_PROJECTS);

    GroupDto group2 = db.users().insertGroup(org, "Group-2");
    ComponentDto project = db.components().insertPrivateProject(org);
    db.users().insertProjectPermissionOnGroup(group2, USER, project);

    GroupDto group3 = db.users().insertGroup(org, "Group-3");
    db.users().insertProjectPermissionOnGroup(group3, USER, project);

    // Anyone group
    db.users().insertPermissionOnAnyone(org, SCAN);

    assertThat(underTest.selectByGroupIds(dbSession, defaultOrganizationUuid, singletonList(group1.getId()), project.getId())).isEmpty();

    assertThat(underTest.selectByGroupIds(dbSession, org.getUuid(), singletonList(group2.getId()), project.getId()))
      .extracting(GroupPermissionDto::getGroupId, GroupPermissionDto::getRole, GroupPermissionDto::getResourceId)
      .containsOnly(tuple(group2.getId(), USER, project.getId()));

    assertThat(underTest.selectByGroupIds(dbSession, org.getUuid(), singletonList(group3.getId()), project.getId()))
      .extracting(GroupPermissionDto::getGroupId, GroupPermissionDto::getRole, GroupPermissionDto::getResourceId)
      .containsOnly(tuple(group3.getId(), USER, project.getId()));

    assertThat(underTest.selectByGroupIds(dbSession, org.getUuid(), singletonList(ANYONE_ID), project.getId()))
      .isEmpty();

    assertThat(underTest.selectByGroupIds(dbSession, org.getUuid(), asList(group1.getId(), group2.getId(), ANYONE_ID), project.getId())).hasSize(1);
    assertThat(underTest.selectByGroupIds(dbSession, org.getUuid(), singletonList(MISSING_ID), project.getId())).isEmpty();
    assertThat(underTest.selectByGroupIds(dbSession, org.getUuid(), singletonList(group1.getId()), 123L)).isEmpty();
    assertThat(underTest.selectByGroupIds(dbSession, org.getUuid(), Collections.emptyList(), project.getId())).isEmpty();
  }

  @Test
  public void selectGlobalPermissionsOfGroup() {
    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org1, "group1");
    GroupDto group2 = db.users().insertGroup(org2, "group2");
    ComponentDto project = db.components().insertPublicProject(org1);

    db.users().insertPermissionOnAnyone(org1, "perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertPermissionOnGroup(group1, "perm3");
    db.users().insertPermissionOnGroup(group2, "perm4");
    db.users().insertProjectPermissionOnGroup(group1, "perm5", project);
    db.users().insertProjectPermissionOnAnyone("perm6", project);

    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, org1.getUuid(), group1.getId())).containsOnly("perm2", "perm3");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, org2.getUuid(), group2.getId())).containsOnly("perm4");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, org1.getUuid(), null)).containsOnly("perm1");

    // group1 is not in org2
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, org2.getUuid(), group1.getId())).isEmpty();
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, org2.getUuid(), null)).isEmpty();
  }

  @Test
  public void selectProjectPermissionsOfGroup_on_public_project() {
    OrganizationDto org1 = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org1, "group1");
    ComponentDto project1 = db.components().insertPublicProject(org1);
    ComponentDto project2 = db.components().insertPublicProject(org1);

    db.users().insertPermissionOnAnyone(org1, "perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm4", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm5", project2);
    db.users().insertProjectPermissionOnAnyone("perm6", project1);

    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, org1.getUuid(), group1.getId(), project1.getId()))
      .containsOnly("perm3", "perm4");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, org1.getUuid(), group1.getId(), project2.getId()))
      .containsOnly("perm5");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, org1.getUuid(), null, project1.getId()))
      .containsOnly("perm6");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, org1.getUuid(), null, project2.getId()))
      .isEmpty();
  }

  @Test
  public void selectProjectPermissionsOfGroup_on_private_project() {
    OrganizationDto org1 = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org1, "group1");
    ComponentDto project1 = db.components().insertPrivateProject(org1);
    ComponentDto project2 = db.components().insertPrivateProject(org1);

    db.users().insertPermissionOnAnyone(org1, "perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm4", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm5", project2);

    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, org1.getUuid(), group1.getId(), project1.getId()))
      .containsOnly("perm3", "perm4");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, org1.getUuid(), group1.getId(), project2.getId()))
      .containsOnly("perm5");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, org1.getUuid(), null, project1.getId()))
      .isEmpty();
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, org1.getUuid(), null, project2.getId()))
      .isEmpty();
  }

  @Test
  public void selectAllPermissionsByGroupId_on_public_project() {
    OrganizationDto org1 = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org1, "group1");
    ComponentDto project1 = db.components().insertPublicProject(org1);
    ComponentDto project2 = db.components().insertPublicProject(org1);
    db.users().insertPermissionOnAnyone(org1, "perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm4", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm5", project2);
    db.users().insertProjectPermissionOnAnyone("perm6", project1);

    List<GroupPermissionDto> result = new ArrayList<>();
    underTest.selectAllPermissionsByGroupId(dbSession, org1.getUuid(), group1.getId(), context -> result.add((GroupPermissionDto) context.getResultObject()));
    assertThat(result).extracting(GroupPermissionDto::getResourceId, GroupPermissionDto::getRole).containsOnly(
      tuple(null, "perm2"),
      tuple(project1.getId(), "perm3"), tuple(project1.getId(), "perm4"), tuple(project2.getId(), "perm5"));
  }

  @Test
  public void selectAllPermissionsByGroupId_on_private_project() {
    OrganizationDto org1 = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org1, "group1");
    ComponentDto project1 = db.components().insertPrivateProject(org1);
    ComponentDto project2 = db.components().insertPrivateProject(org1);
    db.users().insertPermissionOnAnyone(org1, "perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm4", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm5", project2);

    List<GroupPermissionDto> result = new ArrayList<>();
    underTest.selectAllPermissionsByGroupId(dbSession, org1.getUuid(), group1.getId(), context -> result.add((GroupPermissionDto) context.getResultObject()));
    assertThat(result).extracting(GroupPermissionDto::getResourceId, GroupPermissionDto::getRole).containsOnly(
      tuple(null, "perm2"),
      tuple(project1.getId(), "perm3"), tuple(project1.getId(), "perm4"), tuple(project2.getId(), "perm5"));
  }

  @Test
  public void selectGroupIdsWithPermissionOnProjectBut_returns_empty_if_project_does_not_exist() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = randomPublicOrPrivateProject(organization);
    GroupDto group = db.users().insertGroup(organization);
    db.users().insertProjectPermissionOnGroup(group, "foo", project);

    assertThat(underTest.selectGroupIdsWithPermissionOnProjectBut(dbSession, 1234, UserRole.USER))
      .isEmpty();
  }

  @Test
  public void selectGroupIdsWithPermissionOnProjectBut_returns_only_groups_of_project_which_do_not_have_permission() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = randomPublicOrPrivateProject(organization);
    GroupDto group1 = db.users().insertGroup(organization);
    GroupDto group2 = db.users().insertGroup(organization);
    db.users().insertProjectPermissionOnGroup(group1, "p1", project);
    db.users().insertProjectPermissionOnGroup(group2, "p2", project);

    assertThat(underTest.selectGroupIdsWithPermissionOnProjectBut(dbSession, project.getId(), "p2"))
      .containsOnly(group1.getId());
    assertThat(underTest.selectGroupIdsWithPermissionOnProjectBut(dbSession, project.getId(), "p1"))
      .containsOnly(group2.getId());
    assertThat(underTest.selectGroupIdsWithPermissionOnProjectBut(dbSession, project.getId(), "p3"))
      .containsOnly(group1.getId(), group2.getId());
  }

  @Test
  public void selectGroupIdsWithPermissionOnProjectBut_does_not_returns_group_AnyOne_of_project_when_it_does_not_have_permission() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    GroupDto group1 = db.users().insertGroup(organization);
    GroupDto group2 = db.users().insertGroup(organization);
    db.users().insertProjectPermissionOnGroup(group1, "p1", project);
    db.users().insertProjectPermissionOnGroup(group2, "p2", project);
    db.users().insertProjectPermissionOnAnyone("p2", project);

    assertThat(underTest.selectGroupIdsWithPermissionOnProjectBut(dbSession, project.getId(), "p2"))
      .containsOnly(group1.getId());
    assertThat(underTest.selectGroupIdsWithPermissionOnProjectBut(dbSession, project.getId(), "p1"))
      .containsOnly(group2.getId());
  }

  @Test
  public void selectGroupIdsWithPermissionOnProjectBut_does_not_return_groups_which_have_no_permission_at_all_on_specified_project() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = randomPublicOrPrivateProject(organization);
    GroupDto group1 = db.users().insertGroup(organization);
    GroupDto group2 = db.users().insertGroup(organization);
    GroupDto group3 = db.users().insertGroup(organization);
    db.users().insertProjectPermissionOnGroup(group1, "p1", project);
    db.users().insertProjectPermissionOnGroup(group2, "p2", project);

    assertThat(underTest.selectGroupIdsWithPermissionOnProjectBut(dbSession, project.getId(), "p2"))
      .containsOnly(group1.getId());
    assertThat(underTest.selectGroupIdsWithPermissionOnProjectBut(dbSession, project.getId(), "p1"))
      .containsOnly(group2.getId());
  }

  @Test
  public void deleteByRootComponentId_on_private_project() {
    OrganizationDto org = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org);
    GroupDto group2 = db.users().insertGroup(org);
    ComponentDto project1 = db.components().insertPrivateProject(org);
    ComponentDto project2 = db.components().insertPrivateProject(org);
    db.users().insertPermissionOnGroup(group1, "perm1");
    db.users().insertProjectPermissionOnGroup(group1, "perm2", project1);
    db.users().insertProjectPermissionOnGroup(group2, "perm3", project2);

    underTest.deleteByRootComponentId(dbSession, project1.getId());
    dbSession.commit();

    assertThat(db.countSql("select count(id) from group_roles where resource_id=" + project1.getId())).isEqualTo(0);
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(2);
  }

  @Test
  public void deleteByRootComponentId_on_public_project() {
    OrganizationDto org = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org);
    GroupDto group2 = db.users().insertGroup(org);
    ComponentDto project1 = db.components().insertPublicProject(org);
    ComponentDto project2 = db.components().insertPublicProject(org);
    db.users().insertPermissionOnGroup(group1, "perm1");
    db.users().insertProjectPermissionOnGroup(group1, "perm2", project1);
    db.users().insertProjectPermissionOnGroup(group2, "perm3", project2);
    db.users().insertProjectPermissionOnAnyone("perm4", project1);
    db.users().insertProjectPermissionOnAnyone("perm5", project2);

    underTest.deleteByRootComponentId(dbSession, project1.getId());
    dbSession.commit();

    assertThat(db.countSql("select count(id) from group_roles where resource_id=" + project1.getId())).isEqualTo(0);
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(3);
  }

  @Test
  public void delete_global_permission_from_group_on_public_project() {
    OrganizationDto org = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org);
    ComponentDto project1 = db.components().insertPublicProject(org);
    db.users().insertPermissionOnAnyone(org, "perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnAnyone("perm4", project1);

    underTest.delete(dbSession, "perm2", group1.getOrganizationUuid(), group1.getId(), null);
    dbSession.commit();

    assertThatNoPermission("perm2");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(3);
  }

  @Test
  public void delete_global_permission_from_group_on_private_project() {
    OrganizationDto org = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org);
    ComponentDto project1 = db.components().insertPrivateProject(org);
    db.users().insertPermissionOnAnyone(org, "perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);

    underTest.delete(dbSession, "perm2", group1.getOrganizationUuid(), group1.getId(), null);
    dbSession.commit();

    assertThatNoPermission("perm2");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(2);
  }

  @Test
  public void delete_global_permission_from_anyone_on_public_project() {
    OrganizationDto org = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org);
    ComponentDto project1 = db.components().insertPublicProject(org);
    db.users().insertPermissionOnAnyone(org, "perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnAnyone("perm4", project1);

    underTest.delete(dbSession, "perm1", group1.getOrganizationUuid(), null, null);
    dbSession.commit();

    assertThatNoPermission("perm1");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(3);
  }

  @Test
  public void delete_project_permission_from_group_on_private_project() {
    OrganizationDto org = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org);
    ComponentDto project1 = db.components().insertPrivateProject(org);
    db.users().insertPermissionOnAnyone(org, "perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);

    underTest.delete(dbSession, "perm3", group1.getOrganizationUuid(), group1.getId(), project1.getId());
    dbSession.commit();

    assertThatNoPermission("perm3");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(2);
  }

  @Test
  public void delete_project_permission_from_group_on_public_project() {
    OrganizationDto org = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org);
    ComponentDto project1 = db.components().insertPublicProject(org);
    db.users().insertPermissionOnAnyone(org, "perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnAnyone("perm4", project1);

    underTest.delete(dbSession, "perm3", group1.getOrganizationUuid(), group1.getId(), project1.getId());
    dbSession.commit();

    assertThatNoPermission("perm3");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(3);
  }

  @Test
  public void delete_project_permission_from_anybody_on_private_project() {
    OrganizationDto org = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org);
    ComponentDto project1 = db.components().insertPublicProject(org);
    db.users().insertPermissionOnAnyone(org, "perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnAnyone("perm4", project1);

    underTest.delete(dbSession, "perm4", group1.getOrganizationUuid(), null, project1.getId());
    dbSession.commit();

    assertThatNoPermission("perm4");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(3);
  }

  @Test
  public void deleteByOrganization_does_not_fail_on_empty_db() {
    underTest.deleteByOrganization(dbSession, "some uuid");
    dbSession.commit();
  }

  @Test
  public void deleteByOrganization_does_not_fail_if_organization_has_no_group() {
    OrganizationDto organization = db.organizations().insert();

    underTest.deleteByOrganization(dbSession, organization.getUuid());
    dbSession.commit();
  }

  @Test
  public void deleteByOrganization_deletes_all_groups_of_organization() {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    OrganizationDto organization3 = db.organizations().insert();
    insertGroupWithPermissions(organization1);
    insertGroupWithPermissions(organization2);
    insertGroupWithPermissions(organization3);
    insertGroupWithPermissions(organization3);
    insertGroupWithPermissions(organization2);
    db.users().insertPermissionOnAnyone(organization1, "pop");
    db.users().insertPermissionOnAnyone(organization2, "pop");
    db.users().insertPermissionOnAnyone(organization3, "pop");

    underTest.deleteByOrganization(dbSession, organization2.getUuid());
    dbSession.commit();
    verifyOrganizationUuidsInTable(organization1.getUuid(), organization3.getUuid());

    underTest.deleteByOrganization(dbSession, organization1.getUuid());
    dbSession.commit();
    verifyOrganizationUuidsInTable(organization3.getUuid());

    underTest.deleteByOrganization(dbSession, organization3.getUuid());
    dbSession.commit();
    verifyOrganizationUuidsInTable();
  }

  @Test
  public void deleteByRootComponentIdAndGroupId_deletes_all_permissions_of_group_AnyOne_of_specified_component_if_groupId_is_null() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    GroupDto group = db.users().insertGroup(organization);
    db.users().insertProjectPermissionOnAnyone("p1", project);
    db.users().insertProjectPermissionOnGroup(group, "p2", project);
    db.users().insertPermissionOnAnyone(organization, "p3");
    db.users().insertPermissionOnGroup(group, "p4");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), null, project.getId()))
      .containsOnly("p1");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), group.getId(), project.getId()))
      .containsOnly("p2");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, organization.getUuid(), null))
      .containsOnly("p3");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, organization.getUuid(), group.getId()))
      .containsOnly("p4");

    int deletedCount = underTest.deleteByRootComponentIdAndGroupId(dbSession, project.getId(), null);

    assertThat(deletedCount).isEqualTo(1);
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), null, project.getId()))
      .isEmpty();
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), group.getId(), project.getId()))
      .containsOnly("p2");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, organization.getUuid(), null))
      .containsOnly("p3");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, organization.getUuid(), group.getId()))
      .containsOnly("p4");
  }

  @Test
  public void deleteByRootComponentIdAndGroupId_deletes_all_permissions_of_specified_group_of_specified_component_if_groupId_is_non_null() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    GroupDto group1 = db.users().insertGroup(organization);
    GroupDto group2 = db.users().insertGroup(organization);
    db.users().insertProjectPermissionOnAnyone("p1", project);
    db.users().insertProjectPermissionOnGroup(group1, "p2", project);
    db.users().insertProjectPermissionOnGroup(group2, "p3", project);
    db.users().insertProjectPermissionOnGroup(group2, "p4", project);
    db.users().insertPermissionOnAnyone(organization, "p5");
    db.users().insertPermissionOnGroup(group1, "p6");
    db.users().insertPermissionOnGroup(group2, "p7");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), null, project.getId()))
      .containsOnly("p1");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), group1.getId(), project.getId()))
      .containsOnly("p2");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), group2.getId(), project.getId()))
      .containsOnly("p3", "p4");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, organization.getUuid(), null))
      .containsOnly("p5");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, organization.getUuid(), group1.getId()))
      .containsOnly("p6");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, organization.getUuid(), group2.getId()))
      .containsOnly("p7");

    int deletedCount = underTest.deleteByRootComponentIdAndGroupId(dbSession, project.getId(), group1.getId());

    assertThat(deletedCount).isEqualTo(1);
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), null, project.getId()))
      .containsOnly("p1");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), group1.getId(), project.getId()))
      .isEmpty();
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), group2.getId(), project.getId()))
      .containsOnly("p3", "p4");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, organization.getUuid(), group1.getId()))
      .containsOnly("p6");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, organization.getUuid(), group2.getId()))
      .containsOnly("p7");

    deletedCount = underTest.deleteByRootComponentIdAndGroupId(dbSession, project.getId(), group2.getId());

    assertThat(deletedCount).isEqualTo(2);
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), null, project.getId()))
      .containsOnly("p1");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), group1.getId(), project.getId()))
      .isEmpty();
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), group2.getId(), project.getId()))
      .isEmpty();
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, organization.getUuid(), group1.getId()))
      .containsOnly("p6");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, organization.getUuid(), group2.getId()))
      .containsOnly("p7");
  }

  @Test
  public void deleteByRootComponentIdAndGroupId_has_no_effect_if_component_does_not_exist() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto group = db.users().insertGroup(organization);

    assertThat(underTest.deleteByRootComponentIdAndGroupId(dbSession, 1234L, null)).isEqualTo(0);
    assertThat(underTest.deleteByRootComponentIdAndGroupId(dbSession, 1234L, group.getId())).isEqualTo(0);
  }

  @Test
  public void deleteByRootComponentIdAndGroupId_has_no_effect_if_component_has_no_group_permission_at_all() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = randomPublicOrPrivateProject(organization);
    GroupDto group = db.users().insertGroup(organization);

    assertThat(underTest.deleteByRootComponentIdAndGroupId(dbSession, project.getId(), null)).isEqualTo(0);
    assertThat(underTest.deleteByRootComponentIdAndGroupId(dbSession, project.getId(), group.getId())).isEqualTo(0);
  }

  @Test
  public void deleteByRootComponentIdAndGroupId_has_no_effect_if_group_does_not_exist() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = randomPublicOrPrivateProject(organization);

    assertThat(underTest.deleteByRootComponentIdAndGroupId(dbSession, project.getId(), 5678)).isEqualTo(0);
  }

  @Test
  public void deleteByRootComponentIdAndGroupId_has_no_effect_if_component_has_no_group_permission_for_group_AnyOne() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    GroupDto group1 = db.users().insertGroup(organization);
    db.users().insertProjectPermissionOnGroup(group1, "p1", project);
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), null, project.getId()))
      .isEmpty();
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), group1.getId(), project.getId()))
      .containsOnly("p1");
    db.users().insertPermissionOnAnyone(organization, "p2");
    db.users().insertPermissionOnGroup(group1, "p3");

    int deletedCount = underTest.deleteByRootComponentIdAndGroupId(dbSession, project.getId(), null);

    assertThat(deletedCount).isEqualTo(0);
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), null, project.getId()))
      .isEmpty();
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), group1.getId(), project.getId()))
      .containsOnly("p1");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, organization.getUuid(), null))
      .containsOnly("p2");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, organization.getUuid(), group1.getId()))
      .containsOnly("p3");
  }

  @Test
  public void deleteByRootComponentIdAndGroupId_has_no_effect_if_component_has_no_group_permission_for_specified_group() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    GroupDto group1 = db.users().insertGroup(organization);
    GroupDto group2 = db.users().insertGroup(organization);
    db.users().insertProjectPermissionOnGroup(group1, "p1", project);
    db.users().insertPermissionOnAnyone(organization, "p2");
    db.users().insertPermissionOnGroup(group1, "p3");

    int deletedCount = underTest.deleteByRootComponentIdAndGroupId(dbSession, project.getId(), group2.getId());

    assertThat(deletedCount).isEqualTo(0);
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), group1.getId(), project.getId()))
      .containsOnly("p1");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), group2.getId(), project.getId()))
      .isEmpty();
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, organization.getUuid(), null))
      .containsOnly("p2");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, organization.getUuid(), group1.getId()))
      .containsOnly("p3");
  }

  @Test
  public void deleteByRootComponentIdAndPermission_deletes_all_rows_for_specified_role_of_specified_component() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    GroupDto group = db.users().insertGroup(organization);
    Stream.of("p1", "p2").forEach(permission -> {
      db.users().insertPermissionOnAnyone(organization, permission);
      db.users().insertPermissionOnGroup(group, permission);
      db.users().insertProjectPermissionOnGroup(group, permission, project);
      db.users().insertProjectPermissionOnAnyone(permission, project);
    });
    assertThat(getGlobalPermissionsForAnyone(organization)).containsOnly("p1", "p2");
    assertThat(getGlobalPermissionsForGroup(group)).containsOnly("p1", "p2");
    assertThat(getProjectPermissionsForAnyOne(project)).containsOnly("p1", "p2");
    assertThat(getProjectPermissionsForGroup(project, group)).containsOnly("p1", "p2");

    int deletedRows = underTest.deleteByRootComponentIdAndPermission(dbSession, project.getId(), "p1");

    assertThat(deletedRows).isEqualTo(2);
    assertThat(getGlobalPermissionsForAnyone(organization)).containsOnly("p1", "p2");
    assertThat(getGlobalPermissionsForGroup(group)).containsOnly("p1", "p2");
    assertThat(getProjectPermissionsForAnyOne(project)).containsOnly("p2");
    assertThat(getProjectPermissionsForGroup(project, group)).containsOnly("p2");

    deletedRows = underTest.deleteByRootComponentIdAndPermission(dbSession, project.getId(), "p2");

    assertThat(deletedRows).isEqualTo(2);
    assertThat(getGlobalPermissionsForAnyone(organization)).containsOnly("p1", "p2");
    assertThat(getGlobalPermissionsForGroup(group)).containsOnly("p1", "p2");
    assertThat(getProjectPermissionsForAnyOne(project)).isEmpty();
    assertThat(getProjectPermissionsForGroup(project, group)).isEmpty();
  }

  @Test
  public void deleteByRootComponentIdAndPermission_has_no_effect_if_component_has_no_group_permission_at_all() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto group = db.users().insertGroup(organization);
    ComponentDto project = randomPublicOrPrivateProject(organization);
    db.users().insertPermissionOnAnyone(organization, "p1");
    db.users().insertPermissionOnGroup(group, "p1");

    assertThat(underTest.deleteByRootComponentIdAndPermission(dbSession, project.getId(), "p1")).isEqualTo(0);

    assertThat(getGlobalPermissionsForAnyone(organization)).containsOnly("p1");
    assertThat(getGlobalPermissionsForGroup(group)).containsOnly("p1");
    assertThat(getProjectPermissionsForAnyOne(project)).isEmpty();
    assertThat(getProjectPermissionsForGroup(project, group)).isEmpty();
  }

  @Test
  public void deleteByRootComponentIdAndPermission_has_no_effect_if_component_does_not_exist() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    GroupDto group = db.users().insertGroup(organization);
    db.users().insertPermissionOnAnyone(organization, "p1");
    db.users().insertPermissionOnGroup(group, "p1");
    db.users().insertProjectPermissionOnGroup(group, "p1", project);
    db.users().insertProjectPermissionOnAnyone("p1", project);

    assertThat(underTest.deleteByRootComponentIdAndPermission(dbSession, 1324, "p1")).isEqualTo(0);

    assertThat(getGlobalPermissionsForAnyone(organization)).containsOnly("p1");
    assertThat(getGlobalPermissionsForGroup(group)).containsOnly("p1");
    assertThat(getProjectPermissionsForAnyOne(project)).containsOnly("p1");
    assertThat(getProjectPermissionsForGroup(project, group)).containsOnly("p1");
  }

  @Test
  public void deleteByRootComponentIdAndPermission_has_no_effect_if_component_does_not_have_specified_permission() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto group = db.users().insertGroup(organization);
    ComponentDto project = randomPublicOrPrivateProject(organization);
    db.users().insertPermissionOnAnyone(organization, "p1");
    db.users().insertPermissionOnGroup(group, "p1");

    assertThat(underTest.deleteByRootComponentIdAndPermission(dbSession, project.getId(), "p1")).isEqualTo(0);
  }

  private Collection<String> getGlobalPermissionsForAnyone(OrganizationDto organization) {
    return getPermissions("organization_uuid = '" + organization.getUuid() + "' and group_id is null and resource_id is null");
  }

  private Collection<String> getGlobalPermissionsForGroup(GroupDto groupDto) {
    return getPermissions("organization_uuid = '" + groupDto.getOrganizationUuid() + "' and group_id = " + groupDto.getId() + " and resource_id is null");
  }

  private Collection<String> getProjectPermissionsForAnyOne(ComponentDto project) {
    return getPermissions("organization_uuid = '" + project.getOrganizationUuid() + "' and group_id is null and resource_id = " + project.getId());
  }

  private Collection<String> getProjectPermissionsForGroup(ComponentDto project, GroupDto group) {
    return getPermissions("organization_uuid = '" + project.getOrganizationUuid() + "' and group_id = " + group.getId() + " and resource_id = " + project.getId());
  }

  private Collection<String> getPermissions(String whereClauses) {
    return db
      .select(dbSession, "select role from group_roles where " + whereClauses)
      .stream()
      .flatMap(map -> map.entrySet().stream())
      .map(entry -> (String) entry.getValue())
      .collect(MoreCollectors.toList());
  }

  private ComponentDto randomPublicOrPrivateProject(OrganizationDto organization) {
    return new Random().nextBoolean() ? db.components().insertPublicProject(organization) : db.components().insertPrivateProject(organization);
  }

  private PermissionQuery.Builder newQuery() {
    return PermissionQuery.builder().setOrganizationUuid(db.getDefaultOrganization().getUuid());
  }

  private void verifyOrganizationUuidsInTable(String... organizationUuids) {
    assertThat(db.select("select distinct organization_uuid as \"organizationUuid\" from group_roles"))
      .extracting((row) -> (String) row.get("organizationUuid"))
      .containsOnly(organizationUuids);
  }

  private int insertGroupWithPermissions(OrganizationDto organization1) {
    GroupDto group = db.users().insertGroup(organization1);
    db.users().insertPermissionOnGroup(group, "foo");
    db.users().insertPermissionOnGroup(group, "bar");
    db.users().insertPermissionOnGroup(group, "doh");
    return group.getId();
  }

  private void assertThatNoPermission(String permission) {
    assertThat(db.countSql("select count(id) from group_roles where role='" + permission + "'")).isEqualTo(0);
  }

}
