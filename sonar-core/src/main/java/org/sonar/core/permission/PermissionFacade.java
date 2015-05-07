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
import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;
import org.sonar.api.config.Settings;
import org.sonar.api.security.DefaultGroups;
import org.sonar.core.persistence.DbSession;
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
 * This facade wraps db operations related to permissions
 *
 * Should be removed when batch will no more create permission, and be replaced by a new PermissionService in module server (probably be a merge with InternalPermissionService)
 *
 * WARNING, this class is called by Views to apply default permission template on new views
 */
@BatchSide
@ServerSide
public class PermissionFacade {

  private final RoleDao roleDao;
  private final UserDao userDao;
  private final PermissionTemplateDao permissionTemplateDao;
  private final Settings settings;
  private final ResourceDao resourceDao;

  public PermissionFacade(RoleDao roleDao, UserDao userDao, ResourceDao resourceDao, PermissionTemplateDao permissionTemplateDao, Settings settings) {
    this.roleDao = roleDao;
    this.userDao = userDao;
    this.resourceDao = resourceDao;
    this.permissionTemplateDao = permissionTemplateDao;
    this.settings = settings;
  }

  /**
   * @param updateProjectAuthorizationDate is false when doing bulk action in order to not update the same project multiple times for nothing
   */
  private void insertUserPermission(@Nullable Long resourceId, Long userId, String permission, boolean updateProjectAuthorizationDate, DbSession session) {
    UserRoleDto userRoleDto = new UserRoleDto()
      .setRole(permission)
      .setUserId(userId)
      .setResourceId(resourceId);
    if (updateProjectAuthorizationDate) {
      updateProjectAuthorizationDate(resourceId, session);
    }
    roleDao.insertUserRole(userRoleDto, session);
  }

  public void insertUserPermission(@Nullable Long resourceId, Long userId, String permission, DbSession session) {
    insertUserPermission(resourceId, userId, permission, true, session);
  }

  public void deleteUserPermission(@Nullable Long resourceId, Long userId, String permission, DbSession session) {
    UserRoleDto userRoleDto = new UserRoleDto()
      .setRole(permission)
      .setUserId(userId)
      .setResourceId(resourceId);
    updateProjectAuthorizationDate(resourceId, session);
    roleDao.deleteUserRole(userRoleDto, session);
  }

  private void insertGroupPermission(@Nullable Long resourceId, @Nullable Long groupId, String permission, boolean updateProjectAuthorizationDate, DbSession session) {
    GroupRoleDto groupRole = new GroupRoleDto()
      .setRole(permission)
      .setGroupId(groupId)
      .setResourceId(resourceId);
    updateProjectAuthorizationDate(resourceId, session);
    roleDao.insertGroupRole(groupRole, session);
  }

  public void insertGroupPermission(@Nullable Long resourceId, @Nullable Long groupId, String permission, DbSession session) {
    insertGroupPermission(resourceId, groupId, permission, true, session);
  }

  public void insertGroupPermission(@Nullable Long resourceId, String groupName, String permission, DbSession session) {
    if (DefaultGroups.isAnyone(groupName)) {
      insertGroupPermission(resourceId, (Long) null, permission, session);
    } else {
      GroupDto group = userDao.selectGroupByName(groupName, session);
      if (group != null) {
        insertGroupPermission(resourceId, group.getId(), permission, session);
      }
    }
  }

  public void deleteGroupPermission(@Nullable Long resourceId, @Nullable Long groupId, String permission, DbSession session) {
    GroupRoleDto groupRole = new GroupRoleDto()
      .setRole(permission)
      .setGroupId(groupId)
      .setResourceId(resourceId);
    updateProjectAuthorizationDate(resourceId, session);
    roleDao.deleteGroupRole(groupRole, session);
  }

  public void deleteGroupPermission(@Nullable Long resourceId, String groupName, String permission, DbSession session) {
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
   * For each modification of permission on a project, update the authorization_updated_at to help ES reindex only relevant changes
   */
  private void updateProjectAuthorizationDate(@Nullable Long projectId, DbSession session) {
    if (projectId != null) {
      resourceDao.updateAuthorizationDate(projectId, session);
    }
  }

  /**
   * Load permission template and load associated collections of users and groups permissions
   */
  @VisibleForTesting
  PermissionTemplateDto getPermissionTemplateWithPermissions(DbSession session, String templateKey) {
    PermissionTemplateDto permissionTemplateDto = permissionTemplateDao.selectTemplateByKey(session, templateKey);
    if (permissionTemplateDto == null) {
      throw new IllegalArgumentException("Could not retrieve permission template with key " + templateKey);
    }
    PermissionTemplateDto templateWithPermissions = permissionTemplateDao.selectPermissionTemplate(session, permissionTemplateDto.getKee());
    if (templateWithPermissions == null) {
      throw new IllegalArgumentException("Could not retrieve permissions for template with key " + templateKey);
    }
    return templateWithPermissions;
  }

  public void applyPermissionTemplate(DbSession session, String templateKey, Long resourceId) {
    PermissionTemplateDto permissionTemplate = getPermissionTemplateWithPermissions(session, templateKey);
    updateProjectAuthorizationDate(resourceId, session);
    removeAllPermissions(resourceId, session);
    List<PermissionTemplateUserDto> usersPermissions = permissionTemplate.getUsersPermissions();
    if (usersPermissions != null) {
      for (PermissionTemplateUserDto userPermission : usersPermissions) {
        insertUserPermission(resourceId, userPermission.getUserId(), userPermission.getPermission(), false, session);
      }
    }
    List<PermissionTemplateGroupDto> groupsPermissions = permissionTemplate.getGroupsPermissions();
    if (groupsPermissions != null) {
      for (PermissionTemplateGroupDto groupPermission : groupsPermissions) {
        Long groupId = groupPermission.getGroupId() == null ? null : groupPermission.getGroupId();
        insertGroupPermission(resourceId, groupId, groupPermission.getPermission(), false, session);
      }
    }
  }

  public int countComponentPermissions(DbSession session, Long resourceId) {
    return roleDao.countResourceGroupRoles(session, resourceId) + roleDao.countResourceUserRoles(session, resourceId);
  }

  protected void removeAllPermissions(Long resourceId, DbSession session) {
    roleDao.deleteGroupRolesByResourceId(resourceId, session);
    roleDao.deleteUserRolesByResourceId(resourceId, session);
  }

  public List<String> selectGroupPermissions(DbSession session, String group, @Nullable Long componentId) {
    return roleDao.selectGroupPermissions(session, group, componentId);
  }

  public List<String> selectUserPermissions(DbSession session, String user, @Nullable Long componentId) {
    return roleDao.selectUserPermissions(session, user, componentId);
  }

  public void grantDefaultRoles(DbSession session, Long componentId, String qualifier) {
    ResourceDto resource = resourceDao.getResource(componentId, session);
    if (resource == null) {
      throw new IllegalStateException("Unable to find resource with id " + componentId);
    }
    String applicablePermissionTemplateKey = getApplicablePermissionTemplateKey(session, resource.getKey(), qualifier);
    applyPermissionTemplate(session, applicablePermissionTemplateKey, componentId);
  }

  /**
   * Return the permission template for the given componentKey. If no template key pattern match then consider default
   * permission template for the resource qualifier.
   */
  private String getApplicablePermissionTemplateKey(DbSession session, final String componentKey, String qualifier) {
    List<PermissionTemplateDto> allPermissionTemplates = permissionTemplateDao.selectAllPermissionTemplates(session);
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
