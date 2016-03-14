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
package org.sonar.server.authentication;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;
import static org.sonar.api.server.authentication.UnauthorizedException.UNAUTHORIZED_PATH;

public class AuthenticationError {

  private static final Logger LOGGER = Loggers.get(AuthenticationError.class);

  private AuthenticationError() {
    // Utility class
  }

  public static void handleError(Exception e, HttpServletResponse response, String message) {
    LOGGER.error(message, e);
    redirectToUnauthorized(response);
  }

  public static void handleError(HttpServletResponse response, String message) {
    LOGGER.error(message);
    redirectToUnauthorized(response);
  }

  public static void handleUnauthorizedError(UnauthorizedException e, HttpServletResponse response) {
    redirectTo(response, e.getPath());
  }

  private static void redirectToUnauthorized(HttpServletResponse response) {
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
