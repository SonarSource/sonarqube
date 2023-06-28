/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.project.ws;

import java.util.Optional;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserId;
import org.sonar.server.es.EventIndexer;
import org.sonar.server.es.Indexers;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.project.Visibility;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.client.project.ProjectsWsParameters;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.sonar.api.CoreProperties.CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_DEFAULT_VALUE;
import static org.sonar.api.CoreProperties.CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_PROPERTY;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.PUBLIC_PERMISSIONS;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECT;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_VISIBILITY;

public class UpdateVisibilityAction implements ProjectsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final Indexers indexers;
  private final UuidFactory uuidFactory;
  private final Configuration configuration;

  public UpdateVisibilityAction(DbClient dbClient, UserSession userSession, Indexers indexers, UuidFactory uuidFactory, Configuration configuration) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.indexers = indexers;
    this.uuidFactory = uuidFactory;
    this.configuration = configuration;
  }

  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ProjectsWsParameters.ACTION_UPDATE_VISIBILITY)
      .setDescription("Updates visibility of a project, application or a portfolio.<br>" +
        "Requires 'Project administer' permission on the specified entity")
      .setSince("6.4")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_PROJECT)
      .setDescription("Project, application or portfolio key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setRequired(true);

    action.createParam(PARAM_VISIBILITY)
      .setDescription("New visibility")
      .setPossibleValues(Visibility.getLabels())
      .setRequired(true);
  }

  private void validateRequest(DbSession dbSession, EntityDto entityDto) {
    boolean isGlobalAdmin = userSession.isSystemAdministrator();
    boolean isProjectAdmin = userSession.hasEntityPermission(ADMIN, entityDto);
    boolean allowChangingPermissionsByProjectAdmins = configuration.getBoolean(CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_PROPERTY)
      .orElse(CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_DEFAULT_VALUE);
    if (!isProjectAdmin || (!isGlobalAdmin && !allowChangingPermissionsByProjectAdmins)) {
      throw insufficientPrivilegesException();
    }
    //This check likely can be removed when we remove the column 'private' from components table
    checkRequest(noPendingTask(dbSession, entityDto.getKey()), "Component visibility can't be changed as long as it has background task(s) pending or in progress");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    String entityKey = request.mandatoryParam(PARAM_PROJECT);
    boolean changeToPrivate = Visibility.isPrivate(request.mandatoryParam(PARAM_VISIBILITY));

    try (DbSession dbSession = dbClient.openSession(false)) {
      EntityDto entityDto = dbClient.entityDao().selectByKey(dbSession, entityKey)
        .orElseThrow(() -> BadRequestException.create("Component must be a project, a portfolio or an application"));

      validateRequest(dbSession, entityDto);
      if (changeToPrivate != entityDto.isPrivate()) {
        setPrivateForRootComponentUuid(dbSession, entityDto, changeToPrivate);
        if (changeToPrivate) {
          updatePermissionsToPrivate(dbSession, entityDto);
        } else {
          updatePermissionsToPublic(dbSession, entityDto);
        }
        indexers.commitAndIndexEntities(dbSession, singletonList(entityDto), Indexers.EntityEvent.PERMISSION_CHANGE);
      }

      response.noContent();
    }
  }

  private void setPrivateForRootComponentUuid(DbSession dbSession, EntityDto entity, boolean newIsPrivate) {
    Optional<BranchDto> branchDto = dbClient.branchDao().selectMainBranchByProjectUuid(dbSession, entity.getUuid());
    String branchUuid = branchDto.isPresent() ? branchDto.get().getUuid() : entity.getUuid();
    dbClient.componentDao().setPrivateForBranchUuid(dbSession, branchUuid, newIsPrivate, entity.getKey(), entity.getQualifier(), entity.getName());

    if (entity.isProjectOrApp()) {
      dbClient.projectDao().updateVisibility(dbSession, entity.getUuid(), newIsPrivate);

      dbClient.branchDao().selectByProjectUuid(dbSession, entity.getUuid()).stream()
        .filter(branch -> !branch.isMain())
        .forEach(branch -> dbClient.componentDao().setPrivateForBranchUuidWithoutAuditLog(dbSession, branch.getUuid(), newIsPrivate));
    } else {
      dbClient.portfolioDao().updateVisibilityByPortfolioUuid(dbSession, entity.getUuid(), newIsPrivate);
    }
  }

  private boolean noPendingTask(DbSession dbSession, String entityKey) {
    EntityDto entityDto = dbClient.entityDao().selectByKey(dbSession, entityKey).orElseThrow(() -> new IllegalStateException("Can't find entity " + entityKey));
    return dbClient.ceQueueDao().selectByEntityUuid(dbSession, entityDto.getUuid()).isEmpty();
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
    dbClient.userPermissionDao().insert(dbSession, new UserPermissionDto(Uuids.create(), permission, userId.getUuid(), entity.getUuid()),
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
