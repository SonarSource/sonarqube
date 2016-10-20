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

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ServerSide;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceDto;
import org.sonar.db.permission.PermissionRepository;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.user.UserSession;

import static java.util.Arrays.asList;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkProjectAdminUserByComponentKey;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;

@ServerSide
public class PermissionService {

  private final DbClient dbClient;
  private final PermissionRepository permissionRepository;
  private final PermissionIndexer permissionIndexer;
  private final UserSession userSession;

  public PermissionService(DbClient dbClient, PermissionRepository permissionRepository, PermissionIndexer permissionIndexer, UserSession userSession) {
    this.dbClient = dbClient;
    this.permissionRepository = permissionRepository;
    this.permissionIndexer = permissionIndexer;
    this.userSession = userSession;
  }

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
    permissionRepository.applyDefaultPermissionTemplate(session, component, userId);
    session.commit();
    indexProjectPermissions(session, asList(component.uuid()));
  }

  public boolean wouldCurrentUserHavePermissionWithDefaultTemplate(DbSession dbSession, String permission, @Nullable String branch, String projectKey, String qualifier) {
    if (userSession.hasPermission(permission)) {
      return true;
    }

    String effectiveKey = ComponentKeys.createKey(projectKey, branch);

    Long userId = userSession.getUserId() == null ? null : userSession.getUserId().longValue();
    return permissionRepository.wouldUserHavePermissionWithDefaultTemplate(dbSession, userId, permission, effectiveKey, qualifier);
  }

  public void apply(DbSession dbSession, PermissionTemplateDto template, Collection<ComponentDto> projects) {
    if (projects.isEmpty()) {
      return;
    }

    for (ComponentDto project : projects) {
      permissionRepository.apply(dbSession, template, project, null);
    }
    dbSession.commit();
    indexProjectPermissions(dbSession, projects.stream().map(ComponentDto::uuid).collect(Collectors.toList()));
  }

  /**
   * Apply the default permission template to component, whatever it already exists (and has permissions) or if it's
   * provisioned (and has no permissions yet).
   *
   * @param dbSession
   * @param component
   * @param projectCreatorUserId id of the user who creates the project, only if project is provisioned. He will
   *                             benefit from the permissions defined in the template for "project creator".
   */
  public void applyDefault(DbSession dbSession, ComponentDto component, @Nullable Long projectCreatorUserId) {
    permissionRepository.applyDefaultPermissionTemplate(dbSession, component, projectCreatorUserId);
    dbSession.commit();
    indexProjectPermissions(dbSession, asList(component.uuid()));
  }

  private void indexProjectPermissions(DbSession dbSession, List<String> projectUuids) {
    permissionIndexer.index(dbSession, projectUuids);
  }
}
