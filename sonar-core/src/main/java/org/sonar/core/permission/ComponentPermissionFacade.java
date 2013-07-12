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

import java.util.List;

/**
 * Internal use only
 * @since 3.7
 *
 * This facade wraps all the db operations related to component-based permissions
 */
public class ComponentPermissionFacade implements TaskComponent, ServerComponent {

  private final MyBatis myBatis;
  private final RoleDao roleDao;
  private final UserDao userDao;
  private final PermissionDao permissionDao;

  public ComponentPermissionFacade(MyBatis myBatis, RoleDao roleDao, UserDao userDao, PermissionDao permissionDao) {
    this.myBatis = myBatis;
    this.roleDao = roleDao;
    this.userDao = userDao;
    this.permissionDao = permissionDao;
  }

  public void setUserPermission(Long resourceId, String userLogin, String permission) {
    SqlSession session = myBatis.openSession();
    try {
      UserDto user = session.getMapper(UserMapper.class).selectUserByLogin(userLogin);
      if (user != null) {
        UserRoleDto userRole = new UserRoleDto()
          .setRole(permission)
          .setUserId(user.getId())
          .setResourceId(Long.valueOf(resourceId));
        roleDao.deleteUserRole(userRole, session);
        roleDao.insertUserRole(userRole, session);
        session.commit();
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void setGroupPermission(Long resourceId, String groupName, String permission) {
    SqlSession session = myBatis.openSession();
    try {
      GroupRoleDto groupRole = new GroupRoleDto()
        .setRole(permission)
        .setResourceId(Long.valueOf(resourceId));
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

  public int countPermissions(Long resourceId) {
    return roleDao.countGroupRoles(resourceId) + roleDao.countUserRoles(resourceId);
  }

  public void removeAllPermissions(Long resourceId, SqlSession session) {
    roleDao.deleteGroupRolesByResourceId(resourceId, session);
    roleDao.deleteUserRolesByResourceId(resourceId, session);
  }

  public void addUserPermission(Long resourceId, String userLogin, String permission, SqlSession session) {
    UserDto user = userDao.selectActiveUserByLogin(userLogin, session);
    if (user != null) {
      UserRoleDto userRoleDto = new UserRoleDto().setRole(permission).setUserId(user.getId()).setResourceId(resourceId);
      roleDao.insertUserRole(userRoleDto, session);
    }
  }

  public void addGroupPermission(Long resourceId, String groupName, String permission, SqlSession session) {
    GroupRoleDto groupRole = new GroupRoleDto().setRole(permission).setResourceId(resourceId);
    if (DefaultGroups.isAnyone(groupName)) {
      roleDao.insertGroupRole(groupRole, session);
    } else {
      GroupDto group = userDao.selectGroupByName(groupName, session);
      if (group != null) {
        roleDao.insertGroupRole(groupRole.setGroupId(group.getId()), session);
      }
    }
  }

  public PermissionTemplateDto getPermissionTemplate(String templateKey) {
    PermissionTemplateDto permissionTemplateDto = permissionDao.selectTemplateByKey(templateKey);
    if(permissionTemplateDto == null) {
      throw new IllegalArgumentException("Could not retrieve permission template with key " + templateKey);
    }
    PermissionTemplateDto templateWithPermissions = permissionDao.selectPermissionTemplate(permissionTemplateDto.getName());
    if(templateWithPermissions == null) {
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
      if(usersPermissions != null) {
        for (PermissionTemplateUserDto userPermission : usersPermissions) {
          addUserPermission(resourceId, userPermission.getUserLogin(), userPermission.getPermission(), session);

        }
      }
      List<PermissionTemplateGroupDto> groupsPermissions = permissionTemplate.getGroupsPermissions();
      if(groupsPermissions != null) {
        for (PermissionTemplateGroupDto groupPermission : groupsPermissions) {
          String groupName = groupPermission.getGroupName() == null ? DefaultGroups.ANYONE : groupPermission.getGroupName();
          addGroupPermission(resourceId, groupName, groupPermission.getPermission(), session);
        }
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
