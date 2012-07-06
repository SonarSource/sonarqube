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
package org.sonar.plugins.core.security;

import com.google.common.annotations.VisibleForTesting;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchExtension;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.security.ResourcePermissioning;
import org.sonar.api.web.UserRole;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.user.*;

/**
 * @since 3.2
 */
@Properties({
  @Property(key = "sonar.role." + UserRole.ADMIN + ".TRK.defaultGroups",
    name = "Default groups for project administrators",
    defaultValue = DefaultGroups.ADMINISTRATORS,
    global = false,
    project = false),
  @Property(key = "sonar.role." + UserRole.USER + ".TRK.defaultGroups",
    name = "Default groups for project users",
    defaultValue = DefaultGroups.USERS + "," + DefaultGroups.ANYONE,
    global = false,
    project = false),
  @Property(key = "sonar.role." + UserRole.CODEVIEWER + ".TRK.defaultGroups",
    name = "Default groups for project code viewers",
    defaultValue = DefaultGroups.USERS + "," + DefaultGroups.ANYONE,
    global = false,
    project = false)
})
public class DefaultResourcePermissioning implements ResourcePermissioning, BatchExtension {

  private final Settings settings;
  private final MyBatis myBatis;

  public DefaultResourcePermissioning(Settings settings, MyBatis myBatis) {
    this.settings = settings;
    this.myBatis = myBatis;
  }

  public boolean hasRoles(Resource resource) {
    if (resource.getId() != null) {
      SqlSession session = myBatis.openSession();
      try {
        RoleMapper roleMapper = session.getMapper(RoleMapper.class);
        Long resourceId = new Long(resource.getId());
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
            .setResourceId(new Long(resource.getId()));
          session.getMapper(RoleMapper.class).insertUserRole(userRole);
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
          .setResourceId(new Long(resource.getId()));
        if (DefaultGroups.isAnyone(groupName)) {
          session.getMapper(RoleMapper.class).insertGroupRole(groupRole);
          session.commit();
        } else {
          GroupDto group = session.getMapper(UserMapper.class).selectGroupByName(groupName);
          if (group != null) {
            session.getMapper(RoleMapper.class).insertGroupRole(groupRole.setGroupId(group.getId()));
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
    Long resourceId = new Long(resource.getId());
    RoleMapper mapper = session.getMapper(RoleMapper.class);
    mapper.deleteGroupRolesByResourceId(resourceId);
    mapper.deleteUserRolesByResourceId(resourceId);
  }

  private void grantDefaultRoles(Resource resource, String role, SqlSession session) {
    UserMapper userMapper = session.getMapper(UserMapper.class);
    RoleMapper roleMapper = session.getMapper(RoleMapper.class);

    String strategy = getStrategy(resource);
    String[] groupNames = settings.getStringArrayBySeparator("sonar.role." + role + "." + strategy + ".defaultGroups", ",");
    for (String groupName : groupNames) {
      GroupRoleDto groupRole = new GroupRoleDto().setRole(role).setResourceId(new Long(resource.getId()));
      if (DefaultGroups.isAnyone(groupName)) {
        roleMapper.insertGroupRole(groupRole);
      } else {
        GroupDto group = userMapper.selectGroupByName(groupName);
        if (group != null) {
          roleMapper.insertGroupRole(groupRole.setGroupId(group.getId()));
        }
      }
    }

    String[] logins = settings.getStringArrayBySeparator("sonar.role." + role + "." + strategy + ".defaultUsers", ",");
    for (String login : logins) {
      UserDto user = userMapper.selectUserByLogin(login);
      if (user != null) {
        roleMapper.insertUserRole(new UserRoleDto().setRole(role).setUserId(user.getId()).setResourceId(new Long(resource.getId())));
      }
    }
  }

  /**
   * This is workaround to support old versions of the Views plugin.
   * If the Views plugin does not define default permissions, then the standard permissions are re-used for new views.
   */
  @VisibleForTesting
  String getStrategy(Resource resource) {
    String qualifier = resource.getQualifier();
    String result;
    if (Qualifiers.PROJECT.equals(qualifier)) {
      result = qualifier;

    } else if (hasRoleSettings(UserRole.ADMIN, qualifier) || hasRoleSettings(UserRole.USER, qualifier) || hasRoleSettings(UserRole.CODEVIEWER, qualifier)) {
      result = qualifier;
    } else {
      result = Qualifiers.PROJECT;
    }
    return result;
  }

  private boolean hasRoleSettings(String role, String qualifier) {
    return settings.getString("sonar.role." + role + "." + qualifier + ".defaultGroups") != null
      || settings.getString("sonar.role." + role + "." + qualifier + ".defaultUsers") != null;
  }
}
