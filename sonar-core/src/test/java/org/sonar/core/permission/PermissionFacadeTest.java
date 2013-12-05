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

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.web.UserRole;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.user.RoleDao;
import org.sonar.core.user.UserDao;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PermissionFacadeTest extends AbstractDaoTestCase {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  private PermissionFacade permissionFacade;
  private PermissionTemplateDao permissionTemplateDao;

  @Before
  public void setUp() {
    RoleDao roleDao = new RoleDao(getMyBatis());
    UserDao userDao = new UserDao(getMyBatis());
    permissionTemplateDao = new PermissionTemplateDao(getMyBatis());
    Settings settings = new Settings();
    permissionFacade = new PermissionFacade(getMyBatis(), roleDao, userDao, new ResourceDao(getMyBatis()), permissionTemplateDao, settings);
  }

  @Test
  public void should_apply_permission_template() throws Exception {
    setupData("should_apply_permission_template");

    permissionFacade.applyPermissionTemplate("default_20130101_010203", 123L);

    checkTable("should_apply_permission_template", "group_roles", "group_id", "resource_id", "role");
    checkTable("should_apply_permission_template", "user_roles", "group_id", "resource_id", "role");
  }

  @Test
  public void should_count_component_permissions() throws Exception {
    setupData("should_count_component_permissions");

    assertThat(permissionFacade.countComponentPermissions(123L)).isEqualTo(2);
  }

  @Test
  public void should_add_user_permission() throws Exception {
    setupData("should_add_user_permission");

    permissionFacade.insertUserPermission(123L, 200L, UserRole.ADMIN);

    checkTable("should_add_user_permission", "user_roles", "user_id", "resource_id", "role");
  }

  @Test
  public void should_delete_user_permission() throws Exception {
    setupData("should_delete_user_permission");

    permissionFacade.deleteUserPermission(123L, 200L, UserRole.ADMIN);

    checkTable("should_delete_user_permission", "user_roles", "user_id", "resource_id", "role");
  }

  @Test
  public void should_insert_group_permission() throws Exception {
    setupData("should_insert_group_permission");

    SqlSession session = getMyBatis().openSession();
    try {
      permissionFacade.insertGroupPermission(123L, 100L, UserRole.USER);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }

    checkTable("should_insert_group_permission", "group_roles", "group_id", "resource_id", "role");
  }

  @Test
  public void should_insert_group_name_permission() throws Exception {
    setupData("should_insert_group_permission");

    SqlSession session = getMyBatis().openSession();
    try {
      permissionFacade.insertGroupPermission(123L, "devs", UserRole.USER, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }

    checkTable("should_insert_group_permission", "group_roles", "group_id", "resource_id", "role");
  }

  @Test
  public void should_insert_anyone_group_permission() throws Exception {
    setupData("should_insert_anyone_group_permission");

    SqlSession session = getMyBatis().openSession();
    try {
      permissionFacade.insertGroupPermission(123L, "Anyone", UserRole.USER, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }

    checkTable("should_insert_anyone_group_permission", "group_roles", "group_id", "resource_id", "role");
  }

  @Test
  public void should_delete_group_permission() throws Exception {
    setupData("should_delete_group_permission");

    permissionFacade.deleteGroupPermission(123L, 100L, UserRole.USER);

    checkTable("should_delete_group_permission", "group_roles", "group_id", "resource_id", "role");
  }

  @Test
  public void should_delete_group_name_permission() throws Exception {
    setupData("should_delete_group_permission");

    SqlSession session = getMyBatis().openSession();
    try {
      permissionFacade.deleteGroupPermission(123L, "devs", UserRole.USER, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }

    checkTable("should_delete_group_permission", "group_roles", "group_id", "resource_id", "role");
  }

  @Test
  public void should_retrieve_permission_template() throws Exception {
    PermissionTemplateDto permissionTemplateDto = new PermissionTemplateDto().setName("Test template").setKee("test_template");
    PermissionTemplateDto templateWithPermissions = new PermissionTemplateDto().setKee("test_template");
    permissionTemplateDao = mock(PermissionTemplateDao.class);
    when(permissionTemplateDao.selectTemplateByKey("test_template")).thenReturn(permissionTemplateDto);
    when(permissionTemplateDao.selectPermissionTemplate("test_template")).thenReturn(templateWithPermissions);

    permissionFacade = new PermissionFacade(null, null, null, null, permissionTemplateDao, null);

    PermissionTemplateDto permissionTemplate = permissionFacade.getPermissionTemplateWithPermissions("test_template");

    assertThat(permissionTemplate).isSameAs(templateWithPermissions);
  }

  @Test
  public void should_fail_on_unmatched_template() throws Exception {
    throwable.expect(IllegalArgumentException.class);

    permissionTemplateDao = mock(PermissionTemplateDao.class);

    permissionFacade = new PermissionFacade(null, null, null, null, permissionTemplateDao, null);
    permissionFacade.getPermissionTemplateWithPermissions("unmatched");
  }

  @Test
  public void should_remove_all_permissions() throws Exception {
    setupData("should_remove_all_permissions");

    SqlSession session = getMyBatis().openSession();
    try {
      assertThat(permissionFacade.selectGroupPermissions("devs", 123L)).hasSize(1);
      assertThat(permissionFacade.selectGroupPermissions("other", 123L)).isEmpty();
      assertThat(permissionFacade.selectUserPermissions("dave.loper", 123L)).hasSize(1);
      assertThat(permissionFacade.selectUserPermissions("other.user", 123L)).isEmpty();

      permissionFacade.removeAllPermissions(123L, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }

    checkTable("should_remove_all_permissions", "group_roles", "group_id", "resource_id", "role");
    checkTable("should_remove_all_permissions", "user_roles", "user_id", "resource_id", "role");

    assertThat(permissionFacade.selectGroupPermissions("devs", 123L)).isEmpty();
    assertThat(permissionFacade.selectUserPermissions("dave.loper", 123L)).isEmpty();
  }
}
