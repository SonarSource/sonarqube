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

package org.sonar.core.permission;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.user.RoleDao;
import org.sonar.core.user.UserDao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PermissionFacadeTest extends AbstractDaoTestCase {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  DbSession session;
  System2 system2;
  PermissionFacade permissionFacade;
  PermissionTemplateDao permissionTemplateDao;
  ResourceDao resourceDao;

  @Before
  public void setUp() {
    system2 = mock(System2.class);
    when(system2.now()).thenReturn(123456789L);

    session = getMyBatis().openSession(false);
    RoleDao roleDao = new RoleDao();
    UserDao userDao = new UserDao(getMyBatis(), system2);
    permissionTemplateDao = new PermissionTemplateDao(getMyBatis(), System2.INSTANCE);
    Settings settings = new Settings();
    resourceDao = new ResourceDao(getMyBatis(), system2);
    permissionFacade = new PermissionFacade(roleDao, userDao, resourceDao, permissionTemplateDao, settings);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void should_apply_permission_template() throws Exception {
    setupData("should_apply_permission_template");

    assertThat(permissionFacade.selectGroupPermissions(session, "sonar-administrators", 123L)).isEmpty();
    assertThat(permissionFacade.selectGroupPermissions(session, "sonar-users", 123L)).isEmpty();
    assertThat(permissionFacade.selectGroupPermissions(session, "Anyone", 123L)).isEmpty();
    assertThat(permissionFacade.selectUserPermissions(session, "marius", 123L)).isEmpty();

    permissionFacade.applyPermissionTemplate(session, "default_20130101_010203", 123L);

    assertThat(permissionFacade.selectGroupPermissions(session, "sonar-administrators", 123L)).containsOnly("admin", "issueadmin");
    assertThat(permissionFacade.selectGroupPermissions(session, "sonar-users", 123L)).containsOnly("user", "codeviewer");
    assertThat(permissionFacade.selectGroupPermissions(session, "Anyone", 123L)).containsOnly("user", "codeviewer");

    assertThat(permissionFacade.selectUserPermissions(session, "marius", 123L)).containsOnly("admin");

    assertThat(resourceDao.getResource(123L, session).getAuthorizationUpdatedAt()).isEqualTo(123456789L);
  }

  @Test
  public void should_count_component_permissions() throws Exception {
    setupData("should_count_component_permissions");

    assertThat(permissionFacade.countComponentPermissions(session, 123L)).isEqualTo(2);
  }

  @Test
  public void should_add_user_permission() throws Exception {
    setupData("should_add_user_permission");

    permissionFacade.insertUserPermission(123L, 200L, UserRole.ADMIN, session);
    session.commit();

    checkTable("should_add_user_permission", "user_roles", "user_id", "resource_id", "role");
    checkTable("should_add_user_permission", "projects", "authorization_updated_at");
  }

  @Test
  public void should_delete_user_permission() throws Exception {
    setupData("should_delete_user_permission");

    permissionFacade.deleteUserPermission(123L, 200L, UserRole.ADMIN, session);
    session.commit();

    checkTable("should_delete_user_permission", "user_roles", "user_id", "resource_id", "role");
    checkTable("should_delete_user_permission", "projects", "authorization_updated_at");
  }

  @Test
  public void should_insert_group_permission() throws Exception {
    setupData("should_insert_group_permission");

    permissionFacade.insertGroupPermission(123L, 100L, UserRole.USER, session);
    session.commit();

    checkTable("should_insert_group_permission", "group_roles", "group_id", "resource_id", "role");
    checkTable("should_insert_group_permission", "projects", "authorization_updated_at");
  }

  @Test
  public void should_insert_group_name_permission() throws Exception {
    setupData("should_insert_group_permission");

    permissionFacade.insertGroupPermission(123L, "devs", UserRole.USER, session);
    session.commit();

    checkTable("should_insert_group_permission", "group_roles", "group_id", "resource_id", "role");
    checkTable("should_insert_group_permission", "projects", "authorization_updated_at");
  }

  @Test
  public void should_insert_anyone_group_permission() throws Exception {
    setupData("should_insert_anyone_group_permission");

    permissionFacade.insertGroupPermission(123L, "Anyone", UserRole.USER, session);
    session.commit();

    checkTable("should_insert_anyone_group_permission", "group_roles", "group_id", "resource_id", "role");
    checkTable("should_insert_anyone_group_permission", "projects", "authorization_updated_at");
  }

  @Test
  public void should_delete_group_permission() throws Exception {
    setupData("should_delete_group_permission");

    permissionFacade.deleteGroupPermission(123L, 100L, UserRole.USER, session);
    session.commit();

    checkTable("should_delete_group_permission", "group_roles", "group_id", "resource_id", "role");
    checkTable("should_delete_group_permission", "projects", "authorization_updated_at");
  }

  @Test
  public void should_delete_group_name_permission() throws Exception {
    setupData("should_delete_group_permission");

    permissionFacade.deleteGroupPermission(123L, "devs", UserRole.USER, session);
    session.commit();

    checkTable("should_delete_group_permission", "group_roles", "group_id", "resource_id", "role");
    checkTable("should_delete_group_permission", "projects", "authorization_updated_at");
  }

  @Test
  public void should_retrieve_permission_template() throws Exception {
    PermissionTemplateDto permissionTemplateDto = new PermissionTemplateDto().setName("Test template").setKee("test_template");
    PermissionTemplateDto templateWithPermissions = new PermissionTemplateDto().setKee("test_template");
    permissionTemplateDao = mock(PermissionTemplateDao.class);
    when(permissionTemplateDao.selectTemplateByKey(session, "test_template")).thenReturn(permissionTemplateDto);
    when(permissionTemplateDao.selectPermissionTemplate(session, "test_template")).thenReturn(templateWithPermissions);

    permissionFacade = new PermissionFacade(null, null, null, permissionTemplateDao, null);

    PermissionTemplateDto permissionTemplate = permissionFacade.getPermissionTemplateWithPermissions(session, "test_template");

    assertThat(permissionTemplate).isSameAs(templateWithPermissions);
  }

  @Test
  public void should_fail_on_unmatched_template() throws Exception {
    throwable.expect(IllegalArgumentException.class);

    permissionTemplateDao = mock(PermissionTemplateDao.class);

    permissionFacade = new PermissionFacade(null, null, null, permissionTemplateDao, null);
    permissionFacade.getPermissionTemplateWithPermissions(session, "unmatched");
  }

  @Test
  public void should_remove_all_permissions() throws Exception {
    setupData("should_remove_all_permissions");

    assertThat(permissionFacade.selectGroupPermissions(session, "devs", 123L)).hasSize(1);
    assertThat(permissionFacade.selectGroupPermissions(session, "other", 123L)).isEmpty();
    assertThat(permissionFacade.selectUserPermissions(session, "dave.loper", 123L)).hasSize(1);
    assertThat(permissionFacade.selectUserPermissions(session, "other.user", 123L)).isEmpty();

    permissionFacade.removeAllPermissions(123L, session);
    session.commit();

    checkTable("should_remove_all_permissions", "group_roles", "group_id", "resource_id", "role");
    checkTable("should_remove_all_permissions", "user_roles", "user_id", "resource_id", "role");

    assertThat(permissionFacade.selectGroupPermissions(session, "devs", 123L)).isEmpty();
    assertThat(permissionFacade.selectUserPermissions(session, "dave.loper", 123L)).isEmpty();
  }
}
