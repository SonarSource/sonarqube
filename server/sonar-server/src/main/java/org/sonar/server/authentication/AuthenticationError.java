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
package org.sonar.server.authentication;

import javax.servlet.http.HttpServletResponse;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.authentication.event.AuthenticationException;

import static java.lang.String.format;
import static org.sonar.server.authentication.AuthenticationRedirection.encodeMessage;
import static org.sonar.server.authentication.AuthenticationRedirection.redirectTo;

final class AuthenticationError {

  private static final String UNAUTHORIZED_PATH = "/sessions/unauthorized";
  private static final String UNAUTHORIZED_PATH_WITH_MESSAGE = UNAUTHORIZED_PATH + "?message=%s";
  private static final Logger LOGGER = Loggers.get(AuthenticationError.class);

  private AuthenticationError() {
    // Utility class
  }

  static void handleError(Exception e, HttpServletResponse response, String message) {
    LOGGER.warn(message, e);
    redirectToUnauthorized(response);
  }

  static void handleError(HttpServletResponse response, String message) {
    LOGGER.warn(message);
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

  public static void redirectToUnauthorized(HttpServletResponse response) {
    redirectTo(response, UNAUTHORIZED_PATH);
  }

}
