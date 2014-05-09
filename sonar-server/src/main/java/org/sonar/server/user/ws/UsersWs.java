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

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/users")
      .setSince("3.6")
      .setDescription("Users management");

    defineSearchAction(controller);
    defineCreateAction(controller);
    defineUpdateAction(controller);
    defineDeactivateAction(controller);

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
      .setBooleanPossibleValues();

    action.createParam("logins")
      .setDescription("Comma-separated list of user logins")
      .setExampleValue("admin,sbrandhof");

    RailsHandler.addFormatParam(action);
  }

  private void defineCreateAction(NewController controller) {
    NewAction action = controller.createAction("create")
      .setDescription("Create a user. Requires Administer System permission")
      .setSince("3.7")
      .setPost(true)
      .setHandler(RailsHandler.INSTANCE);

    action.createParam("login")
      .setDescription("User login")
      .setRequired(true)
      .setExampleValue("myuser");

    action.createParam("password")
      .setDescription("User password")
      .setRequired(true)
      .setExampleValue("mypassword");

    action.createParam("password_confirmation")
      .setDescription("Must be the same value as \"password\"")
      .setRequired(true)
      .setExampleValue("mypassword");

    action.createParam("name")
      .setDescription("User name")
      .setExampleValue("My Name");

    action.createParam("email")
      .setDescription("User email")
      .setExampleValue("myname@email.com");

    RailsHandler.addFormatParam(action);
  }

  private void defineUpdateAction(NewController controller) {
    NewAction action = controller.createAction("update")
      .setDescription("Update a user. Requires Administer System permission")
      .setSince("3.7")
      .setPost(true)
      .setHandler(RailsHandler.INSTANCE);

    action.createParam("login")
      .setDescription("User login")
      .setRequired(true)
      .setExampleValue("myuser");

    action.createParam("name")
      .setDescription("User name")
      .setExampleValue("My New Name");

    action.createParam("email")
      .setDescription("User email")
      .setExampleValue("mynewname@email.com");

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
