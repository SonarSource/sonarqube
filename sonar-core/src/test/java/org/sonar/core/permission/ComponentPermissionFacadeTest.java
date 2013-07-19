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
import org.sonar.api.web.UserRole;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.user.RoleDao;
import org.sonar.core.user.UserDao;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComponentPermissionFacadeTest extends AbstractDaoTestCase {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  private ComponentPermissionFacade permissionFacade;
  private RoleDao roleDao;
  private UserDao userDao;
  private PermissionDao permissionDao;

  @Before
  public void setUp() {
    roleDao = new RoleDao(getMyBatis());
    userDao = new UserDao(getMyBatis());
    permissionDao = new PermissionDao(getMyBatis());
    permissionFacade = new ComponentPermissionFacade(getMyBatis(), roleDao, userDao, permissionDao);
  }

  @Test
  public void should_apply_permission_template() throws Exception {
    setupData("should_apply_permission_template");

    permissionFacade.applyPermissionTemplate("default_20130101_010203", 123L);

    checkTable("should_apply_permission_template", "group_roles", "group_id", "resource_id", "role");
    checkTable("should_apply_permission_template", "user_roles", "group_id", "resource_id", "role");
  }

  @Test
  public void should_set_user_permission() throws Exception {
    setupData("should_set_user_permission");

    permissionFacade.setUserPermission(123L, "dave.loper", UserRole.ADMIN);

    checkTable("should_set_user_permission", "user_roles", "user_id", "resource_id", "role");
  }

  @Test
  public void should_set_group_permission() throws Exception {
    setupData("should_set_group_permission");

    permissionFacade.setGroupPermission(123L, "devs", UserRole.ADMIN);

    checkTable("should_set_group_permission", "group_roles", "group_id", "resource_id", "role");
  }

  @Test
  public void should_count_component_permissions() throws Exception {
    setupData("should_count_component_permissions");

    assertThat(permissionFacade.countPermissions(123L)).isEqualTo(2);
  }

  @Test
  public void should_add_user_permission() throws Exception {
    setupData("should_add_user_permission");

    SqlSession session = getMyBatis().openSession();
    try {
      permissionFacade.addUserPermission(123L, "dave.loper", UserRole.ADMIN, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }

    checkTable("should_add_user_permission", "user_roles", "user_id", "resource_id", "role");
  }

  @Test
  public void should_add_group_permission() throws Exception {
    setupData("should_add_group_permission");

    SqlSession session = getMyBatis().openSession();
    try {
      permissionFacade.addGroupPermission(123L, "devs", UserRole.USER, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }

    checkTable("should_add_group_permission", "group_roles", "group_id", "resource_id", "role");
  }

  @Test
  public void should_retrieve_permission_template() throws Exception {
    PermissionTemplateDto permissionTemplateDto = new PermissionTemplateDto().setName("Test template");
    PermissionTemplateDto templateWithPermissions = new PermissionTemplateDto();
    permissionDao = mock(PermissionDao.class);
    when(permissionDao.selectTemplateByKey("test_template")).thenReturn(permissionTemplateDto);
    when(permissionDao.selectPermissionTemplate("Test template")).thenReturn(templateWithPermissions);

    permissionFacade = new ComponentPermissionFacade(null, null, null, permissionDao);

    PermissionTemplateDto permissionTemplate = permissionFacade.getPermissionTemplate("test_template");

    assertThat(permissionTemplate).isSameAs(templateWithPermissions);
  }

  @Test
  public void should_fail_on_unmatched_template() throws Exception {
    throwable.expect(IllegalArgumentException.class);

    permissionDao = mock(PermissionDao.class);

    permissionFacade = new ComponentPermissionFacade(null, null, null, permissionDao);
    permissionFacade.getPermissionTemplate("unmatched");
  }

  @Test
  public void should_remove_all_permissions() throws Exception {
    setupData("should_remove_all_permissions");

    SqlSession session = getMyBatis().openSession();
    try {
      permissionFacade.removeAllPermissions(123L, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }

    checkTable("should_remove_all_permissions", "group_roles", "group_id", "resource_id", "role");
    checkTable("should_remove_all_permissions", "user_roles", "user_id", "resource_id", "role");
  }
}
