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
package org.sonar.batch.bootstrap;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.HttpURLConnection;
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

public class BatchWsClient {

  private static final Logger LOG = Loggers.get(BatchWsClient.class);

  private final WsClient target;
  private final boolean hasCredentials;

  public BatchWsClient(WsClient target, boolean hasCredentials) {
    this.target = target;
    this.hasCredentials = hasCredentials;
  }

  /**
   * @throws IllegalStateException if the request could not be executed due to
   *     a connectivity problem or timeout. Because networks can
   *     fail during an exchange, it is possible that the remote server
   *     accepted the request before the failure
   * @throws HttpException if the response code is not in range [200..300)
   */
  public WsResponse call(WsRequest request) {
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
    if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
      if (hasCredentials) {
        // credentials are not valid
        throw MessageException.of(format("Not authorized. Please check the properties %s and %s.",
          CoreProperties.LOGIN, CoreProperties.PASSWORD));
      }
      // not authenticated - see https://jira.sonarsource.com/browse/SONAR-4048
      throw MessageException.of(format("Not authorized. Analyzing this project requires to be authenticated. " +
        "Please provide the values of the properties %s and %s.", CoreProperties.LOGIN, CoreProperties.PASSWORD));

    }
    if (response.code() == HttpURLConnection.HTTP_FORBIDDEN) {
      // SONAR-4397 Details are in response content
      throw MessageException.of(tryParseAsJsonError(response.content()));
    }
    response.failIfNotSuccessful();
  }

  private static String tryParseAsJsonError(String responseContent) {
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
