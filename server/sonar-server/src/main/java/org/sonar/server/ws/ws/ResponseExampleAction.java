/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.ws.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public class ResponseExampleAction implements WebServicesWsAction {
  private WebService.Context context;

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("response_example")
      .setDescription("Display web service response example")
      .setResponseExample(getClass().getResource("response_example-example.json"))
      .setSince("4.4")
      .setHandler(this);

    action.createParam("controller")
      .setRequired(true)
      .setDescription("Controller of the web service")
      .setExampleValue("api/issues");

    action.createParam("action")
      .setRequired(true)
      .setDescription("Action of the web service")
      .setExampleValue("search");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    checkState(context != null, "Webservice global context must be loaded before calling the action");

    String controllerKey = request.mandatoryParam("controller");
    WebService.Controller controller = context.controller(controllerKey);
    checkArgument(controller != null, "Controller does not exist: %s", controllerKey);

    String actionKey = request.mandatoryParam("action");
    WebService.Action action = controller.action(actionKey);
    checkArgument(action != null, "Action does not exist: %s", actionKey);

    if (action.responseExample() == null) {
      response.noContent();
      return;
    }

    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject()
        .prop("format", action.responseExampleFormat())
        .prop("example", action.responseExampleAsString())
        .endObject();
    }
  }

  @Override
  public void setContext(WebService.Context context) {
    this.context = context;
  }

}
