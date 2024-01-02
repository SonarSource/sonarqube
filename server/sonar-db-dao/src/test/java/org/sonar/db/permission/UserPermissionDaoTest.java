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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.UserDto;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_PROFILE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.db.permission.PermissionQuery.DEFAULT_PAGE_SIZE;

public class UserPermissionDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbSession dbSession = db.getSession();
  private UserPermissionDao underTest = new UserPermissionDao(new NoOpAuditPersister());

  @Test
  public void select_global_permissions() {
    UserDto user1 = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    UserDto user2 = insertUser(u -> u.setLogin("login2").setName("Marie").setEmail("email2@email.com"));
    UserDto user3 = insertUser(u -> u.setLogin("zanother").setName("Zoe").setEmail("zanother3@another.com"));
    ComponentDto project = db.components().insertPrivateProject();
    UserPermissionDto global1 = addGlobalPermission(SYSTEM_ADMIN, user1);
    UserPermissionDto global2 = addGlobalPermission(SYSTEM_ADMIN, user2);
    UserPermissionDto global3 = addGlobalPermission(PROVISIONING, user2);
    UserPermissionDto project1Perm = addProjectPermission(USER, user3, project);

    // global permissions of users who has at least one global permission, ordered by user name then permission
    PermissionQuery query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(query, asList(user2.getUuid(), user1.getUuid()), global2, global3, global1);

    // default query returns all users, whatever their permissions
    // (that's a non-sense, but still this is required for api/permissions/groups
    // when filtering users by name)
    query = PermissionQuery.builder().build();
    expectPermissions(query, asList(user2.getUuid(), user1.getUuid(), user3.getUuid()), global2, global3, global1, project1Perm);

    // global permissions "admin"
    query = PermissionQuery.builder().setPermission(SYSTEM_ADMIN).build();
    expectPermissions(query, asList(user2.getUuid(), user1.getUuid()), global2, global1);

    // empty if nobody has the specified global permission
    query = PermissionQuery.builder().setPermission("missing").build();
    expectPermissions(query, emptyList());

    // search by user name (matches 2 users)
    query = PermissionQuery.builder().withAtLeastOnePermission().setSearchQuery("mari").build();
    expectPermissions(query, asList(user2.getUuid(), user1.getUuid()), global2, global3, global1);

    // search by user login
    query = PermissionQuery.builder().withAtLeastOnePermission().setSearchQuery("ogin2").build();
    expectPermissions(query, singletonList(user2.getUuid()), global2, global3);

    // search by user email
    query = PermissionQuery.builder().withAtLeastOnePermission().setSearchQuery("mail2").build();
    expectPermissions(query, singletonList(user2.getUuid()), global2, global3);

    // search by user name (matches 2 users) and global permission
    query = PermissionQuery.builder().setSearchQuery("Mari").setPermission(PROVISIONING).build();
    expectPermissions(query, singletonList(user2.getUuid()), global3);

    // search by user name (no match)
    query = PermissionQuery.builder().setSearchQuery("Unknown").build();
    expectPermissions(query, emptyList());
  }

  @Test
  public void select_project_permissions() {
    UserDto user1 = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    UserDto user2 = insertUser(u -> u.setLogin("login2").setName("Marie").setEmail("email2@email.com"));
    UserDto user3 = insertUser(u -> u.setLogin("zanother").setName("Zoe").setEmail("zanother3@another.com"));
    addGlobalPermission(SYSTEM_ADMIN, user1);
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    UserPermissionDto perm1 = addProjectPermission(USER, user1, project1);
    UserPermissionDto perm2 = addProjectPermission(ISSUE_ADMIN, user1, project1);
    UserPermissionDto perm3 = addProjectPermission(ISSUE_ADMIN, user2, project1);
    addProjectPermission(ISSUE_ADMIN, user3, project2);

    // project permissions of users who has at least one permission on this project
    PermissionQuery query = PermissionQuery.builder().withAtLeastOnePermission().setComponent(project1).build();
    expectPermissions(query, asList(user2.getUuid(), user1.getUuid()), perm3, perm2, perm1);

    // empty if nobody has the specified global permission
    query = PermissionQuery.builder().setPermission("missing").setComponent(project1).build();
    expectPermissions(query, emptyList());

    // search by user name (matches 2 users), users with at least one permission
    query = PermissionQuery.builder().setSearchQuery("Mari").withAtLeastOnePermission().setComponent(project1).build();
    expectPermissions(query, asList(user2.getUuid(), user1.getUuid()), perm3, perm2, perm1);

    // search by user name (matches 2 users) and project permission
    query = PermissionQuery.builder().setSearchQuery("Mari").setPermission(ISSUE_ADMIN).setComponent(project1).build();
    expectPermissions(query, asList(user2.getUuid(), user1.getUuid()), perm3, perm2);

    // search by user name (no match)
    query = PermissionQuery.builder().setSearchQuery("Unknown").setComponent(project1).build();
    expectPermissions(query, emptyList());

    // permissions of unknown project
    query = PermissionQuery.builder().setComponent(newPrivateProjectDto()).withAtLeastOnePermission().build();
    expectPermissions(query, emptyList());
  }

  @Test
  public void selectUserUuidsByQuery_is_ordering_by_users_having_permissions_first_then_by_name_lowercase() {
    UserDto user1 = insertUser(u -> u.setLogin("login1").setName("Z").setEmail("email1@email.com"));
    UserDto user2 = insertUser(u -> u.setLogin("login2").setName("A").setEmail("email2@email.com"));
    UserDto user3 = insertUser(u -> u.setLogin("login3").setName("Z").setEmail("zanother3@another.com"));
    UserDto user4 = insertUser(u -> u.setLogin("login4").setName("A").setEmail("zanother3@another.com"));
    addGlobalPermission(SYSTEM_ADMIN, user1);
    addGlobalPermission(QUALITY_PROFILE_ADMIN, user2);

    PermissionQuery query = PermissionQuery.builder().build();

    assertThat(underTest.selectUserUuidsByQueryAndScope(dbSession, query))
      .containsExactly(user2.getUuid(), user1.getUuid(), user4.getUuid(), user3.getUuid());
  }

  @Test
  public void selectUserUuidsByQuery_is_ordering_by_users_having_permissions_first_then_by_name_lowercase_when_high_number_of_users_for_global_permissions() {
    ComponentDto project = db.components().insertPrivateProject();
    IntStream.rangeClosed(1, DEFAULT_PAGE_SIZE + 1).forEach(i -> {
      UserDto user = insertUser(u -> u.setLogin("login" + i).setName("" + i));
      // Add permission on project to be sure projects are excluded
      db.users().insertProjectPermissionOnUser(user, SCAN.getKey(), project);
    });
    String lastLogin = "login" + (DEFAULT_PAGE_SIZE + 1);
    UserDto lastUser = db.getDbClient().userDao().selectByLogin(dbSession, lastLogin);
    addGlobalPermission(SYSTEM_ADMIN, lastUser);

    PermissionQuery query = PermissionQuery.builder().build();

    assertThat(underTest.selectUserUuidsByQueryAndScope(dbSession, query))
      .hasSize(DEFAULT_PAGE_SIZE)
      .startsWith(lastUser.getUuid());
  }

  @Test
  public void selectUserUuidsByQuery_is_ordering_by_users_having_permissions_first_then_by_name_lowercase_when_high_number_of_users_for_project_permissions() {
    IntStream.rangeClosed(1, DEFAULT_PAGE_SIZE + 1).forEach(i -> {
      UserDto user = insertUser(u -> u.setLogin("login" + i).setName("" + i));
      // Add global permission to be sure they are excluded
      addGlobalPermission(SYSTEM_ADMIN, user);
    });
    String lastLogin = "login" + (DEFAULT_PAGE_SIZE + 1);
    UserDto lastUser = db.getDbClient().userDao().selectByLogin(dbSession, lastLogin);
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(lastUser, SCAN.getKey(), project);

    PermissionQuery query = PermissionQuery.builder()
      .setComponent(project)
      .build();

    assertThat(underTest.selectUserUuidsByQueryAndScope(dbSession, query))
      .hasSize(DEFAULT_PAGE_SIZE)
      .startsWith(lastUser.getUuid());
  }

  @Test
  public void selectUserUuidsByQuery_is_not_ordering_by_number_of_permissions() {
    UserDto user1 = insertUser(u -> u.setLogin("login1").setName("Z").setEmail("email1@email.com"));
    UserDto user2 = insertUser(u -> u.setLogin("login2").setName("A").setEmail("email2@email.com"));
    addGlobalPermission(SYSTEM_ADMIN, user1);
    ComponentDto project1 = db.components().insertPrivateProject();
    addProjectPermission(USER, user2, project1);
    addProjectPermission(USER, user1, project1);
    addProjectPermission(ADMIN, user1, project1);

    PermissionQuery query = PermissionQuery.builder().build();

    // Even if user1 has 3 permissions, the name is used to order
    assertThat(underTest.selectUserUuidsByQuery(dbSession, query))
      .containsExactly(user2.getUuid(), user1.getUuid());
  }

  @Test
  public void countUsersByProjectPermission() {
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    addGlobalPermission(SYSTEM_ADMIN, user1);
    addProjectPermission(USER, user1, project1);
    addProjectPermission(ISSUE_ADMIN, user1, project1);
    addProjectPermission(ISSUE_ADMIN, user2, project1);
    addProjectPermission(ISSUE_ADMIN, user2, project2);

    // no projects -> return empty list
    assertThat(underTest.countUsersByProjectPermission(dbSession, emptyList())).isEmpty();

    // one project
    expectCount(singletonList(project1.uuid()),
      new CountPerProjectPermission(project1.uuid(), USER, 1),
      new CountPerProjectPermission(project1.uuid(), ISSUE_ADMIN, 2));

    // multiple projects
    expectCount(asList(project1.uuid(), project2.uuid(), "invalid"),
      new CountPerProjectPermission(project1.uuid(), USER, 1),
      new CountPerProjectPermission(project1.uuid(), ISSUE_ADMIN, 2),
      new CountPerProjectPermission(project2.uuid(), ISSUE_ADMIN, 1));
  }

  @Test
  public void selectUserUuidsByQuery() {
    UserDto user1 = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    UserDto user2 = insertUser(u -> u.setLogin("login2").setName("Marie").setEmail("email2@email.com"));
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    addProjectPermission(USER, user1, project1);
    addProjectPermission(USER, user2, project1);
    addProjectPermission(ISSUE_ADMIN, user2, project1);

    // logins are ordered by user name: user2 ("Marie") then user1 ("Marius")
    PermissionQuery query = PermissionQuery.builder().setComponent(project1).withAtLeastOnePermission().build();
    assertThat(underTest.selectUserUuidsByQuery(dbSession, query)).containsExactly(user2.getUuid(), user1.getUuid());
    query = PermissionQuery.builder().setComponent(project2).withAtLeastOnePermission().build();
    assertThat(underTest.selectUserUuidsByQuery(dbSession, query)).isEmpty();

    // on a project without permissions
    query = PermissionQuery.builder().setComponent(newPrivateProjectDto()).withAtLeastOnePermission().build();
    assertThat(underTest.selectUserUuidsByQuery(dbSession, query)).isEmpty();

    // search all users whose name matches "mar", whatever the permissions
    query = PermissionQuery.builder().setSearchQuery("mar").build();
    assertThat(underTest.selectUserUuidsByQuery(dbSession, query)).containsExactly(user2.getUuid(), user1.getUuid());

    // search all users whose name matches "mariu", whatever the permissions
    query = PermissionQuery.builder().setSearchQuery("mariu").build();
    assertThat(underTest.selectUserUuidsByQuery(dbSession, query)).containsExactly(user1.getUuid());

    // search all users whose name matches "mariu", whatever the permissions
    query = PermissionQuery.builder().setSearchQuery("mariu").setComponent(project1).build();
    assertThat(underTest.selectUserUuidsByQuery(dbSession, query)).containsExactly(user1.getUuid());
  }

  @Test
  public void selectUserUuidsByQueryAndScope_with_global_scope() {
    UserDto user1 = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    UserDto user2 = insertUser(u -> u.setLogin("login2").setName("Marie").setEmail("email2@email.com"));
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    addProjectPermission(USER, user1, project1);
    addGlobalPermission(PROVISIONING, user1);
    addProjectPermission(ISSUE_ADMIN, user2, project2);
    PermissionQuery query = PermissionQuery.builder().build();

    List<String> result = underTest.selectUserUuidsByQueryAndScope(dbSession, query);

    // users with any kind of global permissions are first on the list and then sorted by name
    assertThat(result).containsExactly(user1.getUuid(), user2.getUuid());
  }

  @Test
  public void selectUserUuidsByQueryAndScope_with_project_scope() {
    UserDto user1 = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    UserDto user2 = insertUser(u -> u.setLogin("login2").setName("Marie").setEmail("email2@email.com"));
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    addProjectPermission(USER, user1, project1);
    addGlobalPermission(PROVISIONING, user1);
    addProjectPermission(ISSUE_ADMIN, user2, project2);
    PermissionQuery query = PermissionQuery.builder()
      .setComponent(project1)
      .build();

    List<String> result = underTest.selectUserUuidsByQueryAndScope(dbSession, query);

    // users with any this projects permissions are first on the list and then sorted by name
    assertThat(result).containsExactly(user1.getUuid(), user2.getUuid());
  }

  @Test
  public void selectUserUuidsByQuery_is_paginated() {
    List<String> userUuids = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      String name = "user-" + i;
      UserDto user = insertUser(u -> u.setName(name));
      addGlobalPermission(PROVISIONING, user);
      addGlobalPermission(SYSTEM_ADMIN, user);
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
  public void selectUserUuidsByQuery_is_sorted_by_insensitive_name() {
    UserDto user1 = insertUser(u -> u.setName("user1"));
    addGlobalPermission(PROVISIONING, user1);
    UserDto user3 = insertUser(u -> u.setName("user3"));
    addGlobalPermission(SYSTEM_ADMIN, user3);
    UserDto user2 = insertUser(u -> u.setName("User2"));
    addGlobalPermission(PROVISIONING, user2);

    assertThat(underTest.selectUserUuidsByQuery(dbSession, PermissionQuery.builder().build()))
      .containsExactly(user1.getUuid(), user2.getUuid(), user3.getUuid());
  }

  @Test
  public void deleteGlobalPermission() {
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
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

    assertThat(db.countSql(dbSession, "select count(uuid) from user_roles where role='perm1' and component_uuid is null")).isZero();
    assertThat(db.countRowsOfTable(dbSession, "user_roles")).isEqualTo(4);
  }

  @Test
  public void deleteProjectPermission() {
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    addGlobalPermission("perm", user1);
    addProjectPermission("perm", user1, project1);
    addProjectPermission("perm", user1, project2);
    addProjectPermission("perm", user2, project1);

    // no such provision -> ignore
    underTest.deleteProjectPermission(dbSession, user1, "anotherPerm", project1);
    assertThat(db.countRowsOfTable(dbSession, "user_roles")).isEqualTo(4);

    underTest.deleteProjectPermission(dbSession, user1, "perm", project1);
    assertThatProjectPermissionDoesNotExist(user1, "perm", project1);
    assertThat(db.countRowsOfTable(dbSession, "user_roles")).isEqualTo(3);
  }

  @Test
  public void deleteProjectPermissions() {
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    addGlobalPermission("perm", user1);
    addProjectPermission("perm", user1, project1);
    addProjectPermission("perm", user2, project1);
    addProjectPermission("perm", user1, project2);

    underTest.deleteProjectPermissions(dbSession, project1);
    assertThat(db.countRowsOfTable(dbSession, "user_roles")).isEqualTo(2);
    assertThatProjectHasNoPermissions(project1);
  }

  @Test
  public void selectGlobalPermissionsOfUser() {
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    UserDto user3 = insertUser();
    ComponentDto project = db.components().insertPrivateProject();
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
  public void selectProjectPermissionsOfUser() {
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto project3 = db.components().insertPrivateProject();
    addGlobalPermission("perm1", user1);
    addProjectPermission("perm2", user1, project1);
    addProjectPermission("perm3", user1, project1);
    addProjectPermission("perm4", user1, project2);
    addProjectPermission("perm5", user2, project1);

    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user1.getUuid(), project1.uuid())).containsOnly("perm2", "perm3");
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user1.getUuid(), project2.uuid())).containsOnly("perm4");
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user1.getUuid(), project3.uuid())).isEmpty();
  }

  @Test
  public void selectGroupUuidsWithPermissionOnProjectBut_returns_empty_if_project_does_not_exist() {
    ComponentDto project = randomPublicOrPrivateProject();
    UserDto user = insertUser();
    db.users().insertProjectPermissionOnUser(user, "foo", project);

    assertThat(underTest.selectUserIdsWithPermissionOnProjectBut(dbSession, "1234", UserRole.USER))
      .isEmpty();
  }

  @Test
  public void selectGroupUuidsWithPermissionOnProjectBut_returns_only_users_of_projects_which_do_not_have_permission() {
    ComponentDto project = randomPublicOrPrivateProject();
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    db.users().insertProjectPermissionOnUser(user1, "p1", project);
    db.users().insertProjectPermissionOnUser(user2, "p2", project);

    assertThat(underTest.selectUserIdsWithPermissionOnProjectBut(dbSession, project.uuid(), "p2"))
      .extracting("uuid", "login")
      .containsOnly(tuple(user1.getUuid(), user1.getLogin()));
    assertThat(underTest.selectUserIdsWithPermissionOnProjectBut(dbSession, project.uuid(), "p1"))
      .extracting("uuid", "login")
      .containsOnly(tuple(user2.getUuid(), user2.getLogin()));
    assertThat(underTest.selectUserIdsWithPermissionOnProjectBut(dbSession, project.uuid(), "p3"))
      .extracting("uuid", "login")
      .containsOnly(tuple(user1.getUuid(), user1.getLogin()), tuple(user2.getUuid(), user2.getLogin()));
  }

  @Test
  public void selectGroupUuidsWithPermissionOnProjectBut_does_not_return_groups_which_have_no_permission_at_all_on_specified_project() {
    ComponentDto project = randomPublicOrPrivateProject();
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    db.users().insertProjectPermissionOnUser(user1, "p1", project);
    db.users().insertProjectPermissionOnUser(user2, "p2", project);

    assertThat(underTest.selectUserIdsWithPermissionOnProjectBut(dbSession, project.uuid(), "p2"))
      .extracting("uuid", "login")
      .containsOnly(tuple(user1.getUuid(), user1.getLogin()));
    assertThat(underTest.selectUserIdsWithPermissionOnProjectBut(dbSession, project.uuid(), "p1"))
      .extracting("uuid", "login")
      .containsOnly(tuple(user2.getUuid(), user2.getLogin()));
  }

  @Test
  public void deleteByUserId() {
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertPermissionOnUser(user1, SCAN);
    db.users().insertPermissionOnUser(user1, ADMINISTER);
    db.users().insertProjectPermissionOnUser(user1, ADMINISTER_QUALITY_GATES.getKey(), project);
    db.users().insertPermissionOnUser(user2, SCAN);
    db.users().insertProjectPermissionOnUser(user2, ADMINISTER_QUALITY_GATES.getKey(), project);

    underTest.deleteByUserUuid(dbSession, user1);
    dbSession.commit();

    assertThat(db.select("select user_uuid as \"userUuid\", component_uuid as \"projectUuid\", role as \"permission\" from user_roles"))
      .extracting((row) -> row.get("userUuid"), (row) -> row.get("projectUuid"), (row) -> row.get("permission"))
      .containsOnly(tuple(user2.getUuid(), null, SCAN.getKey()), tuple(user2.getUuid(), project.uuid(), ADMINISTER_QUALITY_GATES.getKey()));
  }

  @Test
  public void deleteProjectPermissionOfAnyUser_has_no_effect_if_specified_component_does_not_exist() {
    UserDto user = insertUser();
    db.users().insertPermissionOnUser(user, SCAN);
    ComponentDto component = newPrivateProjectDto();

    int deletedCount = underTest.deleteProjectPermissionOfAnyUser(dbSession, SCAN.getKey(), component);

    assertThat(deletedCount).isZero();
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user.getUuid())).containsOnly(SCAN.getKey());
  }

  @Test
  public void deleteProjectPermissionOfAnyUser_has_no_effect_if_specified_component_has_no_permission_at_all() {
    UserDto user = insertUser();
    db.users().insertPermissionOnUser(user, SCAN);
    ComponentDto project = randomPublicOrPrivateProject();

    int deletedCount = underTest.deleteProjectPermissionOfAnyUser(dbSession, SCAN.getKey(), project);

    assertThat(deletedCount).isZero();
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user.getUuid())).containsOnly(SCAN.getKey());
  }

  @Test
  public void deleteProjectPermissionOfAnyUser_has_no_effect_if_specified_component_does_not_have_specified_permission() {
    UserDto user = insertUser();
    db.users().insertPermissionOnUser(user, SCAN);
    ComponentDto project = randomPublicOrPrivateProject();
    db.users().insertProjectPermissionOnUser(user, SCAN.getKey(), project);

    int deletedCount = underTest.deleteProjectPermissionOfAnyUser(dbSession, "p1", project);

    assertThat(deletedCount).isZero();
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user.getUuid())).containsOnly(SCAN.getKey());
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user.getUuid(), project.uuid())).containsOnly(SCAN.getKey());
  }

  @Test
  public void deleteProjectPermissionOfAnyUser_deletes_specified_permission_for_any_user_on_the_specified_component() {
    UserDto user1 = insertUser();
    UserDto user2 = insertUser();
    db.users().insertPermissionOnUser(user1, SCAN);
    db.users().insertPermissionOnUser(user2, SCAN);
    ComponentDto project1 = randomPublicOrPrivateProject();
    ComponentDto project2 = randomPublicOrPrivateProject();
    db.users().insertProjectPermissionOnUser(user1, SCAN.getKey(), project1);
    db.users().insertProjectPermissionOnUser(user2, SCAN.getKey(), project1);
    db.users().insertProjectPermissionOnUser(user1, SCAN.getKey(), project2);
    db.users().insertProjectPermissionOnUser(user2, SCAN.getKey(), project2);
    db.users().insertProjectPermissionOnUser(user2, PROVISION_PROJECTS.getKey(), project2);

    int deletedCount = underTest.deleteProjectPermissionOfAnyUser(dbSession, SCAN.getKey(), project1);

    assertThat(deletedCount).isEqualTo(2);
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user1.getUuid())).containsOnly(SCAN.getKey());
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user2.getUuid())).containsOnly(SCAN.getKey());
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user1.getUuid(), project1.uuid())).isEmpty();
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user2.getUuid(), project1.uuid())).isEmpty();
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user1.getUuid(), project2.uuid())).containsOnly(SCAN.getKey());
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user2.getUuid(), project2.uuid())).containsOnly(SCAN.getKey(), PROVISION_PROJECTS.getKey());

    deletedCount = underTest.deleteProjectPermissionOfAnyUser(dbSession, SCAN.getKey(), project2);

    assertThat(deletedCount).isEqualTo(2);
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user1.getUuid())).containsOnly(SCAN.getKey());
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user2.getUuid())).containsOnly(SCAN.getKey());
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user1.getUuid(), project1.uuid())).isEmpty();
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user2.getUuid(), project1.uuid())).isEmpty();
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user1.getUuid(), project2.uuid())).containsOnly();
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user2.getUuid(), project2.uuid())).containsOnly(PROVISION_PROJECTS.getKey());
  }

  private ComponentDto randomPublicOrPrivateProject() {
    return new Random().nextBoolean() ? db.components().insertPrivateProject() : db.components().insertPublicProject();
  }

  private UserDto insertUser(Consumer<UserDto> populateUserDto) {
    return db.users().insertUser(populateUserDto);
  }

  private UserDto insertUser() {
    return db.users().insertUser();
  }

  private void expectCount(List<String> projectUuids, CountPerProjectPermission... expected) {
    List<CountPerProjectPermission> got = underTest.countUsersByProjectPermission(dbSession, projectUuids);
    assertThat(got).hasSize(expected.length);

    for (CountPerProjectPermission expect : expected) {
      boolean found = got.stream().anyMatch(b -> b.getPermission().equals(expect.getPermission()) &&
        b.getCount() == expect.getCount() &&
        b.getComponentUuid().equals(expect.getComponentUuid()));
      assertThat(found).isTrue();
    }
  }

  private void expectPermissions(PermissionQuery query, Collection<String> expectedUserUuids, UserPermissionDto... expectedPermissions) {
    assertThat(underTest.selectUserUuidsByQuery(dbSession, query)).containsExactly(expectedUserUuids.toArray(new String[0]));
    List<UserPermissionDto> currentPermissions = underTest.selectUserPermissionsByQuery(dbSession, query, expectedUserUuids);
    assertThat(currentPermissions).hasSize(expectedPermissions.length);
    Tuple[] expectedPermissionsAsTuple = Arrays.stream(expectedPermissions)
      .map(expectedPermission -> tuple(expectedPermission.getUserUuid(), expectedPermission.getPermission(), expectedPermission.getComponentUuid()))
      .toArray(Tuple[]::new);
    assertThat(currentPermissions)
      .extracting(UserPermissionDto::getUserUuid, UserPermissionDto::getPermission, UserPermissionDto::getComponentUuid)
      .containsOnly(expectedPermissionsAsTuple);

    // test method "countUsers()"
    long distinctUsers = stream(expectedPermissions).map(UserPermissionDto::getUserUuid).distinct().count();
    assertThat((long) underTest.countUsersByQuery(dbSession, query)).isEqualTo(distinctUsers);
  }

  private UserPermissionDto addGlobalPermission(String permission, UserDto user) {
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), permission, user.getUuid(), null);
    underTest.insert(dbSession, dto, null, user, null);
    db.commit();
    return dto;
  }

  private UserPermissionDto addProjectPermission(String permission, UserDto user, ComponentDto project) {
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), permission, user.getUuid(), project.uuid());
    underTest.insert(dbSession, dto, project, user, null);
    db.commit();
    return dto;
  }

  private void assertThatProjectPermissionDoesNotExist(UserDto user, String permission, ComponentDto project) {
    assertThat(db.countSql(dbSession, "select count(uuid) from user_roles where role='" + permission + "' and user_uuid='" + user.getUuid()
      + "' and component_uuid='" + project.uuid() + "'"))
      .isZero();
  }

  private void assertThatProjectHasNoPermissions(ComponentDto project) {
    assertThat(db.countSql(dbSession, "select count(uuid) from user_roles where component_uuid='" + project.uuid() + "'")).isZero();
  }
}
