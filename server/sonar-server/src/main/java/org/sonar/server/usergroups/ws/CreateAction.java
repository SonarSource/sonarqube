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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.server.user.UserSession;

import static org.sonar.db.MyBatis.closeQuietly;
import static org.sonar.server.usergroups.ws.UserGroupUpdater.DESCRIPTION_MAX_LENGTH;
import static org.sonar.server.usergroups.ws.UserGroupUpdater.NAME_MAX_LENGTH;
import static org.sonar.server.usergroups.ws.UserGroupUpdater.PARAM_DESCRIPTION;
import static org.sonar.server.usergroups.ws.UserGroupUpdater.PARAM_NAME;

public class CreateAction implements UserGroupsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final UserGroupUpdater groupUpdater;

  public CreateAction(DbClient dbClient, UserSession userSession, UserGroupUpdater groupUpdater) {
    this.dbClient = dbClient;
    this.groupUpdater = groupUpdater;
    this.userSession = userSession;
  }

  @Override
  public void define(NewController context) {
    NewAction action = context.createAction("create")
      .setDescription("Create a group.")
      .setHandler(this)
      .setPost(true)
      .setResponseExample(getClass().getResource("example-create.json"))
      .setSince("5.2");

    action.createParam(PARAM_NAME)
      .setDescription(String.format("Name for the new group. A group name cannot be larger than %d characters and must be unique. " +
        "The value 'anyone' (whatever the case) is reserved and cannot be used.", NAME_MAX_LENGTH))
      .setExampleValue("sonar-users")
      .setRequired(true);

    action.createParam(PARAM_DESCRIPTION)
      .setDescription(String.format("Description for the new group. A group description cannot be larger than %d characters.", DESCRIPTION_MAX_LENGTH))
      .setExampleValue("Default group for new users");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);

    String name = request.mandatoryParam(PARAM_NAME);
    String description = request.param(PARAM_DESCRIPTION);

    groupUpdater.validateName(name);
    if (description != null) {
      groupUpdater.validateDescription(description);
    }

    DbSession session = dbClient.openSession(false);
    try {
      groupUpdater.checkNameIsUnique(name, session);
      GroupDto newGroup = dbClient.groupDao().insert(session, new GroupDto().setName(name).setDescription(description));
      session.commit();

      JsonWriter json = response.newJsonWriter().beginObject();
      groupUpdater.writeGroup(json, newGroup, 0);
      json.endObject().close();
    } finally {
      closeQuietly(session);
    }
  }
}
