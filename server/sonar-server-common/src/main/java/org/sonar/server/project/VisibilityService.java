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
package org.sonar.server.project;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserId;
import org.sonar.server.es.Indexers;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.sonar.api.utils.Preconditions.checkState;
import static org.sonar.api.web.UserRole.PUBLIC_PERMISSIONS;
import static org.sonar.db.ce.CeTaskTypes.GITHUB_PROJECT_PERMISSIONS_PROVISIONING;
import static org.sonar.db.ce.CeTaskTypes.GITLAB_PROJECT_PERMISSIONS_PROVISIONING;

@ServerSide
@ComputeEngineSide
public class VisibilityService {
  private final DbClient dbClient;
  private final Indexers indexers;
  private final UuidFactory uuidFactory;

  public VisibilityService(DbClient dbClient,  Indexers indexers, UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.indexers = indexers;
    this.uuidFactory = uuidFactory;
  }

  public void changeVisibility(EntityDto entityDto, boolean isPrivate) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      checkNoPendingTasks(dbSession, entityDto);
      if (isPrivate != entityDto.isPrivate()) {
        setPrivateForRootComponentUuid(dbSession, entityDto, isPrivate);
        if (isPrivate) {
          updatePermissionsToPrivate(dbSession, entityDto);
        } else {
          updatePermissionsToPublic(dbSession, entityDto);
        }
        indexers.commitAndIndexEntities(dbSession, singletonList(entityDto), Indexers.EntityEvent.PERMISSION_CHANGE);
      }
    }
  }

  @VisibleForTesting
  void checkNoPendingTasks(DbSession dbSession, EntityDto entityDto) {
    //This check likely can be removed when we remove the column 'private' from components table in SONAR-20126.
    checkState(countPendingTask(dbSession, entityDto.getKey()) == 0, "Component visibility can't be changed as long as it has background task(s) pending or in progress");
  }

  private long countPendingTask(DbSession dbSession, String entityKey) {
    EntityDto entityDto = dbClient.entityDao().selectByKey(dbSession, entityKey).orElseThrow(() -> new IllegalStateException("Can't find entity " + entityKey));
    return dbClient.ceQueueDao().selectByEntityUuid(dbSession, entityDto.getUuid())
      .stream()
      .filter(task -> !hasDevopsProjectPermissionsProvisioningTaskRunning(task))
      .count();
  }

  private static boolean hasDevopsProjectPermissionsProvisioningTaskRunning(CeQueueDto task) {
    return task.getTaskType().equals(GITHUB_PROJECT_PERMISSIONS_PROVISIONING) || task.getTaskType().equals(GITLAB_PROJECT_PERMISSIONS_PROVISIONING);
  }

  private void setPrivateForRootComponentUuid(DbSession dbSession, EntityDto entity, boolean newIsPrivate) {
    Optional<BranchDto> branchDto = dbClient.branchDao().selectMainBranchByProjectUuid(dbSession, entity.getUuid());
    String branchUuid = branchDto.isPresent() ? branchDto.get().getUuid() : entity.getUuid();
    dbClient.componentDao().setPrivateForBranchUuid(dbSession, branchUuid, newIsPrivate, entity);

    if (entity.isProjectOrApp()) {
      dbClient.projectDao().updateVisibility(dbSession, entity.getUuid(), newIsPrivate);
      dbClient.branchDao().selectByProjectUuid(dbSession, entity.getUuid()).stream()
        .filter(branch -> !branch.isMain())
        .forEach(branch -> dbClient.componentDao().setPrivateForBranchUuidWithoutAuditLog(dbSession, branch.getUuid(), newIsPrivate));
    } else {
      dbClient.portfolioDao().updateVisibilityByPortfolioUuid(dbSession, entity.getUuid(), newIsPrivate);
    }
    entity.setPrivate(newIsPrivate);
  }

  private void updatePermissionsToPrivate(DbSession dbSession, EntityDto entity) {
    // delete project permissions for group AnyOne
    dbClient.groupPermissionDao().deleteByEntityUuidForAnyOne(dbSession, entity);
    // grant UserRole.CODEVIEWER and UserRole.USER to any group or user with at least one permission on project
    PUBLIC_PERMISSIONS.forEach(permission -> {
      dbClient.groupPermissionDao().selectGroupUuidsWithPermissionOnEntityBut(dbSession, entity.getUuid(), permission)
        .forEach(group -> insertProjectPermissionOnGroup(dbSession, entity, permission, group));
      dbClient.userPermissionDao().selectUserIdsWithPermissionOnEntityBut(dbSession, entity.getUuid(), permission)
        .forEach(userUuid -> insertProjectPermissionOnUser(dbSession, entity, permission, userUuid));
    });
  }

  private void insertProjectPermissionOnUser(DbSession dbSession, EntityDto entity, String permission, UserId userId) {
    dbClient.userPermissionDao().insert(dbSession, new UserPermissionDto(Uuids.create(), entity.getOrganizationUuid(), permission, userId.getUuid(), entity.getUuid()),
      entity, userId, null);
  }

  private void insertProjectPermissionOnGroup(DbSession dbSession, EntityDto entity, String permission, String groupUuid) {
    String groupName = ofNullable(dbClient.groupDao().selectByUuid(dbSession, groupUuid)).map(GroupDto::getName).orElse(null);
    dbClient.groupPermissionDao().insert(dbSession, new GroupPermissionDto()
      .setUuid(uuidFactory.create())
      .setEntityUuid(entity.getUuid())
      .setGroupUuid(groupUuid)
      .setGroupName(groupName)
      .setRole(permission)
      .setOrganizationUuid(entity.getOrganizationUuid())
      .setEntityName(entity.getName()), entity, null);
  }

  private void updatePermissionsToPublic(DbSession dbSession, EntityDto entity) {
    PUBLIC_PERMISSIONS.forEach(permission -> {
      // delete project group permission for UserRole.CODEVIEWER and UserRole.USER
      dbClient.groupPermissionDao().deleteByEntityAndPermission(dbSession, permission, entity);
      // delete project user permission for UserRole.CODEVIEWER and UserRole.USER
      dbClient.userPermissionDao().deleteEntityPermissionOfAnyUser(dbSession, permission, entity);
    });
  }
}
