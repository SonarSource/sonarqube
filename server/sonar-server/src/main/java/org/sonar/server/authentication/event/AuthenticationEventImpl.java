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
package org.sonar.server.authentication.event;

import com.google.common.base.Joiner;
import java.util.Collections;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.stream.MoreCollectors;

import static java.util.Objects.requireNonNull;

public class AuthenticationEventImpl implements AuthenticationEvent {
  private static final Logger LOGGER = Loggers.get("auth.event");
  private static final int FLOOD_THRESHOLD = 128;

  @Override
  public void loginSuccess(HttpServletRequest request, @Nullable String login, Source source) {
    checkRequest(request);
    requireNonNull(source, "source can't be null");
    if (!LOGGER.isDebugEnabled()) {
      return;
    }
    LOGGER.debug("login success [method|{}][provider|{}|{}][IP|{}|{}][login|{}]",
      source.getMethod(), source.getProvider(), source.getProviderName(),
      request.getRemoteAddr(), getAllIps(request),
      preventLogFlood(emptyIfNull(login)));
  }

  private static String getAllIps(HttpServletRequest request) {
    return Collections.list(request.getHeaders("X-Forwarded-For")).stream().collect(MoreCollectors.join(Joiner.on(",")));
  }

  @Override
  public void loginFailure(HttpServletRequest request, AuthenticationException e) {
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
  public void logoutSuccess(HttpServletRequest request, @Nullable String login) {
    checkRequest(request);
    if (!LOGGER.isDebugEnabled()) {
      return;
    }
    LOGGER.debug("logout success [IP|{}|{}][login|{}]",
      request.getRemoteAddr(), getAllIps(request),
      preventLogFlood(emptyIfNull(login)));
  }

  @Override
  public void logoutFailure(HttpServletRequest request, String errorMessage) {
    checkRequest(request);
    requireNonNull(errorMessage, "error message can't be null");
    if (!LOGGER.isDebugEnabled()) {
      return;
    }
    LOGGER.debug("logout failure [error|{}][IP|{}|{}]",
      emptyIfNull(errorMessage),
      request.getRemoteAddr(), getAllIps(request));
  }

  private static void checkRequest(HttpServletRequest request) {
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

}
