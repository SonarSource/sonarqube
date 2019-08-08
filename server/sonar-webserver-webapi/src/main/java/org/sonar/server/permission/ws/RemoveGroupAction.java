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
import org.sonar.server.permission.GroupPermissionChange;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.permission.ProjectId;
import org.sonar.server.user.UserSession;
import org.sonar.server.permission.GroupIdOrAnyone;

import static java.util.Arrays.asList;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkProjectAdmin;
import static org.sonar.server.permission.ws.WsParameters.createGroupIdParameter;
import static org.sonar.server.permission.ws.WsParameters.createGroupNameParameter;
import static org.sonar.server.permission.ws.WsParameters.createOrganizationParameter;
import static org.sonar.server.permission.ws.WsParameters.createProjectParameters;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;

public class RemoveGroupAction implements PermissionsWsAction {

  public static final String ACTION = "remove_group";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionUpdater permissionUpdater;
  private final PermissionWsSupport wsSupport;
  private final WsParameters wsParameters;
  private final PermissionService permissionService;

  public RemoveGroupAction(DbClient dbClient, UserSession userSession, PermissionUpdater permissionUpdater, PermissionWsSupport wsSupport,
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
      .setDescription("Remove a permission from a group.<br /> " +
        "This service defaults to global permissions, but can be limited to project permissions by providing project id or project key.<br /> " +
        "The group id or group name must be provided, not both.<br />" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified project</li>" +
        "</ul>")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    wsParameters.createPermissionParameter(action);
    createOrganizationParameter(action).setSince("6.2");
    createGroupNameParameter(action);
    createGroupIdParameter(action);
    createProjectParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      GroupIdOrAnyone group = wsSupport.findGroup(dbSession, request);
      Optional<ProjectId> projectId = wsSupport.findProjectId(dbSession, request);

      checkProjectAdmin(userSession, group.getOrganizationUuid(), projectId);

      PermissionChange change = new GroupPermissionChange(
        PermissionChange.Operation.REMOVE,
        request.mandatoryParam(PARAM_PERMISSION),
        projectId.orElse(null),
        group, permissionService);
      permissionUpdater.apply(dbSession, asList(change));
    }
    response.noContent();
  }
}
