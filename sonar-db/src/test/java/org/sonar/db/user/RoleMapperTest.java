/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.user;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;


public class RoleMapperTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Test
  public void count_roles() {
    dbTester.prepareDbUnit(getClass(), "countRoles.xml");

    RoleMapper mapper = dbTester.getSession().getMapper(RoleMapper.class);
    assertThat(mapper.countResourceGroupRoles(123L)).isEqualTo(2);
    assertThat(mapper.countResourceUserRoles(123L)).isEqualTo(1);
  }

  @Test
  public void delete_roles_by_resource_id() {
    dbTester.prepareDbUnit(getClass(), "deleteRolesByResourceId.xml");

    RoleMapper mapper = dbTester.getSession().getMapper(RoleMapper.class);
    mapper.deleteGroupRolesByResourceId(123L);
    mapper.deleteUserRolesByResourceId(123L);
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "deleteRolesByResourceId-result.xml", "group_roles", "user_roles");
  }

  @Test
  public void insert_roles() {
    dbTester.prepareDbUnit(getClass(), "insertRoles.xml");

    RoleMapper mapper = dbTester.getSession().getMapper(RoleMapper.class);
    mapper.insertGroupRole(new GroupRoleDto().setRole("admin").setGroupId(100L).setResourceId(123L));
    mapper.insertGroupRole(new GroupRoleDto().setRole("user").setResourceId(123L));// Anyone
    mapper.insertUserRole(new UserRoleDto().setRole("codeviewer").setUserId(200L).setResourceId(123L));// Anyone
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "insertRoles-result.xml", "group_roles", "user_roles");
  }
}
