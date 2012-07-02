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

import org.apache.ibatis.session.SqlSession;
import org.sonar.core.persistence.MyBatis;

import java.util.Collection;

/**
 * @since 3.2
 */
public class RoleDao {
  private final MyBatis mybatis;

  public RoleDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public RoleDao insertGroupRoles(Collection<GroupRoleDto> groupRoles) {
    SqlSession session = mybatis.openBatchSession();
    try {
      RoleMapper mapper = session.getMapper(RoleMapper.class);
      for (GroupRoleDto groupRole : groupRoles) {
        mapper.insertGroupRole(groupRole);
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
    return this;
  }

  public RoleDao insertUserRoles(Collection<UserRoleDto> userRoles) {
    SqlSession session = mybatis.openBatchSession();
    try {
      RoleMapper mapper = session.getMapper(RoleMapper.class);
      for (UserRoleDto userRole : userRoles) {
        mapper.insertUserRole(userRole);
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
    return this;
  }
}
