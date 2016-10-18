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
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.permission.index.AuthorizationIndexer;
import org.sonar.server.user.UserSession;

import static java.util.Arrays.asList;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkProjectAdminUserByComponentKey;

@ServerSide
public class PermissionService {

  private final DbClient dbClient;
  private final PermissionRepository permissionRepository;
  private final AuthorizationIndexer authorizationIndexer;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public PermissionService(DbClient dbClient, PermissionRepository permissionRepository, AuthorizationIndexer authorizationIndexer, UserSession userSession,
    ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.permissionRepository = permissionRepository;
    this.authorizationIndexer = authorizationIndexer;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  /**
   * Used by Ruby on Rails
   */
  public List<String> globalPermissions() {
    return GlobalPermissions.ALL;
  }

  public void applyDefaultPermissionTemplate(String componentKey) {
    DbSession session = dbClient.openSession(false);
    try {
      applyDefaultPermissionTemplate(session, componentKey);
    } finally {
      session.close();
    }
  }

  public void applyDefaultPermissionTemplate(DbSession session, String componentKey) {
    ComponentDto component = componentFinder.getByKey(session, componentKey);
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
    indexProjectPermissions(asList(component.uuid()));
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
    indexProjectPermissions(projects.stream().map(ComponentDto::uuid).collect(Collectors.toList()));
  }

  private void indexProjectPermissions(List<String> projectUuids) {
    authorizationIndexer.index(projectUuids);
  }
}
