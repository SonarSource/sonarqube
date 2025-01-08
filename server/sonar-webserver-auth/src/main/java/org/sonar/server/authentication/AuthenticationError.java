/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.authentication.event.AuthenticationException;

import static org.sonar.server.authentication.AuthenticationRedirection.encodeMessage;
import static org.sonar.server.authentication.AuthenticationRedirection.redirectTo;
import static org.sonar.server.authentication.Cookies.newCookieBuilder;

public final class AuthenticationError {

  private static final String UNAUTHORIZED_PATH = "/sessions/unauthorized";

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationError.class);
  private static final String AUTHENTICATION_ERROR_COOKIE = "AUTHENTICATION-ERROR";
  private static final int FIVE_MINUTES_IN_SECONDS = 5 * 60;

  private AuthenticationError() {
    // Utility class
  }

  static void handleError(Exception e, HttpRequest request, HttpResponse response, String message) {
    LOGGER.warn(message, e);
    redirectToUnauthorized(request, response);
  }

  public static void handleError(HttpRequest request, HttpResponse response, String message) {
    LOGGER.warn(message);
    redirectToUnauthorized(request, response);
  }

  static void handleAuthenticationError(AuthenticationException e, HttpRequest request, HttpResponse response) {
    String publicMessage = e.getPublicMessage();
    if (publicMessage != null && !publicMessage.isEmpty()) {
      addErrorCookie(request, response, publicMessage);
    }
    redirectToUnauthorized(request, response);
  }

  public static void addErrorCookie(HttpRequest request, HttpResponse response, String value) {
    response.addCookie(newCookieBuilder(request)
      .setName(AUTHENTICATION_ERROR_COOKIE)
      .setValue(encodeMessage(value))
      .setHttpOnly(false)
      .setExpiry(FIVE_MINUTES_IN_SECONDS)
      .build());
  }

  private static void redirectToUnauthorized(HttpRequest request, HttpResponse response) {
    redirectTo(response, request.getContextPath() + UNAUTHORIZED_PATH);
  }
}
