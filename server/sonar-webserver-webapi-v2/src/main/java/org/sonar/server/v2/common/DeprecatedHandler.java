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
package org.sonar.server.v2.common;

import java.lang.reflect.Field;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.UserSession;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import static org.sonar.process.logging.LogbackHelper.DEPRECATION_LOGGER_NAME;

/**
 * Interceptor that logs deprecation warnings for deprecated web services and parameters that are used. The only thing this handler will not
 * log is used deprecated fields of {@link org.springframework.web.bind.annotation.RequestBody()}. This needs to be covered at some point.
 * </p>
 */
@Component
public class DeprecatedHandler implements HandlerInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(DEPRECATION_LOGGER_NAME);

  private final UserSession userSession;

  public DeprecatedHandler(UserSession userSession) {
    this.userSession = userSession;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (handler instanceof HandlerMethod handlerMethod) {
      preHandle(handlerMethod, request);
    } else {
      LOGGER.debug("Handler is not a HandlerMethod, skipping deprecated check.");
    }

    return true;
  }

  private void preHandle(HandlerMethod handlerMethod, HttpServletRequest request) {
    Level logLevel = getLogLevel();
    Deprecated deprecatedEndpoint = handlerMethod.getMethodAnnotation(Deprecated.class);
    if (deprecatedEndpoint != null) {
      logDeprecatedWebServiceMessage(logLevel, deprecatedEndpoint.since());
    }

    handleParams(handlerMethod, logLevel, request);
  }

  private static void handleParams(HandlerMethod handlerMethod, Level logLevel, HttpServletRequest request) {
    for (MethodParameter param : handlerMethod.getMethodParameters()) {
      if (isV2ParameterObject(param)) {
        checkDeprecatedFields(param.getParameterType(), logLevel, request);
      } else if (isUsedDeprecatedRequestParam(request, param)) {
        String paramName = param.getParameterAnnotation(RequestParam.class).name();
        String deprecatedSince = param.getParameterAnnotation(Deprecated.class).since();
        logDeprecatedParamMessage(logLevel, paramName, deprecatedSince);
      }
    }
  }

  private static void checkDeprecatedFields(Class<?> clazz, Level logLevel, HttpServletRequest request) {
    for (Field field : clazz.getDeclaredFields()) {
      if (isUsedDeprecatedField(request, field)) {
        String deprecatedSince = field.getAnnotation(Deprecated.class).since();
        logDeprecatedParamMessage(logLevel, field.getName(), deprecatedSince);
      }

      if (isApiV2Param(field.getType())) {
        checkDeprecatedFields(field.getType(), logLevel, request);
      }
    }
  }

  private Level getLogLevel() {
    return isAuthenticatedBrowserSessionOrUnauthenticatedUser() ? Level.DEBUG : Level.WARN;
  }

  private boolean isAuthenticatedBrowserSessionOrUnauthenticatedUser() {
    return userSession instanceof ThreadLocalUserSession threadLocalUserSession
      && (threadLocalUserSession.hasSession()
      && (!userSession.isLoggedIn() || userSession.isAuthenticatedBrowserSession()));
  }

  private static void logDeprecatedWebServiceMessage(Level logLevel, String deprecatedSince) {
    LOGGER.atLevel(logLevel).log("Web service is deprecated since {} and will be removed in a future version.", deprecatedSince);
  }

  private static void logDeprecatedParamMessage(Level logLevel, String field, String deprecatedSince) {
    LOGGER.atLevel(logLevel).log("Parameter '{}' is deprecated since {} and will be removed in a future version.", field, deprecatedSince);
  }

  private static boolean isUsedDeprecatedRequestParam(HttpServletRequest request, MethodParameter param) {
    return param.hasParameterAnnotation(Deprecated.class) &&
      param.hasParameterAnnotation(RequestParam.class) &&
      request.getParameter(param.getParameterAnnotation(RequestParam.class).name()) != null;
  }

  private static boolean isUsedDeprecatedField(HttpServletRequest request, Field field) {
    return field.getAnnotation(Deprecated.class) != null && request.getParameter(field.getName()) != null;
  }

  private static boolean isV2ParameterObject(MethodParameter param) {
    return param.hasParameterAnnotation(ParameterObject.class) && isApiV2Param(param.getParameterType());
  }

  private static boolean isApiV2Param(Class<?> clazz) {
    return clazz.getTypeName().startsWith("org.sonar.server.v2");
  }

}
