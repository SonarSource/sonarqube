/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
import javax.annotation.Nullable;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.ProjectPermission;

import static org.sonar.server.common.permission.Operation.ADD;
import static org.sonar.server.common.permission.Operation.REMOVE;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

public class GroupPermissionChanger implements GranteeTypeSpecificPermissionUpdater<GroupPermissionChange> {

  private final DbClient dbClient;
  private final UuidFactory uuidFactory;

  public GroupPermissionChanger(DbClient dbClient, UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
  }

  @Override
  public Class<GroupPermissionChange> getHandledClass() {
    return GroupPermissionChange.class;
  }

  @Override
  public Set<String> loadExistingEntityPermissions(DbSession dbSession, String uuidOfGrantee, @Nullable String entityUuid) {
    if (entityUuid != null) {
      return new HashSet<>(dbClient.groupPermissionDao().selectEntityPermissionsOfGroup(dbSession, uuidOfGrantee, entityUuid));
    }
    return new HashSet<>(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, uuidOfGrantee));
  }

  @Override
  public boolean apply(DbSession dbSession, Set<String> existingPermissions, GroupPermissionChange change) {
    ensureConsistencyWithVisibility(change);
    if (isImplicitlyAlreadyDone(change)) {
      return false;
    }
    switch (change.getOperation()) {
      case ADD:
        if (existingPermissions.contains(change.getPermission())) {
          return false;
        }
        return addPermission(dbSession, change);
      case REMOVE:
        if (!existingPermissions.contains(change.getPermission())) {
          return false;
        }
        return removePermission(dbSession, change);
      default:
        throw new UnsupportedOperationException("Unsupported permission change: " + change.getOperation());
    }
  }

  private static boolean isImplicitlyAlreadyDone(GroupPermissionChange change) {
    EntityDto project = change.getEntity();
    if (project != null) {
      return isImplicitlyAlreadyDone(project, change);
    }
    return false;
  }

  private static boolean isImplicitlyAlreadyDone(EntityDto project, GroupPermissionChange change) {
    return isAttemptToAddPublicPermissionToPublicComponent(change, project);
  }

  private static boolean isAttemptToAddPublicPermissionToPublicComponent(GroupPermissionChange change, EntityDto project) {
    return !project.isPrivate()
      && change.getOperation() == ADD
      && ProjectPermission.isPublic(change.getPermission());
  }

  private static void ensureConsistencyWithVisibility(GroupPermissionChange change) {
    EntityDto project = change.getEntity();
    if (project != null) {
      checkRequest(
        !isAttemptToRemovePublicPermissionFromPublicComponent(change, project),
        "Permission %s can't be removed from a public component", change.getPermission());
    }
  }

  private static boolean isAttemptToRemovePublicPermissionFromPublicComponent(GroupPermissionChange change, EntityDto project) {
    return !project.isPrivate()
      && change.getOperation() == REMOVE
      && ProjectPermission.isPublic(change.getPermission());
  }

  private boolean addPermission(DbSession dbSession, GroupPermissionChange change) {
    GroupPermissionDto addedDto = new GroupPermissionDto()
      .setUuid(uuidFactory.create())
      .setRole(change.getPermission())
      .setGroupUuid(change.getUuidOfGrantee())
      .setEntityName(change.getProjectName())
      .setEntityUuid(change.getProjectUuid())
      .setGroupName(change.getGroupName());

    dbClient.groupPermissionDao().insert(dbSession, addedDto, change.getEntity(), null);
    return true;
  }

  private boolean removePermission(DbSession dbSession, GroupPermissionChange change) {
    checkIfRemainingGlobalAdministrators(dbSession, change);
    dbClient.groupPermissionDao().delete(dbSession,
      change.getPermission(),
      change.getUuidOfGrantee(),
      change.getGroupName(),
      change.getEntity());
    return true;
  }

  private void checkIfRemainingGlobalAdministrators(DbSession dbSession, GroupPermissionChange change) {
    if (GlobalPermission.ADMINISTER.getKey().equals(change.getPermission()) && change.getProjectUuid() == null) {
      // removing global admin permission from group
      int remaining = dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingGroup(dbSession, GlobalPermission.ADMINISTER.getKey(), change.getUuidOfGrantee());
      checkRequest(remaining > 0, "Last group with permission '%s'. Permission cannot be removed.", GlobalPermission.ADMINISTER.getKey());
    }
  }

}
