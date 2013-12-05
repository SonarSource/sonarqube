/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.permission;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.permission.*;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PermissionFinderTest {

  @Mock
  PermissionDao dao;

  @Mock
  ResourceDao resourceDao;

  PermissionFinder finder;

  @Before
  public void setUp() throws Exception {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setId(100L).setName("org.sample.Sample"));
    finder = new PermissionFinder(dao, resourceDao);
  }

  @Test
  public void find_users() throws Exception {
    WithPermissionQuery query = WithPermissionQuery.builder().permission("user").build();
    when(dao.selectUsers(eq(query), anyLong(), anyInt(), anyInt())).thenReturn(
      newArrayList(new UserWithPermissionDto().setName("user1").setPermission("user"))
    );

    UserWithPermissionQueryResult result = finder.findUsersWithPermission(query);
    assertThat(result.users()).hasSize(1);
    assertThat(result.hasMoreResults()).isFalse();
  }

  @Test
  public void find_users_with_paging() throws Exception {
    WithPermissionQuery query = WithPermissionQuery.builder().permission("user").pageIndex(3).pageSize(10).build();
    finder.findUsersWithPermission(query);

    ArgumentCaptor<Integer> argumentOffset = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> argumentLimit = ArgumentCaptor.forClass(Integer.class);
    verify(dao).selectUsers(eq(query), anyLong(), argumentOffset.capture(), argumentLimit.capture());

    assertThat(argumentOffset.getValue()).isEqualTo(20);
    assertThat(argumentLimit.getValue()).isEqualTo(11);
  }

  @Test
  public void find_users_with_paging_having_more_results() throws Exception {
    WithPermissionQuery query = WithPermissionQuery.builder().permission("user").pageIndex(1).pageSize(2).build();
    when(dao.selectUsers(eq(query), anyLong(), anyInt(), anyInt())).thenReturn(newArrayList(
      new UserWithPermissionDto().setName("user1").setPermission("user"),
      new UserWithPermissionDto().setName("user2").setPermission("user"),
      new UserWithPermissionDto().setName("user3").setPermission("user"))
    );
    UserWithPermissionQueryResult result = finder.findUsersWithPermission(query);

    ArgumentCaptor<Integer> argumentOffset = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> argumentLimit = ArgumentCaptor.forClass(Integer.class);
    verify(dao).selectUsers(eq(query), anyLong(), argumentOffset.capture(), argumentLimit.capture());

    assertThat(argumentOffset.getValue()).isEqualTo(0);
    assertThat(argumentLimit.getValue()).isEqualTo(3);
    assertThat(result.hasMoreResults()).isTrue();
  }

  @Test
  public void find_users_with_paging_having_no_more_results() throws Exception {
    WithPermissionQuery query = WithPermissionQuery.builder().permission("user").pageIndex(1).pageSize(10).build();
    when(dao.selectUsers(eq(query), anyLong(), anyInt(), anyInt())).thenReturn(newArrayList(
      new UserWithPermissionDto().setName("user1").setPermission("user"),
      new UserWithPermissionDto().setName("user2").setPermission("user"),
      new UserWithPermissionDto().setName("user4").setPermission("user"),
      new UserWithPermissionDto().setName("user3").setPermission("user"))
    );
    UserWithPermissionQueryResult result = finder.findUsersWithPermission(query);

    ArgumentCaptor<Integer> argumentOffset = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> argumentLimit = ArgumentCaptor.forClass(Integer.class);
    verify(dao).selectUsers(eq(query), anyLong(), argumentOffset.capture(), argumentLimit.capture());

    assertThat(argumentOffset.getValue()).isEqualTo(0);
    assertThat(argumentLimit.getValue()).isEqualTo(11);
    assertThat(result.hasMoreResults()).isFalse();
  }

  @Test
  public void find_groups() throws Exception {
    when(dao.selectGroups(any(WithPermissionQuery.class), anyLong())).thenReturn(
      newArrayList(new GroupWithPermissionDto().setName("users").setPermission("user"))
    );

    GroupWithPermissionQueryResult result = finder.findGroupsWithPermission(
      WithPermissionQuery.builder().permission("user").membership(WithPermissionQuery.IN).build());
    assertThat(result.groups()).hasSize(1);
    assertThat(result.hasMoreResults()).isFalse();
  }

  @Test
  public void find_groups_should_be_paginated() throws Exception {
    when(dao.selectGroups(any(WithPermissionQuery.class), anyLong())).thenReturn(newArrayList(
      new GroupWithPermissionDto().setName("Anyone").setPermission("user"),
      new GroupWithPermissionDto().setName("Admin").setPermission("user"),
      new GroupWithPermissionDto().setName("Users").setPermission(null),
      new GroupWithPermissionDto().setName("Reviewers").setPermission(null),
      new GroupWithPermissionDto().setName("Other").setPermission(null)
    ));

    GroupWithPermissionQueryResult result = finder.findGroupsWithPermission(
      WithPermissionQuery.builder()
        .permission("user")
        .pageSize(2)
        .pageIndex(2)
        .build());

    assertThat(result.hasMoreResults()).isTrue();
    List<GroupWithPermission> groups = result.groups();
    assertThat(groups).hasSize(2);
    assertThat(groups.get(0).name()).isEqualTo("Users");
    assertThat(groups.get(1).name()).isEqualTo("Reviewers");

    assertThat(finder.findGroupsWithPermission(
      WithPermissionQuery.builder()
        .permission("user")
        .pageSize(2)
        .pageIndex(3)
        .build()).hasMoreResults()).isFalse();
  }

  @Test
  public void find_groups_should_filter_membership() throws Exception {
    when(dao.selectGroups(any(WithPermissionQuery.class), anyLong())).thenReturn(newArrayList(
      new GroupWithPermissionDto().setName("Anyone").setPermission("user"),
      new GroupWithPermissionDto().setName("Admin").setPermission("user"),
      new GroupWithPermissionDto().setName("Users").setPermission(null),
      new GroupWithPermissionDto().setName("Reviewers").setPermission(null),
      new GroupWithPermissionDto().setName("Other").setPermission(null)
    ));

    assertThat(finder.findGroupsWithPermission(
      WithPermissionQuery.builder().permission("user").membership(WithPermissionQuery.IN).build()).groups()).hasSize(2);
    assertThat(finder.findGroupsWithPermission(
      WithPermissionQuery.builder().permission("user").membership(WithPermissionQuery.OUT).build()).groups()).hasSize(3);
    assertThat(finder.findGroupsWithPermission(
      WithPermissionQuery.builder().permission("user").membership(WithPermissionQuery.ANY).build()).groups()).hasSize(5);
  }

  @Test
  public void find_groups_with_added_anyone_group() throws Exception {
    when(dao.selectGroups(any(WithPermissionQuery.class), anyLong())).thenReturn(
      newArrayList(new GroupWithPermissionDto().setName("users").setPermission("user"))
    );

    GroupWithPermissionQueryResult result = finder.findGroupsWithPermission( WithPermissionQuery.builder().permission("user")
      .pageIndex(1).membership(WithPermissionQuery.ANY).build());
    assertThat(result.groups()).hasSize(2);
    GroupWithPermission first = result.groups().get(0);
    assertThat(first.name()).isEqualTo("Anyone");
    assertThat(first.hasPermission()).isFalse();
  }

  @Test
  public void find_groups_without_adding_anyone_group_when_search_text_do_not_matched() throws Exception {
    when(dao.selectGroups(any(WithPermissionQuery.class), anyLong())).thenReturn(
      newArrayList(new GroupWithPermissionDto().setName("users").setPermission("user"))
    );

    GroupWithPermissionQueryResult result = finder.findGroupsWithPermission(WithPermissionQuery.builder().permission("user").search("other")
      .pageIndex(1).membership(WithPermissionQuery.ANY).build());
    // Anyone group should not be added
    assertThat(result.groups()).hasSize(1);
  }

  @Test
  public void find_groups_with_added_anyone_group_when_search_text_matched() throws Exception {
    when(dao.selectGroups(any(WithPermissionQuery.class), anyLong())).thenReturn(
      newArrayList(new GroupWithPermissionDto().setName("MyAnyGroup").setPermission("user"))
    );

    GroupWithPermissionQueryResult result = finder.findGroupsWithPermission(WithPermissionQuery.builder().permission("user").search("any")
      .pageIndex(1).membership(WithPermissionQuery.ANY).build());
    assertThat(result.groups()).hasSize(2);
  }

  @Test
  public void find_groups_without_adding_anyone_group_when_out_membership_selected() throws Exception {
    when(dao.selectGroups(any(WithPermissionQuery.class), anyLong())).thenReturn(
      newArrayList(new GroupWithPermissionDto().setName("users").setPermission("user"))
    );

    GroupWithPermissionQueryResult result = finder.findGroupsWithPermission( WithPermissionQuery.builder().permission("user")
      .pageIndex(1).membership(WithPermissionQuery.OUT).build());
    // Anyone group should not be added
    assertThat(result.groups()).hasSize(1);
  }

}
