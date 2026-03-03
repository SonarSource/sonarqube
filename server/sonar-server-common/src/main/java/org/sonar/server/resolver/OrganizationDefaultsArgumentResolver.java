/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonarsource.organizations.api.rest.OrganizationId;
import org.sonarsource.organizations.api.rest.OrganizationKey;
import org.sonarsource.organizations.api.rest.OrganizationLegacyId;
import org.sonarsource.organizations.server.DefaultOrganizationProvider;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.ServletModelAttributeMethodProcessor;

/**
 * Custom argument resolver that automatically injects default organization values for parameters
 * annotated with @OrganizationId, @OrganizationKey, or @OrganizationLegacyId
 * <p>
 * In server mode (on-prem), there is no multi-tenancy, so all requests belong to a single
 * default organization. This resolver automatically forces organization identifiers to their
 * default values, overriding any user-provided values.
 * <p>
 * The default organization values must match those in {@link DefaultOrganizationProvider}.
 */
@ServerSide
public class OrganizationDefaultsArgumentResolver implements HandlerMethodArgumentResolver {
  private static final Map<Class<? extends Annotation>, String> ORGANIZATION_ANNOTATIONS = Map.of(
    OrganizationId.class, DefaultOrganizationProvider.ID.toString(),
    OrganizationKey.class, DefaultOrganizationProvider.KEY,
    OrganizationLegacyId.class, DefaultOrganizationProvider.LEGACY_ID
  );

  private final ServletModelAttributeMethodProcessor delegate = new ServletModelAttributeMethodProcessor(false);

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    if (parameter.hasParameterAnnotation(ParameterObject.class)) {
      return true;
    }
    return hasOrganizationAnnotation(parameter.getParameter());
  }

  /**
   * Resolves a method parameter by either returning a default organization value or injecting
   * defaults into a complex parameter object.
   *
   * @param parameter the method parameter to resolve
   * @param mavContainer the ModelAndViewContainer for the current request
   * @param webRequest the current web request
   * @param binderFactory the factory for creating WebDataBinder instances
   * @return the resolved parameter value:
   *         <ul>
   *           <li>A {@link String} when the parameter is directly annotated with an organization annotation
   *               (@OrganizationId, @OrganizationKey, or @OrganizationLegacyId)</li>
   *           <li>A complex object (record or POJO) when the parameter is annotated with @ParameterObject
   *               and contains fields with organization annotations</li>
   *           <li>{@code null} if the delegate resolver returns null</li>
   *         </ul>
   * @throws Exception if the argument resolution fails
   */
  @Override
  public Object resolveArgument(
    MethodParameter parameter,
    @Nullable ModelAndViewContainer mavContainer,
    NativeWebRequest webRequest,
    @Nullable WebDataBinderFactory binderFactory) throws Exception {

    if (hasOrganizationAnnotation(parameter.getParameter())) {
      return getOrganizationDefault(parameter.getParameter());
    }

    Object boundObject = delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
    if (boundObject == null) {
      return null;
    }
    return injectOrganizationDefaults(boundObject, parameter.getParameterType());
  }

  private Object injectOrganizationDefaults(Object boundObject, Class<?> parameterType)
    throws ReflectiveOperationException {
    return parameterType.isRecord()
      ? injectIntoRecord(boundObject, parameterType)
      : injectIntoClass(boundObject, parameterType);
  }

  private Object injectIntoRecord(Object boundObject, Class<?> recordClass)
    throws ReflectiveOperationException {
    RecordComponent[] components = recordClass.getRecordComponents();
    Object[] args = buildConstructorArguments(boundObject, components);
    Constructor<?> constructor = getCanonicalConstructor(recordClass, components);
    return constructor.newInstance(args);
  }

  private static Object[] buildConstructorArguments(Object boundObject, RecordComponent[] components)
    throws InvocationTargetException, IllegalAccessException {
    Object[] args = new Object[components.length];
    for (int i = 0; i < components.length; i++) {
      RecordComponent component = components[i];
      Object currentValue = component.getAccessor().invoke(boundObject);
      args[i] = getValueOrDefault(component, currentValue);
    }
    return args;
  }

  private Constructor<?> getCanonicalConstructor(Class<?> recordClass, RecordComponent[] components)
    throws NoSuchMethodException {
    Class<?>[] parameterTypes = Arrays.stream(components)
      .map(RecordComponent::getType)
      .toArray(Class<?>[]::new);
    return recordClass.getDeclaredConstructor(parameterTypes);
  }

  private static Object injectIntoClass(Object boundObject, Class<?> classType) throws IllegalAccessException {
    for (Field field : classType.getDeclaredFields()) {
      field.setAccessible(true);
      String defaultValue = getOrganizationDefault(field);
      if (defaultValue != null) {
        field.set(boundObject, defaultValue);
      }
    }
    return boundObject;
  }

  private static Object getValueOrDefault(AnnotatedElement element, @Nullable Object currentValue) {
    String defaultValue = getOrganizationDefault(element);
    return defaultValue != null ? defaultValue : currentValue;
  }

  @CheckForNull
  private static String getOrganizationDefault(AnnotatedElement element) {
    return ORGANIZATION_ANNOTATIONS.entrySet().stream()
      .filter(entry -> element.getAnnotation(entry.getKey()) != null)
      .map(Map.Entry::getValue)
      .findFirst()
      .orElse(null);
  }

  private static boolean hasOrganizationAnnotation(AnnotatedElement element) {
    return ORGANIZATION_ANNOTATIONS.keySet().stream()
      .anyMatch(element::isAnnotationPresent);
  }

}
