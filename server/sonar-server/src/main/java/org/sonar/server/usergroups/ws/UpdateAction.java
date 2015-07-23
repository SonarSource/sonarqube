/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.usergroups.ws;

import java.util.Arrays;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static org.sonar.db.MyBatis.closeQuietly;
import static org.sonar.server.usergroups.ws.GroupUpdater.DESCRIPTION_MAX_LENGTH;
import static org.sonar.server.usergroups.ws.GroupUpdater.NAME_MAX_LENGTH;
import static org.sonar.server.usergroups.ws.GroupUpdater.PARAM_DESCRIPTION;
import static org.sonar.server.usergroups.ws.GroupUpdater.PARAM_ID;
import static org.sonar.server.usergroups.ws.GroupUpdater.PARAM_NAME;

public class UpdateAction implements UserGroupsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GroupUpdater groupUpdater;

  public UpdateAction(DbClient dbClient, UserSession userSession, GroupUpdater groupUpdater) {
    this.dbClient = dbClient;
    this.groupUpdater = groupUpdater;
    this.userSession = userSession;
  }

  @Override
  public void define(NewController context) {
    NewAction action = context.createAction("update")
      .setDescription("Update a group.")
      .setHandler(this)
      .setPost(true)
      .setResponseExample(getClass().getResource("example-update.json"))
      .setSince("5.2");

    action.createParam(PARAM_ID)
      .setDescription("Identifier of the group.")
      .setExampleValue("42")
      .setRequired(true);

    action.createParam(PARAM_NAME)
      .setDescription(String.format("New name for the group. A group name cannot be larger than %d characters and must be unique. " +
        "The value 'anyone' (whatever the case) is reserved and cannot be used.", NAME_MAX_LENGTH))
      .setExampleValue("sonar-users");

    action.createParam(PARAM_DESCRIPTION)
      .setDescription(String.format("New description for the group. A group description cannot be larger than %d characters.", DESCRIPTION_MAX_LENGTH))
      .setExampleValue("Default group for new users");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);

    Long groupId = request.mandatoryParamAsLong(PARAM_ID);
    String name = request.param(PARAM_NAME);
    String description = request.param(PARAM_DESCRIPTION);

    DbSession dbSession = dbClient.openSession(false);
    try {
      groupUpdater.checkNameIsUnique(name, dbSession);
      GroupDto group = dbClient.groupDao().selectById(dbSession, groupId);
      if (group == null) {
        throw new NotFoundException(String.format("Could not find a user group with id '%s'.", groupId));
      }
      if (name != null) {
        groupUpdater.validateName(name);
        group.setName(name);
      }
      if (description != null) {
        groupUpdater.validateDescription(description);
        group.setDescription(description);
      }
      dbClient.groupDao().update(dbSession, group);
      dbSession.commit();

      JsonWriter json = response.newJsonWriter().beginObject();
      groupUpdater.writeGroup(json, group, dbClient.groupMembershipDao().countUsersByGroups(dbSession, Arrays.asList(groupId)).get(group.getName()));
      json.endObject().close();
    } finally {
      closeQuietly(dbSession);
    }
  }
}
