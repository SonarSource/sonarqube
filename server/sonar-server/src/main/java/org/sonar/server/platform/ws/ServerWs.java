/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.io.Resources;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;

public class ServerWs implements WebService {

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/server")
      .setDescription("Get system properties and upgrade db")
      .setSince("2.10");

    defineIndexAction(controller);
    defineSetupAction(controller);

    controller.done();
  }

  private void defineIndexAction(NewController controller) {
    NewAction action = controller.createAction("index")
      .setDescription("Get the server status:" +
        "<ul>" +
        "<li>UP</li>" +
        "<li>DOWN (generally for database connection failures)</li>" +
        "<li>SETUP (if the server must be upgraded)</li>" +
        "<li>MIGRATION_RUNNING (the upgrade process is currently running)</li>" +
        "</ul>")
      .setSince("2.10")
      .setHandler(RailsHandler.INSTANCE)
      .setInternal(true)
      .setResponseExample(Resources.getResource(this.getClass(), "example-index.json"));

    RailsHandler.addFormatParam(action);
  }

  private void defineSetupAction(NewController controller) {
    NewAction action = controller.createAction("setup")
      .setDescription("Upgrade the SonarQube database")
      .setSince("2.10")
      .setPost(true)
      .setInternal(true)
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(this.getClass(), "example-setup.json"));

    action.createParam("format")
      .setDescription("Response format")
      .setPossibleValues("json", "csv", "text");
  }

}
