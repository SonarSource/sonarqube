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
package org.sonar.server.common.permission;

import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.UserPermissionDto;

import static org.sonar.server.common.permission.Operation.ADD;
import static org.sonar.server.common.permission.Operation.REMOVE;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

/**
 * Adds and removes user permissions. Both global and project scopes are supported.
 */
public class UserPermissionChanger implements GranteeTypeSpecificPermissionUpdater<UserPermissionChange> {

  private final DbClient dbClient;
  private final UuidFactory uuidFactory;

  public UserPermissionChanger(DbClient dbClient, UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
  }

  @Override
  public Class<UserPermissionChange> getHandledClass() {
    return UserPermissionChange.class;
  }

  @Override
  public Set<String> loadExistingEntityPermissions(DbSession dbSession, String organizationUuid, String uuidOfGrantee, @Nullable String entityUuid) {
    if (entityUuid != null) {
      return new HashSet<>(dbClient.userPermissionDao().selectEntityPermissionsOfUser(dbSession, uuidOfGrantee, entityUuid));
    }
    return new HashSet<>(dbClient.userPermissionDao().selectGlobalPermissionsOfUser(dbSession, uuidOfGrantee, organizationUuid));
  }

  @Override
  public boolean apply(DbSession dbSession, Set<String> existingPermissions, UserPermissionChange change) {
    ensureConsistencyWithVisibility(change);
    if (isImplicitlyAlreadyDone(change)) {
      return false;
    }
    switch (change.getOperation()) {
      case ADD:
        return addPermission(dbSession, existingPermissions, change);
      case REMOVE:
        return removePermission(dbSession, existingPermissions, change);
      default:
        throw new UnsupportedOperationException("Unsupported permission change: " + change.getOperation());
    }
  }

  private static boolean isImplicitlyAlreadyDone(UserPermissionChange change) {
    EntityDto project = change.getEntity();
    if (project != null) {
      return isImplicitlyAlreadyDone(project, change);
    }
    return false;
  }

  private static boolean isImplicitlyAlreadyDone(EntityDto project, UserPermissionChange change) {
    return isAttemptToAddPublicPermissionToPublicComponent(change, project);
  }

  private static boolean isAttemptToAddPublicPermissionToPublicComponent(UserPermissionChange change, EntityDto project) {
    return !project.isPrivate()
      && change.getOperation() == ADD
      && UserRole.PUBLIC_PERMISSIONS.contains(change.getPermission());
  }

  private static void ensureConsistencyWithVisibility(UserPermissionChange change) {
    EntityDto project = change.getEntity();
    if (project != null) {
      checkRequest(!isAttemptToRemovePublicPermissionFromPublicComponent(change, project),
        "Permission %s can't be removed from a public component", change.getPermission());
    }
  }

  private static boolean isAttemptToRemovePublicPermissionFromPublicComponent(UserPermissionChange change, EntityDto entity) {
    return !entity.isPrivate()
      && change.getOperation() == REMOVE
      && UserRole.PUBLIC_PERMISSIONS.contains(change.getPermission());
  }

  private boolean addPermission(DbSession dbSession, Set<String> existingPermissions, UserPermissionChange change) {
    if (existingPermissions.contains(change.getPermission())) {
      return false;
    }
    UserPermissionDto dto = new UserPermissionDto(uuidFactory.create(), change.getOrganizationUuid(), change.getPermission(), change.getUserId().getUuid(),
      change.getProjectUuid());
    dbClient.userPermissionDao().insert(dbSession, dto, change.getEntity(), change.getUserId(), null);
    return true;
  }

  private boolean removePermission(DbSession dbSession, Set<String> existingPermissions, UserPermissionChange change) {
    if (!existingPermissions.contains(change.getPermission())) {
      return false;
    }
    checkOtherAdminsExist(dbSession, change);
    EntityDto entity = change.getEntity();
    if (entity != null) {
      dbClient.userPermissionDao().deleteEntityPermission(dbSession, change.getUserId(), change.getPermission(), entity);
    } else {
      dbClient.userPermissionDao().deleteGlobalPermission(dbSession, change.getUserId(), change.getPermission(), change.getOrganizationUuid());
    }
    return true;
  }

  private void checkOtherAdminsExist(DbSession dbSession, UserPermissionChange change) {
    if (GlobalPermission.ADMINISTER.getKey().equals(change.getPermission()) && change.getProjectUuid() == null) {
      int remaining = dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingUserPermission(dbSession, change.getOrganizationUuid(), change.getPermission(), change.getUserId().getUuid());
      checkRequest(remaining > 0, "Last user with permission '%s'. Permission cannot be removed.", GlobalPermission.ADMINISTER.getKey());
    }
  }

}
