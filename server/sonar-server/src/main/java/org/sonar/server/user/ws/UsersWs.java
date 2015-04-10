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

import com.google.common.io.Resources;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;

public class UsersWs implements WebService {

  private final BaseUsersWsAction[] actions;

  public UsersWs(BaseUsersWsAction... actions) {
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/users")
      .setSince("3.6")
      .setDescription("Users management");

    defineSearchAction(controller);
    defineDeactivateAction(controller);
    for (BaseUsersWsAction action : actions) {
      action.define(controller);
    }

    controller.done();
  }

  private void defineSearchAction(NewController controller) {
    NewAction action = controller.createAction("search")
      .setDescription("Get a list of users")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(this.getClass(), "example-search.json"));

    action.createParam("includeDeactivated")
      .setDescription("Include deactivated users")
      .setDefaultValue("false")
      .setPossibleValues("true", "false");

    action.createParam("logins")
      .setDescription("Comma-separated list of user logins")
      .setExampleValue("admin,sbrandhof");

    action.createParam("s")
      .setDescription("UTF-8 search query on login or name")
      .setExampleValue("bran");

    RailsHandler.addFormatParam(action);
  }

  private void defineDeactivateAction(NewController controller) {
    NewAction action = controller.createAction("deactivate")
      .setDescription("Deactivate a user. Requires Administer System permission")
      .setSince("3.7")
      .setPost(true)
      .setHandler(RailsHandler.INSTANCE);

    action.createParam("login")
      .setDescription("User login")
      .setRequired(true)
      .setExampleValue("myuser");

    RailsHandler.addFormatParam(action);
  }

}
