/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.resolver;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.sonar.api.server.ServerSide;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

/**
 * Injects default organization/enterprise values into {@code @RequestBody} DTOs that carry an
 * annotated member. Runs as a regular {@link org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice},
 * i.e. after deserialization and before {@code @Valid} validation, alongside every other advice
 * registered in the same context (e.g. Slack's signature-validation advices).
 */
@ControllerAdvice
@ServerSide
// RequestResponseBodyAdviceChain applies afterBodyRead() to every matching advice in a single
// forward pass over the OrderComparator-sorted advice list (not reversed, unlike interceptors).
// HIGHEST_PRECEDENCE guarantees our default is already injected before any other advice's
// afterBodyRead() runs, so a future advice that inspects an org/enterprise field never sees it unset.
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DefaultsRequestBodyAdvice extends RequestBodyAdviceAdapter {

  private final Map<Class<?>, Boolean> annotatedMemberCache = new ConcurrentHashMap<>();

  @Override
  public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
    return hasAnnotatedMember(methodParameter.getParameterType());
  }

  @Override
  public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
    Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
    if (body == null) {
      return null;
    }
    try {
      return DefaultsInjector.injectDefaults(body, parameter.getParameterType());
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to inject default values into @RequestBody parameter", e);
    }
  }

  private boolean hasAnnotatedMember(Class<?> type) {
    return annotatedMemberCache.computeIfAbsent(type, DefaultsRequestBodyAdvice::computeHasAnnotatedMember);
  }

  private static boolean computeHasAnnotatedMember(Class<?> type) {
    if (type.isRecord()) {
      // Records are implicitly final and can't extend another class, so there's no hierarchy to walk.
      return Arrays.stream(type.getRecordComponents()).anyMatch(DefaultsInjector::hasAnnotation);
    }
    return FieldUtils.getAllFieldsList(type).stream().anyMatch(DefaultsInjector::hasAnnotation);
  }
}
