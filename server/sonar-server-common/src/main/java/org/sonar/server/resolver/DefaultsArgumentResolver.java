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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonarsource.enterprises.api.rest.EnterpriseId;
import org.sonarsource.enterprises.server.DefaultEnterpriseProvider;
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
 * Custom argument resolver that automatically injects default values for parameters
 * annotated with organization or enterprise annotations.
 * <p>
 * In server mode (on-prem), there is no multi-tenancy, so all requests belong to a single
 * default organization and enterprise. This resolver automatically forces those identifiers
 * to their default values, overriding any user-provided values.
 * <p>
 * The default values must match those in {@link DefaultOrganizationProvider} and {@link DefaultEnterpriseProvider}.
 */
@ServerSide
public class DefaultsArgumentResolver implements HandlerMethodArgumentResolver {
  private static final Map<Class<? extends Annotation>, String> ANNOTATIONS = Map.of(
    OrganizationId.class, DefaultOrganizationProvider.ID.toString(),
    OrganizationKey.class, DefaultOrganizationProvider.KEY,
    OrganizationLegacyId.class, DefaultOrganizationProvider.LEGACY_ID,
    EnterpriseId.class, DefaultEnterpriseProvider.ENTERPRISE_ID.toString()
  );

  private final ServletModelAttributeMethodProcessor delegate = new ServletModelAttributeMethodProcessor(false);

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    if (parameter.hasParameterAnnotation(ParameterObject.class)) {
      return true;
    }
    // Use MethodParameter's annotation lookup so that annotations declared on interface
    // method parameters are found (raw java.lang.reflect.Parameter only sees the concrete method).
    return hasAnnotation(parameter);
  }

  @Override
  public Object resolveArgument(
    MethodParameter parameter,
    @Nullable ModelAndViewContainer mavContainer,
    NativeWebRequest webRequest,
    @Nullable WebDataBinderFactory binderFactory) throws Exception {

    String defaultValue = getDefault(parameter);
    if (defaultValue != null) {
      return resolveValue(defaultValue, parameter.getParameterType(), parameter.getGenericParameterType());
    }

    Object boundObject = delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
    if (boundObject == null) {
      return null;
    }
    return injectDefaults(boundObject, parameter.getParameterType());
  }

  private Object injectDefaults(Object boundObject, Class<?> parameterType)
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
      args[i] = getValueOrDefault(component, currentValue, component.getType(), component.getGenericType());
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
      String defaultValue = getDefault(field);
      if (defaultValue != null) {
        field.set(boundObject, resolveValue(defaultValue, field.getType(), field.getGenericType()));
      }
    }
    return boundObject;
  }

  private static Object getValueOrDefault(AnnotatedElement element, @Nullable Object currentValue, Class<?> targetType, Type genericType) {
    String defaultValue = getDefault(element);
    return defaultValue != null ? resolveValue(defaultValue, targetType, genericType) : currentValue;
  }

  @CheckForNull
  private static String getDefault(MethodParameter parameter) {
    return ANNOTATIONS.entrySet().stream()
      .filter(entry -> parameter.getParameterAnnotation(entry.getKey()) != null)
      .map(Map.Entry::getValue)
      .findFirst()
      .orElse(null);
  }

  @CheckForNull
  private static String getDefault(AnnotatedElement element) {
    return ANNOTATIONS.entrySet().stream()
      .filter(entry -> element.getAnnotation(entry.getKey()) != null)
      .map(Map.Entry::getValue)
      .findFirst()
      .orElse(null);
  }

  private static boolean hasAnnotation(MethodParameter parameter) {
    return ANNOTATIONS.keySet().stream()
      .anyMatch(parameter::hasParameterAnnotation);
  }

  private static Object resolveValue(String defaultValue, Class<?> targetType, Type genericType) {
    if (!isCollectionOrArray(targetType)) {
      return defaultValue;
    }
    if (targetType.isArray()) {
      Class<?> componentType = targetType.getComponentType();
      Object array = Array.newInstance(componentType, 1);
      Array.set(array, 0, convert(defaultValue, componentType));
      return array;
    }
    Object element = convert(defaultValue, elementType(genericType));
    return targetType.isAssignableFrom(Set.class) ? Set.of(element) : List.of(element);
  }

  private static Class<?> elementType(@Nullable Type genericType) {
    if (genericType instanceof ParameterizedType parameterizedType) {
      Type[] typeArguments = parameterizedType.getActualTypeArguments();
      if (typeArguments.length == 1 && typeArguments[0] instanceof Class<?> elementClass) {
        return elementClass;
      }
    }
    return String.class;
  }

  private static Object convert(String value, Class<?> type) {
    return type == UUID.class ? UUID.fromString(value) : value;
  }

  private static boolean isCollectionOrArray(Class<?> type) {
    return Collection.class.isAssignableFrom(type) || type.isArray();
  }

}
