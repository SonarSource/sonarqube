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
package org.sonar.server.project.ws;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchMapper;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentMapper;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserId;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.es.ProjectIndexers;
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
  private static final Set<String> AUTHORIZED_QUALIFIERS = ImmutableSet.of(Qualifiers.PROJECT, Qualifiers.VIEW, Qualifiers.APP);

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;
  private final ProjectIndexers projectIndexers;
  private final UuidFactory uuidFactory;
  private final Configuration configuration;

  public UpdateVisibilityAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession,
    ProjectIndexers projectIndexers, UuidFactory uuidFactory, Configuration configuration) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
    this.projectIndexers = projectIndexers;
    this.uuidFactory = uuidFactory;
    this.configuration = configuration;
  }

  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ProjectsWsParameters.ACTION_UPDATE_VISIBILITY)
      .setDescription("Updates visibility of a project or view.<br>" +
        "Requires 'Project administer' permission on the specified project or view")
      .setSince("6.4")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setRequired(true);

    action.createParam(PARAM_VISIBILITY)
      .setDescription("New visibility")
      .setPossibleValues(Visibility.getLabels())
      .setRequired(true);
  }

  private void validateRequest(DbSession dbSession, ComponentDto component) {
    checkRequest(component.isRootProject() && AUTHORIZED_QUALIFIERS.contains(component.qualifier()), "Component must be a project, a portfolio or an application");
    boolean isGlobalAdmin = userSession.isSystemAdministrator();
    boolean isProjectAdmin = userSession.hasComponentPermission(ADMIN, component);
    boolean allowChangingPermissionsByProjectAdmins = configuration.getBoolean(CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_PROPERTY)
      .orElse(CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_DEFAULT_VALUE);
    if (!isProjectAdmin || (!isGlobalAdmin && !allowChangingPermissionsByProjectAdmins)) {
      throw insufficientPrivilegesException();
    }
    checkRequest(noPendingTask(dbSession, component), "Component visibility can't be changed as long as it has background task(s) pending or in progress");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    String projectKey = request.mandatoryParam(PARAM_PROJECT);
    boolean changeToPrivate = Visibility.isPrivate(request.mandatoryParam(PARAM_VISIBILITY));

    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto component = componentFinder.getByKey(dbSession, projectKey);
      validateRequest(dbSession, component);
      if (changeToPrivate != component.isPrivate()) {
        setPrivateForRootComponentUuid(dbSession, component, changeToPrivate);
        if (changeToPrivate) {
          updatePermissionsToPrivate(dbSession, component);
        } else {
          updatePermissionsToPublic(dbSession, component);
        }
        projectIndexers.commitAndIndexComponents(dbSession, singletonList(component), ProjectIndexer.Cause.PERMISSION_CHANGE);
      }

      response.noContent();
    }
  }

  private void setPrivateForRootComponentUuid(DbSession dbSession, ComponentDto component, boolean isPrivate) {
    String uuid = component.uuid();
    dbClient.componentDao().setPrivateForRootComponentUuid(dbSession, uuid, isPrivate, component.getKey(), component.qualifier(), component.name());

    if (component.qualifier().equals(Qualifiers.PROJECT) || component.qualifier().equals(Qualifiers.APP)) {
      dbClient.projectDao().updateVisibility(dbSession, uuid, isPrivate);
    }

    ComponentMapper mapper = dbSession.getMapper(ComponentMapper.class);
    dbSession.getMapper(BranchMapper.class).selectByProjectUuid(uuid).stream()
      .filter(branch -> !uuid.equals(branch.getUuid()))
      .forEach(branch -> mapper.setPrivateForRootComponentUuid(branch.getUuid(), isPrivate));
  }

  private boolean noPendingTask(DbSession dbSession, ComponentDto rootComponent) {
    // FIXME this is probably broken in case a branch is passed to the WS
    return dbClient.ceQueueDao().selectByMainComponentUuid(dbSession, rootComponent.uuid()).isEmpty();
  }

  private void updatePermissionsToPrivate(DbSession dbSession, ComponentDto component) {
    // delete project permissions for group AnyOne
    dbClient.groupPermissionDao().deleteByRootComponentUuidForAnyOne(dbSession, component);
    // grant UserRole.CODEVIEWER and UserRole.USER to any group or user with at least one permission on project
    PUBLIC_PERMISSIONS.forEach(permission -> {
      dbClient.groupPermissionDao().selectGroupUuidsWithPermissionOnProjectBut(dbSession, component.uuid(), permission)
        .forEach(group -> insertProjectPermissionOnGroup(dbSession, component, permission, group));
      dbClient.userPermissionDao().selectUserIdsWithPermissionOnProjectBut(dbSession, component.uuid(), permission)
        .forEach(userUuid -> insertProjectPermissionOnUser(dbSession, component, permission, userUuid));
    });
  }

  private void insertProjectPermissionOnUser(DbSession dbSession, ComponentDto component, String permission, UserId userId) {
    dbClient.userPermissionDao().insert(dbSession, new UserPermissionDto(Uuids.create(), permission, userId.getUuid(), component.uuid()),
      component, userId, null);
  }

  private void insertProjectPermissionOnGroup(DbSession dbSession, ComponentDto component, String permission, String groupUuid) {
    String groupName = ofNullable(dbClient.groupDao().selectByUuid(dbSession, groupUuid)).map(GroupDto::getName).orElse(null);
    dbClient.groupPermissionDao().insert(dbSession, new GroupPermissionDto()
      .setUuid(uuidFactory.create())
      .setComponentUuid(component.uuid())
      .setGroupUuid(groupUuid)
      .setGroupName(groupName)
      .setRole(permission)
      .setComponentName(component.name()), component, null);
  }

  private void updatePermissionsToPublic(DbSession dbSession, ComponentDto component) {
    PUBLIC_PERMISSIONS.forEach(permission -> {
      // delete project group permission for UserRole.CODEVIEWER and UserRole.USER
      dbClient.groupPermissionDao().deleteByRootComponentUuidAndPermission(dbSession, permission, component);
      // delete project user permission for UserRole.CODEVIEWER and UserRole.USER
      dbClient.userPermissionDao().deleteProjectPermissionOfAnyUser(dbSession, permission, component);
    });
  }

}
