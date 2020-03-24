/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.GroupPermissionDto;

import static java.lang.String.format;
import static org.sonar.api.web.UserRole.PUBLIC_PERMISSIONS;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.permission.PermissionChange.Operation.ADD;
import static org.sonar.server.permission.PermissionChange.Operation.REMOVE;

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
    if (change.getProject() != null) {
      return isImplicitlyAlreadyDone(change.getProject(), change);
    }
    return false;
  }

  private static boolean isImplicitlyAlreadyDone(ProjectUuid project, GroupPermissionChange change) {
    return isAttemptToAddPublicPermissionToPublicComponent(change, project)
      || isAttemptToRemovePermissionFromAnyoneOnPrivateComponent(change, project);
  }

  private static boolean isAttemptToAddPublicPermissionToPublicComponent(GroupPermissionChange change, ProjectUuid project) {
    return !project.isPrivate()
      && change.getOperation() == ADD
      && PUBLIC_PERMISSIONS.contains(change.getPermission());
  }

  private static boolean isAttemptToRemovePermissionFromAnyoneOnPrivateComponent(GroupPermissionChange change, ProjectUuid project) {
    return project.isPrivate()
      && change.getOperation() == REMOVE
      && change.getGroupIdOrAnyone().isAnyone();
  }

  private static void ensureConsistencyWithVisibility(GroupPermissionChange change) {
    if (change.getProject() != null) {
      checkRequest(
        !isAttemptToAddPermissionToAnyoneOnPrivateComponent(change, change.getProject()),
        "No permission can be granted to Anyone on a private component");
      checkRequest(
        !isAttemptToRemovePublicPermissionFromPublicComponent(change, change.getProject()),
        "Permission %s can't be removed from a public component", change.getPermission());
    }
  }

  private static boolean isAttemptToAddPermissionToAnyoneOnPrivateComponent(GroupPermissionChange change, ProjectUuid project) {
    return project.isPrivate()
      && change.getOperation() == ADD
      && change.getGroupIdOrAnyone().isAnyone();
  }

  private static boolean isAttemptToRemovePublicPermissionFromPublicComponent(GroupPermissionChange change, ProjectUuid project) {
    return !project.isPrivate()
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
      .setComponentUuid(change.getProjectUuid());
    dbClient.groupPermissionDao().insert(dbSession, addedDto);
    return true;
  }

  private static void validateNotAnyoneAndAdminPermission(String permission, GroupIdOrAnyone group) {
    checkRequest(!GlobalPermissions.SYSTEM_ADMIN.equals(permission) || !group.isAnyone(),
      format("It is not possible to add the '%s' permission to group 'Anyone'.", permission));
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
      change.getProjectUuid());
    return true;
  }

  private List<String> loadExistingPermissions(DbSession dbSession, GroupPermissionChange change) {
    String projectUuid = change.getProjectUuid();
    if (projectUuid != null) {
      return dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession,
        change.getOrganizationUuid(),
        change.getGroupIdOrAnyone().getId(),
        projectUuid);
    }
    return dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession,
      change.getOrganizationUuid(),
      change.getGroupIdOrAnyone().getId());
  }

  private void checkIfRemainingGlobalAdministrators(DbSession dbSession, GroupPermissionChange change) {
    if (SYSTEM_ADMIN.equals(change.getPermission()) &&
      !change.getGroupIdOrAnyone().isAnyone() &&
      change.getProjectUuid() == null) {
      // removing global admin permission from group
      int remaining = dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingGroup(dbSession,
        change.getOrganizationUuid(), SYSTEM_ADMIN, change.getGroupIdOrAnyone().getId());
      checkRequest(remaining > 0, "Last group with permission '%s'. Permission cannot be removed.", SYSTEM_ADMIN);
    }
  }

}
