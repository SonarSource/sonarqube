/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.ws;

import org.picocontainer.Startable;
import org.sonar.api.ServerComponent;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ServerException;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * @since 4.2
 */
public class WebServiceEngine implements ServerComponent, Startable {

  private final WebService.Context context;

  public WebServiceEngine(WebService[] webServices) {
    context = new WebService.Context();
    for (WebService webService : webServices) {
      webService.define(context);
    }
  }

  @Override
  public void start() {
    // Force execution of constructor to be sure that web services
    // are validated and initialized at server startup.
  }

  @Override
  public void stop() {
    // nothing
  }

  /**
   * Used by Ruby on Rails to add ws routes. See WEB_INF/lib/java_ws_routing.rb
   */
  public List<WebService.Controller> controllers() {
    return context.controllers();
  }

  public void execute(Request request, Response response,
                      String controllerPath, String actionKey) {
    try {
      WebService.Action action = getAction(controllerPath, actionKey);
      verifyRequest(action, request);
      action.handler().handle(request, response);

    } catch (ServerException e) {
      // TODO support ServerException l10n messages
      sendError(e.httpCode(), e.getMessage(), response);

    } catch (Exception e) {
      sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage(), response);
    }
  }

  private WebService.Action getAction(String controllerPath, String actionKey) {
    WebService.Controller controller = context.controller(controllerPath);
    if (controller == null) {
      throw new BadRequestException(String.format("Unknown web service: %s", controllerPath));
    }
    WebService.Action action = controller.action(actionKey);
    if (action == null) {
      throw new BadRequestException(String.format("Unknown action: %s/%s", controllerPath, actionKey));
    }
    return action;
  }

  private void verifyRequest(WebService.Action action, Request request) {
    if (request.isPost() != action.isPost()) {
      throw new ServerException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method POST is required");
    }
    // TODO verify required parameters
  }

  private void sendError(int status, String message, Response response) {
    response.setStatus(status);

    // Reset response by directly using the stream. Response#newJsonWriter()
    // must not be used because it potentially contains some partial response
    JsonWriter json = JsonWriter.of(new OutputStreamWriter(response.stream()));
    json.beginObject();
    json.name("errors").beginArray();
    json.beginObject().prop("msg", message).endObject();
    json.endArray();
    json.endObject();
    json.close();
  }
}
