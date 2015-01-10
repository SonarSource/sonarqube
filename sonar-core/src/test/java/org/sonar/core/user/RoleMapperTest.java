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

import org.apache.ibatis.session.SqlSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.MyBatis;

import static org.assertj.core.api.Assertions.assertThat;

public class RoleMapperTest extends AbstractDaoTestCase {

  private SqlSession session;

  @Before
  public void openSession() {
    session = getMyBatis().openSession();
  }

  @After
  public void closeSession() {
    MyBatis.closeQuietly(session);
  }

  @Test
  public void count_roles() {
    setupData("countRoles");

    RoleMapper mapper = session.getMapper(RoleMapper.class);
    assertThat(mapper.countResourceGroupRoles(123L)).isEqualTo(2);
    assertThat(mapper.countResourceUserRoles(123L)).isEqualTo(1);
  }

  @Test
  public void delete_roles_by_resource_id() {
    setupData("deleteRolesByResourceId");

    RoleMapper mapper = session.getMapper(RoleMapper.class);
    mapper.deleteGroupRolesByResourceId(123L);
    mapper.deleteUserRolesByResourceId(123L);
    session.commit();


    checkTables("deleteRolesByResourceId", "group_roles", "user_roles");
  }

  @Test
  public void insert_roles() {
    setupData("insertRoles");

    RoleMapper mapper = session.getMapper(RoleMapper.class);
    mapper.insertGroupRole(new GroupRoleDto().setRole("admin").setGroupId(100L).setResourceId(123L));
    mapper.insertGroupRole(new GroupRoleDto().setRole("user").setResourceId(123L));// Anyone
    mapper.insertUserRole(new UserRoleDto().setRole("codeviewer").setUserId(200L).setResourceId(123L));// Anyone
    session.commit();

    checkTables("insertRoles", "group_roles", "user_roles");
  }
}
