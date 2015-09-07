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

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.core.permission.GroupWithPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ResourceDao;
import org.sonar.db.component.ResourceDto;
import org.sonar.db.component.ResourceQuery;
import org.sonar.db.permission.GroupWithPermissionDto;
import org.sonar.db.permission.PermissionDao;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.PermissionTemplateDao;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.permission.UserWithPermissionDto;
import org.sonar.server.exceptions.NotFoundException;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PermissionFinderTest {

  PermissionDao permissionDao = mock(PermissionDao.class);
  ResourceDao resourceDao = mock(ResourceDao.class);
  PermissionTemplateDao permissionTemplateDao = mock(PermissionTemplateDao.class);
  DbSession dbSession = mock(DbSession.class);

  PermissionFinder underTest;

  @Before
  public void setUp() {
    DbClient dbClient = mock(DbClient.class);
    when(dbClient.resourceDao()).thenReturn(resourceDao);
    when(dbClient.permissionDao()).thenReturn(permissionDao);
    when(dbClient.permissionTemplateDao()).thenReturn(permissionTemplateDao);
    when(resourceDao.selectResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setId(100L).setName("org.sample.Sample"));
    underTest = new PermissionFinder(dbClient);
  }

  @Test
  public void find_users() {
    when(permissionDao.selectUsers(any(DbSession.class), any(PermissionQuery.class), anyLong(), anyInt(), anyInt())).thenReturn(
      newArrayList(new UserWithPermissionDto().setName("user1").setPermission("user"))
      );
    when(permissionDao.countUsers(any(DbSession.class), any(PermissionQuery.class), anyLong())).thenReturn(1);

    UserWithPermissionQueryResult result = underTest.findUsersWithPermission(dbSession, PermissionQuery.builder().permission("user").build());
    assertThat(result.users()).hasSize(1);
    assertThat(result.total()).isEqualTo(1);
  }

  @Test
  public void fail_to_find_users_when_component_not_found() {
    when(resourceDao.selectResource(any(ResourceQuery.class))).thenReturn(null);

    try {
      underTest.findUsersWithPermission(dbSession, PermissionQuery.builder().permission("user").component("Unknown").build());
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class).hasMessage("Project 'Unknown' does not exist");
    }
  }

  @Test
  public void find_users_with_paging() {
    underTest.findUsersWithPermission(dbSession, PermissionQuery.builder().permission("user").pageIndex(3).pageSize(10).build());

    ArgumentCaptor<Integer> argumentOffset = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> argumentLimit = ArgumentCaptor.forClass(Integer.class);
    verify(permissionDao).selectUsers(any(DbSession.class), any(PermissionQuery.class), anyLong(), argumentOffset.capture(), argumentLimit.capture());

    assertThat(argumentOffset.getValue()).isEqualTo(20);
    assertThat(argumentLimit.getValue()).isEqualTo(10);
  }

  @Test
  public void find_users_with_paging_having_more_results() {
    when(permissionDao.selectUsers(any(DbSession.class), any(PermissionQuery.class), anyLong(), anyInt(), anyInt())).thenReturn(newArrayList(
      new UserWithPermissionDto().setName("user1").setPermission("user"),
      new UserWithPermissionDto().setName("user2").setPermission("user"),
      new UserWithPermissionDto().setName("user3").setPermission("user"))
      );
    when(permissionDao.countUsers(any(DbSession.class), any(PermissionQuery.class), anyLong())).thenReturn(3);
    UserWithPermissionQueryResult result = underTest.findUsersWithPermission(dbSession, PermissionQuery.builder().permission("user").pageIndex(1).pageSize(2).build());

    ArgumentCaptor<Integer> argumentOffset = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> argumentLimit = ArgumentCaptor.forClass(Integer.class);
    verify(permissionDao).selectUsers(any(DbSession.class), any(PermissionQuery.class), anyLong(), argumentOffset.capture(), argumentLimit.capture());

    assertThat(argumentOffset.getValue()).isEqualTo(0);
    assertThat(argumentLimit.getValue()).isEqualTo(2);
    assertThat(result.total()).isEqualTo(3);
  }

  @Test
  public void find_users_with_paging_having_no_more_results() {
    when(permissionDao.selectUsers(any(DbSession.class), any(PermissionQuery.class), anyLong(), anyInt(), anyInt())).thenReturn(newArrayList(
      new UserWithPermissionDto().setName("user1").setPermission("user"),
      new UserWithPermissionDto().setName("user2").setPermission("user"),
      new UserWithPermissionDto().setName("user4").setPermission("user"),
      new UserWithPermissionDto().setName("user3").setPermission("user"))
      );
    when(permissionDao.countUsers(any(DbSession.class), any(PermissionQuery.class), anyLong())).thenReturn(4);

    UserWithPermissionQueryResult result = underTest.findUsersWithPermission(dbSession, PermissionQuery.builder().permission("user").pageIndex(1).pageSize(10).build());

    ArgumentCaptor<Integer> argumentOffset = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> argumentLimit = ArgumentCaptor.forClass(Integer.class);
    verify(permissionDao).selectUsers(any(DbSession.class), any(PermissionQuery.class), anyLong(), argumentOffset.capture(), argumentLimit.capture());

    assertThat(argumentOffset.getValue()).isEqualTo(0);
    assertThat(argumentLimit.getValue()).isEqualTo(10);
    assertThat(result.total()).isEqualTo(4);
  }

  @Test
  public void find_groups() {
    when(permissionDao.selectGroups(any(DbSession.class), any(PermissionQuery.class), anyLong())).thenReturn(
      newArrayList(new GroupWithPermissionDto().setName("users").setPermission("user"))
      );

    GroupWithPermissionQueryResult result = underTest.findGroupsWithPermission(dbSession, PermissionQuery.builder().permission("user").membership(PermissionQuery.IN).build());

    assertThat(result.groups()).hasSize(1);
  }

  @Test
  public void find_groups_should_be_paginated() {
    when(permissionDao.selectGroups(any(DbSession.class), any(PermissionQuery.class), anyLong())).thenReturn(newArrayList(
      new GroupWithPermissionDto().setName("Anyone").setPermission("user"),
      new GroupWithPermissionDto().setName("Admin").setPermission("user"),
      new GroupWithPermissionDto().setName("Users").setPermission(null),
      new GroupWithPermissionDto().setName("Reviewers").setPermission(null),
      new GroupWithPermissionDto().setName("Other").setPermission(null)
      ));

    GroupWithPermissionQueryResult result = underTest.findGroupsWithPermission(dbSession,
      PermissionQuery.builder()
        .permission("user")
        .pageSize(2)
        .pageIndex(2)
        .build());

    List<GroupWithPermission> groups = result.groups();
    assertThat(groups).hasSize(2);
    assertThat(groups.get(0).name()).isEqualTo("Users");
    assertThat(groups.get(1).name()).isEqualTo("Reviewers");
  }

  @Test
  public void find_groups_should_filter_membership() {
    when(permissionDao.selectGroups(any(DbSession.class), any(PermissionQuery.class), anyLong())).thenReturn(newArrayList(
      new GroupWithPermissionDto().setName("Anyone").setPermission("user"),
      new GroupWithPermissionDto().setName("Admin").setPermission("user"),
      new GroupWithPermissionDto().setName("Users").setPermission(null),
      new GroupWithPermissionDto().setName("Reviewers").setPermission(null),
      new GroupWithPermissionDto().setName("Other").setPermission(null)
      ));

    assertThat(underTest.findGroupsWithPermission(dbSession, PermissionQuery.builder().permission("user").membership(PermissionQuery.IN).build()).groups()).hasSize(2);
    assertThat(underTest.findGroupsWithPermission(dbSession, PermissionQuery.builder().permission("user").membership(PermissionQuery.OUT).build()).groups()).hasSize(3);
    assertThat(underTest.findGroupsWithPermission(dbSession, PermissionQuery.builder().permission("user").membership(PermissionQuery.ANY).build()).groups()).hasSize(5);
  }

  @Test
  public void find_groups_with_added_anyone_group() {
    when(permissionDao.selectGroups(any(DbSession.class), any(PermissionQuery.class), anyLong())).thenReturn(
      newArrayList(new GroupWithPermissionDto().setName("users").setPermission("user"))
      );

    GroupWithPermissionQueryResult result = underTest.findGroupsWithPermission(dbSession, PermissionQuery.builder().permission("user").pageIndex(1).membership(PermissionQuery.ANY)
      .build());
    assertThat(result.groups()).hasSize(2);
    GroupWithPermission first = result.groups().get(0);
    assertThat(first.name()).isEqualTo("Anyone");
    assertThat(first.hasPermission()).isFalse();
  }

  @Test
  public void find_groups_without_adding_anyone_group_when_search_text_do_not_matched() {
    when(permissionDao.selectGroups(any(DbSession.class), any(PermissionQuery.class), anyLong())).thenReturn(
      newArrayList(new GroupWithPermissionDto().setName("users").setPermission("user"))
      );

    GroupWithPermissionQueryResult result = underTest.findGroupsWithPermission(dbSession, PermissionQuery.builder().permission("user").search("other")
      .pageIndex(1).membership(PermissionQuery.ANY).build());
    // Anyone group should not be added
    assertThat(result.groups()).hasSize(1);
  }

  @Test
  public void find_groups_with_added_anyone_group_when_search_text_matched() {
    when(permissionDao.selectGroups(any(DbSession.class), any(PermissionQuery.class), anyLong())).thenReturn(
      newArrayList(new GroupWithPermissionDto().setName("MyAnyGroup").setPermission("user"))
      );

    GroupWithPermissionQueryResult result = underTest.findGroupsWithPermission(dbSession, PermissionQuery.builder().permission("user").search("any")
      .pageIndex(1).membership(PermissionQuery.ANY).build());
    assertThat(result.groups()).hasSize(2);
  }

  @Test
  public void find_groups_without_adding_anyone_group_when_out_membership_selected() {
    when(permissionDao.selectGroups(any(DbSession.class), any(PermissionQuery.class), anyLong())).thenReturn(
      newArrayList(new GroupWithPermissionDto().setName("users").setPermission("user"))
      );

    GroupWithPermissionQueryResult result = underTest.findGroupsWithPermission(dbSession, PermissionQuery.builder().permission("user")
      .pageIndex(1).membership(PermissionQuery.OUT).build());
    // Anyone group should not be added
    assertThat(result.groups()).hasSize(1);
  }

  @Test
  public void find_users_from_permission_template() {
    when(permissionTemplateDao.selectByUuid(anyString())).thenReturn(new PermissionTemplateDto().setId(1L).setUuid("my_template"));

    when(permissionTemplateDao.selectUsers(any(DbSession.class), any(PermissionQuery.class), anyLong(), anyInt(), anyInt())).thenReturn(
      newArrayList(new UserWithPermissionDto().setName("user1").setPermission("user"))
      );

    when(permissionTemplateDao.countUsers(any(DbSession.class), any(PermissionQuery.class), anyLong())).thenReturn(1);

    UserWithPermissionQueryResult result = underTest.findUsersWithPermissionTemplate(dbSession, PermissionQuery.builder().permission("user").template("my_template").build());
    assertThat(result.users()).hasSize(1);
    assertThat(result.total()).isEqualTo(1);
  }

  @Test
  public void fail_to_find_users_from_permission_template_when_template_not_found() {
    when(permissionTemplateDao.selectByUuid(anyString())).thenReturn(null);

    try {
      underTest.findUsersWithPermissionTemplate(dbSession, PermissionQuery.builder().permission("user").template("Unknown").build());
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class).hasMessage("Template 'Unknown' does not exist");
    }
  }

  @Test
  public void find_groups_from_permission_template() {
    when(permissionTemplateDao.selectByUuid(anyString())).thenReturn(new PermissionTemplateDto().setId(1L).setUuid("my_template"));

    when(permissionTemplateDao.selectGroups(any(DbSession.class), any(PermissionQuery.class), anyLong())).thenReturn(
      newArrayList(new GroupWithPermissionDto().setName("users").setPermission("user"))
      );

    GroupWithPermissionQueryResult result = underTest.findGroupsWithPermissionTemplate(dbSession,
      PermissionQuery.builder().permission("user").template("my_template").membership(PermissionQuery.OUT).build());
    assertThat(result.groups()).hasSize(1);
  }

  @Test
  public void fail_to_find_groups_from_permission_template_when_template_not_found() {
    when(permissionTemplateDao.selectByUuid(anyString())).thenReturn(null);

    try {
      underTest.findGroupsWithPermissionTemplate(dbSession, PermissionQuery.builder().permission("user").template("Unknown").build());
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class).hasMessage("Template 'Unknown' does not exist");
    }
  }
}
