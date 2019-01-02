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
package org.sonar.server.permission.ws;

import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.permission.ProjectId;
import org.sonar.server.permission.UserId;
import org.sonar.server.permission.UserPermissionChange;
import org.sonar.server.user.UserSession;

import static java.util.Collections.singletonList;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkProjectAdmin;
import static org.sonar.server.permission.ws.WsParameters.createOrganizationParameter;
import static org.sonar.server.permission.ws.WsParameters.createProjectParameters;
import static org.sonar.server.permission.ws.WsParameters.createUserLoginParameter;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_USER_LOGIN;

public class RemoveUserAction implements PermissionsWsAction {

  public static final String ACTION = "remove_user";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionUpdater permissionUpdater;
  private final PermissionWsSupport wsSupport;
  private final WsParameters wsParameters;
  private final PermissionService permissionService;

  public RemoveUserAction(DbClient dbClient, UserSession userSession, PermissionUpdater permissionUpdater, PermissionWsSupport wsSupport,
    WsParameters wsParameters, PermissionService permissionService) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.permissionUpdater = permissionUpdater;
    this.wsSupport = wsSupport;
    this.wsParameters = wsParameters;
    this.permissionService = permissionService;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setDescription("Remove permission from a user.<br /> " +
        "This service defaults to global permissions, but can be limited to project permissions by providing project id or project key.<br /> " +
        "Requires one of the following permissions:" +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified project</li>" +
        "</ul>")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    wsParameters.createPermissionParameter(action);
    createUserLoginParameter(action);
    createProjectParameters(action);
    createOrganizationParameter(action).setSince("6.2");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserId user = wsSupport.findUser(dbSession, request.mandatoryParam(PARAM_USER_LOGIN));
      Optional<ProjectId> projectId = wsSupport.findProjectId(dbSession, request);
      OrganizationDto org = wsSupport.findOrganization(dbSession, request.param(PARAM_ORGANIZATION));

      checkProjectAdmin(userSession, org.getUuid(), projectId);

      PermissionChange change = new UserPermissionChange(
        PermissionChange.Operation.REMOVE,
        org.getUuid(),
        request.mandatoryParam(PARAM_PERMISSION),
        projectId.orElse(null),
        user, permissionService);
      permissionUpdater.apply(dbSession, singletonList(change));
      response.noContent();
    }
  }
}
