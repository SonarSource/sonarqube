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
package org.sonar.server.ws;

import com.google.common.base.Throwables;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.catalina.connector.ClientAbortException;
import org.picocontainer.Startable;
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
import org.sonar.server.exceptions.ServerException;
import org.sonarqube.ws.MediaTypes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.substring;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;
import static org.sonar.server.ws.RequestVerifier.verifyRequest;
import static org.sonar.server.ws.ServletRequest.SUPPORTED_MEDIA_TYPES_BY_URL_SUFFIX;
import static org.sonar.server.ws.WsUtils.checkFound;

/**
 * @since 4.2
 */
@ServerSide
public class WebServiceEngine implements LocalConnector, Startable {

  private static final Logger LOGGER = Loggers.get(WebServiceEngine.class);

  private final WebService[] webServices;

  private WebService.Context context;

  public WebServiceEngine(WebService[] webServices) {
    this.webServices = webServices;
  }

  @Override
  public void start() {
    context = new WebService.Context();
    for (WebService webService : webServices) {
      webService.define(context);
    }
  }

  @Override
  public void stop() {
    // nothing
  }

  private WebService.Context getContext() {
    return requireNonNull(context, "Web services has not yet been initialized");
  }

  List<WebService.Controller> controllers() {
    return getContext().controllers();
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
      WebService.Action action = getAction(actionExtractor);
      checkFound(action, "Unknown url : %s", request.getPath());
      if (request instanceof ValidatingRequest) {
        ((ValidatingRequest) request).setAction(action);
        ((ValidatingRequest) request).setLocalConnector(this);
      }
      checkActionExtension(actionExtractor.getExtension());
      verifyRequest(action, request);
      action.handler().handle(request, response);
    } catch (IllegalArgumentException e) {
      sendErrors(request, response, e, 400, singletonList(e.getMessage()));
    } catch (BadRequestException e) {
      sendErrors(request, response, e, 400, e.errors());
    } catch (ServerException e) {
      sendErrors(request, response, e, e.httpCode(), singletonList(e.getMessage()));
    } catch (Exception e) {
      sendErrors(request, response, e, 500, singletonList("An error has occurred. Please contact your administrator"));
    }
  }

  @CheckForNull
  private WebService.Action getAction(ActionExtractor actionExtractor) {
    String controllerPath = actionExtractor.getController();
    String actionKey = actionExtractor.getAction();
    WebService.Controller controller = getContext().controller(controllerPath);
    return controller == null ? null : controller.action(actionKey);
  }

  private static void sendErrors(Request request, Response response, Exception exception, int status, List<String> errors) {
    if (isRequestAbortedByClient(exception)) {
      // do not pollute logs. We can't do anything -> use DEBUG level
      // see org.sonar.server.ws.ServletResponse#output()
      LOGGER.debug(String.format("Request %s has been aborted by client", request), exception);
      if (!isResponseCommitted(response)) {
        // can be useful for access.log
        response.stream().setStatus(299);
      }
      return;
    }

    if (status == 500) {
      // Sending exception message into response is a vulnerability. Error must be
      // displayed only in logs.
      LOGGER.error("Fail to process request " + request, exception);
    }

    Response.Stream stream = response.stream();
    if (isResponseCommitted(response)) {
      // status can't be changed
      LOGGER.debug(String.format("Request %s failed during response streaming", request), exception);
      return;
    }

    // response is not committed, status and content can be changed to return the error
    if (stream instanceof ServletResponse.ServletStream) {
      ((ServletResponse.ServletStream) stream).reset();
    }
    stream.setStatus(status);
    stream.setMediaType(MediaTypes.JSON);
    try (JsonWriter json = JsonWriter.of(new OutputStreamWriter(stream.output(), StandardCharsets.UTF_8))) {
      json.beginObject();
      writeErrors(json, errors);
      json.endObject();
    } catch (Exception e) {
      // Do not hide the potential exception raised in the try block.
      throw Throwables.propagate(e);
    }
  }

  private static boolean isRequestAbortedByClient(Exception exception) {
    return Throwables.getCausalChain(exception).stream().anyMatch(t -> t instanceof ClientAbortException);
  }

  private static boolean isResponseCommitted(Response response) {
    Response.Stream stream = response.stream();
    // Request has been aborted by the client or the response was partially streamed, nothing can been done as Tomcat has committed the response
    return stream instanceof ServletResponse.ServletStream && ((ServletResponse.ServletStream) stream).response().isCommitted();
  }

  public static void writeErrors(JsonWriter json, List<String> errorMessages) {
    if (errorMessages.isEmpty()) {
      return;
    }
    json.name("errors").beginArray();
    errorMessages.forEach(message -> {
      json.beginObject();
      json.prop("msg", message);
      json.endObject();
    });
    json.endArray();
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
    private final String path;

    ActionExtractor(String path) {
      this.path = path;
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

    String getPath() {
      return path;
    }
  }

}
