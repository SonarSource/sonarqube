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
package org.sonar.db.permission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.OrganizationPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.OrganizationPermission.SCAN;

public class UserPermissionDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbSession dbSession = db.getSession();
  private UserPermissionDao underTest = new UserPermissionDao();

  @Test
  public void select_global_permissions() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();
    UserDto user1 = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"), organization, org2);
    UserDto user2 = insertUser(u -> u.setLogin("login2").setName("Marie").setEmail("email2@email.com"), organization, org2);
    UserDto user3 = insertUser(u -> u.setLogin("zanother").setName("Zoe").setEmail("zanother3@another.com"), organization);
    ComponentDto project = db.components().insertPrivateProject(organization);
    UserPermissionDto global1 = addGlobalPermission(organization, SYSTEM_ADMIN, user1);
    UserPermissionDto global2 = addGlobalPermission(organization, SYSTEM_ADMIN, user2);
    UserPermissionDto global3 = addGlobalPermission(organization, PROVISIONING, user2);
    UserPermissionDto project1Perm = addProjectPermission(organization, USER, user3, project);
    // permissions on another organization, to be excluded
    UserPermissionDto org2Global1 = addGlobalPermission(org2, SYSTEM_ADMIN, user1);
    UserPermissionDto org2Global2 = addGlobalPermission(org2, PROVISIONING, user2);

    // global permissions of users who has at least one global permission, ordered by user name then permission
    PermissionQuery query = PermissionQuery.builder().setOrganizationUuid(organization.getUuid()).withAtLeastOnePermission().build();
    expectPermissions(query, asList(user2.getId(), user1.getId()), global2, global3, global1);

    // default query returns all users, whatever their permissions nor organizations
    // (that's a non-sense, but still this is required for api/permissions/groups
    // when filtering users by name)
    query = PermissionQuery.builder().setOrganizationUuid(organization.getUuid()).build();
    expectPermissions(query, asList(user2.getId(), user1.getId(), user3.getId()), global2, global3, org2Global2, global1, org2Global1, project1Perm);

    // global permissions "admin"
    query = PermissionQuery.builder().setOrganizationUuid(organization.getUuid()).setPermission(SYSTEM_ADMIN).build();
    expectPermissions(query, asList(user2.getId(), user1.getId()), global2, global1);

    // empty if nobody has the specified global permission
    query = PermissionQuery.builder().setOrganizationUuid(organization.getUuid()).setPermission("missing").build();
    expectPermissions(query, emptyList());

    // search by user name (matches 2 users)
    query = PermissionQuery.builder().setOrganizationUuid(organization.getUuid()).withAtLeastOnePermission().setSearchQuery("mari").build();
    expectPermissions(query, asList(user2.getId(), user1.getId()), global2, global3, global1);

    // search by user login
    query = PermissionQuery.builder().setOrganizationUuid(organization.getUuid()).withAtLeastOnePermission().setSearchQuery("ogin2").build();
    expectPermissions(query, singletonList(user2.getId()), global2, global3);

    // search by user email
    query = PermissionQuery.builder().setOrganizationUuid(organization.getUuid()).withAtLeastOnePermission().setSearchQuery("mail2").build();
    expectPermissions(query, singletonList(user2.getId()), global2, global3);

    // search by user name (matches 2 users) and global permission
    query = PermissionQuery.builder().setOrganizationUuid(organization.getUuid()).setSearchQuery("Mari").setPermission(PROVISIONING).build();
    expectPermissions(query, singletonList(user2.getId()), global3);

    // search by user name (no match)
    query = PermissionQuery.builder().setOrganizationUuid(organization.getUuid()).setSearchQuery("Unknown").build();
    expectPermissions(query, emptyList());
  }

  @Test
  public void select_project_permissions() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user1 = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"), organization);
    UserDto user2 = insertUser(u -> u.setLogin("login2").setName("Marie").setEmail("email2@email.com"), organization);
    UserDto user3 = insertUser(u -> u.setLogin("zanother").setName("Zoe").setEmail("zanother3@another.com"), organization);
    addGlobalPermission(organization, SYSTEM_ADMIN, user1);
    ComponentDto project1 = db.components().insertPrivateProject(organization);
    ComponentDto project2 = db.components().insertPrivateProject(organization);
    UserPermissionDto perm1 = addProjectPermission(organization, USER, user1, project1);
    UserPermissionDto perm2 = addProjectPermission(organization, ISSUE_ADMIN, user1, project1);
    UserPermissionDto perm3 = addProjectPermission(organization, ISSUE_ADMIN, user2, project1);
    addProjectPermission(organization, ISSUE_ADMIN, user3, project2);

    // project permissions of users who has at least one permission on this project
    PermissionQuery query = PermissionQuery.builder().setOrganizationUuid(organization.getUuid()).withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(query, asList(user2.getId(), user1.getId()), perm3, perm2, perm1);

    // empty if nobody has the specified global permission
    query = PermissionQuery.builder().setOrganizationUuid(organization.getUuid()).setPermission("missing").setComponentUuid(project1.uuid()).build();
    expectPermissions(query, emptyList());

    // search by user name (matches 2 users), users with at least one permission
    query = PermissionQuery.builder().setOrganizationUuid(organization.getUuid()).setSearchQuery("Mari").withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(query, asList(user2.getId(), user1.getId()), perm3, perm2, perm1);

    // search by user name (matches 2 users) and project permission
    query = PermissionQuery.builder().setOrganizationUuid(organization.getUuid()).setSearchQuery("Mari").setPermission(ISSUE_ADMIN).setComponentUuid(project1.uuid()).build();
    expectPermissions(query, asList(user2.getId(), user1.getId()), perm3, perm2);

    // search by user name (no match)
    query = PermissionQuery.builder().setOrganizationUuid(organization.getUuid()).setSearchQuery("Unknown").setComponentUuid(project1.uuid()).build();
    expectPermissions(query, emptyList());

    // permissions of unknown project
    query = PermissionQuery.builder().setOrganizationUuid(organization.getUuid()).setComponentUuid("missing").withAtLeastOnePermission().build();
    expectPermissions(query, emptyList());
  }

  @Test
  public void countUsersByProjectPermission() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user1 = insertUser(organization);
    UserDto user2 = insertUser(organization);
    ComponentDto project1 = db.components().insertPrivateProject(organization);
    ComponentDto project2 = db.components().insertPrivateProject(organization);
    addGlobalPermission(organization, SYSTEM_ADMIN, user1);
    addProjectPermission(organization, USER, user1, project1);
    addProjectPermission(organization, ISSUE_ADMIN, user1, project1);
    addProjectPermission(organization, ISSUE_ADMIN, user2, project1);
    addProjectPermission(organization, ISSUE_ADMIN, user2, project2);

    // no projects -> return empty list
    assertThat(underTest.countUsersByProjectPermission(dbSession, emptyList())).isEmpty();

    // one project
    expectCount(singletonList(project1.getId()),
      new CountPerProjectPermission(project1.getId(), USER, 1),
      new CountPerProjectPermission(project1.getId(), ISSUE_ADMIN, 2));

    // multiple projects
    expectCount(asList(project1.getId(), project2.getId(), -1L),
      new CountPerProjectPermission(project1.getId(), USER, 1),
      new CountPerProjectPermission(project1.getId(), ISSUE_ADMIN, 2),
      new CountPerProjectPermission(project2.getId(), ISSUE_ADMIN, 1));
  }

  @Test
  public void selectUserIdsByQuery() {
    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();
    UserDto user1 = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"), org1, org2);
    UserDto user2 = insertUser(u -> u.setLogin("login2").setName("Marie").setEmail("email2@email.com"), org1, org2);
    ComponentDto project1 = db.components().insertPrivateProject(org1);
    ComponentDto project2 = db.components().insertPrivateProject(org2);
    addProjectPermission(org1, USER, user1, project1);
    addProjectPermission(org1, USER, user2, project1);
    addProjectPermission(org2, USER, user1, project2);
    addProjectPermission(org1, ISSUE_ADMIN, user2, project1);
    addProjectPermission(org2, ISSUE_ADMIN, user2, project2);

    // logins are ordered by user name: user2 ("Marie") then user1 ("Marius")
    PermissionQuery query = PermissionQuery.builder().setOrganizationUuid(project1.getOrganizationUuid()).setComponentUuid(project1.uuid()).withAtLeastOnePermission().build();
    assertThat(underTest.selectUserIdsByQuery(dbSession, query)).containsExactly(user2.getId(), user1.getId());
    query = PermissionQuery.builder().setOrganizationUuid("anotherOrg").setComponentUuid(project1.uuid()).withAtLeastOnePermission().build();
    assertThat(underTest.selectUserIdsByQuery(dbSession, query)).isEmpty();

    // on a project without permissions
    query = PermissionQuery.builder().setOrganizationUuid(org1.getUuid()).setComponentUuid("missing").withAtLeastOnePermission().build();
    assertThat(underTest.selectUserIdsByQuery(dbSession, query)).isEmpty();

    // search all users whose name matches "mar", whatever the permissions
    query = PermissionQuery.builder().setOrganizationUuid(org1.getUuid()).setSearchQuery("mar").build();
    assertThat(underTest.selectUserIdsByQuery(dbSession, query)).containsExactly(user2.getId(), user1.getId());

    // search all users whose name matches "mariu", whatever the permissions
    query = PermissionQuery.builder().setOrganizationUuid(org1.getUuid()).setSearchQuery("mariu").build();
    assertThat(underTest.selectUserIdsByQuery(dbSession, query)).containsExactly(user1.getId());

    // search all users whose name matches "mariu", whatever the permissions
    query = PermissionQuery.builder().setOrganizationUuid(org1.getUuid()).setSearchQuery("mariu").setComponentUuid(project1.uuid()).build();
    assertThat(underTest.selectUserIdsByQuery(dbSession, query)).containsExactly(user1.getId());

    // search all users whose name matches "mariu", whatever the organization
    query = PermissionQuery.builder().setOrganizationUuid("missingOrg").setSearchQuery("mariu").build();
    assertThat(underTest.selectUserIdsByQuery(dbSession, query)).isEmpty();
  }

  @Test
  public void selectUserIdsByQuery_is_paginated() {
    OrganizationDto organization = db.organizations().insert();
    List<Integer> userIds = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      String name = "user-" + i;
      UserDto user = insertUser(u -> u.setName(name), organization);
      addGlobalPermission(organization, PROVISIONING, user);
      addGlobalPermission(organization, SYSTEM_ADMIN, user);
      userIds.add(user.getId());
    }

    assertThat(underTest.selectUserIdsByQuery(dbSession, PermissionQuery.builder().setOrganizationUuid(organization.getUuid())
      .setPageSize(3).setPageIndex(1).build()))
        .containsExactly(userIds.get(0), userIds.get(1), userIds.get(2));
    assertThat(underTest.selectUserIdsByQuery(dbSession, PermissionQuery.builder().setOrganizationUuid(organization.getUuid())
      .setPageSize(2).setPageIndex(3).build()))
        .containsExactly(userIds.get(4), userIds.get(5));
    assertThat(underTest.selectUserIdsByQuery(dbSession, PermissionQuery.builder().setOrganizationUuid(organization.getUuid())
      .setPageSize(50).setPageIndex(1).build()))
        .hasSize(10);
  }

  @Test
  public void selectUserIdsByQuery_is_sorted_by_insensitive_name() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user1 = insertUser(u -> u.setName("user1"), organization);
    addGlobalPermission(organization, PROVISIONING, user1);
    UserDto user3 = insertUser(u -> u.setName("user3"), organization);
    addGlobalPermission(organization, SYSTEM_ADMIN, user3);
    UserDto user2 = insertUser(u -> u.setName("User2"), organization);
    addGlobalPermission(organization, PROVISIONING, user2);

    assertThat(underTest.selectUserIdsByQuery(dbSession, PermissionQuery.builder().setOrganizationUuid(organization.getUuid()).build()))
      .containsExactly(user1.getId(), user2.getId(), user3.getId());
  }

  @Test
  public void deleteGlobalPermission() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user1 = insertUser(organization);
    UserDto user2 = insertUser(organization);
    ComponentDto project1 = db.components().insertPrivateProject(organization);
    ComponentDto project2 = db.components().insertPrivateProject(organization);
    addGlobalPermission(organization, "perm1", user1);
    addGlobalPermission(organization, "perm2", user1);
    addProjectPermission(organization, "perm1", user1, project1);
    addProjectPermission(organization, "perm3", user2, project1);
    addProjectPermission(organization, "perm4", user2, project2);

    // user2 does not have global permissions -> do nothing
    underTest.deleteGlobalPermission(dbSession, user2.getId(), "perm1", db.getDefaultOrganization().getUuid());
    assertThat(db.countRowsOfTable(dbSession, "user_roles")).isEqualTo(5);

    // global permission is not granted -> do nothing
    underTest.deleteGlobalPermission(dbSession, user1.getId(), "notGranted", db.getDefaultOrganization().getUuid());
    assertThat(db.countRowsOfTable(dbSession, "user_roles")).isEqualTo(5);

    // permission is on project -> do nothing
    underTest.deleteGlobalPermission(dbSession, user1.getId(), "perm3", db.getDefaultOrganization().getUuid());
    assertThat(db.countRowsOfTable(dbSession, "user_roles")).isEqualTo(5);

    // global permission on another organization-> do nothing
    underTest.deleteGlobalPermission(dbSession, user1.getId(), "notGranted", "anotherOrg");
    assertThat(db.countRowsOfTable(dbSession, "user_roles")).isEqualTo(5);

    // global permission exists -> delete it, but not the project permission with the same name !
    underTest.deleteGlobalPermission(dbSession, user1.getId(), "perm1", organization.getUuid());
    assertThat(db.countSql(dbSession, "select count(id) from user_roles where role='perm1' and resource_id is null")).isEqualTo(0);
    assertThat(db.countRowsOfTable(dbSession, "user_roles")).isEqualTo(4);
  }

  @Test
  public void deleteProjectPermission() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user1 = insertUser(organization);
    UserDto user2 = insertUser(organization);
    ComponentDto project1 = db.components().insertPrivateProject(organization);
    ComponentDto project2 = db.components().insertPrivateProject(organization);
    addGlobalPermission(organization, "perm", user1);
    addProjectPermission(organization, "perm", user1, project1);
    addProjectPermission(organization, "perm", user1, project2);
    addProjectPermission(organization, "perm", user2, project1);

    // no such provision -> ignore
    underTest.deleteProjectPermission(dbSession, user1.getId(), "anotherPerm", project1.getId());
    assertThat(db.countRowsOfTable(dbSession, "user_roles")).isEqualTo(4);

    underTest.deleteProjectPermission(dbSession, user1.getId(), "perm", project1.getId());
    assertThatProjectPermissionDoesNotExist(user1, "perm", project1);
    assertThat(db.countRowsOfTable(dbSession, "user_roles")).isEqualTo(3);
  }

  @Test
  public void deleteProjectPermissions() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user1 = insertUser(organization);
    UserDto user2 = insertUser(organization);
    ComponentDto project1 = db.components().insertPrivateProject(organization);
    ComponentDto project2 = db.components().insertPrivateProject(organization);
    addGlobalPermission(organization, "perm", user1);
    addProjectPermission(organization, "perm", user1, project1);
    addProjectPermission(organization, "perm", user2, project1);
    addProjectPermission(organization, "perm", user1, project2);

    underTest.deleteProjectPermissions(dbSession, project1.getId());
    assertThat(db.countRowsOfTable(dbSession, "user_roles")).isEqualTo(2);
    assertThatProjectHasNoPermissions(project1);
  }

  @Test
  public void selectGlobalPermissionsOfUser() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user1 = insertUser(organization);
    UserDto user2 = insertUser(organization);
    UserDto user3 = insertUser(organization);
    OrganizationDto org = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    addGlobalPermission(db.getDefaultOrganization(), "perm1", user1);
    addGlobalPermission(org, "perm2", user2);
    addGlobalPermission(org, "perm3", user1);
    addProjectPermission(organization, "perm4", user1, project);
    addProjectPermission(organization, "perm5", user1, project);

    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user1.getId(), org.getUuid())).containsOnly("perm3");
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user1.getId(), db.getDefaultOrganization().getUuid())).containsOnly("perm1");
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user1.getId(), "otherOrg")).isEmpty();
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user3.getId(), org.getUuid())).isEmpty();
  }

  @Test
  public void selectProjectPermissionsOfUser() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user1 = insertUser(organization);
    UserDto user2 = insertUser(organization);
    ComponentDto project1 = db.components().insertPrivateProject(organization);
    ComponentDto project2 = db.components().insertPrivateProject(organization);
    ComponentDto project3 = db.components().insertPrivateProject(organization);
    addGlobalPermission(organization, "perm1", user1);
    addProjectPermission(organization, "perm2", user1, project1);
    addProjectPermission(organization, "perm3", user1, project1);
    addProjectPermission(organization, "perm4", user1, project2);
    addProjectPermission(organization, "perm5", user2, project1);

    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user1.getId(), project1.getId())).containsOnly("perm2", "perm3");
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user1.getId(), project2.getId())).containsOnly("perm4");
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user1.getId(), project3.getId())).isEmpty();
  }

  @Test
  public void selectGroupIdsWithPermissionOnProjectBut_returns_empty_if_project_does_not_exist() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = randomPublicOrPrivateProject(organization);
    UserDto user = insertUser(organization);
    db.users().insertProjectPermissionOnUser(user, "foo", project);

    assertThat(underTest.selectUserIdsWithPermissionOnProjectBut(dbSession, 1234, UserRole.USER))
      .isEmpty();
  }

  @Test
  public void selectGroupIdsWithPermissionOnProjectBut_returns_only_users_of_projects_which_do_not_have_permission() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = randomPublicOrPrivateProject(organization);
    UserDto user1 = insertUser(organization);
    UserDto user2 = insertUser(organization);
    db.users().insertProjectPermissionOnUser(user1, "p1", project);
    db.users().insertProjectPermissionOnUser(user2, "p2", project);

    assertThat(underTest.selectUserIdsWithPermissionOnProjectBut(dbSession, project.getId(), "p2"))
      .containsOnly(user1.getId());
    assertThat(underTest.selectUserIdsWithPermissionOnProjectBut(dbSession, project.getId(), "p1"))
      .containsOnly(user2.getId());
    assertThat(underTest.selectUserIdsWithPermissionOnProjectBut(dbSession, project.getId(), "p3"))
      .containsOnly(user1.getId(), user2.getId());
  }

  @Test
  public void selectGroupIdsWithPermissionOnProjectBut_does_not_return_groups_which_have_no_permission_at_all_on_specified_project() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = randomPublicOrPrivateProject(organization);
    UserDto user1 = insertUser(organization);
    UserDto user2 = insertUser(organization);
    db.users().insertProjectPermissionOnUser(user1, "p1", project);
    db.users().insertProjectPermissionOnUser(user2, "p2", project);

    assertThat(underTest.selectUserIdsWithPermissionOnProjectBut(dbSession, project.getId(), "p2"))
      .containsOnly(user1.getId());
    assertThat(underTest.selectUserIdsWithPermissionOnProjectBut(dbSession, project.getId(), "p1"))
      .containsOnly(user2.getId());
  }

  @Test
  public void deleteByOrganization_does_not_fail_if_table_is_empty() {
    underTest.deleteByOrganization(dbSession, "some uuid");
    dbSession.commit();
  }

  @Test
  public void deleteByOrganization_does_not_fail_if_organization_has_no_user_permission() {
    OrganizationDto organization = db.organizations().insert();

    underTest.deleteByOrganization(dbSession, organization.getUuid());
    dbSession.commit();
  }

  @Test
  public void deleteByOrganization_deletes_all_user_permission_of_specified_organization() {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    OrganizationDto organization3 = db.organizations().insert();
    UserDto user1 = insertUser(organization1, organization2, organization3);
    UserDto user2 = insertUser(organization1, organization2, organization3);
    UserDto user3 = insertUser(organization1, organization2, organization3);
    db.users().insertPermissionOnUser(organization1, user1, "foo");
    db.users().insertPermissionOnUser(organization1, user2, "foo");
    db.users().insertPermissionOnUser(organization1, user2, "bar");
    db.users().insertPermissionOnUser(organization2, user2, "foo");
    db.users().insertPermissionOnUser(organization2, user3, "foo");
    db.users().insertPermissionOnUser(organization2, user3, "bar");
    db.users().insertPermissionOnUser(organization3, user3, "foo");
    db.users().insertPermissionOnUser(organization3, user1, "foo");
    db.users().insertPermissionOnUser(organization3, user1, "bar");

    underTest.deleteByOrganization(dbSession, organization3.getUuid());
    dbSession.commit();
    verifyOrganizationUuidsInTable(organization1.getUuid(), organization2.getUuid());

    underTest.deleteByOrganization(dbSession, organization2.getUuid());
    dbSession.commit();
    verifyOrganizationUuidsInTable(organization1.getUuid());

    underTest.deleteByOrganization(dbSession, organization1.getUuid());
    dbSession.commit();
    verifyOrganizationUuidsInTable();
  }

  @Test
  public void delete_permissions_of_an_organization_member() {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization1);
    UserDto user1 = insertUser(organization1, organization2);
    UserDto user2 = insertUser(organization1, organization2);
    // user 1 permissions
    db.users().insertPermissionOnUser(organization1, user1, SCAN);
    db.users().insertPermissionOnUser(organization1, user1, ADMINISTER);
    db.users().insertProjectPermissionOnUser(user1, UserRole.CODEVIEWER, project);
    db.users().insertPermissionOnUser(organization2, user1, SCAN);
    // user 2 permission
    db.users().insertPermissionOnUser(organization1, user2, SCAN);
    db.users().insertProjectPermissionOnUser(user2, UserRole.CODEVIEWER, project);

    underTest.deleteOrganizationMemberPermissions(dbSession, organization1.getUuid(), user1.getId());
    dbSession.commit();

    // user 1 permissions
    assertOrgPermissionsOfUser(user1, organization1);
    assertOrgPermissionsOfUser(user1, organization2, SCAN);
    assertProjectPermissionsOfUser(user1, project);
    // user 2 permissions
    assertOrgPermissionsOfUser(user2, organization1, SCAN);
    assertProjectPermissionsOfUser(user2, project, CODEVIEWER);
  }

  @Test
  public void deleteByUserId() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user1 = insertUser(organization);
    UserDto user2 = insertUser(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);
    db.users().insertPermissionOnUser(user1, SCAN);
    db.users().insertPermissionOnUser(user1, ADMINISTER);
    db.users().insertProjectPermissionOnUser(user1, ADMINISTER_QUALITY_GATES.getKey(), project);
    db.users().insertPermissionOnUser(user2, SCAN);
    db.users().insertProjectPermissionOnUser(user2, ADMINISTER_QUALITY_GATES.getKey(), project);

    underTest.deleteByUserId(dbSession, user1.getId());
    dbSession.commit();

    assertThat(db.select("select user_id as \"userId\", resource_id as \"projectId\", role as \"permission\" from user_roles"))
      .extracting((row) -> row.get("userId"), (row) -> row.get("projectId"), (row) -> row.get("permission"))
      .containsOnly(tuple(user2.getId().longValue(), null, SCAN.getKey()), tuple(user2.getId().longValue(), project.getId(), ADMINISTER_QUALITY_GATES.getKey()));
  }

  @Test
  public void deleteProjectPermissionOfAnyUser_has_no_effect_if_specified_component_does_not_exist() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = insertUser(organization);
    db.users().insertPermissionOnUser(organization, user, SCAN);

    int deletedCount = underTest.deleteProjectPermissionOfAnyUser(dbSession, 124L, SCAN.getKey());

    assertThat(deletedCount).isEqualTo(0);
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user.getId(), organization.getUuid())).containsOnly(SCAN.getKey());
  }

  @Test
  public void deleteProjectPermissionOfAnyUser_has_no_effect_if_specified_component_has_no_permission_at_all() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = insertUser(organization);
    db.users().insertPermissionOnUser(organization, user, SCAN);
    ComponentDto project = randomPublicOrPrivateProject(organization);

    int deletedCount = underTest.deleteProjectPermissionOfAnyUser(dbSession, project.getId(), SCAN.getKey());

    assertThat(deletedCount).isEqualTo(0);
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user.getId(), organization.getUuid())).containsOnly(SCAN.getKey());
  }

  @Test
  public void deleteProjectPermissionOfAnyUser_has_no_effect_if_specified_component_does_not_have_specified_permission() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = insertUser(organization);
    db.users().insertPermissionOnUser(organization, user, SCAN);
    ComponentDto project = randomPublicOrPrivateProject(organization);
    db.users().insertProjectPermissionOnUser(user, SCAN.getKey(), project);

    int deletedCount = underTest.deleteProjectPermissionOfAnyUser(dbSession, project.getId(), "p1");

    assertThat(deletedCount).isEqualTo(0);
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user.getId(), organization.getUuid())).containsOnly(SCAN.getKey());
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user.getId(), project.getId())).containsOnly(SCAN.getKey());
  }

  @Test
  public void deleteProjectPermissionOfAnyUser_deletes_specified_permission_for_any_user_on_the_specified_component() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user1 = insertUser(organization);
    UserDto user2 = insertUser(organization);
    db.users().insertPermissionOnUser(organization, user1, SCAN);
    db.users().insertPermissionOnUser(organization, user2, SCAN);
    ComponentDto project1 = randomPublicOrPrivateProject(organization);
    ComponentDto project2 = randomPublicOrPrivateProject(organization);
    db.users().insertProjectPermissionOnUser(user1, SCAN.getKey(), project1);
    db.users().insertProjectPermissionOnUser(user2, SCAN.getKey(), project1);
    db.users().insertProjectPermissionOnUser(user1, SCAN.getKey(), project2);
    db.users().insertProjectPermissionOnUser(user2, SCAN.getKey(), project2);
    db.users().insertProjectPermissionOnUser(user2, PROVISION_PROJECTS.getKey(), project2);

    int deletedCount = underTest.deleteProjectPermissionOfAnyUser(dbSession, project1.getId(), SCAN.getKey());

    assertThat(deletedCount).isEqualTo(2);
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user1.getId(), organization.getUuid())).containsOnly(SCAN.getKey());
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user2.getId(), organization.getUuid())).containsOnly(SCAN.getKey());
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user1.getId(), project1.getId())).isEmpty();
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user2.getId(), project1.getId())).isEmpty();
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user1.getId(), project2.getId())).containsOnly(SCAN.getKey());
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user2.getId(), project2.getId())).containsOnly(SCAN.getKey(), PROVISION_PROJECTS.getKey());

    deletedCount = underTest.deleteProjectPermissionOfAnyUser(dbSession, project2.getId(), SCAN.getKey());

    assertThat(deletedCount).isEqualTo(2);
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user1.getId(), organization.getUuid())).containsOnly(SCAN.getKey());
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user2.getId(), organization.getUuid())).containsOnly(SCAN.getKey());
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user1.getId(), project1.getId())).isEmpty();
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user2.getId(), project1.getId())).isEmpty();
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user1.getId(), project2.getId())).containsOnly();
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user2.getId(), project2.getId())).containsOnly(PROVISION_PROJECTS.getKey());
  }

  private ComponentDto randomPublicOrPrivateProject(OrganizationDto organization) {
    return new Random().nextBoolean() ? db.components().insertPrivateProject(organization) : db.components().insertPublicProject(organization);
  }

  private UserDto insertUser(Consumer<UserDto> populateUserDto, OrganizationDto... organizations) {
    UserDto user = db.users().insertUser(populateUserDto);
    stream(organizations).forEach(organization -> db.organizations().addMember(organization, user));
    return user;
  }

  private UserDto insertUser(OrganizationDto... organizations) {
    UserDto user = db.users().insertUser();
    stream(organizations).forEach(organization -> db.organizations().addMember(organization, user));
    return user;
  }

  private void verifyOrganizationUuidsInTable(String... organizationUuids) {
    assertThat(db.select("select organization_uuid as \"organizationUuid\" from user_roles"))
      .extracting((row) -> (String) row.get("organizationUuid"))
      .containsOnly(organizationUuids);
  }

  private void expectCount(List<Long> projectIds, CountPerProjectPermission... expected) {
    List<CountPerProjectPermission> got = underTest.countUsersByProjectPermission(dbSession, projectIds);
    assertThat(got).hasSize(expected.length);

    for (CountPerProjectPermission expect : expected) {
      boolean found = got.stream().anyMatch(b -> b.getPermission().equals(expect.getPermission()) &&
        b.getCount() == expect.getCount() &&
        b.getComponentId() == expect.getComponentId());
      assertThat(found).isTrue();
    }
  }

  private void expectPermissions(PermissionQuery query, Collection<Integer> expectedUserIds, UserPermissionDto... expectedPermissions) {
    assertThat(underTest.selectUserIdsByQuery(dbSession, query)).containsExactly(expectedUserIds.toArray(new Integer[0]));
    List<UserPermissionDto> currentPermissions = underTest.selectUserPermissionsByQuery(dbSession, query, expectedUserIds);
    assertThat(currentPermissions).hasSize(expectedPermissions.length);
    List<Tuple> expectedPermissionsAsTuple = Arrays.stream(expectedPermissions)
      .map(expectedPermission -> tuple(expectedPermission.getUserId(), expectedPermission.getPermission(), expectedPermission.getComponentId(),
        expectedPermission.getOrganizationUuid()))
      .collect(Collectors.toList());
    assertThat(currentPermissions)
      .extracting(UserPermissionDto::getUserId, UserPermissionDto::getPermission, UserPermissionDto::getComponentId, UserPermissionDto::getOrganizationUuid)
      .containsOnly(expectedPermissionsAsTuple.toArray(new Tuple[0]));

    // test method "countUsers()"
    long distinctUsers = stream(expectedPermissions).mapToLong(UserPermissionDto::getUserId).distinct().count();
    assertThat((long) underTest.countUsersByQuery(dbSession, query)).isEqualTo(distinctUsers);
  }

  private UserPermissionDto addGlobalPermission(OrganizationDto org, String permission, UserDto user) {
    UserPermissionDto dto = new UserPermissionDto(org.getUuid(), permission, user.getId(), null);
    underTest.insert(dbSession, dto);
    db.commit();
    return dto;
  }

  private UserPermissionDto addProjectPermission(OrganizationDto org, String permission, UserDto user, ComponentDto project) {
    UserPermissionDto dto = new UserPermissionDto(org.getUuid(), permission, user.getId(), project.getId());
    underTest.insert(dbSession, dto);
    db.commit();
    return dto;
  }

  private void assertThatProjectPermissionDoesNotExist(UserDto user, String permission, ComponentDto project) {
    assertThat(db.countSql(dbSession, "select count(id) from user_roles where role='" + permission + "' and user_id=" + user.getId() + " and resource_id=" + project.getId()))
      .isEqualTo(0);
  }

  private void assertThatProjectHasNoPermissions(ComponentDto project) {
    assertThat(db.countSql(dbSession, "select count(id) from user_roles where resource_id=" + project.getId())).isEqualTo(0);
  }

  private void assertOrgPermissionsOfUser(UserDto user, OrganizationDto organization, OrganizationPermission... permissions) {
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user.getId(), organization.getUuid()).stream()
      .map(OrganizationPermission::fromKey))
        .containsOnly(permissions);
  }

  private void assertProjectPermissionsOfUser(UserDto user, ComponentDto project, String... permissions) {
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user.getId(), project.getId())).containsOnly(permissions);
  }
}
