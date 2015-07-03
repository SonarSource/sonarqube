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

package org.sonar.server.user;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.core.user.GroupMembership;
import org.sonar.db.user.GroupMembershipDao;
import org.sonar.db.user.GroupMembershipDto;
import org.sonar.db.user.GroupMembershipQuery;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GroupMembershipFinderTest {

  UserDao userDao = mock(UserDao.class);
  GroupMembershipDao groupMembershipDao = mock(GroupMembershipDao.class);
  GroupMembershipFinder finder;

  @Before
  public void setUp() {
    when(userDao.selectActiveUserByLogin("arthur")).thenReturn(new UserDto().setId(100L).setName("arthur"));
    finder = new GroupMembershipFinder(userDao, groupMembershipDao);
  }

  @Test
  public void find() {
    GroupMembershipQuery query = GroupMembershipQuery.builder().login("arthur").build();
    when(groupMembershipDao.selectGroups(eq(query), anyLong(), anyInt(), anyInt())).thenReturn(
      newArrayList(new GroupMembershipDto().setId(1L).setName("users").setDescription("Users group").setUserId(100L))
      );

    GroupMembershipFinder.Membership result = finder.find(query);
    assertThat(result.groups()).hasSize(1);
    assertThat(result.hasMoreResults()).isFalse();

    GroupMembership group = result.groups().get(0);
    assertThat(group.id()).isEqualTo(1);
    assertThat(group.name()).isEqualTo("users");
    assertThat(group.description()).isEqualTo("Users group");
    assertThat(group.isMember()).isTrue();
  }

  @Test
  public void find_with_paging() {
    GroupMembershipQuery query = GroupMembershipQuery.builder().login("arthur").pageIndex(3).pageSize(10).build();
    finder.find(query);

    ArgumentCaptor<Integer> argumentOffset = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> argumentLimit = ArgumentCaptor.forClass(Integer.class);
    verify(groupMembershipDao).selectGroups(eq(query), anyLong(), argumentOffset.capture(), argumentLimit.capture());

    assertThat(argumentOffset.getValue()).isEqualTo(20);
    assertThat(argumentLimit.getValue()).isEqualTo(11);
  }

  @Test
  public void find_with_paging_having_more_results() {
    GroupMembershipQuery query = GroupMembershipQuery.builder().login("arthur").pageIndex(1).pageSize(2).build();
    when(groupMembershipDao.selectGroups(eq(query), anyLong(), anyInt(), anyInt())).thenReturn(newArrayList(
      new GroupMembershipDto().setId(1L).setName("group1"),
      new GroupMembershipDto().setId(2L).setName("group2"),
      new GroupMembershipDto().setId(3L).setName("group3"))
      );
    GroupMembershipFinder.Membership result = finder.find(query);

    ArgumentCaptor<Integer> argumentOffset = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> argumentLimit = ArgumentCaptor.forClass(Integer.class);
    verify(groupMembershipDao).selectGroups(eq(query), anyLong(), argumentOffset.capture(), argumentLimit.capture());

    assertThat(argumentOffset.getValue()).isEqualTo(0);
    assertThat(argumentLimit.getValue()).isEqualTo(3);
    assertThat(result.hasMoreResults()).isTrue();
  }

  @Test
  public void find_with_paging_having_no_more_results() {
    GroupMembershipQuery query = GroupMembershipQuery.builder().login("arthur").pageIndex(1).pageSize(10).build();
    when(groupMembershipDao.selectGroups(eq(query), anyLong(), anyInt(), anyInt())).thenReturn(newArrayList(
      new GroupMembershipDto().setId(1L).setName("group1"),
      new GroupMembershipDto().setId(2L).setName("group2"),
      new GroupMembershipDto().setId(3L).setName("group3"),
      new GroupMembershipDto().setId(4L).setName("group4"))
      );
    GroupMembershipFinder.Membership result = finder.find(query);

    ArgumentCaptor<Integer> argumentOffset = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> argumentLimit = ArgumentCaptor.forClass(Integer.class);
    verify(groupMembershipDao).selectGroups(eq(query), anyLong(), argumentOffset.capture(), argumentLimit.capture());

    assertThat(argumentOffset.getValue()).isEqualTo(0);
    assertThat(argumentLimit.getValue()).isEqualTo(11);
    assertThat(result.hasMoreResults()).isFalse();
  }
}
