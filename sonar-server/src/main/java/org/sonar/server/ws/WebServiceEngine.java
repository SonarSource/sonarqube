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

import javax.servlet.http.HttpServletResponse;
import java.io.StringWriter;
import java.util.List;

/**
 * @since 4.2
 */
public class WebServiceEngine implements ServerComponent, Startable {

  static class RequestException extends Exception {
    private final int httpStatus;

    RequestException(int httpStatus, String message) {
      super(message);
      this.httpStatus = httpStatus;
    }
  }

  private final WebService.Context context;

  public WebServiceEngine(WebService[] webServices) {
    context = new WebService.Context();
    for (WebService webService : webServices) {
      webService.define(context);
    }
  }

  public WebServiceEngine() {
    this(new WebService[0]);
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

    } catch (RequestException e) {
      sendError(e.httpStatus, e.getMessage(), response);

    } catch (Exception e) {
      // TODO support authentication exceptions and others...
      sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage(), response);
    }
  }

  private WebService.Action getAction(String controllerPath, String actionKey) throws RequestException {
    WebService.Controller controller = context.controller(controllerPath);
    if (controller == null) {
      throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, String.format("Unknown web service: %s", controllerPath));
    }
    WebService.Action action = controller.action(actionKey);
    if (action == null) {
      throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, String.format("Unknown action: %s/%s", controllerPath, actionKey));
    }
    return action;
  }

  private void verifyRequest(WebService.Action action, Request request) throws RequestException {
    if (request.isPost() != action.isPost()) {
      throw new RequestException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method POST is required");
    }
    // TODO verify required parameters
  }

  private void sendError(int status, String message, Response response) {
    response.setStatus(status);

    JsonWriter json = response.newJsonWriter();
    json.beginObject();
    json.name("errors").beginArray();
    json.beginObject().prop("msg", message).endObject();
    json.endArray();
    json.endObject();
    json.close();
  }
}
