/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.client.WsRequest;
import org.sonarqube.ws.client.WsResponse;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.sonar.api.utils.Preconditions.checkState;

public class DefaultScannerWsClient implements ScannerWsClient {
  private static final int MAX_ERROR_MSG_LEN = 128;
  private static final Logger LOG = Loggers.get(DefaultScannerWsClient.class);

  private final WsClient target;
  private final boolean hasCredentials;
  private final GlobalAnalysisMode globalMode;

  public DefaultScannerWsClient(WsClient target, boolean hasCredentials, GlobalAnalysisMode globalMode) {
    this.target = target;
    this.hasCredentials = hasCredentials;
    this.globalMode = globalMode;
  }

  /**
   * If an exception is not thrown, the response needs to be closed by either calling close() directly, or closing the
   * body content's stream/reader.
   *
   * @throws IllegalStateException if the request could not be executed due to a connectivity problem or timeout. Because networks can
   *                               fail during an exchange, it is possible that the remote server accepted the request before the failure
   * @throws MessageException      if there was a problem with authentication or if a error message was parsed from the response.
   * @throws HttpException         if the response code is not in range [200..300). Consider using {@link #createErrorMessage(HttpException)} to create more relevant messages for the users.
   */
  public WsResponse call(WsRequest request) {
    checkState(!globalMode.isMediumTest(), "No WS call should be made in medium test mode");
    Profiler profiler = Profiler.createIfDebug(LOG).start();
    WsResponse response = target.wsConnector().call(request);
    profiler.stopDebug(format("%s %d %s", request.getMethod(), response.code(), response.requestUrl()));
    failIfUnauthorized(response);
    return response;
  }

  public String baseUrl() {
    return target.wsConnector().baseUrl();
  }

  WsConnector wsConnector() {
    return target.wsConnector();
  }

  private void failIfUnauthorized(WsResponse response) {
    int code = response.code();
    if (code == HTTP_UNAUTHORIZED) {
      response.close();
      if (hasCredentials) {
        // credentials are not valid
        throw MessageException.of(format("Not authorized. Please check the properties %s and %s.",
          CoreProperties.LOGIN, CoreProperties.PASSWORD));
      }
      // not authenticated - see https://jira.sonarsource.com/browse/SONAR-4048
      throw MessageException.of(format("Not authorized. Analyzing this project requires to be authenticated. " +
        "Please provide the values of the properties %s and %s.", CoreProperties.LOGIN, CoreProperties.PASSWORD));

    }
    if (code == HTTP_FORBIDDEN) {
      throw MessageException.of("You're not authorized to run analysis. Please contact the project administrator.");
    }
    if (code == HTTP_BAD_REQUEST) {
      String jsonMsg = tryParseAsJsonError(response.content());
      if (jsonMsg != null) {
        throw MessageException.of(jsonMsg);
      }
    }

    // if failed, throws an HttpException
    response.failIfNotSuccessful();
  }

  /**
   * Tries to form a short and relevant error message from the exception, to be displayed in the console.
   */
  public static String createErrorMessage(HttpException exception) {
    String json = tryParseAsJsonError(exception.content());
    if (json != null) {
      return json;
    }

    String msg = "HTTP code " + exception.code();
    if (isHtml(exception.content())) {
      return msg;
    }

    return msg + ": " + StringUtils.left(exception.content(), MAX_ERROR_MSG_LEN);
  }

  @CheckForNull
  private static String tryParseAsJsonError(String responseContent) {
    try {
      JsonParser parser = new JsonParser();
      JsonObject obj = parser.parse(responseContent).getAsJsonObject();
      JsonArray errors = obj.getAsJsonArray("errors");
      List<String> errorMessages = new ArrayList<>();
      for (JsonElement e : errors) {
        errorMessages.add(e.getAsJsonObject().get("msg").getAsString());
      }
      return String.join(", ", errorMessages);
    } catch (Exception e) {
      return null;
    }
  }

  private static boolean isHtml(String responseContent) {
    return StringUtils.stripToEmpty(responseContent).startsWith("<!DOCTYPE html>");
  }
}
