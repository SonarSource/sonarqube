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
package org.sonar.server.permission.ws;

import java.util.List;
import java.util.Optional;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.permission.GroupPermissionChange;
import org.sonar.server.permission.GroupUuidOrAnyone;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.user.UserSession;

import static org.sonar.server.permission.ws.WsParameters.createGroupIdParameter;
import static org.sonar.server.permission.ws.WsParameters.createGroupNameParameter;
import static org.sonar.server.permission.ws.WsParameters.createProjectParameters;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;

public class AddGroupAction implements PermissionsWsAction {
  public static final String ACTION = "add_group";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionUpdater permissionUpdater;
  private final PermissionWsSupport wsSupport;
  private final WsParameters wsParameters;
  private final PermissionService permissionService;
  private final Logger logger = Loggers.get(AddGroupAction.class);

  public AddGroupAction(DbClient dbClient, UserSession userSession, PermissionUpdater permissionUpdater, PermissionWsSupport wsSupport,
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
      .setDescription("Add a permission to a group.<br /> " +
        "This service defaults to global permissions, but can be limited to project permissions by providing project id or project key.<br /> " +
        "The group name or group id must be provided. <br />" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified project</li>" +
        "</ul>")
      .setSince("5.2")
      .setChangelog(
        new Change("8.4", "Parameter 'groupId' is deprecated. Format changes from integer to string. Use 'name' instead."))
      .setPost(true)
      .setHandler(this);

    wsParameters.createPermissionParameter(action, "The permission you would like to grant to the group.");
    wsParameters.createOrganizationParameter(action);
    createGroupNameParameter(action);
    createGroupIdParameter(action);
    createProjectParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      GroupUuidOrAnyone group = wsSupport.findGroup(dbSession, request);
      Optional<ComponentDto> project = wsSupport.findProject(dbSession, request);
      wsSupport.checkPermissionManagementAccess(userSession, group.getOrganizationUuid(), project.orElse(null));
      logger.info("Grant Permission to a group:: permission {}, organizationUuid : {}, groupUuid: {}, user : {}",
              request.mandatoryParam(PARAM_PERMISSION), group.getOrganizationUuid(), group.getUuid(),
              userSession.getLogin());
      PermissionChange change = new GroupPermissionChange(
        PermissionChange.Operation.ADD,
        request.mandatoryParam(PARAM_PERMISSION),
        project.orElse(null),
        group, permissionService);
      permissionUpdater.apply(dbSession, List.of(change));
    }
    response.noContent();
  }
}
