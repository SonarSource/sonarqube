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

package org.sonar.db.permission;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class GroupWithPermissionDaoTest {

  private static final long COMPONENT_ID = 100L;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  PermissionDao dao = dbTester.getDbClient().permissionDao();

  @Test
  public void select_groups_for_project_permission() {
    dbTester.prepareDbUnit(getClass(), "groups_with_permissions.xml");

    PermissionQuery query = PermissionQuery.builder().permission("user").build();
    List<GroupWithPermissionDto> result = dao.selectGroups(query, COMPONENT_ID);
    assertThat(result).hasSize(4);

    GroupWithPermissionDto anyone = result.get(0);
    assertThat(anyone.getName()).isEqualTo("Anyone");
    assertThat(anyone.getDescription()).isNull();
    assertThat(anyone.getPermission()).isNotNull();

    GroupWithPermissionDto group1 = result.get(1);
    assertThat(group1.getName()).isEqualTo("sonar-administrators");
    assertThat(group1.getDescription()).isEqualTo("System administrators");
    assertThat(group1.getPermission()).isNotNull();

    GroupWithPermissionDto group2 = result.get(2);
    assertThat(group2.getName()).isEqualTo("sonar-reviewers");
    assertThat(group2.getDescription()).isEqualTo("Reviewers");
    assertThat(group2.getPermission()).isNull();

    GroupWithPermissionDto group3 = result.get(3);
    assertThat(group3.getName()).isEqualTo("sonar-users");
    assertThat(group3.getDescription()).isEqualTo("Any new users created will automatically join this group");
    assertThat(group3.getPermission()).isNotNull();
  }

  @Test
  public void anyone_group_is_not_returned_when_it_has_no_permission() {
    dbTester.prepareDbUnit(getClass(), "groups_with_permissions.xml");

    // Anyone group has not the permission 'admin', so it's not returned
    PermissionQuery query = PermissionQuery.builder().permission("admin").build();
    List<GroupWithPermissionDto> result = dao.selectGroups(query, COMPONENT_ID);
    assertThat(result).hasSize(3);

    GroupWithPermissionDto group1 = result.get(0);
    assertThat(group1.getName()).isEqualTo("sonar-administrators");
    assertThat(group1.getPermission()).isNotNull();

    GroupWithPermissionDto group2 = result.get(1);
    assertThat(group2.getName()).isEqualTo("sonar-reviewers");
    assertThat(group2.getPermission()).isNull();

    GroupWithPermissionDto group3 = result.get(2);
    assertThat(group3.getName()).isEqualTo("sonar-users");
    assertThat(group3.getPermission()).isNull();
  }

  @Test
  public void select_groups_for_global_permission() {
    dbTester.prepareDbUnit(getClass(), "groups_with_permissions.xml");

    PermissionQuery query = PermissionQuery.builder().permission("admin").build();
    List<GroupWithPermissionDto> result = dao.selectGroups(query, null);
    assertThat(result).hasSize(3);

    GroupWithPermissionDto group1 = result.get(0);
    assertThat(group1.getName()).isEqualTo("sonar-administrators");
    assertThat(group1.getPermission()).isNotNull();

    GroupWithPermissionDto group2 = result.get(1);
    assertThat(group2.getName()).isEqualTo("sonar-reviewers");
    assertThat(group2.getPermission()).isNull();

    GroupWithPermissionDto group3 = result.get(2);
    assertThat(group3.getName()).isEqualTo("sonar-users");
    assertThat(group3.getPermission()).isNull();
  }

  @Test
  public void search_by_groups_name() {
    dbTester.prepareDbUnit(getClass(), "groups_with_permissions.xml");

    List<GroupWithPermissionDto> result = dao.selectGroups(PermissionQuery.builder().permission("user").search("aDMini").build(), COMPONENT_ID);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("sonar-administrators");

    result = dao.selectGroups(PermissionQuery.builder().permission("user").search("sonar").build(), COMPONENT_ID);
    assertThat(result).hasSize(3);
  }

  @Test
  public void search_groups_should_be_sorted_by_group_name() {
    dbTester.prepareDbUnit(getClass(), "groups_with_permissions_should_be_sorted_by_group_name.xml");

    List<GroupWithPermissionDto> result = dao.selectGroups(PermissionQuery.builder().permission("user").build(), COMPONENT_ID);
    assertThat(result).hasSize(4);
    assertThat(result.get(0).getName()).isEqualTo("Anyone");
    assertThat(result.get(1).getName()).isEqualTo("sonar-administrators");
    assertThat(result.get(2).getName()).isEqualTo("sonar-reviewers");
    assertThat(result.get(3).getName()).isEqualTo("sonar-users");
  }

}
