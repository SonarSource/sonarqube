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
import org.sonar.server.exceptions.NotFoundException;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PermissionFinderTest {

  @Mock
  PermissionDao permissionDao;

  @Mock
  ResourceDao resourceDao;

  @Mock
  PermissionTemplateDao permissionTemplateDao;

  PermissionFinder finder;

  @Before
  public void setUp() throws Exception {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setId(100L).setName("org.sample.Sample"));
    finder = new PermissionFinder(permissionDao, resourceDao, permissionTemplateDao);
  }

  @Test
  public void find_users() throws Exception {
    when(permissionDao.selectUsers(any(PermissionQuery.class), anyLong(), anyInt(), anyInt())).thenReturn(
      newArrayList(new UserWithPermissionDto().setName("user1").setPermission("user"))
    );

    UserWithPermissionQueryResult result = finder.findUsersWithPermission(PermissionQuery.builder().permission("user").build());
    assertThat(result.users()).hasSize(1);
    assertThat(result.hasMoreResults()).isFalse();
  }

  @Test
  public void fail_to_find_users_when_component_not_found() throws Exception {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(null);

    try {
      finder.findUsersWithPermission(PermissionQuery.builder().permission("user").component("Unknown").build());
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class).hasMessage("Component 'Unknown' does not exist");
    }
  }

  @Test
  public void find_users_with_paging() throws Exception {
    finder.findUsersWithPermission(PermissionQuery.builder().permission("user").pageIndex(3).pageSize(10).build());

    ArgumentCaptor<Integer> argumentOffset = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> argumentLimit = ArgumentCaptor.forClass(Integer.class);
    verify(permissionDao).selectUsers(any(PermissionQuery.class), anyLong(), argumentOffset.capture(), argumentLimit.capture());

    assertThat(argumentOffset.getValue()).isEqualTo(20);
    assertThat(argumentLimit.getValue()).isEqualTo(11);
  }

  @Test
  public void find_users_with_paging_having_more_results() throws Exception {
    when(permissionDao.selectUsers(any(PermissionQuery.class), anyLong(), anyInt(), anyInt())).thenReturn(newArrayList(
      new UserWithPermissionDto().setName("user1").setPermission("user"),
      new UserWithPermissionDto().setName("user2").setPermission("user"),
      new UserWithPermissionDto().setName("user3").setPermission("user"))
    );
    UserWithPermissionQueryResult result = finder.findUsersWithPermission(PermissionQuery.builder().permission("user").pageIndex(1).pageSize(2).build());

    ArgumentCaptor<Integer> argumentOffset = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> argumentLimit = ArgumentCaptor.forClass(Integer.class);
    verify(permissionDao).selectUsers(any(PermissionQuery.class), anyLong(), argumentOffset.capture(), argumentLimit.capture());

    assertThat(argumentOffset.getValue()).isEqualTo(0);
    assertThat(argumentLimit.getValue()).isEqualTo(3);
    assertThat(result.hasMoreResults()).isTrue();
  }

  @Test
  public void find_users_with_paging_having_no_more_results() throws Exception {
    when(permissionDao.selectUsers(any(PermissionQuery.class), anyLong(), anyInt(), anyInt())).thenReturn(newArrayList(
      new UserWithPermissionDto().setName("user1").setPermission("user"),
      new UserWithPermissionDto().setName("user2").setPermission("user"),
      new UserWithPermissionDto().setName("user4").setPermission("user"),
      new UserWithPermissionDto().setName("user3").setPermission("user"))
    );
    UserWithPermissionQueryResult result = finder.findUsersWithPermission(PermissionQuery.builder().permission("user").pageIndex(1).pageSize(10).build());

    ArgumentCaptor<Integer> argumentOffset = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> argumentLimit = ArgumentCaptor.forClass(Integer.class);
    verify(permissionDao).selectUsers(any(PermissionQuery.class), anyLong(), argumentOffset.capture(), argumentLimit.capture());

    assertThat(argumentOffset.getValue()).isEqualTo(0);
    assertThat(argumentLimit.getValue()).isEqualTo(11);
    assertThat(result.hasMoreResults()).isFalse();
  }

  @Test
  public void find_groups() throws Exception {
    when(permissionDao.selectGroups(any(PermissionQuery.class), anyLong())).thenReturn(
      newArrayList(new GroupWithPermissionDto().setName("users").setPermission("user"))
    );

    GroupWithPermissionQueryResult result = finder.findGroupsWithPermission(
      PermissionQuery.builder().permission("user").membership(PermissionQuery.IN).build());
    assertThat(result.groups()).hasSize(1);
    assertThat(result.hasMoreResults()).isFalse();
  }

  @Test
  public void find_groups_should_be_paginated() throws Exception {
    when(permissionDao.selectGroups(any(PermissionQuery.class), anyLong())).thenReturn(newArrayList(
      new GroupWithPermissionDto().setName("Anyone").setPermission("user"),
      new GroupWithPermissionDto().setName("Admin").setPermission("user"),
      new GroupWithPermissionDto().setName("Users").setPermission(null),
      new GroupWithPermissionDto().setName("Reviewers").setPermission(null),
      new GroupWithPermissionDto().setName("Other").setPermission(null)
    ));

    GroupWithPermissionQueryResult result = finder.findGroupsWithPermission(
      PermissionQuery.builder()
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
      PermissionQuery.builder()
        .permission("user")
        .pageSize(2)
        .pageIndex(3)
        .build()).hasMoreResults()).isFalse();
  }

  @Test
  public void find_groups_should_filter_membership() throws Exception {
    when(permissionDao.selectGroups(any(PermissionQuery.class), anyLong())).thenReturn(newArrayList(
      new GroupWithPermissionDto().setName("Anyone").setPermission("user"),
      new GroupWithPermissionDto().setName("Admin").setPermission("user"),
      new GroupWithPermissionDto().setName("Users").setPermission(null),
      new GroupWithPermissionDto().setName("Reviewers").setPermission(null),
      new GroupWithPermissionDto().setName("Other").setPermission(null)
    ));

    assertThat(finder.findGroupsWithPermission(
      PermissionQuery.builder().permission("user").membership(PermissionQuery.IN).build()).groups()).hasSize(2);
    assertThat(finder.findGroupsWithPermission(
      PermissionQuery.builder().permission("user").membership(PermissionQuery.OUT).build()).groups()).hasSize(3);
    assertThat(finder.findGroupsWithPermission(
      PermissionQuery.builder().permission("user").membership(PermissionQuery.ANY).build()).groups()).hasSize(5);
  }

  @Test
  public void find_groups_with_added_anyone_group() throws Exception {
    when(permissionDao.selectGroups(any(PermissionQuery.class), anyLong())).thenReturn(
      newArrayList(new GroupWithPermissionDto().setName("users").setPermission("user"))
    );

    GroupWithPermissionQueryResult result = finder.findGroupsWithPermission( PermissionQuery.builder().permission("user")
      .pageIndex(1).membership(PermissionQuery.ANY).build());
    assertThat(result.groups()).hasSize(2);
    GroupWithPermission first = result.groups().get(0);
    assertThat(first.name()).isEqualTo("Anyone");
    assertThat(first.hasPermission()).isFalse();
  }

  @Test
  public void find_groups_without_adding_anyone_group_when_search_text_do_not_matched() throws Exception {
    when(permissionDao.selectGroups(any(PermissionQuery.class), anyLong())).thenReturn(
      newArrayList(new GroupWithPermissionDto().setName("users").setPermission("user"))
    );

    GroupWithPermissionQueryResult result = finder.findGroupsWithPermission(PermissionQuery.builder().permission("user").search("other")
      .pageIndex(1).membership(PermissionQuery.ANY).build());
    // Anyone group should not be added
    assertThat(result.groups()).hasSize(1);
  }

  @Test
  public void find_groups_with_added_anyone_group_when_search_text_matched() throws Exception {
    when(permissionDao.selectGroups(any(PermissionQuery.class), anyLong())).thenReturn(
      newArrayList(new GroupWithPermissionDto().setName("MyAnyGroup").setPermission("user"))
    );

    GroupWithPermissionQueryResult result = finder.findGroupsWithPermission(PermissionQuery.builder().permission("user").search("any")
      .pageIndex(1).membership(PermissionQuery.ANY).build());
    assertThat(result.groups()).hasSize(2);
  }

  @Test
  public void find_groups_without_adding_anyone_group_when_out_membership_selected() throws Exception {
    when(permissionDao.selectGroups(any(PermissionQuery.class), anyLong())).thenReturn(
      newArrayList(new GroupWithPermissionDto().setName("users").setPermission("user"))
    );

    GroupWithPermissionQueryResult result = finder.findGroupsWithPermission( PermissionQuery.builder().permission("user")
      .pageIndex(1).membership(PermissionQuery.OUT).build());
    // Anyone group should not be added
    assertThat(result.groups()).hasSize(1);
  }


  @Test
  public void find_users_from_permission_template() throws Exception {
    when(permissionTemplateDao.selectTemplateByKey(anyString())).thenReturn(new PermissionTemplateDto().setId(1L).setKee("my_template"));

    when(permissionTemplateDao.selectUsers(any(PermissionQuery.class), anyLong(), anyInt(), anyInt())).thenReturn(
      newArrayList(new UserWithPermissionDto().setName("user1").setPermission("user"))
    );

    UserWithPermissionQueryResult result = finder.findUsersWithPermissionTemplate(PermissionQuery.builder().permission("user").template("my_template").build());
    assertThat(result.users()).hasSize(1);
    assertThat(result.hasMoreResults()).isFalse();
  }

  @Test
  public void fail_to_find_users_from_permission_template_when_template_not_found() throws Exception {
    when(permissionTemplateDao.selectTemplateByKey(anyString())).thenReturn(null);

    try {
      finder.findUsersWithPermissionTemplate(PermissionQuery.builder().permission("user").template("Unknown").build());
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class).hasMessage("Template 'Unknown' does not exist");
    }
  }

  @Test
  public void find_groups_from_permission_template() throws Exception {
    when(permissionTemplateDao.selectTemplateByKey(anyString())).thenReturn(new PermissionTemplateDto().setId(1L).setKee("my_template"));

    when(permissionTemplateDao.selectGroups(any(PermissionQuery.class), anyLong())).thenReturn(
      newArrayList(new GroupWithPermissionDto().setName("users").setPermission("user"))
    );

    GroupWithPermissionQueryResult result = finder.findGroupsWithPermissionTemplate(
      PermissionQuery.builder().permission("user").template("my_template").membership(PermissionQuery.OUT).build());
    assertThat(result.groups()).hasSize(1);
    assertThat(result.hasMoreResults()).isFalse();
  }

  @Test
  public void fail_to_find_groups_from_permission_template_when_template_not_found() throws Exception {
    when(permissionTemplateDao.selectTemplateByKey(anyString())).thenReturn(null);

    try {
      finder.findGroupsWithPermissionTemplate(PermissionQuery.builder().permission("user").template("Unknown").build());
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class).hasMessage("Template 'Unknown' does not exist");
    }
  }

}
