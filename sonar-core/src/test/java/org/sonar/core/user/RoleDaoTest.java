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

package org.sonar.core.user;

import org.junit.Test;
import org.sonar.api.security.DefaultGroups;
import org.sonar.core.persistence.AbstractDaoTestCase;

import static org.fest.assertions.Assertions.assertThat;

public class RoleDaoTest extends AbstractDaoTestCase {

  @Test
  public void should_retrieve_user_permissions() throws Exception {
    setupData("userPermissions");

    RoleDao dao = new RoleDao(getMyBatis());

    assertThat(dao.selectUserPermissions("admin_user")).containsOnly(Permission.SYSTEM_ADMIN.key(), Permission.QUALITY_PROFILE_ADMIN.key());
    assertThat(dao.selectUserPermissions("profile_admin_user")).containsOnly(Permission.QUALITY_PROFILE_ADMIN.key());
  }

  @Test
  public void should_retrieve_group_permissions() throws Exception {
    setupData("groupPermissions");

    RoleDao dao = new RoleDao(getMyBatis());

    assertThat(dao.selectGroupPermissions("sonar-administrators")).containsOnly(Permission.SYSTEM_ADMIN.key(), Permission.QUALITY_PROFILE_ADMIN.key(),
        Permission.DASHBOARD_SHARING.key(), Permission.DRY_RUN_EXECUTION.key(), Permission.SCAN_EXECUTION.key());
    assertThat(dao.selectGroupPermissions("sonar-users")).containsOnly(Permission.DASHBOARD_SHARING.key(), Permission.DRY_RUN_EXECUTION.key(),
        Permission.SCAN_EXECUTION.key());
    assertThat(dao.selectGroupPermissions(DefaultGroups.ANYONE)).containsOnly(Permission.DRY_RUN_EXECUTION.key(), Permission.SCAN_EXECUTION.key());
  }

  @Test
  public void should_delete_user_global_permission() throws Exception {
    setupData("userPermissions");

    UserRoleDto userRoleToDelete = new UserRoleDto().setUserId(200L).setRole(Permission.QUALITY_PROFILE_ADMIN.key());

    RoleDao dao = new RoleDao(getMyBatis());
    dao.deleteUserRole(userRoleToDelete);

    checkTable("userPermissions", "user_roles", "user_id", "role");
  }

  @Test
  public void should_delete_group_global_permission() throws Exception {
    setupData("groupPermissions");

    GroupRoleDto groupRoleToDelete = new GroupRoleDto().setGroupId(100L).setRole(Permission.QUALITY_PROFILE_ADMIN.key());

    RoleDao dao = new RoleDao(getMyBatis());
    dao.deleteGroupRole(groupRoleToDelete);

    checkTable("groupPermissions", "group_roles", "group_id", "role");
  }

}
