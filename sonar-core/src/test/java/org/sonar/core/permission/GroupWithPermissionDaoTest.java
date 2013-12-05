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

package org.sonar.core.permission;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class GroupWithPermissionDaoTest extends AbstractDaoTestCase {

  private static final long COMPONENT_ID = 100L;

  private PermissionDao dao;

  @Before
  public void setUp() {
    dao = new PermissionDao(getMyBatis());
  }

  @Test
  public void select_all_groups_for_project_permission() throws Exception {
    setupData("groups_with_permissions");

    WithPermissionQuery query = WithPermissionQuery.builder().permission("user").build();
    List<GroupWithPermissionDto> result = dao.selectGroups(query, COMPONENT_ID);
    assertThat(result).hasSize(3);

    GroupWithPermissionDto user1 = result.get(0);
    assertThat(user1.getName()).isEqualTo("sonar-administrators");
    assertThat(user1.getPermission()).isNotNull();

    GroupWithPermissionDto user2 = result.get(1);
    assertThat(user2.getName()).isEqualTo("sonar-reviewers");
    assertThat(user2.getPermission()).isNull();

    GroupWithPermissionDto user3 = result.get(2);
    assertThat(user3.getName()).isEqualTo("sonar-users");
    assertThat(user3.getPermission()).isNotNull();
  }

  @Test
  public void select_all_groups_for_global_permission() throws Exception {
    setupData("groups_with_permissions");

    WithPermissionQuery query = WithPermissionQuery.builder().permission("admin").build();
    List<GroupWithPermissionDto> result = dao.selectGroups(query, null);
    assertThat(result).hasSize(3);

    GroupWithPermissionDto user1 = result.get(0);
    assertThat(user1.getName()).isEqualTo("sonar-administrators");
    assertThat(user1.getPermission()).isNotNull();

    GroupWithPermissionDto user2 = result.get(1);
    assertThat(user2.getName()).isEqualTo("sonar-reviewers");
    assertThat(user2.getPermission()).isNull();

    GroupWithPermissionDto user3 = result.get(2);
    assertThat(user3.getName()).isEqualTo("sonar-users");
    assertThat(user3.getPermission()).isNull();
  }

  @Test
  public void select_only_group_with_permission() throws Exception {
    setupData("groups_with_permissions");

    // user1 and user2 have permission user
    assertThat(dao.selectGroups(WithPermissionQuery.builder().permission("user").membership(WithPermissionQuery.IN).build(), COMPONENT_ID)).hasSize(2);
  }

  @Test
  public void select_only_group_without_permission() throws Exception {
    setupData("groups_with_permissions");

    // Only user3 has not the user permission
    assertThat(dao.selectGroups(WithPermissionQuery.builder().permission("user").membership(WithPermissionQuery.OUT).build(), COMPONENT_ID)).hasSize(1);
  }

  @Test
  public void search_by_groups_name() throws Exception {
    setupData("groups_with_permissions");

    List<GroupWithPermissionDto> result = dao.selectGroups(WithPermissionQuery.builder().permission("user").search("aDMini").build(), COMPONENT_ID);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("sonar-administrators");

    result = dao.selectGroups(WithPermissionQuery.builder().permission("user").search("sonar").build(), COMPONENT_ID);
    assertThat(result).hasSize(3);
  }

  @Test
  public void search_groups_should_be_sorted_by_group_name() throws Exception {
    setupData("groups_with_permissions_should_be_sorted_by_group_name");

    List<GroupWithPermissionDto> result = dao.selectGroups(WithPermissionQuery.builder().permission("user").build(), COMPONENT_ID);
    assertThat(result).hasSize(3);
    assertThat(result.get(0).getName()).isEqualTo("sonar-administrators");
    assertThat(result.get(1).getName()).isEqualTo("sonar-reviewers");
    assertThat(result.get(2).getName()).isEqualTo("sonar-users");
  }

  @Test
  public void search_groups_should_be_paginated() throws Exception {
    setupData("groups_with_permissions");

    List<GroupWithPermissionDto> result = dao.selectGroups(WithPermissionQuery.builder().permission("user").build(), COMPONENT_ID, 0, 2);
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("sonar-administrators");
    assertThat(result.get(1).getName()).isEqualTo("sonar-reviewers");

    result = dao.selectGroups(WithPermissionQuery.builder().permission("user").build(), COMPONENT_ID, 1, 2);
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("sonar-reviewers");
    assertThat(result.get(1).getName()).isEqualTo("sonar-users");

    result = dao.selectGroups(WithPermissionQuery.builder().permission("user").build(), COMPONENT_ID, 2, 1);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("sonar-users");
  }

}
