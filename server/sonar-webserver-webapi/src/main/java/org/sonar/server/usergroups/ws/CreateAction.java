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
package org.sonar.server.usergroups.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.server.common.group.service.GroupService;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.UserGroups;

import static java.lang.String.format;
import static org.sonar.api.user.UserGroupValidation.GROUP_NAME_MAX_LENGTH;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.server.usergroups.ws.GroupWsSupport.DESCRIPTION_MAX_LENGTH;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_DESCRIPTION;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;
import static org.sonar.server.usergroups.ws.GroupWsSupport.toProtobuf;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class CreateAction implements UserGroupsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GroupService groupService;
  private final ManagedInstanceChecker managedInstanceChecker;

  public CreateAction(DbClient dbClient, UserSession userSession, GroupService groupService, ManagedInstanceChecker managedInstanceService) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.groupService = groupService;
    this.managedInstanceChecker = managedInstanceService;
  }

  @Override
  public void define(NewController controller) {
    NewAction action = controller.createAction("create")
      .setDescription("Create a group.<br>" +
        "Requires the following permission: 'Administer System'.")
      .setHandler(this)
      .setPost(true)
      .setResponseExample(getClass().getResource("create-example.json"))
      .setSince("5.2")
      .setDeprecatedSince("10.4")
      .setChangelog(
        new Change("10.4", "Deprecated. Use POST /api/v2/authorizations/groups instead"),
        new Change("8.4", "Field 'id' format in the response changes from integer to string."));

    action.createParam(PARAM_GROUP_NAME)
      .setRequired(true)
      .setMaximumLength(GROUP_NAME_MAX_LENGTH)
      .setDescription(format("Name for the new group. A group name cannot be larger than %d characters and must be unique. " +
        "The value 'anyone' (whatever the case) is reserved and cannot be used.", GROUP_NAME_MAX_LENGTH))
      .setExampleValue("sonar-users");

    action.createParam(PARAM_GROUP_DESCRIPTION)
      .setMaximumLength(DESCRIPTION_MAX_LENGTH)
      .setDescription(format("Description for the new group. A group description cannot be larger than %d characters.", DESCRIPTION_MAX_LENGTH))
      .setExampleValue("Default group for new users");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {

    try (DbSession dbSession = dbClient.openSession(false)) {
      userSession.checkPermission(ADMINISTER);
      managedInstanceChecker.throwIfInstanceIsManaged();
      String groupName = request.mandatoryParam(PARAM_GROUP_NAME);
      String groupDescription = request.param(PARAM_GROUP_DESCRIPTION);
      GroupDto group = groupService.createGroup(dbSession, groupName, groupDescription).groupDto();
      dbSession.commit();
      writeResponse(request, response, group);
    }
  }

  private static void writeResponse(Request request, Response response, GroupDto group) {
    UserGroups.CreateWsResponse.Builder respBuilder = UserGroups.CreateWsResponse.newBuilder();
    // 'default' is always false as it's not possible to create a default group
    respBuilder.setGroup(toProtobuf(group, 0, false));
    writeProtobuf(respBuilder.build(), request, response);
  }
}
