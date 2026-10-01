/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.sonar.server.exceptions.BadRequestException;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rejects query parameters that the selected V2 handler does not declare.
 * Only the query string is inspected, so form-urlencoded body fields are ignored.
 */
public class UnknownRequestParameterHandler implements HandlerInterceptor {

  /**
   * Names with no meaning for the API. {@code _} is the cache-buster appended by clients such as jQuery.
   */
  private static final Set<String> GLOBALLY_ALLOWED_NAMES = Set.of("_");

  private final Map<Method, Set<String>> allowedByMethod = new ConcurrentHashMap<>();

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (handler instanceof HandlerMethod handlerMethod) {
      rejectUnknownParameters(request, handlerMethod);
    }
    return true;
  }

  private void rejectUnknownParameters(HttpServletRequest request, HandlerMethod handlerMethod) {
    Set<String> queryNames = queryParameterNames(request);
    if (queryNames.isEmpty()) {
      return;
    }
    Set<String> allowed = allowedByMethod.computeIfAbsent(handlerMethod.getMethod(), method -> allowedParameters(handlerMethod));
    List<String> unknown = queryNames.stream()
      .filter(name -> !allowed.contains(name))
      .sorted()
      .toList();
    if (!unknown.isEmpty()) {
      throw BadRequestException.create(buildRejection(unknown, request.getRequestURI()));
    }
  }

  private static String buildRejection(List<String> unknownParameters, String requestUri) {
    String quoted = unknownParameters.stream()
      .map(name -> "\"" + name + "\"")
      .collect(Collectors.joining(", "));
    if (unknownParameters.size() == 1) {
      return "Parameter " + quoted + " is not a valid parameter for " + requestUri + ".";
    }
    return "Parameters " + quoted + " are not valid parameters for " + requestUri + ".";
  }

  private static Set<String> queryParameterNames(HttpServletRequest request) {
    String query = request.getQueryString();
    if (query == null || query.isEmpty()) {
      return Set.of();
    }
    Set<String> names = new HashSet<>();
    for (String pair : query.split("&")) {
      if (pair.isEmpty()) {
        continue;
      }
      int separator = pair.indexOf('=');
      String rawName = separator >= 0 ? pair.substring(0, separator) : pair;
      if (!rawName.isEmpty()) {
        names.add(decode(rawName, request));
      }
    }
    return names;
  }

  private static String decode(String rawName, HttpServletRequest request) {
    try {
      return URLDecoder.decode(rawName, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      BadRequestException badRequest = BadRequestException.create("Query string for " + request.getRequestURI() + " is not valid.");
      badRequest.initCause(e);
      throw badRequest;
    }
  }

  private static Set<String> allowedParameters(HandlerMethod handlerMethod) {
    Set<String> allowed = new HashSet<>(GLOBALLY_ALLOWED_NAMES);
    for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
      RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
      if (requestParam != null) {
        if (!requestParam.name().isEmpty()) {
          allowed.add(requestParam.name());
        }
      } else if (parameter.hasParameterAnnotation(ParameterObject.class)) {
        addParameterObjectNames(parameter.nestedIfOptional().getNestedParameterType(), allowed);
      }
    }
    return allowed;
  }

  private static void addParameterObjectNames(Class<?> type, Set<String> allowed) {
    if (type.isRecord()) {
      for (RecordComponent component : type.getRecordComponents()) {
        allowed.add(component.getName());
      }
      return;
    }
    Class<?> current = type;
    while (current != null && current != Object.class) {
      for (Field field : current.getDeclaredFields()) {
        if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) {
          allowed.add(field.getName());
        }
      }
      current = current.getSuperclass();
    }
  }
}
