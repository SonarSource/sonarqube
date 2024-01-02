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
import java.util.Optional;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.user.GroupDto;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonar.api.web.UserRole.PUBLIC_PERMISSIONS;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.permission.PermissionChange.Operation.ADD;
import static org.sonar.server.permission.PermissionChange.Operation.REMOVE;

public class GroupPermissionChanger {

  private final DbClient dbClient;
  private final UuidFactory uuidFactory;

  public GroupPermissionChanger(DbClient dbClient, UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
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
    ComponentDto project = change.getProject();
    if (project != null) {
      return isImplicitlyAlreadyDone(project, change);
    }
    return false;
  }

  private static boolean isImplicitlyAlreadyDone(ComponentDto project, GroupPermissionChange change) {
    return isAttemptToAddPublicPermissionToPublicComponent(change, project)
      || isAttemptToRemovePermissionFromAnyoneOnPrivateComponent(change, project);
  }

  private static boolean isAttemptToAddPublicPermissionToPublicComponent(GroupPermissionChange change, ComponentDto project) {
    return !project.isPrivate()
      && change.getOperation() == ADD
      && PUBLIC_PERMISSIONS.contains(change.getPermission());
  }

  private static boolean isAttemptToRemovePermissionFromAnyoneOnPrivateComponent(GroupPermissionChange change, ComponentDto project) {
    return project.isPrivate()
      && change.getOperation() == REMOVE
      && change.getGroupUuidOrAnyone().isAnyone();
  }

  private static void ensureConsistencyWithVisibility(GroupPermissionChange change) {
    ComponentDto project = change.getProject();
    if (project != null) {
      checkRequest(
        !isAttemptToAddPermissionToAnyoneOnPrivateComponent(change, project),
        "No permission can be granted to Anyone on a private component");
      checkRequest(
        !isAttemptToRemovePublicPermissionFromPublicComponent(change, project),
        "Permission %s can't be removed from a public component", change.getPermission());
    }
  }

  private static boolean isAttemptToAddPermissionToAnyoneOnPrivateComponent(GroupPermissionChange change, ComponentDto project) {
    return project.isPrivate()
      && change.getOperation() == ADD
      && change.getGroupUuidOrAnyone().isAnyone();
  }

  private static boolean isAttemptToRemovePublicPermissionFromPublicComponent(GroupPermissionChange change, ComponentDto project) {
    return !project.isPrivate()
      && change.getOperation() == REMOVE
      && PUBLIC_PERMISSIONS.contains(change.getPermission());
  }

  private boolean addPermission(DbSession dbSession, GroupPermissionChange change) {
    if (loadExistingPermissions(dbSession, change).contains(change.getPermission())) {
      return false;
    }

    validateNotAnyoneAndAdminPermission(change.getPermission(), change.getGroupUuidOrAnyone());

    String groupUuid = change.getGroupUuidOrAnyone().getUuid();
    GroupPermissionDto addedDto = new GroupPermissionDto()
      .setUuid(uuidFactory.create())
      .setRole(change.getPermission())
      .setGroupUuid(groupUuid)
      .setComponentName(change.getProjectName())
      .setComponentUuid(change.getProjectUuid());

    Optional.ofNullable(groupUuid)
      .map(uuid -> dbClient.groupDao().selectByUuid(dbSession, groupUuid))
      .map(GroupDto::getName)
      .ifPresent(addedDto::setGroupName);

    dbClient.groupPermissionDao().insert(dbSession, addedDto, change.getProject(), null);
    return true;
  }

  private static void validateNotAnyoneAndAdminPermission(String permission, GroupUuidOrAnyone group) {
    checkRequest(!GlobalPermissions.SYSTEM_ADMIN.equals(permission) || !group.isAnyone(),
      format("It is not possible to add the '%s' permission to group 'Anyone'.", permission));
  }

  private boolean removePermission(DbSession dbSession, GroupPermissionChange change) {
    if (!loadExistingPermissions(dbSession, change).contains(change.getPermission())) {
      return false;
    }
    checkIfRemainingGlobalAdministrators(dbSession, change);
    String groupUuid = change.getGroupUuidOrAnyone().getUuid();
    String groupName = Optional.ofNullable(groupUuid)
      .map(uuid -> dbClient.groupDao().selectByUuid(dbSession, uuid))
      .map(GroupDto::getName)
      .orElse(null);

    dbClient.groupPermissionDao().delete(dbSession,
      change.getPermission(),
      groupUuid,
      groupName,
      change.getProjectUuid(),
      change.getProject());
    return true;
  }

  private List<String> loadExistingPermissions(DbSession dbSession, GroupPermissionChange change) {
    String projectUuid = change.getProjectUuid();
    if (projectUuid != null) {
      return dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession,
        change.getGroupUuidOrAnyone().getUuid(),
        projectUuid);
    }
    return dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession,
      change.getGroupUuidOrAnyone().getUuid());
  }

  private void checkIfRemainingGlobalAdministrators(DbSession dbSession, GroupPermissionChange change) {
    GroupUuidOrAnyone groupUuidOrAnyone = change.getGroupUuidOrAnyone();
    if (SYSTEM_ADMIN.equals(change.getPermission()) &&
      !groupUuidOrAnyone.isAnyone() &&
      change.getProjectUuid() == null) {
      String groupUuid = checkNotNull(groupUuidOrAnyone.getUuid());
      // removing global admin permission from group
      int remaining = dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingGroup(dbSession, SYSTEM_ADMIN, groupUuid);
      checkRequest(remaining > 0, "Last group with permission '%s'. Permission cannot be removed.", SYSTEM_ADMIN);
    }
  }

}
