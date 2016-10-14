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

import java.util.Optional;
import java.util.Set;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.permission.PermissionChange.Operation;

import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;

public class UserPermissionChanger {

  private final DbClient dbClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public UserPermissionChanger(DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  public boolean apply(DbSession dbSession, UserPermissionChange change) {
    if (shouldSkipChange(dbSession, change)) {
      return false;
    }

    switch (change.getOperation()) {
      case ADD:
        UserPermissionDto dto = new UserPermissionDto(change.getOrganizationUuid(), change.getPermission(), change.getUserId().getId(), change.getNullableProjectId());
        dbClient.userPermissionDao().insert(dbSession, dto);
        break;
      case REMOVE:
        checkOtherAdminUsersExist(dbSession, change);
        Optional<ProjectId> projectId = change.getProjectId();
        if (projectId.isPresent()) {
          dbClient.userPermissionDao().deleteProjectPermission(dbSession, change.getUserId().getId(), change.getPermission(), projectId.get().getId());
        } else {
          dbClient.userPermissionDao().deleteGlobalPermission(dbSession, change.getUserId().getId(), change.getPermission(), change.getOrganizationUuid());
        }
        break;
      default:
        throw new UnsupportedOperationException("Unsupported permission change: " + change.getOperation());
    }
    if (SYSTEM_ADMIN.equals(change.getPermission()) && !change.getProjectId().isPresent()) {
      dbClient.userDao().updateRootFlagFromPermissions(dbSession, change.getUserId().getId(), defaultOrganizationProvider.get().getUuid());
    }
    return true;
  }

  private boolean shouldSkipChange(DbSession dbSession, UserPermissionChange change) {
    Set<String> existingPermissions = dbClient.userPermissionDao().selectPermissionsByLogin(dbSession, change.getUserId().getLogin(), change.getProjectUuid());
    return (Operation.ADD == change.getOperation() && existingPermissions.contains(change.getPermission())) ||
      (Operation.REMOVE == change.getOperation() && !existingPermissions.contains(change.getPermission()));
  }

  private void checkOtherAdminUsersExist(DbSession session, PermissionChange change) {
    if (SYSTEM_ADMIN.equals(change.getPermission()) &&
      !change.getProjectId().isPresent() &&
      dbClient.roleDao().countUserPermissions(session, change.getPermission(), null) <= 1) {
      throw new BadRequestException(String.format("Last user with '%s' permission. Permission cannot be removed.", SYSTEM_ADMIN));
    }
  }
}
