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

import java.util.List;
import java.util.Optional;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.organization.DefaultOrganizationProvider;

import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;

/**
 * Adds and removes user permissions. Both global and project scopes are supported.
 */
public class UserPermissionChanger {

  private final DbClient dbClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public UserPermissionChanger(DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  public boolean apply(DbSession dbSession, UserPermissionChange change) {
    switch (change.getOperation()) {
      case ADD:
        return addPermission(dbSession, change);
      case REMOVE:
        return removePermission(dbSession, change);
      default:
        throw new UnsupportedOperationException("Unsupported permission change: " + change.getOperation());
    }
  }

  private boolean addPermission(DbSession dbSession, UserPermissionChange change) {
    if (loadExistingPermissions(dbSession, change).contains(change.getPermission())) {
      return false;
    }
    UserPermissionDto dto = new UserPermissionDto(change.getOrganizationUuid(), change.getPermission(), change.getUserId().getId(), change.getNullableProjectId());
    dbClient.userPermissionDao().insert(dbSession, dto);
    updateRootFlag(dbSession, change);
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
    updateRootFlag(dbSession, change);
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
      if (remaining == 0) {
        throw new BadRequestException(String.format("Last user with permission '%s'. Permission cannot be removed.", SYSTEM_ADMIN));
      }
    }
  }

  private void updateRootFlag(DbSession dbSession, UserPermissionChange change) {
    if (SYSTEM_ADMIN.equals(change.getPermission()) && !change.getProjectId().isPresent()) {
      dbClient.userDao().updateRootFlagFromPermissions(dbSession, change.getUserId().getId(), defaultOrganizationProvider.get().getUuid());
    }
  }
}
