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
import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.catalina.connector.ClientAbortException;
import org.picocontainer.Startable;
import org.sonar.api.i18n.I18n;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.LocalConnector;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.internal.ValidatingRequest;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.Errors;
import org.sonar.server.exceptions.Message;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.MediaTypes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.substring;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;
import static org.sonar.server.ws.RequestVerifier.verifyRequest;
import static org.sonar.server.ws.ServletRequest.SUPPORTED_MEDIA_TYPES_BY_URL_SUFFIX;

/**
 * @since 4.2
 */
@ServerSide
public class WebServiceEngine implements LocalConnector, Startable {

  private static final Logger LOGGER = Loggers.get(WebServiceEngine.class);

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

  List<WebService.Controller> controllers() {
    return context.controllers();
  }

  @Override
  public LocalResponse call(LocalRequest request) {
    DefaultLocalResponse localResponse = new DefaultLocalResponse();
    execute(new LocalRequestAdapter(request), localResponse);
    return localResponse;
  }

  public void execute(Request request, Response response) {
    try {
      ActionExtractor actionExtractor = new ActionExtractor(request.getPath());
      WebService.Action action = getAction(actionExtractor.getController(), actionExtractor.getAction());
      if (request instanceof ValidatingRequest) {
        ((ValidatingRequest) request).setAction(action);
        ((ValidatingRequest) request).setLocalConnector(this);
      }
      checkActionExtension(actionExtractor.getExtension());
      verifyRequest(action, request);
      action.handler().handle(request, response);
    } catch (IllegalArgumentException e) {
      sendErrors(response, 400, new Errors().add(Message.of(e.getMessage())));
    } catch (BadRequestException e) {
      sendErrors(response, 400, e.errors());
    } catch (ServerException e) {
      sendErrors(response, e.httpCode(), new Errors().add(Message.of(e.getMessage())));
    } catch (Exception e) {
      Throwable cause = e.getCause();
      if (cause != null && cause instanceof ClientAbortException) {
        // Request has been aborted by the client, nothing can been done as Tomcat has committed the response
        LOGGER.warn("Request {} has been aborted by client, error is '{}'", request, e.getMessage());
        return;
      }
      LOGGER.error("Fail to process request " + request, e);
      sendErrors(response, 500, new Errors().add(Message.of(e.getMessage())));
    }
  }

  private WebService.Action getAction(String controllerPath, String actionKey) {
    WebService.Controller controller = context.controller(controllerPath);
    if (controller == null) {
      throw new BadRequestException(format("Unknown controller: %s", controllerPath));
    }
    WebService.Action action = controller.action(actionKey);
    if (action == null) {
      throw new BadRequestException(format("Unknown action: %s/%s", controllerPath, actionKey));
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

  private static void checkActionExtension(@Nullable String actionExtension) {
    if (isNullOrEmpty(actionExtension)) {
      return;
    }
    checkArgument(SUPPORTED_MEDIA_TYPES_BY_URL_SUFFIX.get(actionExtension.toLowerCase(Locale.ENGLISH)) != null, "Unknown action extension: %s", actionExtension);
  }

  private static class ActionExtractor {
    private static final String SLASH = "/";
    private static final String POINT = ".";

    private final String controller;
    private final String action;
    private final String extension;

    ActionExtractor(String path) {
      String pathWithoutExtension = substringBeforeLast(path, POINT);
      this.controller = extractController(pathWithoutExtension);
      this.action = substringAfterLast(pathWithoutExtension, SLASH);
      checkArgument(!action.isEmpty(), "Url is incorrect : '%s'", path);
      this.extension = substringAfterLast(path, POINT);
    }

    private static String extractController(String path) {
      String controller = substringBeforeLast(path, SLASH);
      if (controller.startsWith(SLASH)) {
        return substring(controller, 1);
      }
      return controller;
    }

    String getController() {
      return controller;
    }

    String getAction() {
      return action;
    }

    @CheckForNull
    String getExtension() {
      return extension;
    }
  }

}
