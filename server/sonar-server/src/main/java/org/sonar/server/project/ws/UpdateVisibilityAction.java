/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchMapper;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentMapper;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.es.ProjectIndexers;
import org.sonar.server.project.Visibility;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.client.project.ProjectsWsParameters;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.sonar.api.web.UserRole.PUBLIC_PERMISSIONS;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECT;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_VISIBILITY;

public class UpdateVisibilityAction implements ProjectsWsAction {
  private static final Set<String> AUTHORIZED_QUALIFIERS = ImmutableSet.of(Qualifiers.PROJECT, Qualifiers.VIEW, Qualifiers.APP);

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;
  private final ProjectIndexers projectIndexers;
  private final ProjectsWsSupport projectsWsSupport;

  public UpdateVisibilityAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession,
    ProjectIndexers projectIndexers, ProjectsWsSupport projectsWsSupport) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
    this.projectIndexers = projectIndexers;
    this.projectsWsSupport = projectsWsSupport;
  }

  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ProjectsWsParameters.ACTION_UPDATE_VISIBILITY)
      .setDescription("Updates visibility of a project.<br>" +
        "Requires 'Project administer' permission on the specified project")
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

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    String projectKey = request.mandatoryParam(PARAM_PROJECT);
    boolean changeToPrivate = Visibility.isPrivate(request.mandatoryParam(PARAM_VISIBILITY));

    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto component = componentFinder.getByKey(dbSession, projectKey);
      checkRequest(component.isRootProject() && AUTHORIZED_QUALIFIERS.contains(component.qualifier()), "Component must be a project, a portfolio or an application");
      userSession.checkComponentPermission(UserRole.ADMIN, component);
      checkRequest(noPendingTask(dbSession, component), "Component visibility can't be changed as long as it has background task(s) pending or in progress");

      if (changeToPrivate != component.isPrivate()) {
        OrganizationDto organization = dbClient.organizationDao().selectByUuid(dbSession, component.getOrganizationUuid())
          .orElseThrow(() -> new IllegalStateException(format("Could not find organization with uuid '%s' of project '%s'", component.getOrganizationUuid(), projectKey)));
        projectsWsSupport.checkCanUpdateProjectsVisibility(organization, changeToPrivate);
        setPrivateForRootComponentUuid(dbSession, component.uuid(), changeToPrivate);
        if (changeToPrivate) {
          updatePermissionsToPrivate(dbSession, component);
        } else {
          updatePermissionsToPublic(dbSession, component);
        }
        projectIndexers.commitAndIndex(dbSession, singletonList(component), ProjectIndexer.Cause.PERMISSION_CHANGE);
      }

      response.noContent();
    }
  }

  private void setPrivateForRootComponentUuid(DbSession dbSession, String uuid, boolean isPrivate) {
    dbClient.componentDao().setPrivateForRootComponentUuid(dbSession, uuid, isPrivate);
    ComponentMapper mapper = dbSession.getMapper(ComponentMapper.class);
    dbSession.getMapper(BranchMapper.class).selectByProjectUuid(uuid)
      .stream()
      .filter(branch -> !uuid.equals(branch.getUuid()))
      .forEach(branch -> mapper.setPrivateForRootComponentUuid(branch.getUuid(), isPrivate));
  }

  private boolean noPendingTask(DbSession dbSession, ComponentDto rootComponent) {
    // FIXME this is probably broken in case a branch is passed to the WS
    return dbClient.ceQueueDao().selectByMainComponentUuid(dbSession, rootComponent.uuid()).isEmpty();
  }

  private void updatePermissionsToPrivate(DbSession dbSession, ComponentDto component) {
    // delete project permissions for group AnyOne
    dbClient.groupPermissionDao().deleteByRootComponentIdAndGroupId(dbSession, component.getId(), null);
    // grant UserRole.CODEVIEWER and UserRole.USER to any group or user with at least one permission on project
    PUBLIC_PERMISSIONS.forEach(permission -> {
      dbClient.groupPermissionDao().selectGroupIdsWithPermissionOnProjectBut(dbSession, component.getId(), permission)
        .forEach(groupId -> insertProjectPermissionOnGroup(dbSession, component, permission, groupId));
      dbClient.userPermissionDao().selectUserIdsWithPermissionOnProjectBut(dbSession, component.getId(), permission)
        .forEach(userId -> insertProjectPermissionOnUser(dbSession, component, permission, userId));
    });
  }

  private void insertProjectPermissionOnUser(DbSession dbSession, ComponentDto component, String permission, Integer userId) {
    dbClient.userPermissionDao().insert(dbSession, new UserPermissionDto(component.getOrganizationUuid(), permission, userId, component.getId()));
  }

  private void insertProjectPermissionOnGroup(DbSession dbSession, ComponentDto component, String permission, Integer groupId) {
    dbClient.groupPermissionDao().insert(dbSession, new GroupPermissionDto()
      .setOrganizationUuid(component.getOrganizationUuid())
      .setResourceId(component.getId())
      .setGroupId(groupId)
      .setRole(permission));
  }

  private void updatePermissionsToPublic(DbSession dbSession, ComponentDto component) {
    PUBLIC_PERMISSIONS.forEach(permission -> {
      // delete project group permission for UserRole.CODEVIEWER and UserRole.USER
      dbClient.groupPermissionDao().deleteByRootComponentIdAndPermission(dbSession, component.getId(), permission);
      // delete project user permission for UserRole.CODEVIEWER and UserRole.USER
      dbClient.userPermissionDao().deleteProjectPermissionOfAnyUser(dbSession, component.getId(), permission);
    });
  }

}
