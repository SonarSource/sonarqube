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
import org.sonar.api.ServerExtension;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Resource;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.security.ResourcePermissions;
import org.sonar.api.task.TaskExtension;
import org.sonar.api.web.UserRole;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.user.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 3.2
 */
public class DefaultResourcePermissions implements ResourcePermissions, TaskExtension, ServerExtension {

  private final Settings settings;
  private final MyBatis myBatis;
  private final RoleDao roleDao;
  private final UserDao userDao;
  private final PermissionDao permissionDao;

  public DefaultResourcePermissions(Settings settings, MyBatis myBatis, RoleDao roleDao, UserDao userDao, PermissionDao permissionDao) {
    this.settings = settings;
    this.myBatis = myBatis;
    this.roleDao = roleDao;
    this.userDao = userDao;
    this.permissionDao = permissionDao;
  }

  public boolean hasRoles(Resource resource) {
    if (resource.getId() != null) {
      Long resourceId = Long.valueOf(resource.getId());
      return roleDao.countGroupRoles(resourceId) + roleDao.countUserRoles(resourceId) > 0;
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
          roleDao.deleteUserRole(userRole, session);
          roleDao.insertUserRole(userRole, session);
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
        if (DefaultGroups.isAnyone(groupName)) {
          roleDao.deleteGroupRole(groupRole, session);
          roleDao.insertGroupRole(groupRole, session);
          session.commit();
        } else {
          GroupDto group = userDao.selectGroupByName(groupName, session);
          if (group != null) {
            groupRole.setGroupId(group.getId());
            roleDao.deleteGroupRole(groupRole, session);
            roleDao.insertGroupRole(groupRole, session);
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
    roleDao.deleteGroupRolesByResourceId(resourceId, session);
    roleDao.deleteUserRolesByResourceId(resourceId, session);
  }

  private void grantDefaultRoles(Resource resource, String role, SqlSession session) {
    PermissionTemplateDto applicablePermissionTemplate = getPermissionTemplate(resource.getQualifier());

    List<String> groupNames = getEligibleGroups(role, applicablePermissionTemplate);
    for (String groupName : groupNames) {
      GroupRoleDto groupRole = new GroupRoleDto().setRole(role).setResourceId(Long.valueOf(resource.getId()));
      if (DefaultGroups.isAnyone(groupName)) {
        roleDao.insertGroupRole(groupRole, session);
      } else {
        GroupDto group = userDao.selectGroupByName(groupName, session);
        if (group != null) {
          roleDao.insertGroupRole(groupRole.setGroupId(group.getId()), session);
        }
      }
    }

    List<String> logins = getEligibleUsers(role, applicablePermissionTemplate);
    for (String login : logins) {
      UserDto user = userDao.selectActiveUserByLogin(login, session);
      if (user != null) {
        UserRoleDto userRoleDto = new UserRoleDto().setRole(role).setUserId(user.getId()).setResourceId(Long.valueOf(resource.getId()));
        roleDao.insertUserRole(userRoleDto, session);
      }
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
    String qualifierTemplateId = settings.getString("sonar.permission.template." + qualifier + ".default");
    if(!StringUtils.isBlank(qualifierTemplateId)) {
      return getTemplateWithPermissions(qualifierTemplateId);
    }

    String defaultTemplateId = settings.getString("sonar.permission.template.default");
    if(StringUtils.isBlank(defaultTemplateId)) {
      throw new IllegalStateException("At least one default permission template should be defined");
    }
    return getTemplateWithPermissions(defaultTemplateId);
  }

  private PermissionTemplateDto getTemplateWithPermissions(String templateId) {
    PermissionTemplateDto permissionTemplateDto = permissionDao.selectTemplateById(Long.parseLong(templateId));
    if(permissionTemplateDto == null) {
      throw new IllegalArgumentException("Could not retrieve permission template with id " + templateId);
    }
    PermissionTemplateDto templateWithPermissions = permissionDao.selectPermissionTemplate(permissionTemplateDto.getName());
    if(templateWithPermissions == null) {
      throw new IllegalArgumentException("Could not retrieve permissions for template with id " + templateId);
    }
    return templateWithPermissions;
  }
}
