/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.user;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.DaoTestCase;

import java.util.Arrays;
import java.util.Collection;


public class RoleDaoTest extends DaoTestCase {

  private RoleDao dao;

  @Before
  public void setUp() {
    dao = new RoleDao(getMyBatis());
  }

  @Test
  public void insertGroupRoles() {
    setupData("insertGroupRoles");

    Collection<GroupRoleDto> groupRoles = Arrays.asList(
      new GroupRoleDto().setGroupId(100L).setResourceId(200L).setRole("admin"),

      // no group id => Anyone
      new GroupRoleDto().setResourceId(200L).setRole("user")
    );
    dao.insertGroupRoles(groupRoles);

    checkTables("insertGroupRoles", "group_roles");
  }

  @Test
  public void insertUserRoles() {
    setupData("insertUserRoles");

    Collection<UserRoleDto> userRoles = Arrays.asList(
      new UserRoleDto().setUserId(100L).setResourceId(200L).setRole("admin"),
      new UserRoleDto().setUserId(101L).setResourceId(200L).setRole("user")
    );
    dao.insertUserRoles(userRoles);

    checkTables("insertUserRoles", "user_roles");
  }
}
