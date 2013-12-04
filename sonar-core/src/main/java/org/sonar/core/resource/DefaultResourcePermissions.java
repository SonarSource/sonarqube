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
package org.sonar.core.resource;

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.resources.Resource;
import org.sonar.api.security.ResourcePermissions;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.user.UserDto;
import org.sonar.core.user.UserMapper;

/**
 * @since 3.2
 */
public class DefaultResourcePermissions implements ResourcePermissions {

  private final MyBatis myBatis;
  private final PermissionFacade permissionFacade;

  public DefaultResourcePermissions(MyBatis myBatis, PermissionFacade permissionFacade) {
    this.myBatis = myBatis;
    this.permissionFacade = permissionFacade;
  }

  public boolean hasRoles(Resource resource) {
    if (resource.getId() != null) {
      Long resourceId = Long.valueOf(resource.getId());
      return permissionFacade.countComponentPermissions(resourceId) > 0;
    }
    return false;
  }

  public void grantUserRole(Resource resource, String login, String role) {
    if (resource.getId() != null) {
      SqlSession session = myBatis.openSession();
      try {
        UserDto user = session.getMapper(UserMapper.class).selectUserByLogin(login);
        if (user != null) {
          permissionFacade.deleteUserPermission(Long.valueOf(resource.getId()), user.getId(), role, session);
          permissionFacade.insertUserPermission(Long.valueOf(resource.getId()), user.getId(), role, session);
          session.commit();
        }
      } finally {
        MyBatis.closeQuietly(session);
      }
    }
  }

  public void grantGroupRole(Resource resource, String groupName, String role) {
    if (resource.getId() != null) {
      SqlSession session = myBatis.openSession();
      try {
        permissionFacade.deleteGroupPermission(Long.valueOf(resource.getId()), groupName, role, session);
        permissionFacade.insertGroupPermission(Long.valueOf(resource.getId()), groupName, role, session);
        session.commit();
      } finally {
        MyBatis.closeQuietly(session);
      }
    }
  }

  public void grantDefaultRoles(Resource resource) {
    permissionFacade.grantDefaultRoles(Long.valueOf(resource.getId()), resource.getQualifier());
  }
}
