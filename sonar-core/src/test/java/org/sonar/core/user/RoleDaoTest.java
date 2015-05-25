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
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class RoleDaoTest {

  @ClassRule
  public static final DbTester dbTester = new DbTester();

  DbSession session;

  RoleDao dao;

  @Before
  public void setUp() {
    dbTester.truncateTables();
    session = dbTester.myBatis().openSession(false);
    dao = new RoleDao();
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void retrieve_global_user_permissions() {
    dbTester.prepareDbUnit(getClass(), "globalUserPermissions.xml");

    assertThat(dao.selectUserPermissions(session, "admin_user", null)).containsOnly(GlobalPermissions.SYSTEM_ADMIN, GlobalPermissions.QUALITY_PROFILE_ADMIN);
    assertThat(dao.selectUserPermissions(session, "profile_admin_user", null)).containsOnly(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  @Test
  public void retrieve_resource_user_permissions() {
    dbTester.prepareDbUnit(getClass(), "resourceUserPermissions.xml");

    assertThat(dao.selectUserPermissions(session, "admin_user", 1L)).containsOnly(UserRole.ADMIN, UserRole.USER);
    assertThat(dao.selectUserPermissions(session, "browse_admin_user", 1L)).containsOnly(UserRole.USER);
  }

  @Test
  public void retrieve_global_group_permissions() {
    dbTester.prepareDbUnit(getClass(), "globalGroupPermissions.xml");

    assertThat(dao.selectGroupPermissions(session, "sonar-administrators", null)).containsOnly(GlobalPermissions.SYSTEM_ADMIN, GlobalPermissions.QUALITY_PROFILE_ADMIN,
      GlobalPermissions.DASHBOARD_SHARING);
    assertThat(dao.selectGroupPermissions(session, "sonar-users", null)).containsOnly(GlobalPermissions.DASHBOARD_SHARING);
    assertThat(dao.selectGroupPermissions(session, DefaultGroups.ANYONE, null)).containsOnly(GlobalPermissions.PREVIEW_EXECUTION, GlobalPermissions.SCAN_EXECUTION);
    assertThat(dao.selectGroupPermissions(session, "anyone", null)).containsOnly(GlobalPermissions.PREVIEW_EXECUTION, GlobalPermissions.SCAN_EXECUTION);
    assertThat(dao.selectGroupPermissions(session, "AnYoNe", null)).containsOnly(GlobalPermissions.PREVIEW_EXECUTION, GlobalPermissions.SCAN_EXECUTION);
  }

  @Test
  public void retrieve_resource_group_permissions() {
    dbTester.prepareDbUnit(getClass(), "resourceGroupPermissions.xml");

    assertThat(dao.selectGroupPermissions(session, "sonar-administrators", 1L)).containsOnly(UserRole.ADMIN, UserRole.CODEVIEWER);
    assertThat(dao.selectGroupPermissions(session, "sonar-users", 1L)).containsOnly(UserRole.CODEVIEWER);
  }

  @Test
  public void delete_global_user_permission() {
    dbTester.prepareDbUnit(getClass(), "globalUserPermissions.xml");

    UserRoleDto userRoleToDelete = new UserRoleDto().setUserId(200L).setRole(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    dao.deleteUserRole(userRoleToDelete, session);
    session.commit();

    dbTester.assertDbUnit(getClass(), "globalUserPermissions-result.xml", "user_roles");
  }

  @Test
  public void delete_resource_user_permission() {
    dbTester.prepareDbUnit(getClass(), "resourceUserPermissions.xml");

    UserRoleDto userRoleToDelete = new UserRoleDto().setUserId(200L).setRole(UserRole.USER).setResourceId(1L);

    dao.deleteUserRole(userRoleToDelete, session);
    session.commit();

    dbTester.assertDbUnit(getClass(), "resourceUserPermissions-result.xml", "user_roles");
  }

  @Test
  public void delete_global_group_permission() {
    dbTester.prepareDbUnit(getClass(), "globalGroupPermissions.xml");

    GroupRoleDto groupRoleToDelete = new GroupRoleDto().setGroupId(100L).setRole(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    dao.deleteGroupRole(groupRoleToDelete, session);
    session.commit();

    dbTester.assertDbUnit(getClass(), "globalGroupPermissions-result.xml", "group_roles");
  }

  @Test
  public void delete_resource_group_permission() {
    dbTester.prepareDbUnit(getClass(), "resourceGroupPermissions.xml");

    GroupRoleDto groupRoleToDelete = new GroupRoleDto().setGroupId(100L).setRole(UserRole.CODEVIEWER).setResourceId(1L);

    dao.deleteGroupRole(groupRoleToDelete, session);
    session.commit();

    dbTester.assertDbUnit(getClass(), "resourceGroupPermissions-result.xml", "group_roles");
  }

  @Test
  public void delete_all_group_permissions_by_group_id() {
    dbTester.prepareDbUnit(getClass(), "deleteGroupPermissionsByGroupId.xml");

    dao.deleteGroupRolesByGroupId(session, 100L);
    session.commit();

    dbTester.assertDbUnit(getClass(), "deleteGroupPermissionsByGroupId-result.xml", "group_roles");
  }
}
