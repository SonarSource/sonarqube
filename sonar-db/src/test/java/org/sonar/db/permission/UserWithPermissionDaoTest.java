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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserRoleDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.user.UserTesting.newUserDto;

public class UserWithPermissionDaoTest {

  private static final long COMPONENT_ID = 100L;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  DbSession session = dbTester.getSession();

  PermissionDao underTest = new PermissionDao(dbTester.myBatis());

  @Test
  public void select_all_users_for_project_permission() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    PermissionQuery query = PermissionQuery.builder().permission("user").build();
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

    PermissionQuery query = PermissionQuery.builder().permission("admin").build();
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
    assertThat(selectUsers(session, PermissionQuery.builder().permission("user").membership(PermissionQuery.IN).build(), COMPONENT_ID)).hasSize(2);
  }

  @Test
  public void select_only_user_without_permission() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    // Only user3 has not the user permission
    assertThat(selectUsers(session, PermissionQuery.builder().permission("user").membership(PermissionQuery.OUT).build(), COMPONENT_ID)).hasSize(1);
  }

  @Test
  public void search_by_user_name() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    List<UserWithPermissionDto> result = selectUsers(session, PermissionQuery.builder().permission("user").search("SEr1").build(), COMPONENT_ID);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("User1");

    result = selectUsers(session, PermissionQuery.builder().permission("user").search("user").build(), COMPONENT_ID);
    assertThat(result).hasSize(3);
  }

  @Test
  public void select_only_enable_users() {
    dbTester.prepareDbUnit(getClass(), "select_only_enable_users.xml");

    PermissionQuery query = PermissionQuery.builder().permission("user").build();
    List<UserWithPermissionDto> result = selectUsers(session, query, COMPONENT_ID);
    assertThat(result).hasSize(3);

    // Disabled user should not be returned
    assertThat(Iterables.find(result, new Predicate<UserWithPermissionDto>() {
      @Override
      public boolean apply(@Nullable UserWithPermissionDto input) {
        return input.getLogin().equals("disabledUser");
      }
    }, null)).isNull();
  }

  @Test
  public void should_be_sorted_by_user_name() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions_should_be_sorted_by_user_name.xml");

    List<UserWithPermissionDto> result = selectUsers(session, PermissionQuery.builder().permission("user").build(), COMPONENT_ID);
    assertThat(result).hasSize(3);
    assertThat(result.get(0).getName()).isEqualTo("User1");
    assertThat(result.get(1).getName()).isEqualTo("User2");
    assertThat(result.get(2).getName()).isEqualTo("User3");
  }

  @Test
  public void should_be_paginated() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    List<UserWithPermissionDto> result = underTest.selectUsers(session, PermissionQuery.builder().permission("user").build(), COMPONENT_ID, 0, 2);
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("User1");
    assertThat(result.get(1).getName()).isEqualTo("User2");

    result = underTest.selectUsers(session, PermissionQuery.builder().permission("user").build(), COMPONENT_ID, 1, 2);
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("User2");
    assertThat(result.get(1).getName()).isEqualTo("User3");

    result = underTest.selectUsers(session, PermissionQuery.builder().permission("user").build(), COMPONENT_ID, 2, 1);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("User3");
  }

  @Test
  public void user_count_by_component_and_permission() {
    UserDto user1 = insertUser(newUserDto());
    UserDto user2 = insertUser(newUserDto());
    UserDto user3 = insertUser(newUserDto());

    insertUserRole(ISSUE_ADMIN, user1.getId(), 42L);
    insertUserRole(ADMIN, user1.getId(), 123L);
    insertUserRole(ADMIN, user2.getId(), 123L);
    insertUserRole(ADMIN, user3.getId(), 123L);
    insertUserRole(USER, user1.getId(), 123L);
    insertUserRole(USER, user1.getId(), 456L);
    commit();

    final List<CountByProjectAndPermissionDto> result = new ArrayList<>();
    underTest.usersCountByComponentIdAndPermission(dbTester.getSession(), Arrays.asList(123L, 456L, 789L), new ResultHandler() {
      @Override
      public void handleResult(ResultContext context) {
        result.add((CountByProjectAndPermissionDto) context.getResultObject());
      }
    });
    assertThat(result).hasSize(3);
    assertThat(result).extracting("permission").containsOnly(ADMIN, USER);
    assertThat(result).extracting("componentId").containsOnly(123L, 456L);
    assertThat(result).extracting("count").containsOnly(3, 1);
  }

  private List<UserWithPermissionDto> selectUsers(DbSession session, PermissionQuery query, @Nullable Long componentId) {
    return underTest.selectUsers(session, query, componentId, 0, Integer.MAX_VALUE);
  }

  private UserDto insertUser(UserDto userDto) {
    return dbTester.getDbClient().userDao().insert(dbTester.getSession(), userDto.setActive(true));
  }

  private void insertUserRole(String permission, long userId, long resourceId) {
    dbTester.getDbClient().roleDao().insertUserRole(dbTester.getSession(), new UserRoleDto()
      .setRole(permission)
      .setUserId(userId)
      .setResourceId(resourceId));
  }

  private void commit() {
    dbTester.getSession().commit();
  }
}
