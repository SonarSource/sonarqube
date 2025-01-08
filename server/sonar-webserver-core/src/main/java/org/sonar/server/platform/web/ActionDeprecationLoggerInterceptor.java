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
package org.sonar.server.platform.web;

import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.ActionInterceptor;

import static org.sonar.process.logging.LogbackHelper.DEPRECATION_LOGGER_NAME;

/**
 * Logs deprecation messages for deprecated web service endpoints and parameters for API V1
 * Messages are logged:
 * at DEBUG level for anonymous users and browsers session,
 * at WARN level for authenticated users using tokens
 */
public class ActionDeprecationLoggerInterceptor implements ActionInterceptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(DEPRECATION_LOGGER_NAME);
  private final UserSession userSession;

  public ActionDeprecationLoggerInterceptor(UserSession userSession) {
    this.userSession = userSession;
  }

  @Override
  public void preAction(WebService.Action action, Request request) {
    Level logLevel = getLogLevel();

    String deprecatedSinceEndpoint = action.deprecatedSince();
    if (deprecatedSinceEndpoint != null) {
      logWebServiceMessage(logLevel, deprecatedSinceEndpoint);
    }

    action.params().forEach(param -> logParamMessage(request, logLevel, param));
  }

  private Level getLogLevel() {
    return isBrowserSessionOrAnonymous() ? Level.DEBUG : Level.WARN;
  }

  private boolean isBrowserSessionOrAnonymous() {
    return userSession instanceof ThreadLocalUserSession threadLocalUserSession
      && (threadLocalUserSession.hasSession()
        && (!userSession.isLoggedIn() || userSession.isAuthenticatedBrowserSession()));
  }

  private static void logWebServiceMessage(Level logLevel, String deprecatedSinceEndpoint) {
    LOGGER.atLevel(logLevel).log("Web service is deprecated since {} and will be removed in a future version.", deprecatedSinceEndpoint);
  }

  private static void logParamMessage(Request request, Level logLevel, WebService.Param param) {
    String paramKey = param.key();
    String deprecatedSince = param.deprecatedSince();
    if (request.hasParam(paramKey) && deprecatedSince != null) {
      logParamMessage(logLevel, param.key(), deprecatedSince);
    }

    String paramDeprecatedKey = param.deprecatedKey();
    String deprecatedKeySince = param.deprecatedKeySince();
    if (paramDeprecatedKey != null && request.hasParam(paramDeprecatedKey) && deprecatedKeySince != null) {
      logParamMessage(logLevel, paramDeprecatedKey, deprecatedKeySince);
    }
  }

  private static void logParamMessage(Level logLevel, String paramKey, @Nullable String deprecatedSince) {
    LOGGER.atLevel(logLevel).log("Parameter '{}' is deprecated since {} and will be removed in a future version.", paramKey, deprecatedSince);
  }

}
