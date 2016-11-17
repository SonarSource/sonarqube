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
package org.sonar.server.permission;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ServerSide;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateGroupDto;
import org.sonar.db.permission.template.PermissionTemplateUserDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.sonar.api.security.DefaultGroups.isAnyone;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkProjectAdminUserByComponentKey;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;

@ServerSide
public class PermissionTemplateService {

  private final DbClient dbClient;
  private final Settings settings;
  private final PermissionIndexer permissionIndexer;
  private final UserSession userSession;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public PermissionTemplateService(DbClient dbClient, Settings settings, PermissionIndexer permissionIndexer, UserSession userSession,
    DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.settings = settings;
    this.permissionIndexer = permissionIndexer;
    this.userSession = userSession;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  /**
   * @deprecated replaced by {@link #applyDefault(DbSession, ComponentDto, Long)}, which <b>does not
   * verify that user is authorized to administrate the component</b>.
   */
  @Deprecated
  public void applyDefaultPermissionTemplate(String componentKey) {
    DbSession session = dbClient.openSession(false);
    try {
      applyDefaultPermissionTemplate(session, componentKey);
    } finally {
      session.close();
    }
  }

  /**
   * @deprecated replaced by {@link #applyDefault(DbSession, ComponentDto, Long)}, which <b>does not
   * verify that user is authorized to administrate the component</b>.
   */
  @Deprecated
  public void applyDefaultPermissionTemplate(DbSession session, String componentKey) {
    ComponentDto component = checkFoundWithOptional(dbClient.componentDao().selectByKey(session, componentKey), "Component key '%s' not found", componentKey);
    ResourceDto provisioned = dbClient.resourceDao().selectProvisionedProject(session, componentKey);
    if (provisioned == null) {
      checkProjectAdminUserByComponentKey(userSession, componentKey);
    } else {
      userSession.checkPermission(GlobalPermissions.PROVISIONING);
    }

    Integer currentUserId = userSession.getUserId();
    Long userId = Qualifiers.PROJECT.equals(component.qualifier()) && currentUserId != null ? currentUserId.longValue() : null;
    applyDefault(session, component, userId);
  }

  public boolean wouldUserHavePermissionWithDefaultTemplate(DbSession dbSession, @Nullable Long userId, String permission, @Nullable String branch, String projectKey,
    String qualifier) {
    if (userSession.hasPermission(permission)) {
      return true;
    }

    String effectiveKey = ComponentKeys.createKey(projectKey, branch);
    PermissionTemplateDto template = findDefaultTemplate(dbSession, new ComponentDto().setKey(effectiveKey).setQualifier(qualifier));
    if (template == null) {
      return false;
    }

    List<String> potentialPermissions = dbClient.permissionTemplateDao().selectPotentialPermissionsByUserIdAndTemplateId(dbSession, userId, template.getId());
    return potentialPermissions.contains(permission);
  }

  /**
   * Apply a permission template to a set of projects. Authorization to administrate these projects
   * is not verified. The projects must exist, so the "project creator" permissions defined in the
   * template are ignored.
   */
  public void apply(DbSession dbSession, PermissionTemplateDto template, Collection<ComponentDto> projects) {
    if (projects.isEmpty()) {
      return;
    }

    for (ComponentDto project : projects) {
      copyPermissions(dbSession, template, project, null);
    }
    dbSession.commit();
    indexProjectPermissions(dbSession, projects.stream().map(ComponentDto::uuid).collect(Collectors.toList()));
  }

  /**
   * Apply the default permission template to project. The project can already exist (so it has permissions) or
   * can be provisioned (so has no permissions yet).
   *
   * @param dbSession
   * @param component
   * @param projectCreatorUserId id of the user who creates the project, only if project is provisioned. He will
   *                             benefit from the permissions defined in the template for "project creator".
   */
  public void applyDefault(DbSession dbSession, ComponentDto component, @Nullable Long projectCreatorUserId) {
    PermissionTemplateDto template = findDefaultTemplate(dbSession, component);
    checkArgument(template != null, "Cannot retrieve default permission template");
    copyPermissions(dbSession, template, component, projectCreatorUserId);
    dbSession.commit();
    indexProjectPermissions(dbSession, asList(component.uuid()));
  }

  public boolean hasDefaultTemplateWithPermissionOnProjectCreator(DbSession dbSession, ComponentDto component) {
    PermissionTemplateDto template = findDefaultTemplate(dbSession, component);
    return hasProjectCreatorPermission(dbSession, template);
  }

  private boolean hasProjectCreatorPermission(DbSession dbSession, @Nullable PermissionTemplateDto template) {
    return template != null && dbClient.permissionTemplateCharacteristicDao().selectByTemplateIds(dbSession, singletonList(template.getId())).stream()
      .anyMatch(PermissionTemplateCharacteristicDto::getWithProjectCreator);
  }

  private void indexProjectPermissions(DbSession dbSession, List<String> projectUuids) {
    permissionIndexer.index(dbSession, projectUuids);
  }

  private void copyPermissions(DbSession dbSession, PermissionTemplateDto template, ComponentDto project, @Nullable Long projectCreatorUserId) {
    dbClient.resourceDao().updateAuthorizationDate(project.getId(), dbSession);
    dbClient.groupPermissionDao().deleteByRootComponentId(dbSession, project.getId());
    dbClient.userPermissionDao().deleteProjectPermissions(dbSession, project.getId());

    List<PermissionTemplateUserDto> usersPermissions = dbClient.permissionTemplateDao().selectUserPermissionsByTemplateId(dbSession, template.getId());
    String organizationUuid = template.getOrganizationUuid();
    usersPermissions
      .forEach(up -> {
        UserPermissionDto dto = new UserPermissionDto(organizationUuid, up.getPermission(), up.getUserId(), project.getId());
        dbClient.userPermissionDao().insert(dbSession, dto);
      });

    List<PermissionTemplateGroupDto> groupsPermissions = dbClient.permissionTemplateDao().selectGroupPermissionsByTemplateId(dbSession, template.getId());
    groupsPermissions.forEach(gp -> {
      GroupPermissionDto dto = new GroupPermissionDto()
        .setOrganizationUuid(organizationUuid)
        .setGroupId(isAnyone(gp.getGroupName()) ? null : gp.getGroupId())
        .setRole(gp.getPermission())
        .setResourceId(project.getId());
      dbClient.groupPermissionDao().insert(dbSession, dto);
    });

    List<PermissionTemplateCharacteristicDto> characteristics = dbClient.permissionTemplateCharacteristicDao().selectByTemplateIds(dbSession, asList(template.getId()));
    if (projectCreatorUserId != null) {
      Set<String> permissionsForCurrentUserAlreadyInDb = usersPermissions.stream()
        .filter(userPermission -> projectCreatorUserId.equals(userPermission.getUserId()))
        .map(PermissionTemplateUserDto::getPermission)
        .collect(java.util.stream.Collectors.toSet());
      characteristics.stream()
        .filter(PermissionTemplateCharacteristicDto::getWithProjectCreator)
        .filter(characteristic -> !permissionsForCurrentUserAlreadyInDb.contains(characteristic.getPermission()))
        .forEach(c -> {
          UserPermissionDto dto = new UserPermissionDto(organizationUuid, c.getPermission(), projectCreatorUserId, project.getId());
          dbClient.userPermissionDao().insert(dbSession, dto);
        });
    }
  }

  /**
   * Return the permission template for the given component. If no template key pattern match then consider default
   * template for the component qualifier.
   */
  @CheckForNull
  private PermissionTemplateDto findDefaultTemplate(DbSession dbSession, ComponentDto component) {
    List<PermissionTemplateDto> allPermissionTemplates = dbClient.permissionTemplateDao().selectAll(dbSession, defaultOrganizationProvider.get().getUuid(), null);
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
    String qualifierTemplateKey = settings.getString("sonar.permission.template." + component.qualifier() + ".default");
    if (!StringUtils.isBlank(qualifierTemplateKey)) {
      return dbClient.permissionTemplateDao().selectByUuid(dbSession, qualifierTemplateKey);
    }

    String defaultTemplateKey = settings.getString("sonar.permission.template.default");
    if (StringUtils.isBlank(defaultTemplateKey)) {
      throw new IllegalStateException("At least one default permission template should be defined");
    }
    return dbClient.permissionTemplateDao().selectByUuid(dbSession, defaultTemplateKey);
  }

  private static void checkAtMostOneMatchForComponentKey(String componentKey, List<PermissionTemplateDto> matchingTemplates) {
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
