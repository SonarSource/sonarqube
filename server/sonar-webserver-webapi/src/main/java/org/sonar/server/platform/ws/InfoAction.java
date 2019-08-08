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
package org.sonar.server.platform.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.user.UserSession;

/**
 * Implementation of the {@code info} action for the System WebService.
 */
public class InfoAction implements SystemWsAction {
  private final SystemInfoWriter systemInfoWriter;
  private final UserSession userSession;

  public InfoAction(UserSession userSession, SystemInfoWriter systemInfoWriter) {
    this.userSession = userSession;
    this.systemInfoWriter = systemInfoWriter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("info")
      .setDescription("Get detailed information about system configuration.<br/>" +
        "Requires 'Administer' permissions.")
      .setSince("5.1")
      .setInternal(true)
      .setResponseExample(getClass().getResource("info-example.json"))
      .setHandler(this);

    action.setChangelog(
      new Change("5.5", "Becomes internal to easily update result")
    );
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();
    JsonWriter json = response.newJsonWriter();
    json.beginObject();
    systemInfoWriter.write(json);
    json.endObject();
    json.close();
  }

}
