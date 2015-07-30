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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.user.RoleDao;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class PermissionRepositoryTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  System2 system2 = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  PermissionRepository underTest;

  @Before
  public void setUp() {
    when(system2.now()).thenReturn(123456789L);

    Settings settings = new Settings();
    underTest = new PermissionRepository(dbTester.getDbClient(), settings);
  }

  @Test
  public void should_apply_permission_template() {
    dbTester.prepareDbUnit(getClass(), "should_apply_permission_template.xml");

    RoleDao roleDao = dbTester.getDbClient().roleDao();
    assertThat(roleDao.selectGroupPermissions(dbTester.getSession(), "sonar-administrators", 123L)).isEmpty();
    assertThat(roleDao.selectGroupPermissions(dbTester.getSession(), "sonar-users", 123L)).isEmpty();
    assertThat(roleDao.selectGroupPermissions(dbTester.getSession(), "Anyone", 123L)).isEmpty();
    assertThat(roleDao.selectUserPermissions(dbTester.getSession(), "marius", 123L)).isEmpty();

    underTest.applyPermissionTemplate(dbTester.getSession(), "default_20130101_010203", 123L);

    assertThat(roleDao.selectGroupPermissions(dbTester.getSession(), "sonar-administrators", 123L)).containsOnly("admin", "issueadmin");
    assertThat(roleDao.selectGroupPermissions(dbTester.getSession(), "sonar-users", 123L)).containsOnly("user", "codeviewer");
    assertThat(roleDao.selectGroupPermissions(dbTester.getSession(), "Anyone", 123L)).containsOnly("user", "codeviewer");

    assertThat(roleDao.selectUserPermissions(dbTester.getSession(), "marius", 123L)).containsOnly("admin");

    assertThat(dbTester.getDbClient().resourceDao().selectResource(123L, dbTester.getSession()).getAuthorizationUpdatedAt()).isEqualTo(123456789L);
  }

  @Test
  public void should_add_user_permission() {
    dbTester.prepareDbUnit(getClass(), "should_add_user_permission.xml");

    underTest.insertUserPermission(123L, 200L, UserRole.ADMIN, dbTester.getSession());
    dbTester.getSession().commit();

    dbTester.assertDbUnitTable(getClass(), "should_add_user_permission-result.xml", "user_roles", "user_id", "resource_id", "role");
    dbTester.assertDbUnitTable(getClass(), "should_add_user_permission-result.xml", "projects", "authorization_updated_at");
  }

  @Test
  public void should_delete_user_permission() {
    dbTester.prepareDbUnit(getClass(), "should_delete_user_permission.xml");

    underTest.deleteUserPermission(123L, 200L, UserRole.ADMIN, dbTester.getSession());
    dbTester.getSession().commit();

    dbTester.assertDbUnitTable(getClass(), "should_delete_user_permission-result.xml", "user_roles", "user_id", "resource_id", "role");
    dbTester.assertDbUnitTable(getClass(), "should_delete_user_permission-result.xml", "projects", "authorization_updated_at");
  }

  @Test
  public void should_insert_group_permission() {
    dbTester.prepareDbUnit(getClass(), "should_insert_group_permission.xml");

    underTest.insertGroupPermission(123L, 100L, UserRole.USER, dbTester.getSession());
    dbTester.getSession().commit();

    dbTester.assertDbUnitTable(getClass(), "should_insert_group_permission-result.xml", "group_roles", "group_id", "resource_id", "role");
    dbTester.assertDbUnitTable(getClass(), "should_insert_group_permission-result.xml", "projects", "authorization_updated_at");
  }

  @Test
  public void should_insert_group_name_permission() {
    dbTester.prepareDbUnit(getClass(), "should_insert_group_permission.xml");

    underTest.insertGroupPermission(123L, "devs", UserRole.USER, dbTester.getSession());
    dbTester.getSession().commit();

    dbTester.assertDbUnitTable(getClass(), "should_insert_group_permission-result.xml", "group_roles", "group_id", "resource_id", "role");
    dbTester.assertDbUnitTable(getClass(), "should_insert_group_permission-result.xml", "projects", "authorization_updated_at");
  }

  @Test
  public void should_insert_anyone_group_permission() {
    dbTester.prepareDbUnit(getClass(), "should_insert_anyone_group_permission.xml");

    underTest.insertGroupPermission(123L, "Anyone", UserRole.USER, dbTester.getSession());
    dbTester.getSession().commit();

    dbTester.assertDbUnitTable(getClass(), "should_insert_anyone_group_permission-result.xml", "group_roles", "group_id", "resource_id", "role");
    dbTester.assertDbUnitTable(getClass(), "should_insert_anyone_group_permission-result.xml", "projects", "authorization_updated_at");
  }

  @Test
  public void should_delete_group_permission() {
    dbTester.prepareDbUnit(getClass(), "should_delete_group_permission.xml");

    underTest.deleteGroupPermission(123L, 100L, UserRole.USER, dbTester.getSession());
    dbTester.getSession().commit();

    dbTester.assertDbUnitTable(getClass(), "should_delete_group_permission-result.xml", "group_roles", "group_id", "resource_id", "role");
    dbTester.assertDbUnitTable(getClass(), "should_delete_group_permission-result.xml", "projects", "authorization_updated_at");
  }

  @Test
  public void should_delete_group_name_permission() {
    dbTester.prepareDbUnit(getClass(), "should_delete_group_permission.xml");

    underTest.deleteGroupPermission(123L, "devs", UserRole.USER, dbTester.getSession());
    dbTester.getSession().commit();

    dbTester.assertDbUnitTable(getClass(), "should_delete_group_permission-result.xml", "group_roles", "group_id", "resource_id", "role");
    dbTester.assertDbUnitTable(getClass(), "should_delete_group_permission-result.xml", "projects", "authorization_updated_at");
  }
}
