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
package org.sonar.server.authentication;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.authentication.event.AuthenticationException;

import static java.lang.String.format;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;

final class AuthenticationError {

  private static final String UNAUTHORIZED_PATH = "/sessions/unauthorized";
  private static final String UNAUTHORIZED_PATH_WITH_MESSAGE = UNAUTHORIZED_PATH + "?message=%s";
  private static final Logger LOGGER = Loggers.get(AuthenticationError.class);

  private AuthenticationError() {
    // Utility class
  }

  static void handleError(Exception e, HttpServletResponse response, String message) {
    LOGGER.error(message, e);
    redirectToUnauthorized(response);
  }

  static void handleError(HttpServletResponse response, String message) {
    LOGGER.error(message);
    redirectToUnauthorized(response);
  }

  static void handleAuthenticationError(AuthenticationException e, HttpServletResponse response, String contextPath) {
    redirectTo(response, getPath(e, contextPath));
  }

  private static String getPath(AuthenticationException e, String contextPath) {
    String publicMessage = e.getPublicMessage();
    if (publicMessage == null || publicMessage.isEmpty()) {
      return UNAUTHORIZED_PATH;
    }
    return contextPath + format(UNAUTHORIZED_PATH_WITH_MESSAGE, encodeMessage(publicMessage));
  }

  private static String encodeMessage(String message) {
    try {
      return encode(message, UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(format("Fail to encode %s", message), e);
    }
  }

  public static void redirectToUnauthorized(HttpServletResponse response) {
    redirectTo(response, UNAUTHORIZED_PATH);
  }

  private static void redirectTo(HttpServletResponse response, String url) {
    try {
      response.sendRedirect(url);
    } catch (IOException e) {
      throw new IllegalStateException(format("Fail to redirect to %s", url), e);
    }
  }
}
