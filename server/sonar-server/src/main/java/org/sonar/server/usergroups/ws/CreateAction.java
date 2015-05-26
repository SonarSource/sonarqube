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

import com.google.common.base.Preconditions;
import java.net.HttpURLConnection;
import javax.annotation.Nullable;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.user.GroupDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.user.UserSession;

import static org.sonar.core.persistence.MyBatis.closeQuietly;

public class CreateAction implements UserGroupsWsAction {

  private static final String PARAM_DESCRIPTION = "description";
  private static final String PARAM_NAME = "name";

  private static final int NAME_MAX_LENGTH = 255;
  private static final int DESCRIPTION_MAX_LENGTH = 200;

  private final DbClient dbClient;
  private final UserSession userSession;

  public CreateAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
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
      .setDescription("Name for the new group. A group name cannot be larger than 255 characters and must be unique. " +
        "The value 'anyone' (whatever the case) is reserved and cannot be used.")
      .setExampleValue("sonar-users")
      .setRequired(true);

    action.createParam(PARAM_DESCRIPTION)
      .setDescription("Description for the new group. A group description cannot be larger than 200 characters.")
      .setExampleValue("Default group for new users");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);

    String name = request.mandatoryParam(PARAM_NAME);
    String description = request.param(PARAM_DESCRIPTION);

    validateName(name);
    validateDescription(description);

    GroupDto newGroup = new GroupDto().setName(name).setDescription(description);
    DbSession session = dbClient.openSession(false);
    try {
      checkNameIsUnique(name, session);
      newGroup = dbClient.groupDao().insert(session, new GroupDto().setName(name).setDescription(description));
      session.commit();
    } finally {
      closeQuietly(session);
    }

    response.newJsonWriter().beginObject().name("group").beginObject()
      .prop("id", newGroup.getId().toString())
      .prop(PARAM_NAME, newGroup.getName())
      .prop(PARAM_DESCRIPTION, newGroup.getDescription())
      .prop("membersCount", 0)
      .endObject().endObject().close();
  }

  private void validateName(String name) {
    checkNameLength(name);
    checkNameNotAnyone(name);
  }

  private void checkNameLength(String name) {
    Preconditions.checkArgument(!name.isEmpty(), "Name cannot be empty");
    Preconditions.checkArgument(name.length() <= NAME_MAX_LENGTH, String.format("Name cannot be longer than %d characters", NAME_MAX_LENGTH));
  }

  private void checkNameNotAnyone(String name) {
    Preconditions.checkArgument(!DefaultGroups.isAnyone(name), String.format("Name '%s' is reserved (regardless of case)", DefaultGroups.ANYONE));
  }

  private void checkNameIsUnique(String name, DbSession session) {
    // TODO check in case sensitive way
    if (dbClient.groupDao().selectByKey(session, name) != null) {
      throw new ServerException(HttpURLConnection.HTTP_CONFLICT, String.format("Name '%s' is already taken", name));
    }
  }

  private void validateDescription(@Nullable String description) {
    if (description != null) {
      Preconditions.checkArgument(description.length() <= DESCRIPTION_MAX_LENGTH, String.format("Description cannot be longer than %d characters", DESCRIPTION_MAX_LENGTH));
    }
  }
}
