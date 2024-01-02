/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.logging;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.httpd.HttpAction;
import org.sonar.server.log.ServerLogging;

import static java.lang.String.format;

public class ChangeLogLevelHttpAction implements HttpAction {

  private static final String PATH = "/changeLogLevel";
  private static final String PARAM_LEVEL = "level";

  private final ServerLogging logging;

  public ChangeLogLevelHttpAction(ServerLogging logging) {
    this.logging = logging;
  }

  @Override
  public String getContextPath() {
    return PATH;
  }

  @Override
  public void handle(HttpRequest request, HttpResponse response) {
    if (!"POST".equals(request.getRequestLine().getMethod())) {
      response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_METHOD_NOT_ALLOWED);
      return;
    }

    HttpEntityEnclosingRequest postRequest = (HttpEntityEnclosingRequest) request;
    final URI requestUri;
    try {
      requestUri = new URI(postRequest.getRequestLine().getUri());
    } catch (URISyntaxException e) {
      throw new IllegalStateException("the request URI can't be syntactically invalid", e);
    }

    List<NameValuePair> requestParams = URLEncodedUtils.parse(requestUri, StandardCharsets.UTF_8);
    Optional<String> levelRequested = requestParams.stream()
      .filter(nvp -> PARAM_LEVEL.equals(nvp.getName()))
      .map(NameValuePair::getValue)
      .findFirst();

    final String levelStr;
    if (levelRequested.isEmpty()) {
      response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
      response.setEntity(new StringEntity(format("Parameter '%s' is missing", PARAM_LEVEL), StandardCharsets.UTF_8));
      return;
    } else {
      levelStr = levelRequested.get();
    }

    try {
      LoggerLevel level = LoggerLevel.valueOf(levelStr);
      logging.changeLevel(level);
      response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK);
    } catch (IllegalArgumentException e) {
      Loggers.get(ChangeLogLevelHttpAction.class).debug("Value '{}' for parameter '" + PARAM_LEVEL + "' is invalid: {}", levelStr, e);
      response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
      response.setEntity(
        new StringEntity(format("Value '%s' for parameter '%s' is invalid", levelStr, PARAM_LEVEL), StandardCharsets.UTF_8));
    }
  }
}
