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

package org.sonar.core.permission;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.api.config.Settings;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.task.TaskComponent;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.GroupRoleDto;
import org.sonar.core.user.RoleDao;
import org.sonar.core.user.UserDao;
import org.sonar.core.user.UserRoleDto;

import javax.annotation.Nullable;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
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
  private final Settings settings;
  private final ResourceDao resourceDao;

  public PermissionFacade(MyBatis myBatis, RoleDao roleDao, UserDao userDao, ResourceDao resourceDao, PermissionTemplateDao permissionTemplateDao, Settings settings) {
    this.myBatis = myBatis;
    this.roleDao = roleDao;
    this.userDao = userDao;
    this.resourceDao = resourceDao;
    this.permissionTemplateDao = permissionTemplateDao;
    this.settings = settings;
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
      deleteGroupPermission(resourceId, (Long) null, permission, session);
    } else {
      GroupDto group = userDao.selectGroupByName(groupName, session);
      if (group != null) {
        deleteGroupPermission(resourceId, group.getId(), permission, session);
      }
    }
  }

  /**
   * Load permission template and load associated collections of users and groups permissions
   */
  @VisibleForTesting
  PermissionTemplateDto getPermissionTemplateWithPermissions(String templateKey) {
    PermissionTemplateDto permissionTemplateDto = permissionTemplateDao.selectTemplateByKey(templateKey);
    if (permissionTemplateDto == null) {
      throw new IllegalArgumentException("Could not retrieve permission template with key " + templateKey);
    }
    PermissionTemplateDto templateWithPermissions = permissionTemplateDao.selectPermissionTemplate(permissionTemplateDto.getKee());
    if (templateWithPermissions == null) {
      throw new IllegalArgumentException("Could not retrieve permissions for template with key " + templateKey);
    }
    return templateWithPermissions;
  }

  public void applyPermissionTemplate(String templateKey, Long resourceId) {
    PermissionTemplateDto permissionTemplate = getPermissionTemplateWithPermissions(templateKey);
    SqlSession session = myBatis.openSession();
    try {
      removeAllPermissions(resourceId, session);
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

  public int countComponentPermissions(Long resourceId) {
    return roleDao.countResourceGroupRoles(resourceId) + roleDao.countResourceUserRoles(resourceId);
  }

  public void removeAllPermissions(Long resourceId, SqlSession session) {
    roleDao.deleteGroupRolesByResourceId(resourceId, session);
    roleDao.deleteUserRolesByResourceId(resourceId, session);
  }

  public List<String> selectGroupPermissions(String group, Long componentId) {
    return roleDao.selectGroupPermissions(group, componentId);
  }

  public List<String> selectUserPermissions(String user, Long componentId) {
    return roleDao.selectUserPermissions(user, componentId);
  }

  public void grantDefaultRoles(Long componentId, String qualifier) {
    ResourceDto resource = resourceDao.getResource(componentId);
    if (resource == null) {
      throw new IllegalStateException("Unable to find resource with id " + componentId);
    }
    String applicablePermissionTemplateKey = getApplicablePermissionTemplateKey(resource.getKey(), qualifier);
    applyPermissionTemplate(applicablePermissionTemplateKey, componentId);
  }

  /**
   * Return the permission template for the given componentKey. If no template key pattern match then consider default
   * permission template for the resource qualifier.
   */
  private String getApplicablePermissionTemplateKey(final String componentKey, String qualifier) {
    List<PermissionTemplateDto> allPermissionTemplates = permissionTemplateDao.selectAllPermissionTemplates();
    List<PermissionTemplateDto> matchingTemplates = new ArrayList<PermissionTemplateDto>();
    for (PermissionTemplateDto permissionTemplateDto : allPermissionTemplates) {
      String keyPattern = permissionTemplateDto.getKeyPattern();
      if (StringUtils.isNotBlank(keyPattern) && componentKey.matches(keyPattern)) {
        matchingTemplates.add(permissionTemplateDto);
      }
    }
    checkAtMostOneMatchForComponentKey(componentKey, matchingTemplates);
    if (matchingTemplates.size() == 1) {
      return matchingTemplates.get(0).getKee();
    }
    String qualifierTemplateKey = settings.getString("sonar.permission.template." + qualifier + ".default");
    if (!StringUtils.isBlank(qualifierTemplateKey)) {
      return qualifierTemplateKey;
    }

    String defaultTemplateKey = settings.getString("sonar.permission.template.default");
    if (StringUtils.isBlank(defaultTemplateKey)) {
      throw new IllegalStateException("At least one default permission template should be defined");
    }
    return defaultTemplateKey;
  }

  private void checkAtMostOneMatchForComponentKey(final String componentKey, List<PermissionTemplateDto> matchingTemplates) {
    if (matchingTemplates.size() > 1) {
      StringBuilder templatesNames = new StringBuilder();
      for (Iterator<PermissionTemplateDto> it = matchingTemplates.iterator(); it.hasNext();) {
        templatesNames.append("\"").append(it.next().getName()).append("\"");
        if (it.hasNext()) {
          templatesNames.append(", ");
        }
      }
      throw new IllegalStateException(MessageFormat.format(
        "The \"{0}\" key matches multiple permission templates: {1}."
          + " A system administrator must update these templates so that only one of them matches the key.", componentKey,
        templatesNames.toString()));
    }
  }
}
