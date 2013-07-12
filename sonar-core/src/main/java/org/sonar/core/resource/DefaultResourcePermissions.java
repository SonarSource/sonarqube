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
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.security.ResourcePermissions;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.ComponentPermissionFacade;
import org.sonar.core.permission.PermissionTemplateDto;
import org.sonar.core.permission.PermissionTemplateGroupDto;
import org.sonar.core.permission.PermissionTemplateUserDto;
import org.sonar.core.persistence.MyBatis;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 3.2
 */
public class DefaultResourcePermissions implements ResourcePermissions {

  private final Settings settings;
  private final MyBatis myBatis;
  private final ComponentPermissionFacade permissionFacade;

  public DefaultResourcePermissions(Settings settings, MyBatis myBatis, ComponentPermissionFacade permissionFacade) {
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
      permissionFacade.setUserPermission(Long.valueOf(resource.getId()), login, role);
    }
  }

  public void grantGroupRole(Resource resource, String groupName, String role) {
    if (resource.getId() != null) {
      permissionFacade.setGroupPermission(Long.valueOf(resource.getId()), groupName, role);
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

    List<String> groupNames = getEligibleGroups(role, applicablePermissionTemplate);
    for (String groupName : groupNames) {
      Long resourceId = Long.valueOf(resource.getId());
      permissionFacade.addGroupPermission(resourceId, groupName, role, session);
    }

    List<String> logins = getEligibleUsers(role, applicablePermissionTemplate);
    for (String login : logins) {
      Long resourceId = Long.valueOf(resource.getId());
      permissionFacade.addUserPermission(resourceId, login, role, session);
    }
  }

  private List<String> getEligibleGroups(String role, PermissionTemplateDto permissionTemplate) {
    List<String> eligibleGroups = new ArrayList<String>();
    List<PermissionTemplateGroupDto> groupsPermissions = permissionTemplate.getGroupsPermissions();
    if(groupsPermissions != null) {
      for (PermissionTemplateGroupDto groupPermission : groupsPermissions) {
        if(role.equals(groupPermission.getPermission())) {
          String groupName = groupPermission.getGroupName() != null ? groupPermission.getGroupName() : DefaultGroups.ANYONE;
          eligibleGroups.add(groupName);
        }
      }
    }
    return eligibleGroups;
  }

  private List<String> getEligibleUsers(String role, PermissionTemplateDto permissionTemplate) {
    List<String> eligibleUsers = new ArrayList<String>();
    List<PermissionTemplateUserDto> usersPermissions = permissionTemplate.getUsersPermissions();
    if(usersPermissions != null) {
      for (PermissionTemplateUserDto userPermission : usersPermissions) {
        if(role.equals(userPermission.getPermission())) {
          eligibleUsers.add(userPermission.getUserLogin());
        }
      }
    }
    return eligibleUsers;
  }

  private PermissionTemplateDto getPermissionTemplate(String qualifier) {
    String qualifierTemplateKey = settings.getString("sonar.permission.template." + qualifier + ".default");
    if(!StringUtils.isBlank(qualifierTemplateKey)) {
      return permissionFacade.getPermissionTemplate(qualifierTemplateKey);
    }

    String defaultTemplateKey = settings.getString("sonar.permission.template.default");
    if(StringUtils.isBlank(defaultTemplateKey)) {
      throw new IllegalStateException("At least one default permission template should be defined");
    }
    return permissionFacade.getPermissionTemplate(defaultTemplateKey);
  }
}
