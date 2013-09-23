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
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.AbstractDaoTestCase;

import static org.fest.assertions.Assertions.assertThat;

public class RoleDaoTest extends AbstractDaoTestCase {

  @Test
  public void retrieve_global_user_permissions() throws Exception {
    setupData("globalUserPermissions");

    RoleDao dao = new RoleDao(getMyBatis());

    assertThat(dao.selectUserPermissions("admin_user", null)).containsOnly(GlobalPermissions.SYSTEM_ADMIN, GlobalPermissions.QUALITY_PROFILE_ADMIN);
    assertThat(dao.selectUserPermissions("profile_admin_user", null)).containsOnly(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  @Test
  public void retrieve_resource_user_permissions() throws Exception {
    setupData("resourceUserPermissions");

    RoleDao dao = new RoleDao(getMyBatis());

    assertThat(dao.selectUserPermissions("admin_user", 1L)).containsOnly(UserRole.ADMIN, UserRole.USER);
    assertThat(dao.selectUserPermissions("browse_admin_user", 1L)).containsOnly(UserRole.USER);
  }

  @Test
  public void retrieve_global_group_permissions() throws Exception {
    setupData("globalGroupPermissions");

    RoleDao dao = new RoleDao(getMyBatis());

    assertThat(dao.selectGroupPermissions("sonar-administrators", null)).containsOnly(GlobalPermissions.SYSTEM_ADMIN, GlobalPermissions.QUALITY_PROFILE_ADMIN,
        GlobalPermissions.DASHBOARD_SHARING, GlobalPermissions.DRY_RUN_EXECUTION, GlobalPermissions.SCAN_EXECUTION);
    assertThat(dao.selectGroupPermissions("sonar-users", null)).containsOnly(GlobalPermissions.DASHBOARD_SHARING, GlobalPermissions.DRY_RUN_EXECUTION,
        GlobalPermissions.SCAN_EXECUTION);
    assertThat(dao.selectGroupPermissions(DefaultGroups.ANYONE, null)).containsOnly(GlobalPermissions.DRY_RUN_EXECUTION, GlobalPermissions.SCAN_EXECUTION);
  }

  @Test
  public void retrieve_resource_group_permissions() throws Exception {
    setupData("resourceGroupPermissions");

    RoleDao dao = new RoleDao(getMyBatis());

    assertThat(dao.selectGroupPermissions("sonar-administrators", 1L)).containsOnly(UserRole.ADMIN, UserRole.CODEVIEWER, UserRole.USER);
    assertThat(dao.selectGroupPermissions("sonar-users", 1L)).containsOnly(UserRole.CODEVIEWER, UserRole.USER);
  }

  @Test
  public void delete_global_user_permission() throws Exception {
    setupData("globalUserPermissions");

    UserRoleDto userRoleToDelete = new UserRoleDto().setUserId(200L).setRole(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    RoleDao dao = new RoleDao(getMyBatis());
    dao.deleteUserRole(userRoleToDelete);

    checkTable("globalUserPermissions", "user_roles", "user_id", "role");
  }

  @Test
  public void delete_resource_user_permission() throws Exception {
    setupData("resourceUserPermissions");

    UserRoleDto userRoleToDelete = new UserRoleDto().setUserId(200L).setRole(UserRole.USER).setResourceId(1L);

    RoleDao dao = new RoleDao(getMyBatis());
    dao.deleteUserRole(userRoleToDelete);

    checkTable("resourceUserPermissions", "user_roles", "user_id", "role");
  }

  @Test
  public void delete_global_group_permission() throws Exception {
    setupData("globalGroupPermissions");

    GroupRoleDto groupRoleToDelete = new GroupRoleDto().setGroupId(100L).setRole(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    RoleDao dao = new RoleDao(getMyBatis());
    dao.deleteGroupRole(groupRoleToDelete);

    checkTable("globalGroupPermissions", "group_roles", "group_id", "role");
  }

  @Test
  public void delete_resource_group_permission() throws Exception {
    setupData("resourceGroupPermissions");

    GroupRoleDto groupRoleToDelete = new GroupRoleDto().setGroupId(100L).setRole(UserRole.CODEVIEWER).setResourceId(1L);

    RoleDao dao = new RoleDao(getMyBatis());
    dao.deleteGroupRole(groupRoleToDelete);

    checkTable("resourceGroupPermissions", "group_roles", "group_id", "role");
  }

}
