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

package org.sonar.db.permission;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Settings;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ResourceDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupRoleDto;
import org.sonar.db.user.UserRoleDto;

/**
 * This facade wraps db operations related to permissions
 * <p/>
 * Should be removed when batch will no more create permission, and be replaced by a new PermissionService in module server (probably be a merge with InternalPermissionService)
 * <p/>
 * WARNING, this class is called by Views to apply default permission template on new views
 */
@ServerSide
public class PermissionRepository {

  private final DbClient dbClient;
  private final Settings settings;

  public PermissionRepository(DbClient dbClient, Settings settings) {
    this.dbClient = dbClient;
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
      updateProjectAuthorizationDate(session, resourceId);
    }
    dbClient.roleDao().insertUserRole(session, userRoleDto);
  }

  public void insertUserPermission(@Nullable Long resourceId, Long userId, String permission, DbSession session) {
    insertUserPermission(resourceId, userId, permission, true, session);
  }

  public void deleteUserPermission(@Nullable Long resourceId, Long userId, String permission, DbSession session) {
    UserRoleDto userRoleDto = new UserRoleDto()
      .setRole(permission)
      .setUserId(userId)
      .setResourceId(resourceId);
    updateProjectAuthorizationDate(session, resourceId);
    dbClient.roleDao().deleteUserRole(userRoleDto, session);
  }

  private void insertGroupPermission(@Nullable Long resourceId, @Nullable Long groupId, String permission, boolean updateProjectAuthorizationDate, DbSession session) {
    GroupRoleDto groupRole = new GroupRoleDto()
      .setRole(permission)
      .setGroupId(groupId)
      .setResourceId(resourceId);
    updateProjectAuthorizationDate(session, resourceId);
    dbClient.roleDao().insertGroupRole(session, groupRole);
  }

  public void insertGroupPermission(@Nullable Long resourceId, @Nullable Long groupId, String permission, DbSession session) {
    insertGroupPermission(resourceId, groupId, permission, true, session);
  }

  public void insertGroupPermission(@Nullable Long resourceId, String groupName, String permission, DbSession session) {
    if (DefaultGroups.isAnyone(groupName)) {
      insertGroupPermission(resourceId, (Long) null, permission, session);
    } else {
      GroupDto group = dbClient.groupDao().selectByName(session, groupName);
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
    updateProjectAuthorizationDate(session, resourceId);
    dbClient.roleDao().deleteGroupRole(groupRole, session);
  }

  public void deleteGroupPermission(@Nullable Long resourceId, String groupName, String permission, DbSession session) {
    if (DefaultGroups.isAnyone(groupName)) {
      deleteGroupPermission(resourceId, (Long) null, permission, session);
    } else {
      GroupDto group = dbClient.groupDao().selectByName(session, groupName);
      if (group != null) {
        deleteGroupPermission(resourceId, group.getId(), permission, session);
      }
    }
  }

  /**
   * For each modification of permission on a project, update the authorization_updated_at to help ES reindex only relevant changes
   */
  private void updateProjectAuthorizationDate(DbSession session, @Nullable Long projectId) {
    if (projectId != null) {
      dbClient.resourceDao().updateAuthorizationDate(projectId, session);
    }
  }

  public void applyPermissionTemplate(DbSession session, String templateUuid, long resourceId) {
    PermissionTemplateDto permissionTemplate = dbClient.permissionTemplateDao().selectPermissionTemplateWithPermissions(session, templateUuid);
    updateProjectAuthorizationDate(session, resourceId);
    dbClient.roleDao().removeAllPermissions(session, resourceId);
    List<PermissionTemplateUserDto> usersPermissions = permissionTemplate.getUsersPermissions();
    //TODO should return an empty list if there's no user permissions
    if (usersPermissions != null) {
      for (PermissionTemplateUserDto userPermission : usersPermissions) {
        insertUserPermission(resourceId, userPermission.getUserId(), userPermission.getPermission(), false, session);
      }
    }
    List<PermissionTemplateGroupDto> groupsPermissions = permissionTemplate.getGroupsPermissions();
    //TODO should return an empty list if there's no group permission
    if (groupsPermissions != null) {
      for (PermissionTemplateGroupDto groupPermission : groupsPermissions) {
        Long groupId = groupPermission.getGroupId() == null ? null : groupPermission.getGroupId();
        insertGroupPermission(resourceId, groupId, groupPermission.getPermission(), false, session);
      }
    }
  }

  public void grantDefaultRoles(DbSession session, long componentId, String qualifier) {
    ResourceDto resource = dbClient.resourceDao().selectResource(componentId, session);
    String applicablePermissionTemplateKey = getApplicablePermissionTemplateKey(session, resource.getKey(), qualifier);
    applyPermissionTemplate(session, applicablePermissionTemplateKey, componentId);
  }

  /**
   * Return the permission template for the given componentKey. If no template key pattern match then consider default
   * permission template for the resource qualifier.
   */
  private String getApplicablePermissionTemplateKey(DbSession session, final String componentKey, String qualifier) {
    List<PermissionTemplateDto> allPermissionTemplates = dbClient.permissionTemplateDao().selectAll(session);
    List<PermissionTemplateDto> matchingTemplates = new ArrayList<>();
    for (PermissionTemplateDto permissionTemplateDto : allPermissionTemplates) {
      String keyPattern = permissionTemplateDto.getKeyPattern();
      if (StringUtils.isNotBlank(keyPattern) && componentKey.matches(keyPattern)) {
        matchingTemplates.add(permissionTemplateDto);
      }
    }
    checkAtMostOneMatchForComponentKey(componentKey, matchingTemplates);
    if (matchingTemplates.size() == 1) {
      return matchingTemplates.get(0).getUuid();
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
