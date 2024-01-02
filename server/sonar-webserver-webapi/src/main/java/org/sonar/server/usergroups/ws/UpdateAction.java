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
import org.sonar.api.user.UserGroupValidation;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserMembershipQuery;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.UserGroups;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.sonar.api.user.UserGroupValidation.GROUP_NAME_MAX_LENGTH;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_CURRENT_NAME;
import static org.sonar.server.usergroups.ws.GroupWsSupport.DESCRIPTION_MAX_LENGTH;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_DESCRIPTION;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_ID;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;
import static org.sonar.server.usergroups.ws.GroupWsSupport.toProtobuf;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class UpdateAction implements UserGroupsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GroupWsSupport support;

  public UpdateAction(DbClient dbClient, UserSession userSession, GroupWsSupport support) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.support = support;
  }

  @Override
  public void define(NewController context) {
    NewAction action = context.createAction("update")
      .setDescription("Update a group.<br>" +
        "Requires the following permission: 'Administer System'.")
      .setHandler(this)
      .setPost(true)
      .setResponseExample(getClass().getResource("update.example.json"))
      .setSince("5.2")
      .setChangelog(
        new Change("8.5", "Parameter 'id' deprecated in favor of 'currentName'"),
        new Change("8.4", "Parameter 'id' format changes from integer to string"),
        new Change("6.4", "The default group is no longer editable"));

    action.createParam(PARAM_GROUP_ID)
      .setDescription("Identifier of the group. Use '" + PARAM_CURRENT_NAME + "' instead.")
      .setExampleValue(UUID_EXAMPLE_01)
      .setDeprecatedSince("8.5");

    action.createParam(PARAM_CURRENT_NAME)
      .setDescription("Name of the group to be updated. Mandatory unless '" + PARAM_GROUP_ID + "' is used.")
      .setExampleValue(UUID_EXAMPLE_01)
      .setSince("8.5");

    action.createParam(PARAM_GROUP_NAME)
      .setMaximumLength(GROUP_NAME_MAX_LENGTH)
      .setDescription(format("New optional name for the group. A group name cannot be larger than %d characters and must be unique. " +
        "Value 'anyone' (whatever the case) is reserved and cannot be used. If value is empty or not defined, then name is not changed.", GROUP_NAME_MAX_LENGTH))
      .setExampleValue("my-group");

    action.createParam(PARAM_GROUP_DESCRIPTION)
      .setMaximumLength(DESCRIPTION_MAX_LENGTH)
      .setDescription(format("New optional description for the group. A group description cannot be larger than %d characters. " +
        "If value is not defined, then description is not changed.", DESCRIPTION_MAX_LENGTH))
      .setExampleValue("Default group for new users");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String groupUuid = request.param(PARAM_GROUP_ID);
      String currentName = request.param(PARAM_CURRENT_NAME);

      if ((groupUuid == null && currentName == null) || (groupUuid != null && currentName != null)) {
        throw new IllegalArgumentException(format("Need to specify one and only one of '%s' or '%s'", PARAM_GROUP_ID, PARAM_CURRENT_NAME));
      }

      GroupDto group;
      if (groupUuid != null) {
        group = ofNullable(dbClient.groupDao().selectByUuid(dbSession, groupUuid))
          .orElseThrow(() -> new NotFoundException(format("Could not find a user group with id '%s'.", groupUuid)));
      } else {
        group = dbClient.groupDao().selectByName(dbSession, currentName)
          .orElseThrow(() -> new NotFoundException(format("Could not find a user group with name '%s'.", currentName)));
      }

      userSession.checkPermission(ADMINISTER);
      support.checkGroupIsNotDefault(dbSession, group);

      boolean changed = false;
      String newName = request.param(PARAM_GROUP_NAME);
      if (newName != null) {
        changed = true;
        UserGroupValidation.validateGroupName(newName);
        support.checkNameDoesNotExist(dbSession, newName);
        group.setName(newName);
      }

      String description = request.param(PARAM_GROUP_DESCRIPTION);
      if (description != null) {
        changed = true;
        group.setDescription(description);
      }

      if (changed) {
        dbClient.groupDao().update(dbSession, group);
        dbSession.commit();
      }

      writeResponse(dbSession, request, response, group);
    }
  }

  private void writeResponse(DbSession dbSession, Request request, Response response, GroupDto group) {
    UserMembershipQuery query = UserMembershipQuery.builder()
      .groupUuid(group.getUuid())
      .membership(UserMembershipQuery.IN)
      .build();
    int membersCount = dbClient.groupMembershipDao().countMembers(dbSession, query);

    UserGroups.UpdateWsResponse.Builder respBuilder = UserGroups.UpdateWsResponse.newBuilder();
    // 'default' is always false as it's not possible to update a default group
    respBuilder.setGroup(toProtobuf(group, membersCount, false));
    writeProtobuf(respBuilder.build(), request, response);
  }

}
