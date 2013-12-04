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
import org.sonar.core.permission.PermissionDao;
import org.sonar.core.permission.UserWithPermissionDto;
import org.sonar.core.permission.WithPermissionQuery;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;

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
  public void find() throws Exception {
    WithPermissionQuery query = WithPermissionQuery.builder().permission("user").build();
    when(dao.selectUsers(eq(query), anyLong(), anyInt(), anyInt())).thenReturn(
      newArrayList(new UserWithPermissionDto().setName("user1"))
    );

    UserWithPermissionQueryResult result = finder.find(query);
    assertThat(result.users()).hasSize(1);
    assertThat(result.hasMoreResults()).isFalse();
  }

  @Test
  public void find_with_paging() throws Exception {
    WithPermissionQuery query = WithPermissionQuery.builder().permission("user").pageIndex(3).pageSize(10).build();
    finder.find(query);

    ArgumentCaptor<Integer> argumentOffset = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> argumentLimit = ArgumentCaptor.forClass(Integer.class);
    verify(dao).selectUsers(eq(query), anyLong(), argumentOffset.capture(), argumentLimit.capture());

    assertThat(argumentOffset.getValue()).isEqualTo(20);
    assertThat(argumentLimit.getValue()).isEqualTo(11);
  }

  @Test
  public void find_with_paging_having_more_results() throws Exception {
    WithPermissionQuery query = WithPermissionQuery.builder().permission("user").pageIndex(1).pageSize(2).build();
    when(dao.selectUsers(eq(query), anyLong(), anyInt(), anyInt())).thenReturn(newArrayList(
      new UserWithPermissionDto().setName("user1"),
      new UserWithPermissionDto().setName("user2"),
      new UserWithPermissionDto().setName("user3"))
    );
    UserWithPermissionQueryResult result = finder.find(query);

    ArgumentCaptor<Integer> argumentOffset = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> argumentLimit = ArgumentCaptor.forClass(Integer.class);
    verify(dao).selectUsers(eq(query), anyLong(), argumentOffset.capture(), argumentLimit.capture());

    assertThat(argumentOffset.getValue()).isEqualTo(0);
    assertThat(argumentLimit.getValue()).isEqualTo(3);
    assertThat(result.hasMoreResults()).isTrue();
  }

  @Test
  public void find_with_paging_having_no_more_results() throws Exception {
    WithPermissionQuery query = WithPermissionQuery.builder().permission("user").pageIndex(1).pageSize(10).build();
    when(dao.selectUsers(eq(query), anyLong(), anyInt(), anyInt())).thenReturn(newArrayList(
      new UserWithPermissionDto().setName("user1"),
      new UserWithPermissionDto().setName("user2"),
      new UserWithPermissionDto().setName("user4"),
      new UserWithPermissionDto().setName("user3"))
    );
    UserWithPermissionQueryResult result = finder.find(query);

    ArgumentCaptor<Integer> argumentOffset = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> argumentLimit = ArgumentCaptor.forClass(Integer.class);
    verify(dao).selectUsers(eq(query), anyLong(), argumentOffset.capture(), argumentLimit.capture());

    assertThat(argumentOffset.getValue()).isEqualTo(0);
    assertThat(argumentLimit.getValue()).isEqualTo(11);
    assertThat(result.hasMoreResults()).isFalse();
  }

}
