/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
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
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Settings;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.template.PermissionTemplate;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateGroupDto;
import org.sonar.db.permission.template.PermissionTemplateUserDto;

import static org.sonar.api.security.DefaultGroups.isAnyone;

/**
 * This facade wraps db operations related to permissions
 * <p/>
 * Should be removed when batch will no more create permission, and be replaced by a new PermissionService in module server (probably be a merge with InternalPermissionService)
 * <p/>
 * WARNING, this class is called by Deveveloper Cockpit to apply default permission template on new developers
 */
public class PermissionRepository {

  private final DbClient dbClient;
  private final Settings settings;

  public PermissionRepository(DbClient dbClient, Settings settings) {
    this.dbClient = dbClient;
    this.settings = settings;
  }

  private void insertGroupPermission(@Nullable Long resourceId, @Nullable Long groupId, String permission, DbSession session) {
    GroupPermissionDto dto = new GroupPermissionDto()
      .setRole(permission)
      .setGroupId(groupId)
      .setResourceId(resourceId);
    dbClient.groupPermissionDao().insert(session, dto);
  }

  public void deleteGroupPermission(@Nullable Long resourceId, @Nullable Long groupId, String permission, DbSession session) {
    GroupPermissionDto groupRole = new GroupPermissionDto()
      .setRole(permission)
      .setGroupId(groupId)
      .setResourceId(resourceId);
    updateProjectAuthorizationDate(session, resourceId);
    dbClient.roleDao().deleteGroupRole(groupRole, session);
  }

  /**
   * For each modification of permission on a project, update the authorization_updated_at to help ES reindex only relevant changes
   */
  private void updateProjectAuthorizationDate(DbSession session, @Nullable Long projectId) {
    if (projectId != null) {
      dbClient.resourceDao().updateAuthorizationDate(projectId, session);
    }
  }

  public void applyPermissionTemplate(DbSession session, String templateUuid, ComponentDto project) {
    applyPermissionTemplate(session, templateUuid, project, null);
  }

  private void applyPermissionTemplate(DbSession session, String templateUuid, ComponentDto project, @Nullable Long currentUserId) {
    PermissionTemplate template = dbClient.permissionTemplateDao().selectPermissionTemplateWithPermissions(session, templateUuid);
    updateProjectAuthorizationDate(session, project.getId());
    dbClient.roleDao().deleteGroupRolesByResourceId(session, project.getId());
    dbClient.userPermissionDao().delete(session, null, project.uuid(), null);

    List<PermissionTemplateUserDto> usersPermissions = template.getUserPermissions();
    String organizationUuid = template.getTemplate().getOrganizationUuid();
    usersPermissions
      .forEach(up -> {
        UserPermissionDto dto = new UserPermissionDto(organizationUuid, up.getPermission(), up.getUserId(), project.getId());
        dbClient.userPermissionDao().insert(session, dto);
      });

    List<PermissionTemplateGroupDto> groupsPermissions = template.getGroupPermissions();
    groupsPermissions.forEach(groupPermission -> insertGroupPermission(project.getId(), isAnyone(groupPermission.getGroupName()) ? null : groupPermission.getGroupId(),
      groupPermission.getPermission(), session));

    List<PermissionTemplateCharacteristicDto> characteristics = template.getCharacteristics();
    if (currentUserId != null) {
      Set<String> permissionsForCurrentUserAlreadyInDb = usersPermissions.stream()
        .filter(userPermission -> currentUserId.equals(userPermission.getUserId()))
        .map(PermissionTemplateUserDto::getPermission)
        .collect(Collectors.toSet());
      characteristics.stream()
        .filter(PermissionTemplateCharacteristicDto::getWithProjectCreator)
        .filter(characteristic -> !permissionsForCurrentUserAlreadyInDb.contains(characteristic.getPermission()))
        .forEach(c -> {
          UserPermissionDto dto = new UserPermissionDto(template.getTemplate().getOrganizationUuid(), c.getPermission(), currentUserId, project.getId());
          dbClient.userPermissionDao().insert(session, dto);
        });
    }
  }

  /**
   * Warning, this method is also used by the Developer Cockpit plugin
   */
  public void applyDefaultPermissionTemplate(DbSession session, long componentId) {
    ComponentDto component = dbClient.componentDao().selectOrFailById(session, componentId);
    applyDefaultPermissionTemplate(session, component, null);
  }

  public void applyDefaultPermissionTemplate(DbSession dbSession, ComponentDto componentDto, @Nullable Long userId) {
    String applicablePermissionTemplateKey = getApplicablePermissionTemplateKey(dbSession, componentDto.getKey(), componentDto.qualifier());
    applyPermissionTemplate(dbSession, applicablePermissionTemplateKey, componentDto, userId);
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

  public boolean wouldUserHavePermissionWithDefaultTemplate(DbSession dbSession, @Nullable Long currentUserId, String permission, String projectKey, String qualifier) {
    String templateUuid = getApplicablePermissionTemplateKey(dbSession, projectKey, qualifier);
    PermissionTemplateDto template = dbClient.permissionTemplateDao().selectByUuid(dbSession, templateUuid);
    if (template == null) {
      return false;
    }

    List<String> potentialPermissions = dbClient.permissionTemplateDao().selectPotentialPermissionsByUserIdAndTemplateId(dbSession, currentUserId, template.getId());

    return potentialPermissions.contains(permission);
  }

  private static void checkAtMostOneMatchForComponentKey(final String componentKey, List<PermissionTemplateDto> matchingTemplates) {
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
          + " A system administrator must update these templates so that only one of them matches the key.",
        componentKey,
        templatesNames.toString()));
    }
  }
}
