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
package org.sonar.server.authentication.event;

import java.util.Collections;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.http.HttpRequest;

import static java.util.Objects.requireNonNull;

public class AuthenticationEventImpl implements AuthenticationEvent {
  private static final Logger LOGGER = LoggerFactory.getLogger("auth.event");
  private static final int FLOOD_THRESHOLD = 128;
  private static final Pattern PATTERN_LINE_BREAK = Pattern.compile("[\n\r]");

  @Override
  public void loginSuccess(HttpRequest request, @Nullable String login, Source source) {
    checkRequest(request);
    requireNonNull(source, "source can't be null");
    LOGGER.atDebug().setMessage("login success [method|{}][provider|{}|{}][IP|{}|{}][login|{}]")
      .addArgument(source::getMethod)
      .addArgument(source::getProvider)
      .addArgument(source::getProviderName)
      .addArgument(request::getRemoteAddr)
      .addArgument(() -> getAllIps(request))
      .addArgument(() -> preventLogFlood(sanitizeLog(emptyIfNull(login))))
      .log();
  }

  private static String getAllIps(HttpRequest request) {
    return String.join(",", Collections.list(request.getHeaders("X-Forwarded-For")));
  }

  @Override
  public void loginFailure(HttpRequest request, AuthenticationException e) {
    checkRequest(request);
    requireNonNull(e, "AuthenticationException can't be null");
    if (!LOGGER.isDebugEnabled()) {
      return;
    }
    Source source = e.getSource();
    LOGGER.debug("login failure [cause|{}][method|{}][provider|{}|{}][IP|{}|{}][login|{}]",
      emptyIfNull(e.getMessage()),
      source.getMethod(), source.getProvider(), source.getProviderName(),
      request.getRemoteAddr(), getAllIps(request),
      preventLogFlood(emptyIfNull(e.getLogin())));
  }

  @Override
  public void logoutSuccess(HttpRequest request, @Nullable String login) {
    checkRequest(request);
    if (!LOGGER.isDebugEnabled()) {
      return;
    }
    LOGGER.debug("logout success [IP|{}|{}][login|{}]",
      request.getRemoteAddr(), getAllIps(request),
      preventLogFlood(emptyIfNull(login)));
  }

  @Override
  public void logoutFailure(HttpRequest request, String errorMessage) {
    checkRequest(request);
    requireNonNull(errorMessage, "error message can't be null");
    if (!LOGGER.isDebugEnabled()) {
      return;
    }
    LOGGER.debug("logout failure [error|{}][IP|{}|{}]",
      emptyIfNull(errorMessage),
      request.getRemoteAddr(), getAllIps(request));
  }

  private static void checkRequest(HttpRequest request) {
    requireNonNull(request, "request can't be null");
  }

  private static String emptyIfNull(@Nullable String login) {
    return login == null ? "" : login;
  }

  private static String preventLogFlood(String str) {
    if (str.length() > FLOOD_THRESHOLD) {
      return str.substring(0, FLOOD_THRESHOLD) + "...(" + str.length() + ")";
    }
    return str;
  }

  private static String sanitizeLog(String message) {
    return PATTERN_LINE_BREAK.matcher(message).replaceAll("_");
  }

}
