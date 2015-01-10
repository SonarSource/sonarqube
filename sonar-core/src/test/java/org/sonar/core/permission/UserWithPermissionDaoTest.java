/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.core.permission;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import javax.annotation.Nullable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class UserWithPermissionDaoTest extends AbstractDaoTestCase {

  private static final long COMPONENT_ID = 100L;

  private PermissionDao dao;

  @Before
  public void setUp() {
    dao = new PermissionDao(getMyBatis());
  }

  @Test
  public void select_all_users_for_project_permission() throws Exception {
    setupData("users_with_permissions");

    PermissionQuery query = PermissionQuery.builder().permission("user").build();
    List<UserWithPermissionDto> result = dao.selectUsers(query, COMPONENT_ID);
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
  public void select_all_users_for_global_permission() throws Exception {
    setupData("users_with_permissions");

    PermissionQuery query = PermissionQuery.builder().permission("admin").build();
    List<UserWithPermissionDto> result = dao.selectUsers(query, null);
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
  public void select_only_user_with_permission() throws Exception {
    setupData("users_with_permissions");

    // user1 and user2 have permission user
    assertThat(dao.selectUsers(PermissionQuery.builder().permission("user").membership(PermissionQuery.IN).build(), COMPONENT_ID)).hasSize(2);
  }

  @Test
  public void select_only_user_without_permission() throws Exception {
    setupData("users_with_permissions");

    // Only user3 has not the user permission
    assertThat(dao.selectUsers(PermissionQuery.builder().permission("user").membership(PermissionQuery.OUT).build(), COMPONENT_ID)).hasSize(1);
  }

  @Test
  public void search_by_user_name() throws Exception {
    setupData("users_with_permissions");

    List<UserWithPermissionDto> result = dao.selectUsers(PermissionQuery.builder().permission("user").search("SEr1").build(), COMPONENT_ID);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("User1");

    result = dao.selectUsers(PermissionQuery.builder().permission("user").search("user").build(), COMPONENT_ID);
    assertThat(result).hasSize(3);
  }

  @Test
  public void select_only_enable_users() throws Exception {
    setupData("select_only_enable_users");

    PermissionQuery query = PermissionQuery.builder().permission("user").build();
    List<UserWithPermissionDto> result = dao.selectUsers(query, COMPONENT_ID);
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
  public void should_be_sorted_by_user_name() throws Exception {
    setupData("users_with_permissions_should_be_sorted_by_user_name");

    List<UserWithPermissionDto> result = dao.selectUsers(PermissionQuery.builder().permission("user").build(), COMPONENT_ID);
    assertThat(result).hasSize(3);
    assertThat(result.get(0).getName()).isEqualTo("User1");
    assertThat(result.get(1).getName()).isEqualTo("User2");
    assertThat(result.get(2).getName()).isEqualTo("User3");
  }

  @Test
  public void should_be_paginated() throws Exception {
    setupData("users_with_permissions");

    List<UserWithPermissionDto> result = dao.selectUsers(PermissionQuery.builder().permission("user").build(), COMPONENT_ID, 0, 2);
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("User1");
    assertThat(result.get(1).getName()).isEqualTo("User2");

    result = dao.selectUsers(PermissionQuery.builder().permission("user").build(), COMPONENT_ID, 1, 2);
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("User2");
    assertThat(result.get(1).getName()).isEqualTo("User3");

    result = dao.selectUsers(PermissionQuery.builder().permission("user").build(), COMPONENT_ID, 2, 1);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("User3");
  }

}
