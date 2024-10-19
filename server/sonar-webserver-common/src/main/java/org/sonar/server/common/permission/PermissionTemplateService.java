/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.common.permission;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ServerSide;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.db.permission.template.DefaultTemplates;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateGroupDto;
import org.sonar.db.permission.template.PermissionTemplateUserDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserId;
import org.sonar.server.es.Indexers;
import org.sonar.server.exceptions.TemplateMatchingKeyException;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.sonar.api.security.DefaultGroups.isAnyone;
import static org.sonar.api.web.UserRole.PUBLIC_PERMISSIONS;
import static org.sonar.db.permission.OrganizationPermission.SCAN;

@ServerSide
public class PermissionTemplateService {

  private final DbClient dbClient;
  private final Indexers indexers;
  private final UserSession userSession;
  private final DefaultTemplatesResolver defaultTemplatesResolver;
  private final UuidFactory uuidFactory;

  public PermissionTemplateService(DbClient dbClient, Indexers indexers, UserSession userSession,
    DefaultTemplatesResolver defaultTemplatesResolver, UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.indexers = indexers;
    this.userSession = userSession;
    this.defaultTemplatesResolver = defaultTemplatesResolver;
    this.uuidFactory = uuidFactory;
  }

  public boolean wouldUserHaveScanPermissionWithDefaultTemplate(DbSession dbSession, String organizationUuid, @Nullable String userUuid, String projectKey) {
    if (userSession.hasPermission(SCAN, organizationUuid)) {
      return true;
    }

    ProjectDto projectDto = new ProjectDto().setOrganizationUuid(organizationUuid).setKey(projectKey).setQualifier(Qualifiers.PROJECT);
    PermissionTemplateDto template = findTemplate(dbSession, projectDto);
    if (template == null) {
      return false;
    }

    List<String> potentialPermissions = dbClient.permissionTemplateDao().selectPotentialPermissionsByUserUuidAndTemplateUuid(dbSession, userUuid, template.getUuid());
    return potentialPermissions.contains(SCAN.getKey());
  }

  /**
   * Apply a permission template to a set of projects. Authorization to administrate these projects
   * is not verified. The projects must exist, so the "project creator" permissions defined in the
   * template are ignored.
   */
  public void applyAndCommit(DbSession dbSession, PermissionTemplateDto template, Collection<EntityDto> entities) {
    if (entities.isEmpty()) {
      return;
    }

    for (EntityDto entity : entities) {
      dbClient.groupPermissionDao().deleteByEntityUuid(dbSession, entity);
      dbClient.userPermissionDao().deleteEntityPermissions(dbSession, entity);
      copyPermissions(dbSession, template, entity, null);
    }
    indexers.commitAndIndexEntities(dbSession, entities, Indexers.EntityEvent.PERMISSION_CHANGE);
  }

  /**
   * Apply the default permission template to a new project (has no permissions yet).
   *
   * @param projectCreatorUserId id of the user creating the project.
   */
  public void applyDefaultToNewComponent(DbSession dbSession, EntityDto entityDto, @Nullable String projectCreatorUserId) {
    PermissionTemplateDto template = findTemplate(dbSession, entityDto);
    checkArgument(template != null, "Cannot retrieve default permission template");
    copyPermissions(dbSession, template, entityDto, projectCreatorUserId);
  }

  public boolean hasDefaultTemplateWithPermissionOnProjectCreator(DbSession dbSession, ProjectDto projectDto) {
    PermissionTemplateDto template = findTemplate(dbSession, projectDto);
    return hasProjectCreatorPermission(dbSession, template);
  }

  private boolean hasProjectCreatorPermission(DbSession dbSession, @Nullable PermissionTemplateDto template) {
    return template != null && dbClient.permissionTemplateCharacteristicDao().selectByTemplateUuids(dbSession, singletonList(template.getUuid())).stream()
      .anyMatch(PermissionTemplateCharacteristicDto::getWithProjectCreator);
  }

  private void copyPermissions(DbSession dbSession, PermissionTemplateDto template, EntityDto entity, @Nullable String projectCreatorUserUuid) {
    List<PermissionTemplateUserDto> usersPermissions = dbClient.permissionTemplateDao().selectUserPermissionsByTemplateId(dbSession, template.getUuid());
    String organizationUuid = template.getOrganizationUuid();
    Set<String> permissionTemplateUserUuids = usersPermissions.stream().map(PermissionTemplateUserDto::getUserUuid).collect(Collectors.toSet());
    Map<String, UserId> userIdByUuid = dbClient.userDao().selectByUuids(dbSession, permissionTemplateUserUuids).stream().collect(Collectors.toMap(UserDto::getUuid, u -> u));
    usersPermissions
      .stream()
      .filter(up -> permissionValidForProject(entity.isPrivate(), up.getPermission()))
      .forEach(up -> {
        UserPermissionDto dto = new UserPermissionDto(uuidFactory.create(), organizationUuid, up.getPermission(), up.getUserUuid(), entity.getUuid());
        dbClient.userPermissionDao().insert(dbSession, dto, entity, userIdByUuid.get(up.getUserUuid()), template);
      });

    List<PermissionTemplateGroupDto> groupsPermissions = dbClient.permissionTemplateDao().selectGroupPermissionsByTemplateUuid(dbSession, template.getUuid());
    groupsPermissions
      .stream()
      .filter(gp -> groupNameValidForProject(entity.isPrivate(), gp.getGroupName()))
      .filter(gp -> permissionValidForProject(entity.isPrivate(), gp.getPermission()))
      .forEach(gp -> {
        String groupUuid = isAnyone(gp.getGroupName()) ? null : gp.getGroupUuid();
        String groupName = groupUuid == null ? null : dbClient.groupDao().selectByUuid(dbSession, groupUuid).getName();
        GroupPermissionDto dto = new GroupPermissionDto()
          .setUuid(uuidFactory.create())
          .setOrganizationUuid(organizationUuid)
          .setGroupUuid(groupUuid)
          .setGroupName(groupName)
          .setRole(gp.getPermission())
          .setEntityUuid(entity.getUuid())
          .setEntityName(entity.getName());

        dbClient.groupPermissionDao().insert(dbSession, dto, entity, template);
      });

    List<PermissionTemplateCharacteristicDto> characteristics = dbClient.permissionTemplateCharacteristicDao().selectByTemplateUuids(dbSession, singletonList(template.getUuid()));
    if (projectCreatorUserUuid != null) {
      Set<String> permissionsForCurrentUserAlreadyInDb = usersPermissions.stream()
        .filter(userPermission -> projectCreatorUserUuid.equals(userPermission.getUserUuid()))
        .map(PermissionTemplateUserDto::getPermission)
        .collect(java.util.stream.Collectors.toSet());

      UserDto userDto = dbClient.userDao().selectByUuid(dbSession, projectCreatorUserUuid);
      characteristics.stream()
        .filter(PermissionTemplateCharacteristicDto::getWithProjectCreator)
        .filter(up -> permissionValidForProject(entity.isPrivate(), up.getPermission()))
        .filter(characteristic -> !permissionsForCurrentUserAlreadyInDb.contains(characteristic.getPermission()))
        .forEach(c -> {
          UserPermissionDto dto = new UserPermissionDto(uuidFactory.create(), organizationUuid, c.getPermission(), userDto.getUuid(), entity.getUuid());
          dbClient.userPermissionDao().insert(dbSession, dto, entity, userDto, template);
        });
    }
  }

  private static boolean permissionValidForProject(boolean isPrivateEntity, String permission) {
    return isPrivateEntity || !PUBLIC_PERMISSIONS.contains(permission);
  }

  private static boolean groupNameValidForProject(boolean isPrivateEntity, String groupName) {
    return !isPrivateEntity || !isAnyone(groupName);
  }

  /**
   * Return the permission template for the given component. If no template key pattern match then consider default
   * template for the component qualifier.
   */
  @CheckForNull
  private PermissionTemplateDto findTemplate(DbSession dbSession, EntityDto entityDto) {
    String organizationUuid = entityDto.getOrganizationUuid();
    List<PermissionTemplateDto> allPermissionTemplates = dbClient.permissionTemplateDao().selectAll(dbSession, organizationUuid, null);
    List<PermissionTemplateDto> matchingTemplates = new ArrayList<>();
    for (PermissionTemplateDto permissionTemplateDto : allPermissionTemplates) {
      String keyPattern = permissionTemplateDto.getKeyPattern();
      if (StringUtils.isNotBlank(keyPattern) && entityDto.getKey().matches(keyPattern)) {
        matchingTemplates.add(permissionTemplateDto);
      }
    }
    checkAtMostOneMatchForComponentKey(entityDto.getKey(), matchingTemplates);
    if (matchingTemplates.size() == 1) {
      return matchingTemplates.get(0);
    }

    DefaultTemplates defaultTemplates = dbClient.organizationDao().getDefaultTemplates(dbSession, organizationUuid)
        .orElseThrow(() -> new IllegalStateException(
            format("No Default templates defined for organization with uuid '%s'", organizationUuid)));

    String qualifier = entityDto.getQualifier();
    DefaultTemplatesResolver.ResolvedDefaultTemplates resolvedDefaultTemplates = defaultTemplatesResolver.resolve(dbSession, defaultTemplates);
    switch (qualifier) {
      case Qualifiers.PROJECT:
        return dbClient.permissionTemplateDao().selectByUuid(dbSession, resolvedDefaultTemplates.getProject());
      case Qualifiers.VIEW:
        String portDefaultTemplateUuid = resolvedDefaultTemplates.getPortfolio().orElseThrow(
          () -> new IllegalStateException("Failed to find default template for portfolios"));
        return dbClient.permissionTemplateDao().selectByUuid(dbSession, portDefaultTemplateUuid);
      case Qualifiers.APP:
        String appDefaultTemplateUuid = resolvedDefaultTemplates.getApplication().orElseThrow(
          () -> new IllegalStateException("Failed to find default template for applications"));
        return dbClient.permissionTemplateDao().selectByUuid(dbSession, appDefaultTemplateUuid);
      default:
        throw new IllegalArgumentException(format("Qualifier '%s' is not supported", qualifier));
    }
  }

  private static void checkAtMostOneMatchForComponentKey(String componentKey, List<PermissionTemplateDto> matchingTemplates) {
    if (matchingTemplates.size() > 1) {
      StringBuilder templatesNames = new StringBuilder();
      for (Iterator<PermissionTemplateDto> it = matchingTemplates.iterator(); it.hasNext(); ) {
        templatesNames.append("\"").append(it.next().getName()).append("\"");
        if (it.hasNext()) {
          templatesNames.append(", ");
        }
      }
      throw new TemplateMatchingKeyException(MessageFormat.format(
        "The \"{0}\" key matches multiple permission templates: {1}."
          + " A system administrator must update these templates so that only one of them matches the key.",
        componentKey,
        templatesNames.toString()));
    }
  }

}
