/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import static org.sonar.api.web.UserRole.PUBLIC_PERMISSIONS;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.permission.PermissionChange.Operation.ADD;
import static org.sonar.server.permission.PermissionChange.Operation.REMOVE;
import static org.sonar.server.permission.ws.RequestValidator.validateNotAnyoneAndAdminPermission;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class GroupPermissionChanger {

  private final DbClient dbClient;

  public GroupPermissionChanger(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public boolean apply(DbSession dbSession, GroupPermissionChange change) {
    ensureConsistencyWithVisibility(change);
    if (isImplicitlyAlreadyDone(change)) {
      return false;
    }
    switch (change.getOperation()) {
      case ADD:
        return addPermission(dbSession, change);
      case REMOVE:
        return removePermission(dbSession, change);
      default:
        throw new UnsupportedOperationException("Unsupported permission change: " + change.getOperation());
    }
  }

  private static boolean isImplicitlyAlreadyDone(GroupPermissionChange change) {
    return change.getProjectId()
      .map(projectId -> isImplicitlyAlreadyDone(projectId, change))
      .orElse(false);
  }

  private static boolean isImplicitlyAlreadyDone(ProjectId projectId, GroupPermissionChange change) {
    return isAttemptToAddPublicPermissionToPublicComponent(change, projectId)
      || isAttemptToRemovePermissionFromAnyoneOnPrivateComponent(change, projectId);
  }

  private static boolean isAttemptToAddPublicPermissionToPublicComponent(GroupPermissionChange change, ProjectId projectId) {
    return !projectId.isPrivate()
      && change.getOperation() == ADD
      && PUBLIC_PERMISSIONS.contains(change.getPermission());
  }

  private static boolean isAttemptToRemovePermissionFromAnyoneOnPrivateComponent(GroupPermissionChange change, ProjectId projectId) {
    return projectId.isPrivate()
      && change.getOperation() == REMOVE
      && change.getGroupIdOrAnyone().isAnyone();
  }

  private static void ensureConsistencyWithVisibility(GroupPermissionChange change) {
    change.getProjectId()
      .ifPresent(projectId -> {
        checkRequest(
          !isAttemptToAddPermissionToAnyoneOnPrivateComponent(change, projectId),
          "No permission can be granted to Anyone on a private component");
        checkRequest(
          !isAttemptToRemovePublicPermissionFromPublicComponent(change, projectId),
          "Permission %s can't be removed from a public component", change.getPermission());
      });
  }

  private static boolean isAttemptToAddPermissionToAnyoneOnPrivateComponent(GroupPermissionChange change, ProjectId projectId) {
    return projectId.isPrivate()
      && change.getOperation() == ADD
      && change.getGroupIdOrAnyone().isAnyone();
  }

  private static boolean isAttemptToRemovePublicPermissionFromPublicComponent(GroupPermissionChange change, ProjectId projectId) {
    return !projectId.isPrivate()
      && change.getOperation() == REMOVE
      && PUBLIC_PERMISSIONS.contains(change.getPermission());
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
