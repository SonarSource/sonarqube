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
package org.sonar.server.permission;

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
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ServerSide;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateGroupDto;
import org.sonar.db.permission.template.PermissionTemplateUserDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserId;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.es.ProjectIndexers;
import org.sonar.server.exceptions.TemplateMatchingKeyException;
import org.sonar.server.permission.DefaultTemplatesResolver.ResolvedDefaultTemplates;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.sonar.api.security.DefaultGroups.isAnyone;
import static org.sonar.api.web.UserRole.PUBLIC_PERMISSIONS;
import static org.sonar.db.permission.GlobalPermission.SCAN;

@ServerSide
public class PermissionTemplateService {

  private final DbClient dbClient;
  private final ProjectIndexers projectIndexers;
  private final UserSession userSession;
  private final DefaultTemplatesResolver defaultTemplatesResolver;
  private final UuidFactory uuidFactory;

  public PermissionTemplateService(DbClient dbClient, ProjectIndexers projectIndexers, UserSession userSession,
    DefaultTemplatesResolver defaultTemplatesResolver, UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.projectIndexers = projectIndexers;
    this.userSession = userSession;
    this.defaultTemplatesResolver = defaultTemplatesResolver;
    this.uuidFactory = uuidFactory;
  }

  public boolean wouldUserHaveScanPermissionWithDefaultTemplate(DbSession dbSession, @Nullable String userUuid, String projectKey) {
    if (userSession.hasPermission(SCAN)) {
      return true;
    }

    ComponentDto dto = new ComponentDto().setKey(projectKey).setQualifier(Qualifiers.PROJECT);
    PermissionTemplateDto template = findTemplate(dbSession, dto);
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
  public void applyAndCommit(DbSession dbSession, PermissionTemplateDto template, Collection<ComponentDto> projects) {
    if (projects.isEmpty()) {
      return;
    }

    for (ComponentDto project : projects) {
      dbClient.groupPermissionDao().deleteByRootComponentUuid(dbSession, project);
      dbClient.userPermissionDao().deleteProjectPermissions(dbSession, project);
      copyPermissions(dbSession, template, project, null);
    }
    projectIndexers.commitAndIndexComponents(dbSession, projects, ProjectIndexer.Cause.PERMISSION_CHANGE);
  }

  /**
   * Apply the default permission template to a new project (has no permissions yet).
   * @param projectCreatorUserId id of the user creating the project.
   */
  public void applyDefaultToNewComponent(DbSession dbSession, ComponentDto component, @Nullable String projectCreatorUserId) {
    PermissionTemplateDto template = findTemplate(dbSession, component);
    checkArgument(template != null, "Cannot retrieve default permission template");
    copyPermissions(dbSession, template, component, projectCreatorUserId);
  }

  public boolean hasDefaultTemplateWithPermissionOnProjectCreator(DbSession dbSession, ComponentDto component) {
    PermissionTemplateDto template = findTemplate(dbSession, component);
    return hasProjectCreatorPermission(dbSession, template);
  }

  private boolean hasProjectCreatorPermission(DbSession dbSession, @Nullable PermissionTemplateDto template) {
    return template != null && dbClient.permissionTemplateCharacteristicDao().selectByTemplateUuids(dbSession, singletonList(template.getUuid())).stream()
      .anyMatch(PermissionTemplateCharacteristicDto::getWithProjectCreator);
  }

  private void copyPermissions(DbSession dbSession, PermissionTemplateDto template, ComponentDto project, @Nullable String projectCreatorUserUuid) {
    List<PermissionTemplateUserDto> usersPermissions = dbClient.permissionTemplateDao().selectUserPermissionsByTemplateId(dbSession, template.getUuid());
    Set<String> permissionTemplateUserUuids = usersPermissions.stream().map(PermissionTemplateUserDto::getUserUuid).collect(Collectors.toSet());
    Map<String, UserId> userIdByUuid = dbClient.userDao().selectByUuids(dbSession, permissionTemplateUserUuids).stream().collect(Collectors.toMap(UserDto::getUuid, u -> u));
    usersPermissions
      .stream()
      .filter(up -> permissionValidForProject(project, up.getPermission()))
      .forEach(up -> {
        UserPermissionDto dto = new UserPermissionDto(uuidFactory.create(), up.getPermission(), up.getUserUuid(), project.uuid());
        dbClient.userPermissionDao().insert(dbSession, dto, project, userIdByUuid.get(up.getUserUuid()), template);
      });

    List<PermissionTemplateGroupDto> groupsPermissions = dbClient.permissionTemplateDao().selectGroupPermissionsByTemplateUuid(dbSession, template.getUuid());
    groupsPermissions
      .stream()
      .filter(gp -> groupNameValidForProject(project, gp.getGroupName()))
      .filter(gp -> permissionValidForProject(project, gp.getPermission()))
      .forEach(gp -> {
        String groupUuid = isAnyone(gp.getGroupName()) ? null : gp.getGroupUuid();
        String groupName = groupUuid == null ? null : dbClient.groupDao().selectByUuid(dbSession, groupUuid).getName();
        GroupPermissionDto dto = new GroupPermissionDto()
          .setUuid(uuidFactory.create())
          .setGroupUuid(groupUuid)
          .setGroupName(groupName)
          .setRole(gp.getPermission())
          .setComponentUuid(project.uuid())
          .setComponentName(project.name());
        dbClient.groupPermissionDao().insert(dbSession, dto, project, template);
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
        .filter(up -> permissionValidForProject(project, up.getPermission()))
        .filter(characteristic -> !permissionsForCurrentUserAlreadyInDb.contains(characteristic.getPermission()))
        .forEach(c -> {
          UserPermissionDto dto = new UserPermissionDto(uuidFactory.create(), c.getPermission(), userDto.getUuid(), project.uuid());
          dbClient.userPermissionDao().insert(dbSession, dto, project, userDto, template);
        });
    }
  }

  private static boolean permissionValidForProject(ComponentDto project, String permission) {
    return project.isPrivate() || !PUBLIC_PERMISSIONS.contains(permission);
  }

  private static boolean groupNameValidForProject(ComponentDto project, String groupName) {
    return !project.isPrivate() || !isAnyone(groupName);
  }

  /**
   * Return the permission template for the given component. If no template key pattern match then consider default
   * template for the component qualifier.
   */
  @CheckForNull
  private PermissionTemplateDto findTemplate(DbSession dbSession, ComponentDto component) {
    List<PermissionTemplateDto> allPermissionTemplates = dbClient.permissionTemplateDao().selectAll(dbSession, null);
    List<PermissionTemplateDto> matchingTemplates = new ArrayList<>();
    for (PermissionTemplateDto permissionTemplateDto : allPermissionTemplates) {
      String keyPattern = permissionTemplateDto.getKeyPattern();
      if (StringUtils.isNotBlank(keyPattern) && component.getKey().matches(keyPattern)) {
        matchingTemplates.add(permissionTemplateDto);
      }
    }
    checkAtMostOneMatchForComponentKey(component.getKey(), matchingTemplates);
    if (matchingTemplates.size() == 1) {
      return matchingTemplates.get(0);
    }

    String qualifier = component.qualifier();
    ResolvedDefaultTemplates resolvedDefaultTemplates = defaultTemplatesResolver.resolve(dbSession);
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
