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

import java.util.List;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.UserPermissionDto;

import static org.sonar.api.web.UserRole.PUBLIC_PERMISSIONS;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.permission.PermissionChange.Operation.ADD;
import static org.sonar.server.permission.PermissionChange.Operation.REMOVE;

/**
 * Adds and removes user permissions. Both global and project scopes are supported.
 */
public class UserPermissionChanger {

  private final DbClient dbClient;
  private final UuidFactory uuidFactory;

  public UserPermissionChanger(DbClient dbClient, UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
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
    ComponentDto project = change.getProject();
    if (project != null) {
      return isImplicitlyAlreadyDone(project, change);
    }
    return false;
  }

  private static boolean isImplicitlyAlreadyDone(ComponentDto project, UserPermissionChange change) {
    return isAttemptToAddPublicPermissionToPublicComponent(change, project);
  }

  private static boolean isAttemptToAddPublicPermissionToPublicComponent(UserPermissionChange change, ComponentDto project) {
    return !project.isPrivate()
      && change.getOperation() == ADD
      && PUBLIC_PERMISSIONS.contains(change.getPermission());
  }

  private static void ensureConsistencyWithVisibility(UserPermissionChange change) {
    ComponentDto project = change.getProject();
    if (project != null) {
      checkRequest(!isAttemptToRemovePublicPermissionFromPublicComponent(change, project),
        "Permission %s can't be removed from a public component", change.getPermission());
    }
  }

  private static boolean isAttemptToRemovePublicPermissionFromPublicComponent(UserPermissionChange change, ComponentDto projectUuid) {
    return !projectUuid.isPrivate()
      && change.getOperation() == REMOVE
      && PUBLIC_PERMISSIONS.contains(change.getPermission());
  }

  private boolean addPermission(DbSession dbSession, UserPermissionChange change) {
    if (loadExistingPermissions(dbSession, change).contains(change.getPermission())) {
      return false;
    }
    UserPermissionDto dto = new UserPermissionDto(uuidFactory.create(), change.getPermission(), change.getUserId().getUuid(),
      change.getProjectUuid());
    dbClient.userPermissionDao().insert(dbSession, dto, change.getProject(), change.getUserId(), null);
    return true;
  }

  private boolean removePermission(DbSession dbSession, UserPermissionChange change) {
    if (!loadExistingPermissions(dbSession, change).contains(change.getPermission())) {
      return false;
    }
    checkOtherAdminsExist(dbSession, change);
    ComponentDto project = change.getProject();
    if (project != null) {
      dbClient.userPermissionDao().deleteProjectPermission(dbSession, change.getUserId(), change.getPermission(), project);
    } else {
      dbClient.userPermissionDao().deleteGlobalPermission(dbSession, change.getUserId(), change.getPermission());
    }
    return true;
  }

  private List<String> loadExistingPermissions(DbSession dbSession, UserPermissionChange change) {
    String projectUuid = change.getProjectUuid();
    if (projectUuid != null) {
      return dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, change.getUserId().getUuid(), projectUuid);
    }
    return dbClient.userPermissionDao().selectGlobalPermissionsOfUser(dbSession, change.getUserId().getUuid());
  }

  private void checkOtherAdminsExist(DbSession dbSession, UserPermissionChange change) {
    if (SYSTEM_ADMIN.equals(change.getPermission()) && change.getProjectUuid() == null) {
      int remaining = dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingUserPermission(dbSession, change.getPermission(), change.getUserId().getUuid());
      checkRequest(remaining > 0, "Last user with permission '%s'. Permission cannot be removed.", SYSTEM_ADMIN);
    }
  }
}
