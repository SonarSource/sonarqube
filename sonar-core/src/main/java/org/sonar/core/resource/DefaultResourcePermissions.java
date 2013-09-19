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

import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Resource;
import org.sonar.api.security.ResourcePermissions;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.permission.PermissionTemplateDto;
import org.sonar.core.permission.PermissionTemplateGroupDto;
import org.sonar.core.permission.PermissionTemplateUserDto;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.user.UserDto;
import org.sonar.core.user.UserMapper;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.2
 */
public class DefaultResourcePermissions implements ResourcePermissions {

  private final Settings settings;
  private final MyBatis myBatis;
  private final PermissionFacade permissionFacade;

  public DefaultResourcePermissions(Settings settings, MyBatis myBatis, PermissionFacade permissionFacade) {
    this.settings = settings;
    this.myBatis = myBatis;
    this.permissionFacade = permissionFacade;
  }

  public boolean hasRoles(Resource resource) {
    if (resource.getId() != null) {
      Long resourceId = Long.valueOf(resource.getId());
      return permissionFacade.countPermissions(resourceId) > 0;
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
    permissionFacade.removeAllPermissions(resourceId, session);
  }

  private void grantDefaultRoles(Resource resource, String role, SqlSession session) {
    PermissionTemplateDto applicablePermissionTemplate = getPermissionTemplate(resource.getQualifier());

    List<Long> groupIds = getEligibleGroups(role, applicablePermissionTemplate);
    for (Long groupId : groupIds) {
      Long resourceId = Long.valueOf(resource.getId());
      permissionFacade.insertGroupPermission(resourceId, groupId, role, session);
    }

    List<Long> userIds = getEligibleUsers(role, applicablePermissionTemplate);
    for (Long userId : userIds) {
      Long resourceId = Long.valueOf(resource.getId());
      permissionFacade.insertUserPermission(resourceId, userId, role, session);
    }
  }

  private List<Long> getEligibleGroups(String role, PermissionTemplateDto permissionTemplate) {
    List<Long> eligibleGroups = newArrayList();
    List<PermissionTemplateGroupDto> groupsPermissions = permissionTemplate.getGroupsPermissions();
    if (groupsPermissions != null) {
      for (PermissionTemplateGroupDto groupPermission : groupsPermissions) {
        if (role.equals(groupPermission.getPermission())) {
          Long groupId = groupPermission.getGroupId() != null ? groupPermission.getGroupId() : null;
          eligibleGroups.add(groupId);
        }
      }
    }
    return eligibleGroups;
  }

  private List<Long> getEligibleUsers(String role, PermissionTemplateDto permissionTemplate) {
    List<Long> eligibleUsers = newArrayList();
    List<PermissionTemplateUserDto> usersPermissions = permissionTemplate.getUsersPermissions();
    if (usersPermissions != null) {
      for (PermissionTemplateUserDto userPermission : usersPermissions) {
        if (role.equals(userPermission.getPermission())) {
          eligibleUsers.add(userPermission.getUserId());
        }
      }
    }
    return eligibleUsers;
  }

  private PermissionTemplateDto getPermissionTemplate(String qualifier) {
    String qualifierTemplateKey = settings.getString("sonar.permission.template." + qualifier + ".default");
    if (!StringUtils.isBlank(qualifierTemplateKey)) {
      return permissionFacade.getPermissionTemplate(qualifierTemplateKey);
    }

    String defaultTemplateKey = settings.getString("sonar.permission.template.default");
    if (StringUtils.isBlank(defaultTemplateKey)) {
      throw new IllegalStateException("At least one default permission template should be defined");
    }
    return permissionFacade.getPermissionTemplate(defaultTemplateKey);
  }
}
