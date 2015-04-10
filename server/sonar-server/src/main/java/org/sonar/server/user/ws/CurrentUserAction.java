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
package org.sonar.server.user.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.user.UserSession;

public class CurrentUserAction implements BaseUsersWsAction {

  @Override
  public void define(NewController context) {
    context.createAction("current")
      .setDescription("Get the details of the current authenticated user.")
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(getClass().getResource("example-current.json"))
      .setSince("5.2");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    UserSession session = UserSession.get();
    JsonWriter json = response.newJsonWriter().beginObject();

    writeUserDetails(json, session);

    json.endObject().close();
  }

  private void writeUserDetails(JsonWriter json, UserSession session) {
    json.prop("isLoggedIn", session.isLoggedIn())
      .prop("login", session.login())
      .prop("name", session.name());
    writePermissions(json, session);
  }

  private void writePermissions(JsonWriter json, UserSession session) {
    json.name("permissions").beginObject();
    writeGlobalPermissions(json, session);
    json.endObject();
  }

  private void writeGlobalPermissions(JsonWriter json, UserSession session) {
    json.name("global").beginArray();
    for (String permission : session.globalPermissions()) {
      json.value(permission);
    }
    json.endArray();
  }

}
