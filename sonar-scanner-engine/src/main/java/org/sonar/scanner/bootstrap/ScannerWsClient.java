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
package org.sonar.scanner.bootstrap;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
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

public class ScannerWsClient {

  private static final Logger LOG = Loggers.get(ScannerWsClient.class);

  private final WsClient target;
  private final boolean hasCredentials;
  private final GlobalAnalysisMode globalMode;

  public ScannerWsClient(WsClient target, boolean hasCredentials, GlobalAnalysisMode globalMode) {
    this.target = target;
    this.hasCredentials = hasCredentials;
    this.globalMode = globalMode;
  }

  /**
   * If an exception is not thrown, the response needs to be closed by either calling close() directly, or closing the 
   * body content's stream/reader.
   * @throws IllegalStateException if the request could not be executed due to
   *     a connectivity problem or timeout. Because networks can
   *     fail during an exchange, it is possible that the remote server
   *     accepted the request before the failure
   * @throws HttpException if the response code is not in range [200..300)
   */
  public WsResponse call(WsRequest request) {
    Preconditions.checkState(!globalMode.isMediumTest(), "No WS call should be made in medium test mode");
    Profiler profiler = Profiler.createIfDebug(LOG).start();
    WsResponse response = target.wsConnector().call(request);
    profiler.stopDebug(format("%s %d %s", request.getMethod(), response.code(), response.requestUrl()));
    failIfUnauthorized(response);
    return response;
  }

  public String baseUrl() {
    return target.wsConnector().baseUrl();
  }

  @VisibleForTesting
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
    if (code == HTTP_FORBIDDEN || code == HTTP_BAD_REQUEST) {
      // SONAR-4397 Details are in response content
      throw MessageException.of(tryParseAsJsonError(response.content()));
    }
    response.failIfNotSuccessful();
  }

  public static String tryParseAsJsonError(String responseContent) {
    try {
      JsonParser parser = new JsonParser();
      JsonObject obj = parser.parse(responseContent).getAsJsonObject();
      JsonArray errors = obj.getAsJsonArray("errors");
      List<String> errorMessages = new ArrayList<>();
      for (JsonElement e : errors) {
        errorMessages.add(e.getAsJsonObject().get("msg").getAsString());
      }
      return Joiner.on(", ").join(errorMessages);
    } catch (Exception e) {
      return responseContent;
    }
  }
}
