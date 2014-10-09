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
package org.sonar.core.resource;

import org.sonar.api.resources.Resource;
import org.sonar.api.security.ResourcePermissions;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.DbSession;
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

  @Override
  public boolean hasRoles(Resource resource) {
    DbSession session = myBatis.openSession(false);
    try {
      if (resource.getId() != null) {
        Long resourceId = Long.valueOf(resource.getId());
        return permissionFacade.countComponentPermissions(session, resourceId) > 0;
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
    return false;
  }

  @Override
  public void grantUserRole(Resource resource, String login, String role) {
    if (resource.getId() != null) {
      DbSession session = myBatis.openSession(false);
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

  @Override
  public void grantGroupRole(Resource resource, String groupName, String role) {
    if (resource.getId() != null) {
      DbSession session = myBatis.openSession(false);
      try {
        permissionFacade.deleteGroupPermission(Long.valueOf(resource.getId()), groupName, role, session);
        permissionFacade.insertGroupPermission(Long.valueOf(resource.getId()), groupName, role, session);
        session.commit();
      } finally {
        MyBatis.closeQuietly(session);
      }
    }
  }

  public void grantDefaultRoles(DbSession session, Resource resource) {
    permissionFacade.grantDefaultRoles(session, Long.valueOf(resource.getId()), resource.getQualifier());
  }

  @Override
  public void grantDefaultRoles(Resource resource) {
    DbSession session = myBatis.openSession(false);
    try {
      grantDefaultRoles(session, resource);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
