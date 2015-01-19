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

package org.sonar.core.user;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;

import static org.assertj.core.api.Assertions.assertThat;

public class RoleDaoTest extends AbstractDaoTestCase {

  DbSession session;

  RoleDao dao;

  @Before
  public void setUp() throws Exception {
    session = getMyBatis().openSession(false);
    dao = new RoleDao();
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void retrieve_global_user_permissions() throws Exception {
    setupData("globalUserPermissions");

    assertThat(dao.selectUserPermissions(session, "admin_user", null)).containsOnly(GlobalPermissions.SYSTEM_ADMIN, GlobalPermissions.QUALITY_PROFILE_ADMIN);
    assertThat(dao.selectUserPermissions(session, "profile_admin_user", null)).containsOnly(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  @Test
  public void retrieve_resource_user_permissions() throws Exception {
    setupData("resourceUserPermissions");

    assertThat(dao.selectUserPermissions(session, "admin_user", 1L)).containsOnly(UserRole.ADMIN, UserRole.USER);
    assertThat(dao.selectUserPermissions(session, "browse_admin_user", 1L)).containsOnly(UserRole.USER);
  }

  @Test
  public void retrieve_global_group_permissions() throws Exception {
    setupData("globalGroupPermissions");

    assertThat(dao.selectGroupPermissions(session, "sonar-administrators", null)).containsOnly(GlobalPermissions.SYSTEM_ADMIN, GlobalPermissions.QUALITY_PROFILE_ADMIN,
      GlobalPermissions.DASHBOARD_SHARING);
    assertThat(dao.selectGroupPermissions(session, "sonar-users", null)).containsOnly(GlobalPermissions.DASHBOARD_SHARING);
    assertThat(dao.selectGroupPermissions(session, DefaultGroups.ANYONE, null)).containsOnly(GlobalPermissions.PREVIEW_EXECUTION, GlobalPermissions.SCAN_EXECUTION);
    assertThat(dao.selectGroupPermissions(session, "anyone", null)).containsOnly(GlobalPermissions.PREVIEW_EXECUTION, GlobalPermissions.SCAN_EXECUTION);
    assertThat(dao.selectGroupPermissions(session, "AnYoNe", null)).containsOnly(GlobalPermissions.PREVIEW_EXECUTION, GlobalPermissions.SCAN_EXECUTION);
  }

  @Test
  public void retrieve_resource_group_permissions() throws Exception {
    setupData("resourceGroupPermissions");

    assertThat(dao.selectGroupPermissions(session, "sonar-administrators", 1L)).containsOnly(UserRole.ADMIN, UserRole.CODEVIEWER);
    assertThat(dao.selectGroupPermissions(session, "sonar-users", 1L)).containsOnly(UserRole.CODEVIEWER);
  }

  @Test
  public void delete_global_user_permission() throws Exception {
    setupData("globalUserPermissions");

    UserRoleDto userRoleToDelete = new UserRoleDto().setUserId(200L).setRole(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    dao.deleteUserRole(userRoleToDelete, session);
    session.commit();

    checkTable("globalUserPermissions", "user_roles", "user_id", "role");
  }

  @Test
  public void delete_resource_user_permission() throws Exception {
    setupData("resourceUserPermissions");

    UserRoleDto userRoleToDelete = new UserRoleDto().setUserId(200L).setRole(UserRole.USER).setResourceId(1L);

    dao.deleteUserRole(userRoleToDelete, session);
    session.commit();

    checkTable("resourceUserPermissions", "user_roles", "user_id", "role");
  }

  @Test
  public void delete_global_group_permission() throws Exception {
    setupData("globalGroupPermissions");

    GroupRoleDto groupRoleToDelete = new GroupRoleDto().setGroupId(100L).setRole(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    dao.deleteGroupRole(groupRoleToDelete, session);
    session.commit();

    checkTable("globalGroupPermissions", "group_roles", "group_id", "role");
  }

  @Test
  public void delete_resource_group_permission() throws Exception {
    setupData("resourceGroupPermissions");

    GroupRoleDto groupRoleToDelete = new GroupRoleDto().setGroupId(100L).setRole(UserRole.CODEVIEWER).setResourceId(1L);

    dao.deleteGroupRole(groupRoleToDelete, session);
    session.commit();

    checkTable("resourceGroupPermissions", "group_roles", "group_id", "role");
  }

}
