/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.resource;

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerExtension;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Resource;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.security.ResourcePermissions;
import org.sonar.api.task.TaskExtension;
import org.sonar.api.web.UserRole;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.GroupRoleDto;
import org.sonar.core.user.RoleMapper;
import org.sonar.core.user.UserDto;
import org.sonar.core.user.UserMapper;
import org.sonar.core.user.UserRoleDto;

/**
 * @since 3.2
 */
public class DefaultResourcePermissions implements ResourcePermissions, TaskExtension, ServerExtension {

  private final Settings settings;
  private final MyBatis myBatis;

  public DefaultResourcePermissions(Settings settings, MyBatis myBatis) {
    this.settings = settings;
    this.myBatis = myBatis;
  }

  public boolean hasRoles(Resource resource) {
    if (resource.getId() != null) {
      SqlSession session = myBatis.openSession();
      try {
        RoleMapper roleMapper = session.getMapper(RoleMapper.class);
        Long resourceId = Long.valueOf(resource.getId());
        return roleMapper.countGroupRoles(resourceId) + roleMapper.countUserRoles(resourceId) > 0;

      } finally {
        MyBatis.closeQuietly(session);
      }
    }
    return false;
  }

  public void grantUserRole(Resource resource, String login, String role) {
    if (resource.getId() != null) {
      SqlSession session = myBatis.openSession();
      try {
        UserDto user = session.getMapper(UserMapper.class).selectUserByLogin(login);
        if (user != null) {
          UserRoleDto userRole = new UserRoleDto()
              .setRole(role)
              .setUserId(user.getId())
              .setResourceId(Long.valueOf(resource.getId()));
          RoleMapper roleMapper = session.getMapper(RoleMapper.class);
          roleMapper.deleteUserRole(userRole);
          roleMapper.insertUserRole(userRole);
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
        GroupRoleDto groupRole = new GroupRoleDto()
            .setRole(role)
            .setResourceId(Long.valueOf(resource.getId()));
        RoleMapper roleMapper = session.getMapper(RoleMapper.class);
        if (DefaultGroups.isAnyone(groupName)) {
          roleMapper.deleteGroupRole(groupRole);
          roleMapper.insertGroupRole(groupRole);
          session.commit();
        } else {
          GroupDto group = session.getMapper(UserMapper.class).selectGroupByName(groupName);
          if (group != null) {
            groupRole.setGroupId(group.getId());
            roleMapper.deleteGroupRole(groupRole);
            roleMapper.insertGroupRole(groupRole);
            session.commit();
          }
        }
      } finally {
        MyBatis.closeQuietly(session);
      }
    }
  }

  public void grantDefaultRoles(Resource resource) {
    if (resource.getId() != null) {
      SqlSession session = myBatis.openSession();
      try {
        removeRoles(resource, session);
        grantDefaultRoles(resource, UserRole.ADMIN, session);
        grantDefaultRoles(resource, UserRole.USER, session);
        grantDefaultRoles(resource, UserRole.CODEVIEWER, session);
        session.commit();
      } finally {
        MyBatis.closeQuietly(session);
      }
    }
  }

  private void removeRoles(Resource resource, SqlSession session) {
    Long resourceId = Long.valueOf(resource.getId());
    RoleMapper mapper = session.getMapper(RoleMapper.class);
    mapper.deleteGroupRolesByResourceId(resourceId);
    mapper.deleteUserRolesByResourceId(resourceId);
  }

  private void grantDefaultRoles(Resource resource, String role, SqlSession session) {
    UserMapper userMapper = session.getMapper(UserMapper.class);
    RoleMapper roleMapper = session.getMapper(RoleMapper.class);

    String[] groupNames = settings.getStringArrayBySeparator("sonar.role." + role + "." + resource.getQualifier() + ".defaultGroups", ",");
    for (String groupName : groupNames) {
      GroupRoleDto groupRole = new GroupRoleDto().setRole(role).setResourceId(Long.valueOf(resource.getId()));
      if (DefaultGroups.isAnyone(groupName)) {
        roleMapper.insertGroupRole(groupRole);
      } else {
        GroupDto group = userMapper.selectGroupByName(groupName);
        if (group != null) {
          roleMapper.insertGroupRole(groupRole.setGroupId(group.getId()));
        }
      }
    }

    String[] logins = settings.getStringArrayBySeparator("sonar.role." + role + "." + resource.getQualifier() + ".defaultUsers", ",");
    for (String login : logins) {
      UserDto user = userMapper.selectUserByLogin(login);
      if (user != null) {
        roleMapper.insertUserRole(new UserRoleDto().setRole(role).setUserId(user.getId()).setResourceId(Long.valueOf(resource.getId())));
      }
    }
  }
}
