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

import com.google.common.base.Joiner;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;

import static java.lang.String.format;

public class WsClientLoggingInterceptor implements Interceptor {

  private static final Logger LOG = Loggers.get(WsClientLoggingInterceptor.class);

  @Override
  public Response intercept(Chain chain) throws IOException {
    Response response = logAndSendRequest(chain);
    if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
      if (StringUtils.isBlank(response.request().header("Authorization"))) {
        // not authenticated - see https://jira.sonarsource.com/browse/SONAR-4048
        throw MessageException.of(format("Not authorized. Analyzing this project requires to be authenticated. " +
          "Please provide the values of the properties %s and %s.", CoreProperties.LOGIN, CoreProperties.PASSWORD));
      }
      // credentials are not valid
      throw MessageException.of(format("Not authorized. Please check the properties %s and %s.",
        CoreProperties.LOGIN, CoreProperties.PASSWORD));
    }
    if (response.code() == HttpURLConnection.HTTP_FORBIDDEN) {
      // SONAR-4397 Details are in response content
      throw MessageException.of(tryParseAsJsonError(response.body().string()));
    }
    return response;
  }

  private Response logAndSendRequest(Chain chain) throws IOException {
    Request request = chain.request();
    Response response;
    Profiler profiler = Profiler.createIfDebug(LOG).startTrace(format("%s %s", request.method(), request.url()));
    response = chain.proceed(request);
    profiler.stopDebug(format("%s %d %s", request.method(), response.code(), request.url()));
    return response;
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
