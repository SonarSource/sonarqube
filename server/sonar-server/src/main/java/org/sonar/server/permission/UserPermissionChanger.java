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
import org.sonar.db.permission.UserPermissionDto;

import static org.sonar.api.web.UserRole.PUBLIC_PERMISSIONS;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.permission.PermissionChange.Operation.ADD;
import static org.sonar.server.permission.PermissionChange.Operation.REMOVE;
import static org.sonar.server.ws.WsUtils.checkRequest;

/**
 * Adds and removes user permissions. Both global and project scopes are supported.
 */
public class UserPermissionChanger {

  private final DbClient dbClient;

  public UserPermissionChanger(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public boolean apply(DbSession dbSession, UserPermissionChange change) {
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

  private static boolean isImplicitlyAlreadyDone(UserPermissionChange change) {
    return change.getProjectId()
      .map(projectId -> isImplicitlyAlreadyDone(projectId, change))
      .orElse(false);
  }

  private static boolean isImplicitlyAlreadyDone(ProjectId projectId, UserPermissionChange change) {
    return isAttemptToAddPublicPermissionToPublicComponent(change, projectId);
  }

  private static boolean isAttemptToAddPublicPermissionToPublicComponent(UserPermissionChange change, ProjectId projectId) {
    return !projectId.isPrivate()
      && change.getOperation() == ADD
      && PUBLIC_PERMISSIONS.contains(change.getPermission());
  }

  private static void ensureConsistencyWithVisibility(UserPermissionChange change) {
    change.getProjectId()
      .ifPresent(projectId -> checkRequest(
        !isAttemptToRemovePublicPermissionFromPublicComponent(change, projectId),
        "Permission %s can't be removed from a public component", change.getPermission()));
  }

  private static boolean isAttemptToRemovePublicPermissionFromPublicComponent(UserPermissionChange change, ProjectId projectId) {
    return !projectId.isPrivate()
      && change.getOperation() == REMOVE
      && PUBLIC_PERMISSIONS.contains(change.getPermission());
  }

  private boolean addPermission(DbSession dbSession, UserPermissionChange change) {
    if (loadExistingPermissions(dbSession, change).contains(change.getPermission())) {
      return false;
    }
    UserPermissionDto dto = new UserPermissionDto(change.getOrganizationUuid(), change.getPermission(), change.getUserId().getId(), change.getNullableProjectId());
    dbClient.userPermissionDao().insert(dbSession, dto);
    return true;
  }

  private boolean removePermission(DbSession dbSession, UserPermissionChange change) {
    if (!loadExistingPermissions(dbSession, change).contains(change.getPermission())) {
      return false;
    }
    checkOtherAdminsExist(dbSession, change);
    Optional<ProjectId> projectId = change.getProjectId();
    if (projectId.isPresent()) {
      dbClient.userPermissionDao().deleteProjectPermission(dbSession, change.getUserId().getId(), change.getPermission(), projectId.get().getId());
    } else {
      dbClient.userPermissionDao().deleteGlobalPermission(dbSession, change.getUserId().getId(), change.getPermission(), change.getOrganizationUuid());
    }
    return true;
  }

  private List<String> loadExistingPermissions(DbSession dbSession, UserPermissionChange change) {
    Optional<ProjectId> projectId = change.getProjectId();
    if (projectId.isPresent()) {
      return dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession,
        change.getUserId().getId(),
        projectId.get().getId());
    }
    return dbClient.userPermissionDao().selectGlobalPermissionsOfUser(dbSession,
      change.getUserId().getId(),
      change.getOrganizationUuid());
  }

  private void checkOtherAdminsExist(DbSession dbSession, UserPermissionChange change) {
    if (SYSTEM_ADMIN.equals(change.getPermission()) && !change.getProjectId().isPresent()) {
      int remaining = dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingUserPermission(dbSession,
        change.getOrganizationUuid(), change.getPermission(), change.getUserId().getId());
      checkRequest(remaining > 0, "Last user with permission '%s'. Permission cannot be removed.", SYSTEM_ADMIN);
    }
  }
}
