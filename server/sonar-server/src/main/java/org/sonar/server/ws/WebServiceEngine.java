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
package org.sonar.server.ws;

import com.google.common.base.Charsets;
import org.picocontainer.Startable;
import org.sonar.api.ServerSide;
import org.sonar.api.i18n.I18n;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.internal.ValidatingRequest;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.Errors;
import org.sonar.server.exceptions.Message;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.plugins.MimeTypes;
import org.sonar.server.user.UserSession;

import javax.servlet.http.HttpServletResponse;

import java.io.OutputStreamWriter;
import java.util.List;

/**
 * @since 4.2
 */
@ServerSide
public class WebServiceEngine implements Startable {

  private final WebService.Context context;

  private final I18n i18n;

  public WebServiceEngine(WebService[] webServices, I18n i18n) {
    context = new WebService.Context();
    for (WebService webService : webServices) {
      webService.define(context);
    }
    this.i18n = i18n;
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

  public void execute(ValidatingRequest request, ServletResponse response,
    String controllerPath, String actionKey) {
    try {
      WebService.Action action = getAction(controllerPath, actionKey);
      request.setAction(action);
      verifyRequest(action, request);
      action.handler().handle(request, response);

    } catch (IllegalArgumentException e) {
      // TODO replace by BadRequestException in Request#mandatoryParam()
      sendErrors(response, 400, new Errors().add(Message.of(e.getMessage())));
    } catch (BadRequestException e) {
      sendErrors(response, 400, e.errors());
    } catch (ServerException e) {
      sendErrors(response, e.httpCode(), new Errors().add(Message.of(e.getMessage())));
    } catch (Exception e) {
      Loggers.get(getClass()).error("Fail to process request " + request, e);
      sendErrors(response, 500, new Errors().add(Message.of(e.getMessage())));
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
    // verify the HTTP verb
    if (action.isPost() && !"POST".equals(request.method())) {
      throw new ServerException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "HTTP method POST is required");
    }
  }

  private void sendErrors(ServletResponse response, int status, Errors errors) {
    ServletResponse.ServletStream stream = response.stream();
    stream.reset();
    stream.setStatus(status);
    stream.setMediaType(MimeTypes.JSON);
    JsonWriter json = JsonWriter.of(new OutputStreamWriter(stream.output(), Charsets.UTF_8));

    try {
      json.beginObject();
      errors.writeJson(json, i18n, UserSession.get().locale());
      json.endObject();
    } finally {
      // TODO if close() fails, the runtime exception should not hide the
      // potential exception raised in the try block.
      json.close();
    }
  }
}
