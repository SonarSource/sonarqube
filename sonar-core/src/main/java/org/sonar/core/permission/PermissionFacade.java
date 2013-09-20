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

package org.sonar.core.permission;

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.task.TaskComponent;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.user.*;

import javax.annotation.Nullable;

import java.util.List;

/**
 * Internal use only
 *
 * @since 3.7
 *        <p/>
 *        This facade wraps db operations related to permissions
 */
public class PermissionFacade implements TaskComponent, ServerComponent {

  private final MyBatis myBatis;
  private final RoleDao roleDao;
  private final UserDao userDao;
  private final PermissionTemplateDao permissionTemplateDao;

  public PermissionFacade(MyBatis myBatis, RoleDao roleDao, UserDao userDao, PermissionTemplateDao permissionTemplateDao) {
    this.myBatis = myBatis;
    this.roleDao = roleDao;
    this.userDao = userDao;
    this.permissionTemplateDao = permissionTemplateDao;
  }

  public void insertUserPermission(@Nullable Long resourceId, Long userId, String permission, @Nullable SqlSession session) {
    UserRoleDto userRoleDto = new UserRoleDto()
      .setRole(permission)
      .setUserId(userId)
      .setResourceId(resourceId);
    if (session != null) {
      roleDao.insertUserRole(userRoleDto, session);
    } else {
      roleDao.insertUserRole(userRoleDto);
    }
  }

  public void insertUserPermission(@Nullable Long resourceId, Long userId, String permission) {
    insertUserPermission(resourceId, userId, permission, null);
  }

  public void deleteUserPermission(@Nullable Long resourceId, Long userId, String permission, @Nullable SqlSession session) {
    UserRoleDto userRoleDto = new UserRoleDto()
      .setRole(permission)
      .setUserId(userId)
      .setResourceId(resourceId);
    if (session != null) {
      roleDao.deleteUserRole(userRoleDto, session);
    } else {
      roleDao.deleteUserRole(userRoleDto);
    }
  }

  public void deleteUserPermission(@Nullable Long resourceId, Long userId, String permission) {
    deleteUserPermission(resourceId, userId, permission, null);
  }

  public void insertGroupPermission(@Nullable Long resourceId, @Nullable Long groupId, String permission, @Nullable SqlSession session) {
    GroupRoleDto groupRole = new GroupRoleDto()
      .setRole(permission)
      .setGroupId(groupId)
      .setResourceId(resourceId);
    if (session != null) {
      roleDao.insertGroupRole(groupRole, session);
    } else {
      roleDao.insertGroupRole(groupRole);
    }
  }

  public void insertGroupPermission(@Nullable Long resourceId, @Nullable Long groupId, String permission) {
    insertGroupPermission(resourceId, groupId, permission, null);
  }

  public void insertGroupPermission(@Nullable Long resourceId, String groupName, String permission, @Nullable SqlSession session) {
    if (DefaultGroups.isAnyone(groupName)) {
      insertGroupPermission(resourceId, (Long) null, permission, session);
    } else {
      GroupDto group = userDao.selectGroupByName(groupName, session);
      if (group != null) {
        insertGroupPermission(resourceId, group.getId(), permission, session);
      }
    }
  }

  public void deleteGroupPermission(@Nullable Long resourceId, @Nullable Long groupId, String permission, @Nullable SqlSession session) {
    GroupRoleDto groupRole = new GroupRoleDto()
      .setRole(permission)
      .setGroupId(groupId)
      .setResourceId(resourceId);
    if (session != null) {
      roleDao.deleteGroupRole(groupRole, session);
    } else {
      roleDao.deleteGroupRole(groupRole);
    }
  }

  public void deleteGroupPermission(@Nullable Long resourceId, @Nullable Long groupId, String permission) {
    deleteGroupPermission(resourceId, groupId, permission, null);
  }

  public void deleteGroupPermission(@Nullable Long resourceId, String groupName, String permission, @Nullable SqlSession session) {
    if (DefaultGroups.isAnyone(groupName)) {
      deleteGroupPermission(resourceId,  (Long) null, permission, session);
    } else {
      GroupDto group = userDao.selectGroupByName(groupName, session);
      if (group != null) {
        deleteGroupPermission(resourceId, group.getId(), permission, session);
      }
    }
  }

  public PermissionTemplateDto getPermissionTemplate(String templateKey) {
    PermissionTemplateDto permissionTemplateDto = permissionTemplateDao.selectTemplateByKey(templateKey);
    if (permissionTemplateDto == null) {
      throw new IllegalArgumentException("Could not retrieve permission template with key " + templateKey);
    }
    PermissionTemplateDto templateWithPermissions = permissionTemplateDao.selectPermissionTemplate(permissionTemplateDto.getName());
    if (templateWithPermissions == null) {
      throw new IllegalArgumentException("Could not retrieve permissions for template with key " + templateKey);
    }
    return templateWithPermissions;
  }

  public void applyPermissionTemplate(String templateKey, Long resourceId) {
    SqlSession session = myBatis.openSession();
    try {
      removeAllPermissions(resourceId, session);
      PermissionTemplateDto permissionTemplate = getPermissionTemplate(templateKey);
      List<PermissionTemplateUserDto> usersPermissions = permissionTemplate.getUsersPermissions();
      if (usersPermissions != null) {
        for (PermissionTemplateUserDto userPermission : usersPermissions) {
          insertUserPermission(resourceId, userPermission.getUserId(), userPermission.getPermission(), session);
        }
      }
      List<PermissionTemplateGroupDto> groupsPermissions = permissionTemplate.getGroupsPermissions();
      if (groupsPermissions != null) {
        for (PermissionTemplateGroupDto groupPermission : groupsPermissions) {
          Long groupId = groupPermission.getGroupId() == null ? null : groupPermission.getGroupId();
          insertGroupPermission(resourceId, groupId, groupPermission.getPermission(), session);
        }
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public int countPermissions(Long resourceId) {
    return roleDao.countGroupRoles(resourceId) + roleDao.countUserRoles(resourceId);
  }

  public void removeAllPermissions(Long resourceId, SqlSession session) {
    roleDao.deleteGroupRolesByResourceId(resourceId, session);
    roleDao.deleteUserRolesByResourceId(resourceId, session);
  }
}
