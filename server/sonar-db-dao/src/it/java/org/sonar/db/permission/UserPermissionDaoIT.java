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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.permission.PermissionQuery.DEFAULT_PAGE_SIZE;

class UserPermissionDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = db.getSession();
  private final UserPermissionDao underTest = new UserPermissionDao(new NoOpAuditPersister());

  @Test
  void select_global_permissions() {
    UserDto user1 = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    UserDto user2 = insertUser(u -> u.setLogin("login2").setName("Marie").setEmail("email2@email.com"));
    UserDto user3 = insertUser(u -> u.setLogin("zanother").setName("Zoe").setEmail("zanother3@another.com"));
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    UserPermissionDto global1 = addGlobalPermission(GlobalPermission.ADMINISTER.getKey(), user1);
    UserPermissionDto global2 = addGlobalPermission(GlobalPermission.ADMINISTER.getKey(), user2);
    UserPermissionDto global3 = addGlobalPermission(GlobalPermission.PROVISION_PROJECTS.getKey(), user2);
    UserPermissionDto project1Perm = addProjectPermission(UserRole.USER, user3, project);

    // global permissions of users who has at least one global permission, ordered by user name then permission
    PermissionQuery query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(query, asList(user2.getUuid(), user1.getUuid()), global2, global3, global1);

    // default query returns all users, whatever their permissions
    // (that's a non-sense, but still this is required for api/permissions/groups
    // when filtering users by name)
    query = PermissionQuery.builder().build();
    expectPermissions(query, asList(user2.getUuid(), user1.getUuid(), user3.getUuid()), global2, global3, global1, project1Perm);

    // global permissions "admin"
    query = PermissionQuery.builder().setPermission(GlobalPermission.ADMINISTER.getKey()).build();
    expectPermissions(query, asList(user2.getUuid(), user1.getUuid()), global2, global1);

    // empty if nobody has the specified global permission
    query = PermissionQuery.builder().setPermission("missing").build();
    expectPermissions(query, emptyList());

    // search by username (matches 2 users)
    query = PermissionQuery.builder().withAtLeastOnePermission().setSearchQuery("mari").build();
    expectPermissions(query, asList(user2.getUuid(), user1.getUuid()), global2, global3, global1);

    // search by user login
    query = PermissionQuery.builder().withAtLeastOnePermission().setSearchQuery("ogin2").build();
    expectPermissions(query, singletonList(user2.getUuid()), global2, global3);

    // search by user email
    query = PermissionQuery.builder().withAtLeastOnePermission().setSearchQuery("mail2").build();
    expectPermissions(query, singletonList(user2.getUuid()), global2, global3);

    // search by user name (matches 2 users) and global permission
    query = PermissionQuery.builder().setSearchQuery("Mari").setPermission(GlobalPermission.PROVISION_PROJECTS.getKey()).build();
    expectPermissions(query, singletonList(user2.getUuid()), global3);

    // search by user name (no match)
    query = PermissionQuery.builder().setSearchQuery("Unknown").build();
    expectPermissions(query, emptyList());
  }

  @Test
  void select_project_permissions() {
    UserDto user1 = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    UserDto user2 = insertUser(u -> u.setLogin("login2").setName("Marie").setEmail("email2@email.com"));
    UserDto user3 = insertUser(u -> u.setLogin("zanother").setName("Zoe").setEmail("zanother3@another.com"));
    addGlobalPermission(GlobalPermission.ADMINISTER.getKey(), user1);
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    UserPermissionDto perm1 = addProjectPermission(UserRole.USER, user1, project1);
    UserPermissionDto perm2 = addProjectPermission(UserRole.ISSUE_ADMIN, user1, project1);
    UserPermissionDto perm3 = addProjectPermission(UserRole.ISSUE_ADMIN, user2, project1);
    addProjectPermission(UserRole.ISSUE_ADMIN, user3, project2);

    // project permissions of users who has at least one permission on this project
    PermissionQuery query = PermissionQuery.builder().withAtLeastOnePermission().setEntity(project1).build();
    expectPermissions(query, asList(user2.getUuid(), user1.getUuid()), perm3, perm2, perm1);

    // empty if nobody has the specified global permission
    query = PermissionQuery.builder().setPermission("missing").setEntity(project1).build();
    expectPermissions(query, emptyList());

    // search by user name (matches 2 users), users with at least one permission
    query = PermissionQuery.builder().setSearchQuery("Mari").withAtLeastOnePermission().setEntity(project1).build();
    expectPermissions(query, asList(user2.getUuid(), user1.getUuid()), perm3, perm2, perm1);

    // search by user name (matches 2 users) and project permission
    query = PermissionQuery.builder().setSearchQuery("Mari").setPermission(UserRole.ISSUE_ADMIN).setEntity(project1).build();
    expectPermissions(query, asList(user2.getUuid(), user1.getUuid()), perm3, perm2);

    // search by user name (no match)
    query = PermissionQuery.builder().setSearchQuery("Unknown").setEntity(project1).build();
    expectPermissions(query, emptyList());

    // permissions of unknown project
    query = PermissionQuery.builder().setEntity(newPrivateProjectDto()).withAtLeastOnePermission().build();
    expectPermissions(query, emptyList());
  }

  @Test
  void selectUserUuidsByQuery_is_ordering_by_users_having_permissions_first_then_by_name_lowercase() {
    UserDto user1 = insertUser(u -> u.setLogin("login1").setName("Z").setEmail("email1@email.com"));
    UserDto user2 = insertUser(u -> u.setLogin("login2").setName("A").setEmail("email2@email.com"));
    UserDto user3 = insertUser(u -> u.setLogin("login3").setName("Z").setEmail("zanother3@another.com"));
    UserDto user4 = insertUser(u -> u.setLogin("login4").setName("A").setEmail("zanother3@another.com"));
    addGlobalPermission(GlobalPermission.ADMINISTER.getKey(), user1);
    addGlobalPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES.getKey(), user2);

    PermissionQuery query = PermissionQuery.builder().build();

    assertThat(underTest.selectUserUuidsByQueryAndScope(dbSession, query))
      .containsExactly(user2.getUuid(), user1.getUuid(), user4.getUuid(), user3.getUuid());
  }

  @Test
  void selectUserUuidsByQuery_is_ordering_by_users_having_permissions_first_then_by_name_lowercase_when_high_number_of_users_for_global_permissions() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    IntStream.rangeClosed(1, DEFAULT_PAGE_SIZE + 1).forEach(i -> {
      UserDto user = insertUser(u -> u.setLogin("login" + i).setName("" + i));
      // Add permission on project to be sure projects are excluded
      db.users().insertProjectPermissionOnUser(user, GlobalPermission.SCAN.getKey(), project);
    });
    String lastLogin = "login" + (DEFAULT_PAGE_SIZE + 1);
    UserDto lastUser = db.getDbClient().userDao().selectByLogin(dbSession, lastLogin);
    addGlobalPermission(GlobalPermission.ADMINISTER.getKey(), lastUser);

    PermissionQuery query = PermissionQuery.builder().build();

    assertThat(underTest.selectUserUuidsByQueryAndScope(dbSession, query))
      .hasSize(DEFAULT_PAGE_SIZE)
      .startsWith(lastUser.getUuid());
  }

  @Test
  void selectUserUuidsByQuery_is_ordering_by_users_having_permissions_first_then_by_name_lowercase_when_high_number_of_users_for_project_permissions() {
    IntStream.rangeClosed(1, DEFAULT_PAGE_SIZE + 1).forEach(i -> {
      UserDto user = insertUser(u -> u.setLogin("login" + i).setName("" + i));
      // Add global permission to be sure they are excluded
      addGlobalPermission(GlobalPermission.ADMINISTER.getKey(), user);
    });
    String lastLogin = "login" + (DEFAULT_PAGE_SIZE + 1);
    UserDto lastUser = db.getDbClient().userDao().selectByLogin(dbSession, lastLogin);
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.users().insertProjectPermissionOnUser(lastUser, GlobalPermission.SCAN.getKey(), project);

    PermissionQuery query = PermissionQuery.builder()
      .setEntity(project)
      .build();

    assertThat(underTest.selectUserUuidsByQueryAndScope(dbSession, query))
      .hasSize(DEFAULT_PAGE_SIZE)
      .startsWith(lastUser.getUuid());
  }

  @Test
  void selectUserUuidsByQuery_is_not_ordering_by_number_of_permissions() {
    UserDto user1 = insertUser(u -> u.setLogin("login1").setName("Z").setEmail("email1@email.com"));
    UserDto user2 = insertUser(u -> u.setLogin("login2").setName("A").setEmail("email2@email.com"));
    addGlobalPermission(GlobalPermission.ADMINISTER.getKey(), user1);
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    addProjectPermission(UserRole.USER, user2, project1);
    addProjectPermission(UserRole.USER, user1, project1);
    addProjectPermission(UserRole.ADMIN, user1, project1);

    PermissionQuery query = PermissionQuery.builder().build();

    // Even if user1 has 3 permissions, the name is used to order
    assertThat(underTest.selectUserUuidsByQuery(dbSession, query))
      .containsExactly(user2.getUuid(), user1.getUuid());
  }

  @Test
  void countUsersByEntityPermission() {
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    addGlobalPermission(GlobalPermission.ADMINISTER.getKey(), user1);
    addProjectPermission(UserRole.USER, user1, project1);
    addProjectPermission(UserRole.ISSUE_ADMIN, user1, project1);
    addProjectPermission(UserRole.ISSUE_ADMIN, user2, project1);
    addProjectPermission(UserRole.ISSUE_ADMIN, user2, project2);

    // no projects -> return empty list
    assertThat(underTest.countUsersByEntityPermission(dbSession, emptyList())).isEmpty();

    // one project
    expectCount(singletonList(project1.getUuid()),
      new CountPerEntityPermission(project1.getUuid(), UserRole.USER, 1),
      new CountPerEntityPermission(project1.getUuid(), UserRole.ISSUE_ADMIN, 2));

    // multiple projects
    expectCount(asList(project1.getUuid(), project2.getUuid(), "invalid"),
      new CountPerEntityPermission(project1.getUuid(), UserRole.USER, 1),
      new CountPerEntityPermission(project1.getUuid(), UserRole.ISSUE_ADMIN, 2),
      new CountPerEntityPermission(project2.getUuid(), UserRole.ISSUE_ADMIN, 1));
  }

  @Test
  void selectUserUuidsByQuery() {
    UserDto user1 = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    UserDto user2 = insertUser(u -> u.setLogin("login2").setName("Marie").setEmail("email2@email.com"));
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    addProjectPermission(UserRole.USER, user1, project1);
    addProjectPermission(UserRole.USER, user2, project1);
    addProjectPermission(UserRole.ISSUE_ADMIN, user2, project1);

    // logins are ordered by user name: user2 ("Marie") then user1 ("Marius")
    PermissionQuery query = PermissionQuery.builder().setEntity(project1).withAtLeastOnePermission().build();
    assertThat(underTest.selectUserUuidsByQuery(dbSession, query)).containsExactly(user2.getUuid(), user1.getUuid());
    query = PermissionQuery.builder().setEntity(project2).withAtLeastOnePermission().build();
    assertThat(underTest.selectUserUuidsByQuery(dbSession, query)).isEmpty();

    // on a project without permissions
    query = PermissionQuery.builder().setEntity(newPrivateProjectDto()).withAtLeastOnePermission().build();
    assertThat(underTest.selectUserUuidsByQuery(dbSession, query)).isEmpty();

    // search all users whose name matches "mar", whatever the permissions
    query = PermissionQuery.builder().setSearchQuery("mar").build();
    assertThat(underTest.selectUserUuidsByQuery(dbSession, query)).containsExactly(user2.getUuid(), user1.getUuid());

    // search all users whose name matches "mariu", whatever the permissions
    query = PermissionQuery.builder().setSearchQuery("mariu").build();
    assertThat(underTest.selectUserUuidsByQuery(dbSession, query)).containsExactly(user1.getUuid());

    // search all users whose name matches "mariu", whatever the permissions
    query = PermissionQuery.builder().setSearchQuery("mariu").setEntity(project1).build();
    assertThat(underTest.selectUserUuidsByQuery(dbSession, query)).containsExactly(user1.getUuid());
  }

  @Test
  void selectUserUuidsByQueryAndScope_with_global_scope() {
    UserDto user1 = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    UserDto user2 = insertUser(u -> u.setLogin("login2").setName("Marie").setEmail("email2@email.com"));
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    addProjectPermission(UserRole.USER, user1, project1);
    addGlobalPermission(GlobalPermission.PROVISION_PROJECTS.getKey(), user1);
    addProjectPermission(UserRole.ISSUE_ADMIN, user2, project2);
    PermissionQuery query = PermissionQuery.builder().build();

    List<String> result = underTest.selectUserUuidsByQueryAndScope(dbSession, query);

    // users with any kind of global permissions are first on the list and then sorted by name
    assertThat(result).containsExactly(user1.getUuid(), user2.getUuid());
  }

  @Test
  void selectUserUuidsByQueryAndScope_with_project_scope() {
    UserDto user1 = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    UserDto user2 = insertUser(u -> u.setLogin("login2").setName("Marie").setEmail("email2@email.com"));
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    addProjectPermission(UserRole.USER, user1, project1);
    addGlobalPermission(GlobalPermission.PROVISION_PROJECTS.getKey(), user1);
    addProjectPermission(UserRole.ISSUE_ADMIN, user2, project2);
    PermissionQuery query = PermissionQuery.builder()
      .setEntity(project1)
      .build();

    List<String> result = underTest.selectUserUuidsByQueryAndScope(dbSession, query);

    // users with any this projects permissions are first on the list and then sorted by name
    assertThat(result).containsExactly(user1.getUuid(), user2.getUuid());
  }

  @Test
  void selectUserUuidsByQuery_is_paginated() {
    List<String> userUuids = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      String name = "user-" + i;
      UserDto user = insertUser(u -> u.setName(name));
      addGlobalPermission(GlobalPermission.PROVISION_PROJECTS.getKey(), user);
      addGlobalPermission(GlobalPermission.ADMINISTER.getKey(), user);
      userUuids.add(user.getUuid());
    }

    assertThat(underTest.selectUserUuidsByQuery(dbSession, PermissionQuery.builder()
      .setPageSize(3).setPageIndex(1).build()))
      .containsExactly(userUuids.get(0), userUuids.get(1), userUuids.get(2));
    assertThat(underTest.selectUserUuidsByQuery(dbSession, PermissionQuery.builder()
      .setPageSize(2).setPageIndex(3).build()))
      .containsExactly(userUuids.get(4), userUuids.get(5));
    assertThat(underTest.selectUserUuidsByQuery(dbSession, PermissionQuery.builder()
      .setPageSize(50).setPageIndex(1).build()))
      .hasSize(10);
  }

  @Test
  void selectUserUuidsByQuery_is_sorted_by_insensitive_name() {
    UserDto user1 = insertUser(u -> u.setName("user1"));
    addGlobalPermission(GlobalPermission.PROVISION_PROJECTS.getKey(), user1);
    UserDto user3 = insertUser(u -> u.setName("user3"));
    addGlobalPermission(GlobalPermission.ADMINISTER.getKey(), user3);
    UserDto user2 = insertUser(u -> u.setName("User2"));
    addGlobalPermission(GlobalPermission.PROVISION_PROJECTS.getKey(), user2);

    assertThat(underTest.selectUserUuidsByQuery(dbSession, PermissionQuery.builder().build()))
      .containsExactly(user1.getUuid(), user2.getUuid(), user3.getUuid());
  }

  @Test
  void deleteGlobalPermission() {
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    addGlobalPermission("perm1", user1);
    addGlobalPermission("perm2", user1);
    addProjectPermission("perm1", user1, project1);
    addProjectPermission("perm3", user2, project1);
    addProjectPermission("perm4", user2, project2);

    // user2 does not have global permissions -> do nothing
    underTest.deleteGlobalPermission(dbSession, user2, "perm1");
    assertThat(db.countRowsOfTable(dbSession, "user_roles")).isEqualTo(5);

    // global permission is not granted -> do nothing
    underTest.deleteGlobalPermission(dbSession, user1, "notGranted");
    assertThat(db.countRowsOfTable(dbSession, "user_roles")).isEqualTo(5);

    // permission is on project -> do nothing
    underTest.deleteGlobalPermission(dbSession, user1, "perm3");
    assertThat(db.countRowsOfTable(dbSession, "user_roles")).isEqualTo(5);

    // global permission exists -> delete it, but not the project permission with the same name !
    underTest.deleteGlobalPermission(dbSession, user1, "perm1");

    assertThat(db.countSql(dbSession, "select count(uuid) from user_roles where role='perm1' and entity_uuid is null")).isZero();
    assertThat(db.countRowsOfTable(dbSession, "user_roles")).isEqualTo(4);
  }

  @Test
  void deleteEntityPermission() {
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    addGlobalPermission("perm", user1);
    addProjectPermission("perm", user1, project1);
    addProjectPermission("perm", user1, project2);
    addProjectPermission("perm", user2, project1);

    // no such provision -> ignore
    underTest.deleteEntityPermission(dbSession, user1, "anotherPerm", project1);
    assertThat(db.countRowsOfTable(dbSession, "user_roles")).isEqualTo(4);

    underTest.deleteEntityPermission(dbSession, user1, "perm", project1);
    assertThatProjectPermissionDoesNotExist(user1, "perm", project1.getUuid());
    assertThat(db.countRowsOfTable(dbSession, "user_roles")).isEqualTo(3);
  }

  @Test
  void deleteEntityPermissions() {
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    addGlobalPermission("perm", user1);
    addProjectPermission("perm", user1, project1);
    addProjectPermission("perm", user2, project1);
    addProjectPermission("perm", user1, project2);

    underTest.deleteEntityPermissions(dbSession, project1);
    assertThat(db.countRowsOfTable(dbSession, "user_roles")).isEqualTo(2);
    assertThatProjectHasNoPermissions(project1.getUuid());
  }

  @Test
  void selectGlobalPermissionsOfUser() {
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    UserDto user3 = insertUser();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    addGlobalPermission("perm1", user1);
    addGlobalPermission("perm2", user2);
    addGlobalPermission("perm3", user1);
    addProjectPermission("perm4", user1, project);
    addProjectPermission("perm5", user2, project);

    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user1.getUuid())).containsOnly("perm1", "perm3");
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user2.getUuid())).containsOnly("perm2");
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user3.getUuid())).isEmpty();
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, "unknown")).isEmpty();
  }

  @Test
  void selectEntityPermissionsOfUser() {
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project3 = db.components().insertPrivateProject().getProjectDto();
    addGlobalPermission("perm1", user1);
    addProjectPermission("perm2", user1, project1);
    addProjectPermission("perm3", user1, project1);
    addProjectPermission("perm4", user1, project2);
    addProjectPermission("perm5", user2, project1);

    assertThat(underTest.selectEntityPermissionsOfUser(dbSession, user1.getUuid(), project1.getUuid())).containsOnly("perm2", "perm3");
    assertThat(underTest.selectEntityPermissionsOfUser(dbSession, user1.getUuid(), project2.getUuid())).containsOnly("perm4");
    assertThat(underTest.selectEntityPermissionsOfUser(dbSession, user1.getUuid(), project3.getUuid())).isEmpty();
  }

  @Test
  void selectUserIdsWithPermissionOnEntityBut_returns_empty_if_project_does_not_exist() {
    ProjectData project = randomPublicOrPrivateProject();
    UserDto user = insertUser();
    db.users().insertProjectPermissionOnUser(user, "foo", project.getMainBranchComponent());

    assertThat(underTest.selectUserIdsWithPermissionOnEntityBut(dbSession, "1234", UserRole.USER))
      .isEmpty();
  }

  @Test
  void selectUserIdsWithPermissionOnEntityBut_returns_only_users_of_projects_which_do_not_have_permission() {
    ProjectData project = randomPublicOrPrivateProject();
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    db.users().insertProjectPermissionOnUser(user1, "p1", project.getMainBranchComponent());
    db.users().insertProjectPermissionOnUser(user2, "p2", project.getMainBranchComponent());

    assertThat(underTest.selectUserIdsWithPermissionOnEntityBut(dbSession, project.projectUuid(), "p2"))
      .extracting("uuid", "login")
      .containsOnly(tuple(user1.getUuid(), user1.getLogin()));
    assertThat(underTest.selectUserIdsWithPermissionOnEntityBut(dbSession, project.projectUuid(), "p1"))
      .extracting("uuid", "login")
      .containsOnly(tuple(user2.getUuid(), user2.getLogin()));
    assertThat(underTest.selectUserIdsWithPermissionOnEntityBut(dbSession, project.projectUuid(), "p3"))
      .extracting("uuid", "login")
      .containsOnly(tuple(user1.getUuid(), user1.getLogin()), tuple(user2.getUuid(), user2.getLogin()));
  }

  @Test
  void selectUserIdsWithPermissionOnEntityBut_does_not_return_groups_which_have_no_permission_at_all_on_specified_project() {
    ProjectDto project = randomPublicOrPrivateProject().getProjectDto();
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    db.users().insertProjectPermissionOnUser(user1, "p1", project);
    db.users().insertProjectPermissionOnUser(user2, "p2", project);

    assertThat(underTest.selectUserIdsWithPermissionOnEntityBut(dbSession, project.getUuid(), "p2"))
      .extracting("uuid", "login")
      .containsOnly(tuple(user1.getUuid(), user1.getLogin()));
    assertThat(underTest.selectUserIdsWithPermissionOnEntityBut(dbSession, project.getUuid(), "p1"))
      .extracting("uuid", "login")
      .containsOnly(tuple(user2.getUuid(), user2.getLogin()));
  }

  @Test
  void deleteByUserId() {
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.users().insertGlobalPermissionOnUser(user1, GlobalPermission.SCAN);
    db.users().insertGlobalPermissionOnUser(user1, GlobalPermission.ADMINISTER);
    db.users().insertProjectPermissionOnUser(user1, GlobalPermission.ADMINISTER_QUALITY_GATES.getKey(), project);
    db.users().insertGlobalPermissionOnUser(user2, GlobalPermission.SCAN);
    db.users().insertProjectPermissionOnUser(user2, GlobalPermission.ADMINISTER_QUALITY_GATES.getKey(), project);

    underTest.deleteByUserUuid(dbSession, user1);
    dbSession.commit();

    assertThat(db.select("select user_uuid as \"userUuid\", entity_uuid as \"entityUuid\", role as \"permission\" from user_roles"))
      .extracting((row) -> row.get("userUuid"), (row) -> row.get("entityUuid"), (row) -> row.get("permission"))
      .containsOnly(tuple(user2.getUuid(), null, GlobalPermission.SCAN.getKey()), tuple(user2.getUuid(), project.getUuid(),
        GlobalPermission.ADMINISTER_QUALITY_GATES.getKey()));
  }

  @Test
  void deleteEntityPermissionOfAnyUser_has_no_effect_if_specified_entity_does_not_exist() {
    UserDto user = insertUser();
    db.users().insertGlobalPermissionOnUser(user, GlobalPermission.SCAN);

    ProjectDto projectDto = ComponentTesting.newProjectDto();

    int deletedCount = underTest.deleteEntityPermissionOfAnyUser(dbSession, GlobalPermission.SCAN.getKey(), projectDto);

    assertThat(deletedCount).isZero();
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user.getUuid())).containsOnly(GlobalPermission.SCAN.getKey());
  }

  @Test
  void deleteEntityPermissionOfAnyUser_has_no_effect_if_specified_component_has_no_permission_at_all() {
    UserDto user = insertUser();
    db.users().insertGlobalPermissionOnUser(user, GlobalPermission.SCAN);
    ProjectData project = randomPublicOrPrivateProject();

    int deletedCount = underTest.deleteEntityPermissionOfAnyUser(dbSession, GlobalPermission.SCAN.getKey(), project.getProjectDto());

    assertThat(deletedCount).isZero();
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user.getUuid())).containsOnly(GlobalPermission.SCAN.getKey());
  }

  @Test
  void deleteEntityPermissionOfAnyUser_has_no_effect_if_specified_component_does_not_have_specified_permission() {
    UserDto user = insertUser();
    db.users().insertGlobalPermissionOnUser(user, GlobalPermission.SCAN);
    ProjectData project = randomPublicOrPrivateProject();
    db.users().insertProjectPermissionOnUser(user, GlobalPermission.SCAN.getKey(), project.getMainBranchComponent());

    int deletedCount = underTest.deleteEntityPermissionOfAnyUser(dbSession, "p1", project.getProjectDto());

    assertThat(deletedCount).isZero();
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user.getUuid())).containsOnly(GlobalPermission.SCAN.getKey());
    assertThat(underTest.selectEntityPermissionsOfUser(dbSession, user.getUuid(), project.projectUuid())).containsOnly(GlobalPermission.SCAN.getKey());
  }

  @Test
  void deleteEntityPermissionOfAnyUser_deletes_specified_permission_for_any_user_on_the_specified_component() {
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    db.users().insertGlobalPermissionOnUser(user1, GlobalPermission.SCAN);
    db.users().insertGlobalPermissionOnUser(user2, GlobalPermission.SCAN);
    ProjectData project1 = randomPublicOrPrivateProject();
    ProjectData project2 = randomPublicOrPrivateProject();
    db.users().insertProjectPermissionOnUser(user1, GlobalPermission.SCAN.getKey(), project1.getMainBranchComponent());
    db.users().insertProjectPermissionOnUser(user2, GlobalPermission.SCAN.getKey(), project1.getMainBranchComponent());
    db.users().insertProjectPermissionOnUser(user1, GlobalPermission.SCAN.getKey(), project2.getMainBranchComponent());
    db.users().insertProjectPermissionOnUser(user2, GlobalPermission.SCAN.getKey(), project2.getMainBranchComponent());
    db.users().insertProjectPermissionOnUser(user2, GlobalPermission.PROVISION_PROJECTS.getKey(), project2.getMainBranchComponent());

    int deletedCount = underTest.deleteEntityPermissionOfAnyUser(dbSession, GlobalPermission.SCAN.getKey(), project1.getProjectDto());

    assertThat(deletedCount).isEqualTo(2);
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user1.getUuid())).containsOnly(GlobalPermission.SCAN.getKey());
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user2.getUuid())).containsOnly(GlobalPermission.SCAN.getKey());
    assertThat(underTest.selectEntityPermissionsOfUser(dbSession, user1.getUuid(), project1.projectUuid())).isEmpty();
    assertThat(underTest.selectEntityPermissionsOfUser(dbSession, user2.getUuid(), project1.projectUuid())).isEmpty();
    assertThat(underTest.selectEntityPermissionsOfUser(dbSession, user1.getUuid(), project2.projectUuid())).containsOnly(GlobalPermission.SCAN.getKey());
    assertThat(underTest.selectEntityPermissionsOfUser(dbSession, user2.getUuid(), project2.projectUuid())).containsOnly(GlobalPermission.SCAN.getKey(), GlobalPermission.PROVISION_PROJECTS.getKey());

    deletedCount = underTest.deleteEntityPermissionOfAnyUser(dbSession, GlobalPermission.SCAN.getKey(), project2.getProjectDto());

    assertThat(deletedCount).isEqualTo(2);
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user1.getUuid())).containsOnly(GlobalPermission.SCAN.getKey());
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user2.getUuid())).containsOnly(GlobalPermission.SCAN.getKey());
    assertThat(underTest.selectEntityPermissionsOfUser(dbSession, user1.getUuid(), project1.projectUuid())).isEmpty();
    assertThat(underTest.selectEntityPermissionsOfUser(dbSession, user2.getUuid(), project1.projectUuid())).isEmpty();
    assertThat(underTest.selectEntityPermissionsOfUser(dbSession, user1.getUuid(), project2.projectUuid())).containsOnly();
    assertThat(underTest.selectEntityPermissionsOfUser(dbSession, user2.getUuid(), project2.projectUuid())).containsOnly(GlobalPermission.PROVISION_PROJECTS.getKey());
  }

  private ProjectData randomPublicOrPrivateProject() {
    return new Random().nextBoolean() ? db.components().insertPrivateProject() : db.components().insertPublicProject();
  }

  private UserDto insertUser(Consumer<UserDto> populateUserDto) {
    return db.users().insertUser(populateUserDto);
  }

  private UserDto insertUser() {
    return db.users().insertUser();
  }

  private void expectCount(List<String> entityUuids, CountPerEntityPermission... expected) {
    List<CountPerEntityPermission> got = underTest.countUsersByEntityPermission(dbSession, entityUuids);
    assertThat(got).hasSize(expected.length);

    for (CountPerEntityPermission expect : expected) {
      boolean found = got.stream().anyMatch(b -> b.getPermission().equals(expect.getPermission()) &&
        b.getCount() == expect.getCount() &&
        b.getEntityUuid().equals(expect.getEntityUuid()));
      assertThat(found).isTrue();
    }
  }

  private void expectPermissions(PermissionQuery query, Collection<String> expectedUserUuids, UserPermissionDto... expectedPermissions) {
    assertThat(underTest.selectUserUuidsByQuery(dbSession, query)).containsExactly(expectedUserUuids.toArray(new String[0]));
    List<UserPermissionDto> currentPermissions = underTest.selectUserPermissionsByQuery(dbSession, query, expectedUserUuids);
    assertThat(currentPermissions).hasSize(expectedPermissions.length);
    Tuple[] expectedPermissionsAsTuple = Arrays.stream(expectedPermissions)
      .map(expectedPermission -> tuple(expectedPermission.getUserUuid(), expectedPermission.getPermission(),
        expectedPermission.getEntityUuid()))
      .toArray(Tuple[]::new);
    assertThat(currentPermissions)
      .extracting(UserPermissionDto::getUserUuid, UserPermissionDto::getPermission, UserPermissionDto::getEntityUuid)
      .containsOnly(expectedPermissionsAsTuple);

    long distinctUsers = stream(expectedPermissions).map(UserPermissionDto::getUserUuid).distinct().count();
    assertThat((long) underTest.countUsersByQuery(dbSession, query)).isEqualTo(distinctUsers);
  }

  private UserPermissionDto addGlobalPermission(String permission, UserDto user) {
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), permission, user.getUuid(), null);
    underTest.insert(dbSession, dto, null, user, null);
    db.commit();
    return dto;
  }

  private UserPermissionDto addProjectPermission(String permission, UserDto user, EntityDto project) {
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), permission, user.getUuid(), project.getUuid());
    underTest.insert(dbSession, dto, project, user, null);
    db.commit();
    return dto;
  }

  private void assertThatProjectPermissionDoesNotExist(UserDto user, String permission, String projectUuid) {
    assertThat(db.countSql(dbSession, "select count(uuid) from user_roles where role='" + permission + "' and user_uuid='" + user.getUuid()
      + "' and entity_uuid ='" + projectUuid + "'"))
      .isZero();
  }

  private void assertThatProjectHasNoPermissions(String projectUuid) {
    assertThat(db.countSql(dbSession, "select count(uuid) from user_roles where entity_uuid='" + projectUuid + "'")).isZero();
  }
}
