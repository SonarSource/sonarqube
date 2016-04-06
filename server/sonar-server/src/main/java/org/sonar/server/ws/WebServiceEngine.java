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
package org.sonar.server.ws;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.sonar.api.i18n.I18n;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.LocalConnector;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.internal.ValidatingRequest;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.Errors;
import org.sonar.server.exceptions.Message;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.MediaTypes;

import static java.lang.String.format;
import static org.sonar.server.ws.RequestVerifier.verifyRequest;

/**
 * @since 4.2
 */
@ServerSide
public class WebServiceEngine implements LocalConnector, Startable {

  private final WebService.Context context;
  private final I18n i18n;
  private final UserSession userSession;

  public WebServiceEngine(WebService[] webServices, I18n i18n, UserSession userSession) {
    this.userSession = userSession;
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

  @Override
  public LocalResponse call(LocalRequest request) {
    String controller = StringUtils.substringBeforeLast(request.getPath(), "/");
    String action = StringUtils.substringAfterLast(request.getPath(), "/");
    DefaultLocalResponse localResponse = new DefaultLocalResponse();
    execute(new LocalRequestAdapter(request), localResponse, controller, action);
    return localResponse;
  }

  public void execute(Request request, Response response, String controllerPath, String actionKey) {
    try {
      WebService.Action action = getAction(controllerPath, actionKey);
      if (request instanceof ValidatingRequest) {
        ((ValidatingRequest) request).setAction(action);
        ((ValidatingRequest) request).setLocalConnector(this);
      }
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
    String actionKeyWithoutFormatSuffix = actionKey.contains(".") ?
      actionKey.substring(0, actionKey.lastIndexOf('.'))
      : actionKey;
    WebService.Controller controller = context.controller(controllerPath);
    if (controller == null) {
      throw new BadRequestException(format("Unknown web service: %s", controllerPath));
    }
    WebService.Action action = controller.action(actionKeyWithoutFormatSuffix);
    if (action == null) {
      throw new BadRequestException(format("Unknown action: %s/%s", controllerPath, actionKeyWithoutFormatSuffix));
    }
    return action;
  }

  private void sendErrors(Response response, int status, Errors errors) {
    Response.Stream stream = response.stream();
    if (stream instanceof ServletResponse.ServletStream) {
      ((ServletResponse.ServletStream) stream).reset();
    }
    stream.setStatus(status);
    stream.setMediaType(MediaTypes.JSON);
    JsonWriter json = JsonWriter.of(new OutputStreamWriter(stream.output(), StandardCharsets.UTF_8));

    try {
      json.beginObject();
      errors.writeJson(json, i18n, userSession.locale());
      json.endObject();
    } finally {
      // TODO if close() fails, the runtime exception should not hide the
      // potential exception raised in the try block.
      json.close();
    }
  }
}
