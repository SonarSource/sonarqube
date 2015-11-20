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
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.RoleDao;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class PermissionRepositoryTest {

  static final String DEFAULT_TEMPLATE = "default_20130101_010203";
  static final long PROJECT_ID = 123L;
  static final long NOW = 123456789L;

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  System2 system2 = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system2);
  DbSession session = dbTester.getSession();

  Settings settings = new Settings();
  PermissionRepository underTest = new PermissionRepository(dbTester.getDbClient(), settings);

  @Before
  public void setUp() {
    when(system2.now()).thenReturn(NOW);
  }

  @Test
  public void apply_permission_template() {
    dbTester.prepareDbUnit(getClass(), "should_apply_permission_template.xml");

    RoleDao roleDao = dbTester.getDbClient().roleDao();
    assertThat(roleDao.selectGroupPermissions(session, "sonar-administrators", PROJECT_ID)).isEmpty();
    assertThat(roleDao.selectGroupPermissions(session, "sonar-users", PROJECT_ID)).isEmpty();
    assertThat(roleDao.selectGroupPermissions(session, "Anyone", PROJECT_ID)).isEmpty();
    assertThat(roleDao.selectUserPermissions(session, "marius", PROJECT_ID)).isEmpty();

    underTest.applyPermissionTemplate(session, "default_20130101_010203", PROJECT_ID);

    assertThat(roleDao.selectGroupPermissions(session, "sonar-administrators", PROJECT_ID)).containsOnly("admin", "issueadmin");
    assertThat(roleDao.selectGroupPermissions(session, "sonar-users", PROJECT_ID)).containsOnly("user", "codeviewer");
    assertThat(roleDao.selectGroupPermissions(session, "Anyone", PROJECT_ID)).containsOnly("user", "codeviewer");

    assertThat(roleDao.selectUserPermissions(session, "marius", PROJECT_ID)).containsOnly("admin");

    checkAuthorizationUpdatedAtIsUpdated();
  }

  @Test
  public void apply_default_permission_template_from_component_id() {
    dbTester.prepareDbUnit(getClass(), "apply_default_permission_template.xml");
    settings.setProperty("sonar.permission.template.default", DEFAULT_TEMPLATE);

    underTest.applyDefaultPermissionTemplate(session, PROJECT_ID);
    session.commit();

    dbTester.assertDbUnitTable(getClass(), "apply_default_permission_template-result.xml", "user_roles", "user_id", "resource_id", "role");
  }

  @Test
  public void apply_default_permission_template_from_component() {
    dbTester.prepareDbUnit(getClass(), "apply_default_permission_template.xml");
    settings.setProperty("sonar.permission.template.default", DEFAULT_TEMPLATE);

    underTest.applyDefaultPermissionTemplate(session, dbTester.getDbClient().componentDao().selectOrFailByKey(session, "org.struts:struts"));
    session.commit();

    dbTester.assertDbUnitTable(getClass(), "apply_default_permission_template-result.xml", "user_roles", "user_id", "resource_id", "role");
  }

  @Test
  public void should_add_user_permission() {
    dbTester.prepareDbUnit(getClass(), "should_add_user_permission.xml");

    underTest.insertUserPermission(PROJECT_ID, 200L, UserRole.ADMIN, session);
    session.commit();

    dbTester.assertDbUnitTable(getClass(), "should_add_user_permission-result.xml", "user_roles", "user_id", "resource_id", "role");
    dbTester.assertDbUnitTable(getClass(), "should_add_user_permission-result.xml", "projects", "authorization_updated_at");

    checkAuthorizationUpdatedAtIsUpdated();
  }

  @Test
  public void should_delete_user_permission() {
    dbTester.prepareDbUnit(getClass(), "should_delete_user_permission.xml");

    underTest.deleteUserPermission(PROJECT_ID, 200L, UserRole.ADMIN, session);
    session.commit();

    dbTester.assertDbUnitTable(getClass(), "should_delete_user_permission-result.xml", "user_roles", "user_id", "resource_id", "role");
    dbTester.assertDbUnitTable(getClass(), "should_delete_user_permission-result.xml", "projects", "authorization_updated_at");
    checkAuthorizationUpdatedAtIsUpdated();
  }

  @Test
  public void should_insert_group_permission() {
    dbTester.prepareDbUnit(getClass(), "should_insert_group_permission.xml");

    underTest.insertGroupPermission(PROJECT_ID, 100L, UserRole.USER, session);
    session.commit();

    dbTester.assertDbUnitTable(getClass(), "should_insert_group_permission-result.xml", "group_roles", "group_id", "resource_id", "role");
    dbTester.assertDbUnitTable(getClass(), "should_insert_group_permission-result.xml", "projects", "authorization_updated_at");
    checkAuthorizationUpdatedAtIsUpdated();
  }

  @Test
  public void should_insert_group_name_permission() {
    dbTester.prepareDbUnit(getClass(), "should_insert_group_permission.xml");

    underTest.insertGroupPermission(PROJECT_ID, "devs", UserRole.USER, session);
    session.commit();

    dbTester.assertDbUnitTable(getClass(), "should_insert_group_permission-result.xml", "group_roles", "group_id", "resource_id", "role");
    dbTester.assertDbUnitTable(getClass(), "should_insert_group_permission-result.xml", "projects", "authorization_updated_at");
  }

  @Test
  public void should_insert_anyone_group_permission() {
    dbTester.prepareDbUnit(getClass(), "should_insert_anyone_group_permission.xml");

    underTest.insertGroupPermission(PROJECT_ID, "Anyone", UserRole.USER, session);
    session.commit();

    dbTester.assertDbUnitTable(getClass(), "should_insert_anyone_group_permission-result.xml", "group_roles", "group_id", "resource_id", "role");
    dbTester.assertDbUnitTable(getClass(), "should_insert_anyone_group_permission-result.xml", "projects", "authorization_updated_at");
  }

  @Test
  public void should_delete_group_permission() {
    dbTester.prepareDbUnit(getClass(), "should_delete_group_permission.xml");

    underTest.deleteGroupPermission(PROJECT_ID, 100L, UserRole.USER, session);
    session.commit();

    dbTester.assertDbUnitTable(getClass(), "should_delete_group_permission-result.xml", "group_roles", "group_id", "resource_id", "role");
    dbTester.assertDbUnitTable(getClass(), "should_delete_group_permission-result.xml", "projects", "authorization_updated_at");
    checkAuthorizationUpdatedAtIsUpdated();
  }

  @Test
  public void should_delete_group_name_permission() {
    dbTester.prepareDbUnit(getClass(), "should_delete_group_permission.xml");

    underTest.deleteGroupPermission(PROJECT_ID, "devs", UserRole.USER, session);
    session.commit();

    dbTester.assertDbUnitTable(getClass(), "should_delete_group_permission-result.xml", "group_roles", "group_id", "resource_id", "role");
    dbTester.assertDbUnitTable(getClass(), "should_delete_group_permission-result.xml", "projects", "authorization_updated_at");
  }

  private void checkAuthorizationUpdatedAtIsUpdated() {
    assertThat(dbTester.getDbClient().resourceDao().selectResource(PROJECT_ID, session).getAuthorizationUpdatedAt()).isEqualTo(NOW);
  }

  private void checkAuthorizationUpdatedAtIsNotUpdated() {
    assertThat(dbTester.getDbClient().resourceDao().selectResource(PROJECT_ID, session).getAuthorizationUpdatedAt()).isNull();
  }

}
