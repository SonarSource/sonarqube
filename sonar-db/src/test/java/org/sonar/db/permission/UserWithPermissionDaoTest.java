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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.UserDbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserPermissionDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.user.UserTesting.newUserDto;

public class UserWithPermissionDaoTest {

  private static final long COMPONENT_ID = 100L;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  UserDbTester userDb = new UserDbTester(dbTester);
  PermissionDbTester permissionDb = new PermissionDbTester(dbTester);
  ComponentDbTester componentDb = new ComponentDbTester(dbTester);
  DbSession session = dbTester.getSession();

  PermissionDao underTest = new PermissionDao(dbTester.myBatis());

  @Test
  public void select_all_users_for_project_permission() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    OldPermissionQuery query = OldPermissionQuery.builder().permission("user").build();
    List<UserWithPermissionDto> result = selectUsers(session, query, COMPONENT_ID);
    assertThat(result).hasSize(3);

    UserWithPermissionDto user1 = result.get(0);
    assertThat(user1.getLogin()).isEqualTo("user1");
    assertThat(user1.getName()).isEqualTo("User1");
    assertThat(user1.getPermission()).isNotNull();

    UserWithPermissionDto user2 = result.get(1);
    assertThat(user2.getLogin()).isEqualTo("user2");
    assertThat(user2.getName()).isEqualTo("User2");
    assertThat(user2.getPermission()).isNotNull();

    UserWithPermissionDto user3 = result.get(2);
    assertThat(user3.getLogin()).isEqualTo("user3");
    assertThat(user3.getName()).isEqualTo("User3");
    assertThat(user3.getPermission()).isNull();
  }

  @Test
  public void select_all_users_for_global_permission() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    OldPermissionQuery query = OldPermissionQuery.builder().permission("admin").build();
    List<UserWithPermissionDto> result = selectUsers(session, query, null);
    assertThat(result).hasSize(3);

    UserWithPermissionDto user1 = result.get(0);
    assertThat(user1.getName()).isEqualTo("User1");
    assertThat(user1.getPermission()).isNotNull();

    UserWithPermissionDto user2 = result.get(1);
    assertThat(user2.getName()).isEqualTo("User2");
    assertThat(user2.getPermission()).isNull();

    UserWithPermissionDto user3 = result.get(2);
    assertThat(user3.getName()).isEqualTo("User3");
    assertThat(user3.getPermission()).isNull();
  }

  @Test
  public void select_only_user_with_permission() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    // user1 and user2 have permission user
    assertThat(selectUsers(session, OldPermissionQuery.builder().permission("user").membership(OldPermissionQuery.IN).build(), COMPONENT_ID)).hasSize(2);
  }

  @Test
  public void select_only_user_without_permission() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    // Only user3 has not the user permission
    assertThat(selectUsers(session, OldPermissionQuery.builder().permission("user").membership(OldPermissionQuery.OUT).build(), COMPONENT_ID)).hasSize(1);
  }

  @Test
  public void search_by_user_name() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    List<UserWithPermissionDto> result = selectUsers(session, OldPermissionQuery.builder().permission("user").search("SEr1").build(), COMPONENT_ID);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("User1");

    result = selectUsers(session, OldPermissionQuery.builder().permission("user").search("user").build(), COMPONENT_ID);
    assertThat(result).hasSize(3);
  }

  @Test
  public void select_only_enable_users() {
    dbTester.prepareDbUnit(getClass(), "select_only_enable_users.xml");

    OldPermissionQuery query = OldPermissionQuery.builder().permission("user").build();
    List<UserWithPermissionDto> result = selectUsers(session, query, COMPONENT_ID);
    assertThat(result).hasSize(3);

    // Disabled user should not be returned
    assertThat(result.stream().anyMatch(userPermission -> "disabledUser".equals(userPermission.getLogin()))).isFalse();
  }

  @Test
  public void select_user_permissions_by_query() {
    UserDto user3 = userDb.insertUser(newUserDto().setName("3-name").setLogin("3-login"));
    UserDto user2 = userDb.insertUser(newUserDto().setName("2-name").setLogin("2-login"));
    UserDto user1 = userDb.insertUser(newUserDto().setName("1-name").setLogin("1-login"));
    UserDto user4 = userDb.insertUser(newUserDto().setName("4-name").setLogin("4-login"));

    ComponentDto project = componentDb.insertComponent(newProjectDto("project-uuid"));

    permissionDb.addGlobalPermissionToUser(GlobalPermissions.SYSTEM_ADMIN, user3.getId());
    permissionDb.addGlobalPermissionToUser(GlobalPermissions.PROVISIONING, user3.getId());
    permissionDb.addGlobalPermissionToUser(GlobalPermissions.SYSTEM_ADMIN, user2.getId());
    permissionDb.addGlobalPermissionToUser(GlobalPermissions.SYSTEM_ADMIN, user1.getId());
    permissionDb.addProjectPermissionToUser(UserRole.USER, user4.getId(), project.getId());

    PermissionQuery.Builder dbQuery = PermissionQuery.builder();
    List<String> logins = selectLoginsByQuery(dbQuery);
    int count = countUsersByQuery(dbQuery);
    List<UserPermissionDto> permissions = selectUserPermissionsByQuery(dbQuery);

    assertThat(logins)
      .hasSize(4)
      .containsExactly("1-login", "2-login", "3-login", "4-login");
    assertThat(count).isEqualTo(4);

    assertThat(permissions).hasSize(5).extracting(UserPermissionDto::getUserId, UserPermissionDto::getPermission)
      .containsOnlyOnce(
        tuple(user1.getId(), GlobalPermissions.SYSTEM_ADMIN),
        tuple(user2.getId(), GlobalPermissions.SYSTEM_ADMIN),
        tuple(user3.getId(), GlobalPermissions.SYSTEM_ADMIN),
        tuple(user3.getId(), GlobalPermissions.PROVISIONING),
        tuple(user4.getId(), UserRole.USER));
  }

  @Test
  public void select_logins_paginated() {
    IntStream.rangeClosed(0, 9)
      .forEach(i -> userDb.insertUser(newUserDto().setName(i + "-name").setLogin(i + "-login")));

    PermissionQuery.Builder dbQuery = PermissionQuery.builder().setPageIndex(2).setPageSize(3);
    List<String> result = selectLoginsByQuery(dbQuery);
    int count = countUsersByQuery(dbQuery);

    assertThat(result).hasSize(3).containsOnlyOnce("3-login", "4-login", "5-login");
    assertThat(count).isEqualTo(10);
  }

  @Test
  public void select_logins_with_query() {
    userDb.insertUser(newUserDto().setName("1-name").setLogin("1-login"));
    userDb.insertUser(newUserDto().setName("unknown"));

    PermissionQuery.Builder dbQuery = PermissionQuery.builder().setSearchQuery("nam");
    List<String> users = selectLoginsByQuery(dbQuery);
    int count = countUsersByQuery(dbQuery);

    assertThat(users).hasSize(1);
    assertThat(users.get(0)).isEqualTo("1-login");
    assertThat(count).isEqualTo(1);
  }

  @Test
  public void select_user_permissions() {
    UserDto firstUser = userDb.insertUser(newUserDto().setLogin("user-login").setName("1-name"));
    UserDto secondUser = userDb.insertUser(newUserDto().setLogin("second-login").setName("2-name"));
    UserDto anotherUser = userDb.insertUser(newUserDto().setLogin("another-login"));
    ComponentDto project = componentDb.insertComponent(newProjectDto());
    permissionDb.addProjectPermissionToUser(UserRole.ADMIN, firstUser.getId(), project.getId());
    permissionDb.addProjectPermissionToUser(UserRole.ADMIN, secondUser.getId(), project.getId());
    permissionDb.addProjectPermissionToUser(UserRole.ADMIN, anotherUser.getId(), project.getId());

    PermissionQuery.Builder dbQuery = PermissionQuery.builder()
      .setComponentUuid(project.uuid())
      .setLogins(newArrayList("user-login", "second-login"))
      .withPermissionOnly();
    List<UserPermissionDto> result = selectUserPermissionsByQuery(dbQuery);

    assertThat(result).hasSize(2)
      .extracting(UserPermissionDto::getUserId, UserPermissionDto::getPermission, UserPermissionDto::getComponentId)
      .contains(tuple(firstUser.getId(), UserRole.ADMIN, project.getId()));
  }

  @Test
  public void select_logins_with_global_permissions() {
    UserDto user3 = userDb.insertUser(newUserDto().setName("3-name"));
    UserDto user2 = userDb.insertUser(newUserDto().setName("2-name"));
    UserDto user1 = userDb.insertUser(newUserDto().setName("1-name"));
    UserDto user4 = userDb.insertUser(newUserDto().setName("4-name"));

    ComponentDto project = componentDb.insertComponent(newProjectDto("project-uuid"));

    permissionDb.addGlobalPermissionToUser(GlobalPermissions.SCAN_EXECUTION, user3.getId());
    permissionDb.addGlobalPermissionToUser(GlobalPermissions.PROVISIONING, user3.getId());
    permissionDb.addGlobalPermissionToUser(GlobalPermissions.SCAN_EXECUTION, user2.getId());
    permissionDb.addGlobalPermissionToUser(GlobalPermissions.SYSTEM_ADMIN, user1.getId());
    // project permission
    permissionDb.addProjectPermissionToUser(GlobalPermissions.SCAN_EXECUTION, user4.getId(), project.getId());

    PermissionQuery.Builder dbQuery = PermissionQuery.builder()
      .setComponentUuid(null)
      .setPermission(GlobalPermissions.SCAN_EXECUTION)
      .withPermissionOnly();
    List<String> result = selectLoginsByQuery(dbQuery);
    int count = countUsersByQuery(dbQuery);

    assertThat(result).hasSize(2).containsExactly(user2.getLogin(), user3.getLogin());
    assertThat(count).isEqualTo(2);
  }

  @Test
  public void select_logins_with_project_permissions() {
    // create reversed list of user
    List<UserDto> dbUsers = IntStream.rangeClosed(1, 4)
      .mapToObj(i -> userDb.insertUser(newUserDto().setName(i + "-name").setLogin(i + "-login")))
      .collect(Collectors.toList());

    ComponentDto project = componentDb.insertComponent(newProjectDto("project-uuid"));

    permissionDb.addProjectPermissionToUser(GlobalPermissions.SCAN_EXECUTION, dbUsers.get(0).getId(), project.getId());
    permissionDb.addProjectPermissionToUser(GlobalPermissions.PROVISIONING, dbUsers.get(0).getId(), project.getId());
    permissionDb.addProjectPermissionToUser(GlobalPermissions.SCAN_EXECUTION, dbUsers.get(1).getId(), project.getId());
    permissionDb.addProjectPermissionToUser(GlobalPermissions.SYSTEM_ADMIN, dbUsers.get(2).getId(), project.getId());
    // global permission
    permissionDb.addGlobalPermissionToUser(GlobalPermissions.SCAN_EXECUTION, dbUsers.get(3).getId());

    PermissionQuery.Builder dbQuery = PermissionQuery.builder()
      .setComponentUuid(null)
      .setPermission(GlobalPermissions.SCAN_EXECUTION)
      .withPermissionOnly()
      .setComponentUuid(project.uuid());
    List<String> result = selectLoginsByQuery(dbQuery);
    int count = countUsersByQuery(dbQuery);

    assertThat(result).hasSize(2).containsOnlyOnce(dbUsers.get(0).getLogin(), dbUsers.get(1).getLogin());
    assertThat(count).isEqualTo(2);
  }

  @Test
  public void should_be_sorted_by_user_name() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions_should_be_sorted_by_user_name.xml");

    List<UserWithPermissionDto> result = selectUsers(session, OldPermissionQuery.builder().permission("user").build(), COMPONENT_ID);
    assertThat(result).hasSize(3);
    assertThat(result.get(0).getName()).isEqualTo("User1");
    assertThat(result.get(1).getName()).isEqualTo("User2");
    assertThat(result.get(2).getName()).isEqualTo("User3");
  }

  @Test
  public void should_be_paginated() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    List<UserWithPermissionDto> result = underTest.selectUsers(session, OldPermissionQuery.builder().permission("user").build(), COMPONENT_ID, 0, 2);
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("User1");
    assertThat(result.get(1).getName()).isEqualTo("User2");

    result = underTest.selectUsers(session, OldPermissionQuery.builder().permission("user").build(), COMPONENT_ID, 1, 2);
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("User2");
    assertThat(result.get(1).getName()).isEqualTo("User3");

    result = underTest.selectUsers(session, OldPermissionQuery.builder().permission("user").build(), COMPONENT_ID, 2, 1);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("User3");
  }

  @Test
  public void user_count_by_component_and_permission() {
    UserDto user1 = userDb.insertUser();
    UserDto user2 = userDb.insertUser();
    UserDto user3 = userDb.insertUser();

    addPermissionToUser(ISSUE_ADMIN, user1.getId(), 42L);
    addPermissionToUser(ADMIN, user1.getId(), 123L);
    addPermissionToUser(ADMIN, user2.getId(), 123L);
    addPermissionToUser(ADMIN, user3.getId(), 123L);
    addPermissionToUser(USER, user1.getId(), 123L);
    addPermissionToUser(USER, user1.getId(), 456L);

    final List<CountByProjectAndPermissionDto> result = new ArrayList<>();
    underTest.usersCountByComponentIdAndPermission(dbTester.getSession(), Arrays.asList(123L, 456L, 789L),
      context -> result.add((CountByProjectAndPermissionDto) context.getResultObject()));
    assertThat(result).hasSize(3);
    assertThat(result).extracting("permission").containsOnly(ADMIN, USER);
    assertThat(result).extracting("componentId").containsOnly(123L, 456L);
    assertThat(result).extracting("count").containsOnly(3, 1);
  }

  private List<UserWithPermissionDto> selectUsers(DbSession session, OldPermissionQuery query, @Nullable Long componentId) {
    return underTest.selectUsers(session, query, componentId, 0, Integer.MAX_VALUE);
  }

  private List<String> selectLoginsByQuery(PermissionQuery.Builder query) {
    return underTest.selectLoginsByPermissionQuery(session, query.build());
  }

  private int countUsersByQuery(PermissionQuery.Builder query) {
    return underTest.countUsersByQuery(session, query.build());
  }

  private List<UserPermissionDto> selectUserPermissionsByQuery(PermissionQuery.Builder query) {
    return underTest.selectUserPermissionsByQuery(session, query.build());
  }

  private void addPermissionToUser(String permission, long userId, long resourceId) {
    dbTester.getDbClient().roleDao().insertUserRole(dbTester.getSession(), new UserPermissionDto()
      .setPermission(permission)
      .setUserId(userId)
      .setComponentId(resourceId));
    dbTester.commit();
  }

}
