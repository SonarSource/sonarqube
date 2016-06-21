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
package org.sonar.server.authentication.ws;

import com.google.common.io.Resources;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;

public class AuthenticationWs implements WebService {

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/authentication");
    controller.setDescription("Handle authentication.");

    defineLoginAction(controller);
    defineValidateAction(controller);

    controller.done();
  }

  private void defineValidateAction(NewController controller) {
    NewAction action = controller.createAction("validate")
      .setDescription("Check credentials.")
      .setSince("3.3")
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(this.getClass(), "example-validate.json"));

    RailsHandler.addFormatParam(action);
  }

  private static void defineLoginAction(NewController controller) {
    NewAction action = controller.createAction("login")
      .setDescription("Authenticate a user.")
      .setSince("6.0")
      .setPost(true)
      .setHandler((request, response) -> {
        // This action will never be called as it's defined as a servlet filter
      });
    action.createParam("login")
      .setDescription("Login of the user")
      .setRequired(true);
    action.createParam("password")
      .setDescription("Password of the user")
      .setRequired(true);
  }

}
