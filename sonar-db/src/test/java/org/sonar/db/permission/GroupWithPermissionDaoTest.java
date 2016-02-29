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
import javax.annotation.Nullable;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupRoleDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.user.GroupTesting.newGroupDto;


public class GroupWithPermissionDaoTest {

  private static final long COMPONENT_ID = 100L;

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  final DbSession session = db.getSession();

  PermissionDao underTest = new PermissionDao(db.myBatis());

  @Test
  public void select_groups_for_project_permission() {
    db.prepareDbUnit(getClass(), "groups_with_permissions.xml");

    PermissionQuery query = PermissionQuery.builder().permission("user").build();
    List<GroupWithPermissionDto> result = underTest.selectGroups(session, query, COMPONENT_ID);
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
    db.prepareDbUnit(getClass(), "groups_with_permissions.xml");

    // Anyone group has not the permission 'admin', so it's not returned
    PermissionQuery query = PermissionQuery.builder().permission("admin").build();
    List<GroupWithPermissionDto> result = underTest.selectGroups(session, query, COMPONENT_ID);
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
    db.prepareDbUnit(getClass(), "groups_with_permissions.xml");

    PermissionQuery query = PermissionQuery.builder().permission("admin").build();
    List<GroupWithPermissionDto> result = underTest.selectGroups(session, query, null);
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
    db.prepareDbUnit(getClass(), "groups_with_permissions.xml");

    List<GroupWithPermissionDto> result = underTest.selectGroups(session, PermissionQuery.builder().permission("user").search("aDMini").build(), COMPONENT_ID);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("sonar-administrators");

    result = underTest.selectGroups(session, PermissionQuery.builder().permission("user").search("sonar").build(), COMPONENT_ID);
    assertThat(result).hasSize(3);
  }

  @Test
  public void search_groups_should_be_sorted_by_group_name() {
    db.prepareDbUnit(getClass(), "groups_with_permissions_should_be_sorted_by_group_name.xml");

    List<GroupWithPermissionDto> result = underTest.selectGroups(session, PermissionQuery.builder().permission("user").build(), COMPONENT_ID);
    int count = underTest.countGroups(session, "user", COMPONENT_ID);

    assertThat(result).hasSize(4);
    assertThat(count).isEqualTo(2);
    assertThat(result.get(0).getName()).isEqualTo("Anyone");
    assertThat(result.get(1).getName()).isEqualTo("sonar-administrators");
    assertThat(result.get(2).getName()).isEqualTo("sonar-reviewers");
    assertThat(result.get(3).getName()).isEqualTo("sonar-users");
  }

  @Test
  public void group_count_by_permission_and_component_id() {
    GroupDto group1 = insertGroup(newGroupDto());
    GroupDto group2 = insertGroup(newGroupDto());
    GroupDto group3 = insertGroup(newGroupDto());

    insertGroupRole(ISSUE_ADMIN, group1.getId(), 42L);
    insertGroupRole(ADMIN, group1.getId(), 123L);
    insertGroupRole(ADMIN, group2.getId(), 123L);
    insertGroupRole(ADMIN, group3.getId(), 123L);
    // anyone group
    insertGroupRole(ADMIN, null, 123L);
    insertGroupRole(USER, group1.getId(), 123L);
    insertGroupRole(USER, group1.getId(), 456L);

    commit();

    final List<CountByProjectAndPermissionDto> result = new ArrayList<>();
    underTest.groupsCountByComponentIdAndPermission(session, Arrays.asList(123L, 456L, 789L), new ResultHandler() {
      @Override
      public void handleResult(ResultContext context) {
        result.add((CountByProjectAndPermissionDto) context.getResultObject());
      }
    });

    assertThat(result).hasSize(3);
    assertThat(result).extracting("permission").containsOnly(ADMIN, USER);
    assertThat(result).extracting("componentId").containsOnly(123L, 456L);
    assertThat(result).extracting("count").containsOnly(4, 1);
  }

  private GroupDto insertGroup(GroupDto groupDto) {
    return db.getDbClient().groupDao().insert(session, groupDto);
  }

  private void insertGroupRole(String permission, @Nullable Long groupId, long componentId) {
    db.getDbClient().roleDao().insertGroupRole(session, new GroupRoleDto()
      .setRole(permission)
      .setGroupId(groupId)
      .setResourceId(componentId));
  }

  private void commit() {
    session.commit();
  }
}
