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

package org.sonar.core.user;

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerExtension;
import org.sonar.api.task.TaskExtension;
import org.sonar.core.persistence.MyBatis;

public class RoleDao implements TaskExtension, ServerExtension {

  private final MyBatis mybatis;

  public RoleDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public void insertGroupRole(GroupRoleDto groupRole, SqlSession session) {
    RoleMapper mapper = session.getMapper(RoleMapper.class);
    mapper.insertGroupRole(groupRole);
  }

  public void insertUserRole(UserRoleDto userRole, SqlSession session) {
    RoleMapper mapper = session.getMapper(RoleMapper.class);
    mapper.insertUserRole(userRole);
  }

  public void deleteUserRole(UserRoleDto userRole, SqlSession session) {
    RoleMapper mapper = session.getMapper(RoleMapper.class);
    mapper.deleteUserRole(userRole);
  }

  public void deleteGroupRole(GroupRoleDto groupRole, SqlSession session) {
    RoleMapper mapper = session.getMapper(RoleMapper.class);
    mapper.deleteGroupRole(groupRole);
  }

  public void deleteGroupRolesByResourceId(Long resourceId, SqlSession session) {
    RoleMapper mapper = session.getMapper(RoleMapper.class);
    mapper.deleteGroupRolesByResourceId(resourceId);
  }

  public void deleteUserRolesByResourceId(Long resourceId, SqlSession session) {
    RoleMapper mapper = session.getMapper(RoleMapper.class);
    mapper.deleteUserRolesByResourceId(resourceId);
  }

  public int countGroupRoles(Long resourceId) {
    SqlSession session = mybatis.openSession();
    try {
      RoleMapper mapper = session.getMapper(RoleMapper.class);
      return mapper.countGroupRoles(resourceId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public int countUserRoles(Long resourceId) {
    SqlSession session = mybatis.openSession();
    try {
      RoleMapper mapper = session.getMapper(RoleMapper.class);
      return mapper.countUserRoles(resourceId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
