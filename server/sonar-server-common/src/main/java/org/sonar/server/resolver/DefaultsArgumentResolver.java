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

import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonarsource.enterprises.server.DefaultEnterpriseProvider;
import org.sonarsource.organizations.server.DefaultOrganizationProvider;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.ServletModelAttributeMethodProcessor;

/**
 * Custom argument resolver that automatically injects default values for parameters
 * annotated with organization or enterprise annotations.
 * <p>
 * In server mode (on-prem), there is no multi-tenancy, so all requests belong to a single
 * default organization and enterprise. This resolver automatically forces those identifiers
 * to their default values, overriding any user-provided values.
 * <p>
 * The default values must match those in {@link DefaultOrganizationProvider} and {@link DefaultEnterpriseProvider}.
 * <p>
 * {@code @RequestBody} DTOs are handled separately by {@link DefaultsRequestBodyAdvice}.
 */
@ServerSide
public class DefaultsArgumentResolver implements HandlerMethodArgumentResolver {
  private final ServletModelAttributeMethodProcessor delegate = new ServletModelAttributeMethodProcessor(false);

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    if (parameter.hasParameterAnnotation(ParameterObject.class)) {
      return true;
    }
    return DefaultsInjector.hasAnnotation(parameter);
  }

  @Override
  public Object resolveArgument(
    MethodParameter parameter,
    @Nullable ModelAndViewContainer mavContainer,
    NativeWebRequest webRequest,
    @Nullable WebDataBinderFactory binderFactory) throws Exception {

    String defaultValue = DefaultsInjector.getDefault(parameter);
    if (defaultValue != null) {
      return DefaultsInjector.resolveValue(defaultValue, parameter.getParameterType(), parameter.getGenericParameterType());
    }

    Object boundObject = delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
    if (boundObject == null) {
      return null;
    }
    return DefaultsInjector.injectDefaults(boundObject, parameter.getParameterType());
  }
}
