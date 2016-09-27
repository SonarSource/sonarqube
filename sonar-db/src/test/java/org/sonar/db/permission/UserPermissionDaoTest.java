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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.UserDto;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.user.UserTesting.newUserDto;

public class UserPermissionDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private UserPermissionDao underTest = new UserPermissionDao();
  private UserDto user1 = newUserDto().setLogin("login1").setName("Marius").setActive(true);
  private UserDto user2 = newUserDto().setLogin("login2").setName("Marie").setActive(true);
  private UserDto user3 = newUserDto().setLogin("login3").setName("Bernard").setActive(true);
  private ComponentDto project1 = newProjectDto();
  private ComponentDto project2 = newProjectDto();

  @Before
  public void setUp() throws Exception {
    dbTester.getDbClient().userDao().insert(dbTester.getSession(), user1);
    dbTester.getDbClient().userDao().insert(dbTester.getSession(), user2);
    dbTester.getDbClient().userDao().insert(dbTester.getSession(), user3);
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), project1);
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), project2);
    dbTester.commit();
  }

  @Test
  public void select_global_permissions() {
    UserPermissionDto global1 = insertGlobalPermission(SYSTEM_ADMIN, user1.getId());
    UserPermissionDto global2 = insertGlobalPermission(SYSTEM_ADMIN, user2.getId());
    UserPermissionDto global3 = insertGlobalPermission(PROVISIONING, user2.getId());
    UserPermissionDto project1Perm = insertProjectPermission(USER, user3.getId(), this.project1.getId());

    // global permissions of users who has at least one global permission
    PermissionQuery query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(query, null, global1, global2, global3);

    // default query returns all permissions
    query = PermissionQuery.builder().build();
    expectPermissions(query, null, global1, global2, global3, project1Perm);

    // global permissions of user1
    query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(query, asList(user1.getLogin()), global1);

    // global permissions of user2
    query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(query, asList(user2.getLogin()), global2, global3);

    // global permissions of user1, user2 and another one
    query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(query, asList(user1.getLogin(), user2.getLogin(), "missing"), global1, global2, global3);

    // empty global permissions if login does not exist
    query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(query, asList("missing"));

    // empty global permissions if user does not have any
    query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(query, asList(user3.getLogin()));

    // user3 has no global permissions
    query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(query, asList(user3.getLogin()));

    // global permissions "admin"
    query = PermissionQuery.builder().setPermission(SYSTEM_ADMIN).build();
    expectPermissions(query, null, global1, global2);

    // empty if nobody has the specified global permission
    query = PermissionQuery.builder().setPermission("missing").build();
    expectPermissions(query, null);

    // search by user name (matches 2 users)
    query = PermissionQuery.builder().withAtLeastOnePermission().setSearchQuery("Mari").build();
    expectPermissions(query, null, global1, global2, global3);

    // search by user name (matches 2 users) and global permission
    query = PermissionQuery.builder().setSearchQuery("Mari").setPermission(PROVISIONING).build();
    expectPermissions(query, null, global3);

    // search by user name (no match)
    query = PermissionQuery.builder().setSearchQuery("Unknown").build();
    expectPermissions(query, null);
  }

  @Test
  public void select_project_permissions() {
    insertGlobalPermission(SYSTEM_ADMIN, user1.getId());
    UserPermissionDto perm1 = insertProjectPermission(USER, user1.getId(), project1.getId());
    UserPermissionDto perm2 = insertProjectPermission(ISSUE_ADMIN, user1.getId(), project1.getId());
    UserPermissionDto perm3 = insertProjectPermission(ISSUE_ADMIN, user2.getId(), project1.getId());
    insertProjectPermission(ISSUE_ADMIN, user3.getId(), project2.getId());

    // project permissions of users who has at least one permission on this project
    PermissionQuery query = PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(query, null, perm1, perm2, perm3);

    // default query on the project returns all permissions
    query = PermissionQuery.builder().setComponentUuid(project1.uuid()).build();
    expectPermissions(query, null, perm1, perm2, perm3);

    // project permissions of user1
    query = PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(query, asList(user1.getLogin()), perm1, perm2);

    // project permissions of user2
    query = PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(query, asList(user2.getLogin()), perm3);

    // project permissions of user2 and another one
    query = PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(query, asList(user2.getLogin(), "missing"), perm3);

    // empty project permissions if login does not exist
    query = PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(query, asList("missing"));

    // empty project permissions if user does not have any
    query = PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(query, asList(user3.getLogin()));

    // empty if nobody has the specified global permission
    query = PermissionQuery.builder().setPermission("missing").setComponentUuid(project1.uuid()).build();
    expectPermissions(query, null);

    // search by user name (matches 2 users)
    query = PermissionQuery.builder().setSearchQuery("Mari").setComponentUuid(project1.uuid()).build();
    expectPermissions(query, null, perm1, perm2, perm3);

    // search by user name (matches 2 users) and project permission
    query = PermissionQuery.builder().setSearchQuery("Mari").setPermission(ISSUE_ADMIN).setComponentUuid(project1.uuid()).build();
    expectPermissions(query, null, perm2, perm3);

    // search by user name (no match)
    query = PermissionQuery.builder().setSearchQuery("Unknown").setComponentUuid(project1.uuid()).build();
    expectPermissions(query, null);

    // permissions of unknown project
    query = PermissionQuery.builder().setComponentUuid("missing").build();
    expectPermissions(query, null);
  }

  private void expectPermissions(PermissionQuery query, @Nullable Collection<String> logins, UserPermissionDto... expected) {
    // test method "select()"
    List<ExtendedUserPermissionDto> permissions = underTest.select(dbTester.getSession(), query, logins);
    assertThat(permissions).hasSize(expected.length);
    for (UserPermissionDto permission : expected) {
      assertThat(permissions).matches(list -> list.stream().anyMatch(new HasPermission(permission)));
    }

    if (logins == null) {
      // test method "countUsers()", which does not make sense if users are filtered
      long distinctUsers = Arrays.stream(expected).mapToLong(p -> p.getUserId()).distinct().count();
      assertThat((long) underTest.countUsers(dbTester.getSession(), query)).isEqualTo(distinctUsers);
    }
  }

  private UserPermissionDto insertGlobalPermission(String permission, long userId) {
    UserPermissionDto dto = new UserPermissionDto(permission, userId, null);
    underTest.insert(dbTester.getSession(), dto);
    dbTester.commit();
    return dto;
  }

  private UserPermissionDto insertProjectPermission(String permission, long userId, long projectId) {
    UserPermissionDto dto = new UserPermissionDto(permission, userId, projectId);
    underTest.insert(dbTester.getSession(), dto);
    dbTester.commit();
    return dto;
  }

  private final class HasPermission implements Predicate<ExtendedUserPermissionDto> {
    private final UserPermissionDto expect;

    HasPermission(UserPermissionDto dto) {
      this.expect = dto;
    }

    @Override
    public boolean test(ExtendedUserPermissionDto dto) {
      return dto.getUserId() == expect.getUserId() &&
        dto.getPermission().equals(expect.getPermission()) &&
        Objects.equals(dto.getComponentId(), expect.getComponentId());
    }
  }

  // @Test
  // public void count_logins_by_query() {
  // UserDto user1 = userDb.insertUser(newUserDto());
  // UserDto user2 = userDb.insertUser(newUserDto());
  // UserDto user3 = userDb.insertUser(newUserDto());
  // UserDto user4 = userDb.insertUser(newUserDto());
  // ComponentDto project = componentDb.insertComponent(newProjectDto());
  //
  // permissionDb.addGlobalPermissionToUser(SYSTEM_ADMIN, user1.getId());
  // permissionDb.addGlobalPermissionToUser(SYSTEM_ADMIN, user2.getId());
  // permissionDb.addGlobalPermissionToUser(SYSTEM_ADMIN, user3.getId());
  // permissionDb.addGlobalPermissionToUser(PROVISIONING, user3.getId());
  // permissionDb.addProjectPermissionToUser(USER, user4.getId(), project.getId());
  //
  // assertThat(countUsersByQuery(PermissionQuery.builder().build())).isEqualTo(4);
  // assertThat(countUsersByQuery(PermissionQuery.builder().setPermission(PROVISIONING).build())).isEqualTo(1);
  // assertThat(countUsersByQuery(PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project.uuid()).build())).isEqualTo(1);
  // }
  //
  // @Test
  // public void select_logins_by_query_is_ordered_by_name() {
  // UserDto user3 = userDb.insertUser(newUserDto().setName("3-name").setLogin("3-login"));
  // UserDto user2 = userDb.insertUser(newUserDto().setName("2-name").setLogin("2-login"));
  // UserDto user1 = userDb.insertUser(newUserDto().setName("1-name").setLogin("1-login"));
  // UserDto user4 = userDb.insertUser(newUserDto().setName("4-name").setLogin("4-login"));
  //
  // ComponentDto project = componentDb.insertComponent(newProjectDto("project-uuid"));
  //
  // permissionDb.addGlobalPermissionToUser(SYSTEM_ADMIN, user3.getId());
  // permissionDb.addGlobalPermissionToUser(PROVISIONING, user3.getId());
  // permissionDb.addGlobalPermissionToUser(SYSTEM_ADMIN, user2.getId());
  // permissionDb.addGlobalPermissionToUser(SYSTEM_ADMIN, user1.getId());
  // permissionDb.addProjectPermissionToUser(USER, user4.getId(), project.getId());
  //
  // assertThat(selectLoginsByQuery(PermissionQuery.builder().build())).containsExactly("1-login", "2-login", "3-login", "4-login");
  // }
  //
  // @Test
  // public void select_logins_are_paginated() {
  // IntStream.rangeClosed(0, 9)
  // .forEach(i -> userDb.insertUser(newUserDto().setName(i + "-name").setLogin(i + "-login")));
  //
  // PermissionQuery.Builder dbQuery = PermissionQuery.builder().setPageIndex(2).setPageSize(3);
  // List<String> result = selectLoginsByQuery(dbQuery.build());
  // int count = countUsersByQuery(dbQuery.build());
  //
  // assertThat(result).hasSize(3).containsOnlyOnce("3-login", "4-login", "5-login");
  // assertThat(count).isEqualTo(10);
  // }
  //
  // @Test
  // public void select_logins_with_query() {
  // userDb.insertUser(newUserDto().setName("1-name").setLogin("1-login"));
  // userDb.insertUser(newUserDto().setName("unknown"));
  //
  // PermissionQuery.Builder dbQuery = PermissionQuery.builder().setSearchQuery("nam");
  // List<String> users = selectLoginsByQuery(dbQuery.build());
  //
  // assertThat(users).hasSize(1);
  // assertThat(users.get(0)).isEqualTo("1-login");
  // }
  //
  // @Test
  // public void select_logins_with_global_permissions() {
  // UserDto user3 = userDb.insertUser(newUserDto().setName("3-name"));
  // UserDto user2 = userDb.insertUser(newUserDto().setName("2-name"));
  // UserDto user1 = userDb.insertUser(newUserDto().setName("1-name"));
  // UserDto user4 = userDb.insertUser(newUserDto().setName("4-name"));
  //
  // ComponentDto project = componentDb.insertComponent(newProjectDto("project-uuid"));
  //
  // permissionDb.addGlobalPermissionToUser(SCAN_EXECUTION, user3.getId());
  // permissionDb.addGlobalPermissionToUser(PROVISIONING, user3.getId());
  // permissionDb.addGlobalPermissionToUser(SCAN_EXECUTION, user2.getId());
  // permissionDb.addGlobalPermissionToUser(SYSTEM_ADMIN, user1.getId());
  // // project permission
  // permissionDb.addProjectPermissionToUser(SCAN_EXECUTION, user4.getId(), project.getId());
  //
  // PermissionQuery.Builder dbQuery = PermissionQuery.builder()
  // .setComponentUuid(null)
  // .setPermission(SCAN_EXECUTION)
  // .withAtLeastOnePermission();
  // List<String> result = selectLoginsByQuery(dbQuery.build());
  // int count = countUsersByQuery(dbQuery.build());
  //
  // assertThat(result).hasSize(2).containsExactly(user2.getLogin(), user3.getLogin());
  // assertThat(count).isEqualTo(2);
  // }
  //
  // @Test
  // public void select_logins_with_project_permissions() {
  // // create reversed list of user
  // List<UserDto> dbUsers = IntStream.rangeClosed(1, 4)
  // .mapToObj(i -> userDb.insertUser(newUserDto().setName(i + "-name").setLogin(i + "-login")))
  // .collect(Collectors.toList());
  //
  // ComponentDto project = componentDb.insertComponent(newProjectDto("project-uuid"));
  //
  // permissionDb.addProjectPermissionToUser(SCAN_EXECUTION, dbUsers.get(0).getId(), project.getId());
  // permissionDb.addProjectPermissionToUser(PROVISIONING, dbUsers.get(0).getId(), project.getId());
  // permissionDb.addProjectPermissionToUser(SCAN_EXECUTION, dbUsers.get(1).getId(), project.getId());
  // permissionDb.addProjectPermissionToUser(SYSTEM_ADMIN, dbUsers.get(2).getId(), project.getId());
  // // global permission
  // permissionDb.addGlobalPermissionToUser(SCAN_EXECUTION, dbUsers.get(3).getId());
  //
  // PermissionQuery.Builder dbQuery = PermissionQuery.builder()
  // .setComponentUuid(null)
  // .setPermission(SCAN_EXECUTION)
  // .withAtLeastOnePermission()
  // .setComponentUuid(project.uuid());
  // List<String> result = selectLoginsByQuery(dbQuery.build());
  // int count = countUsersByQuery(dbQuery.build());
  //
  // assertThat(result).hasSize(2).containsOnlyOnce(dbUsers.get(0).getLogin(), dbUsers.get(1).getLogin());
  // assertThat(count).isEqualTo(2);
  // }
  //
  // @Test
  // public void select_user_permissions_by_logins_with_global_permissions() {
  // UserDto firstUser = userDb.insertUser(newUserDto());
  // permissionDb.addGlobalPermissionToUser(ADMIN, firstUser.getId());
  //
  // UserDto secondUser = userDb.insertUser(newUserDto());
  // permissionDb.addGlobalPermissionToUser(USER, secondUser.getId());
  //
  // UserDto thirdUser = userDb.insertUser(newUserDto());
  // ComponentDto project = componentDb.insertComponent(newProjectDto());
  // permissionDb.addProjectPermissionToUser(ADMIN, thirdUser.getId(), project.getId());
  //
  // assertThat(underTest.selectUserPermissionsByLoginsAnProject(session, (asList(firstUser.getLogin(), secondUser.getLogin(),
  // thirdUser.getLogin())), null))
  // .extracting(org.sonar.db.user.UserPermissionDto::getUserId, org.sonar.db.user.UserPermissionDto::getPermission,
  // org.sonar.db.user.UserPermissionDto::getComponentId)
  // .containsOnly(
  // tuple(firstUser.getId(), ADMIN, null),
  // tuple(secondUser.getId(), USER, null));
  //
  // assertThat(underTest.selectUserPermissionsByLoginsAnProject(session, singletonList(thirdUser.getLogin()), null)).isEmpty();
  // assertThat(underTest.selectUserPermissionsByLoginsAnProject(session, singletonList("unknown"), null)).isEmpty();
  // assertThat(underTest.selectUserPermissionsByLoginsAnProject(session, Collections.emptyList(), null)).isEmpty();
  // }
  //
  // @Test
  // public void select_user_permissions_by_logins_with_project_permissions() {
  // UserDto firstUser = userDb.insertUser(newUserDto());
  // ComponentDto project = componentDb.insertComponent(newProjectDto());
  // permissionDb.addProjectPermissionToUser(ADMIN, firstUser.getId(), project.getId());
  //
  // UserDto secondUser = userDb.insertUser(newUserDto());
  // permissionDb.addProjectPermissionToUser(USER, secondUser.getId(), project.getId());
  //
  // UserDto thirdUser = userDb.insertUser(newUserDto());
  // ComponentDto anotherProject = componentDb.insertComponent(newProjectDto());
  // permissionDb.addProjectPermissionToUser(ADMIN, thirdUser.getId(), anotherProject.getId());
  //
  // assertThat(underTest.selectUserPermissionsByLoginsAnProject(session, (asList(firstUser.getLogin(), secondUser.getLogin(),
  // thirdUser.getLogin())), project.getId()))
  // .extracting(org.sonar.db.user.UserPermissionDto::getUserId, org.sonar.db.user.UserPermissionDto::getPermission,
  // org.sonar.db.user.UserPermissionDto::getComponentId)
  // .containsOnly(
  // tuple(firstUser.getId(), ADMIN, project.getId()),
  // tuple(secondUser.getId(), USER, project.getId()));
  //
  // assertThat(underTest.selectUserPermissionsByLoginsAnProject(session, (asList(firstUser.getLogin(), secondUser.getLogin(),
  // thirdUser.getLogin())), anotherProject.getId()))
  // .extracting(org.sonar.db.user.UserPermissionDto::getUserId, org.sonar.db.user.UserPermissionDto::getPermission,
  // org.sonar.db.user.UserPermissionDto::getComponentId)
  // .containsOnly(
  // tuple(thirdUser.getId(), ADMIN, anotherProject.getId()));
  //
  // assertThat(underTest.selectUserPermissionsByLoginsAnProject(session, singletonList(thirdUser.getLogin()), project.getId())).isEmpty();
  // assertThat(underTest.selectUserPermissionsByLoginsAnProject(session, singletonList("unknown"), project.getId())).isEmpty();
  // assertThat(underTest.selectUserPermissionsByLoginsAnProject(session, Collections.emptyList(), project.getId())).isEmpty();
  // }
  //
  // @Test
  // public void user_count_by_component_and_permission() {
  // UserDto user1 = userDb.insertUser();
  // UserDto user2 = userDb.insertUser();
  // UserDto user3 = userDb.insertUser();
  //
  // addPermissionToUser(ISSUE_ADMIN, user1.getId(), 42L);
  // addPermissionToUser(ADMIN, user1.getId(), 123L);
  // addPermissionToUser(ADMIN, user2.getId(), 123L);
  // addPermissionToUser(ADMIN, user3.getId(), 123L);
  // addPermissionToUser(USER, user1.getId(), 123L);
  // addPermissionToUser(USER, user1.getId(), 456L);
  //
  // final List<CountByProjectAndPermissionDto> result = new ArrayList<>();
  // underTest.usersCountByComponentIdAndPermission(dbTester.getSession(), asList(123L, 456L, 789L),
  // context -> result.add((CountByProjectAndPermissionDto) context.getResultObject()));
  // assertThat(result).hasSize(3);
  // assertThat(result).extracting("permission").containsOnly(ADMIN, USER);
  // assertThat(result).extracting("componentId").containsOnly(123L, 456L);
  // assertThat(result).extracting("count").containsOnly(3, 1);
  // }
  //
  // private List<String> selectLoginsByQuery(PermissionQuery query) {
  // return underTest.selectLoginsByPermissionQuery(session, query);
  // }
  //
  // private int countUsersByQuery(PermissionQuery query) {
  // return underTest.countUsersByQuery(session, query);
  // }
  //
  // private void addPermissionToUser(String permission, long userId, long resourceId) {
  // dbTester.getDbClient().roleDao().insertUserRole(dbTester.getSession(), new org.sonar.db.user.UserPermissionDto()
  // .setPermission(permission)
  // .setUserId(userId)
  // .setComponentId(resourceId));
  // dbTester.commit();
  // }

}
