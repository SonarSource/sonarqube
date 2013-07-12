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

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.user.RoleDao;
import org.sonar.core.user.UserDao;

public class ComponentPermissionFacadeTest extends AbstractDaoTestCase {

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
}
