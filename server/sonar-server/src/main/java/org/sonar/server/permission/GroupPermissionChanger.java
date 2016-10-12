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
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.UserSession;

import static org.sonar.server.permission.ws.PermissionRequestValidator.validateNotAnyoneAndAdminPermission;

public class GroupPermissionChanger {

  private final DbClient dbClient;
  private final UserSession userSession;

  public GroupPermissionChanger(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  public boolean apply(DbSession dbSession, GroupPermissionChange change) {
    PermissionPrivilegeChecker.checkProjectAdminUserByComponentUuid(userSession, change.getProjectUuid());

    if (shouldSkip(dbSession, change)) {
      return false;
    }

    switch (change.getOperation()) {
      case ADD:
        validateNotAnyoneAndAdminPermission(change.getPermission(), change.getGroupIdOrAnyone());
        GroupPermissionDto addedDto = new GroupPermissionDto()
          .setRole(change.getPermission())
          .setOrganizationUuid(change.getOrganizationUuid())
          .setGroupId(change.getGroupIdOrAnyone().getId())
          .setResourceId(change.getNullableProjectId());
        dbClient.groupPermissionDao().insert(dbSession, addedDto);
        break;
      case REMOVE:
        checkAdminUsersExistOutsideTheRemovedGroup(dbSession, change);
        GroupPermissionDto deletedDto = new GroupPermissionDto()
          .setRole(change.getPermission())
          .setOrganizationUuid(change.getOrganizationUuid())
          .setGroupId(change.getGroupIdOrAnyone().getId())
          .setResourceId(change.getNullableProjectId());
        dbClient.roleDao().deleteGroupRole(deletedDto, dbSession);
        break;
      default:
        throw new UnsupportedOperationException("Unsupported permission change: " + change.getOperation());
    }
    return true;
  }

  private boolean shouldSkip(DbSession dbSession, GroupPermissionChange change) {
    List<String> existingPermissions;
    if (change.getGroupIdOrAnyone().isAnyone()) {
      existingPermissions = dbClient.groupPermissionDao().selectAnyonePermissions(dbSession, change.getNullableProjectId());
    } else {
      existingPermissions = dbClient.groupPermissionDao().selectGroupPermissions(dbSession, change.getGroupIdOrAnyone().getId(), change.getNullableProjectId());
    }
    switch (change.getOperation()) {
      case ADD:
        return existingPermissions.contains(change.getPermission());
      case REMOVE:
        return !existingPermissions.contains(change.getPermission());
      default:
        throw new UnsupportedOperationException("Unsupported operation: " + change.getOperation());
    }
  }

  private void checkAdminUsersExistOutsideTheRemovedGroup(DbSession dbSession, GroupPermissionChange change) {
    if (GlobalPermissions.SYSTEM_ADMIN.equals(change.getPermission()) &&
      !change.getProjectRef().isPresent() &&
      // TODO support organizations
      dbClient.roleDao().countUserPermissions(dbSession, change.getPermission(), change.getGroupIdOrAnyone().getId()) <= 0) {
      throw new BadRequestException(String.format("Last group with '%s' permission. Permission cannot be removed.", GlobalPermissions.SYSTEM_ADMIN));
    }
  }

}
