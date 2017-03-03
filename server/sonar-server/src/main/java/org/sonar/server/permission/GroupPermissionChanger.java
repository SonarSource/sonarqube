/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.util.List;
import java.util.Optional;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.GroupPermissionDto;

import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateNotAnyoneAndAdminPermission;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class GroupPermissionChanger {

  private final DbClient dbClient;

  public GroupPermissionChanger(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public boolean apply(DbSession dbSession, GroupPermissionChange change) {
    switch (change.getOperation()) {
      case ADD:
        return addPermission(dbSession, change);
      case REMOVE:
        return removePermission(dbSession, change);
      default:
        throw new UnsupportedOperationException("Unsupported permission change: " + change.getOperation());
    }
  }

  private boolean addPermission(DbSession dbSession, GroupPermissionChange change) {
    if (loadExistingPermissions(dbSession, change).contains(change.getPermission())) {
      return false;
    }

    validateNotAnyoneAndAdminPermission(change.getPermission(), change.getGroupIdOrAnyone());
    GroupPermissionDto addedDto = new GroupPermissionDto()
      .setRole(change.getPermission())
      .setOrganizationUuid(change.getOrganizationUuid())
      .setGroupId(change.getGroupIdOrAnyone().getId())
      .setResourceId(change.getNullableProjectId());
    dbClient.groupPermissionDao().insert(dbSession, addedDto);
    return true;
  }

  private boolean removePermission(DbSession dbSession, GroupPermissionChange change) {
    if (!loadExistingPermissions(dbSession, change).contains(change.getPermission())) {
      return false;
    }
    checkIfRemainingGlobalAdministrators(dbSession, change);
    dbClient.groupPermissionDao().delete(dbSession,
      change.getPermission(),
      change.getOrganizationUuid(),
      change.getGroupIdOrAnyone().getId(),
      change.getNullableProjectId());
    return true;
  }

  private List<String> loadExistingPermissions(DbSession dbSession, GroupPermissionChange change) {
    Optional<ProjectId> projectId = change.getProjectId();
    if (projectId.isPresent()) {
      return dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession,
        change.getOrganizationUuid(),
        change.getGroupIdOrAnyone().getId(),
        projectId.get().getId());
    }
    return dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession,
      change.getOrganizationUuid(),
      change.getGroupIdOrAnyone().getId());
  }

  private void checkIfRemainingGlobalAdministrators(DbSession dbSession, GroupPermissionChange change) {
    if (SYSTEM_ADMIN.equals(change.getPermission()) &&
      !change.getGroupIdOrAnyone().isAnyone() &&
      !change.getProjectId().isPresent()) {
      // removing global admin permission from group
      int remaining = dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingGroup(dbSession,
        change.getOrganizationUuid(), SYSTEM_ADMIN, change.getGroupIdOrAnyone().getId());
      checkRequest(remaining > 0, "Last group with permission '%s'. Permission cannot be removed.", SYSTEM_ADMIN);
    }
  }

}
