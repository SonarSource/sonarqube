/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.project.Visibility;
import org.sonar.server.project.VisibilityService;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.client.project.ProjectsWsParameters;

import static org.sonar.api.CoreProperties.CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_DEFAULT_VALUE;
import static org.sonar.api.CoreProperties.CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_PROPERTY;
import static org.sonar.db.permission.ProjectPermission.ADMIN;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECT;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_VISIBILITY;

public class UpdateVisibilityAction implements ProjectsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final Configuration configuration;
  private final VisibilityService visibilityService;
  private final ManagedInstanceChecker managedInstanceChecker;

  public UpdateVisibilityAction(DbClient dbClient, UserSession userSession, Configuration configuration,
    VisibilityService visibilityService, ManagedInstanceChecker managedInstanceChecker) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.configuration = configuration;
    this.visibilityService = visibilityService;
    this.managedInstanceChecker = managedInstanceChecker;
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

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    String entityKey = request.mandatoryParam(PARAM_PROJECT);
    boolean changeToPrivate = Visibility.isPrivate(request.mandatoryParam(PARAM_VISIBILITY));

    try (DbSession dbSession = dbClient.openSession(false)) {
      EntityDto entityDto = dbClient.entityDao().selectByKey(dbSession, entityKey)
        .orElseThrow(() -> BadRequestException.create("Component must be a project, a portfolio or an application"));

      validateRequest(dbSession, entityDto);
      visibilityService.changeVisibility(entityDto, changeToPrivate);
      response.noContent();
    }
  }

  private void validateRequest(DbSession dbSession, EntityDto entityDto) {
    boolean isGlobalAdmin = userSession.isSystemAdministrator();
    boolean isProjectAdmin = userSession.hasEntityPermission(ADMIN, entityDto);
    boolean allowChangingPermissionsByProjectAdmins = configuration.getBoolean(CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_PROPERTY)
      .orElse(CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_DEFAULT_VALUE);
    if (!isProjectAdmin || (!isGlobalAdmin && !allowChangingPermissionsByProjectAdmins)) {
      throw insufficientPrivilegesException();
    }
    if (entityDto.isProject()) {
      managedInstanceChecker.throwIfProjectIsManaged(dbSession, entityDto.getUuid());
    }
  }

}
