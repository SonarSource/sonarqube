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
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.sonarsource.enterprises.api.rest.EnterpriseId;
import org.sonarsource.enterprises.server.DefaultEnterpriseProvider;
import org.sonarsource.organizations.api.rest.OrganizationId;
import org.sonarsource.organizations.api.rest.OrganizationKey;
import org.sonarsource.organizations.api.rest.OrganizationLegacyId;
import org.sonarsource.organizations.server.DefaultOrganizationProvider;
import org.springframework.core.MethodParameter;

/**
 * Shared reflection-based machinery for injecting default organization/enterprise values into
 * annotated members. Used by both {@link DefaultsArgumentResolver} (for {@code @RequestParam}/
 * {@code @ParameterObject} parameters) and {@link DefaultsRequestBodyAdvice} (for {@code @RequestBody} DTOs).
 */
final class DefaultsInjector {

  static final Map<Class<? extends Annotation>, String> ANNOTATIONS = Map.of(
    OrganizationId.class, DefaultOrganizationProvider.ID.toString(),
    OrganizationKey.class, DefaultOrganizationProvider.KEY,
    OrganizationLegacyId.class, DefaultOrganizationProvider.LEGACY_ID,
    EnterpriseId.class, DefaultEnterpriseProvider.ENTERPRISE_ID.toString()
  );

  private DefaultsInjector() {
    // utility class
  }

  static Object injectDefaults(Object boundObject, Class<?> parameterType)
    throws ReflectiveOperationException {
    return parameterType.isRecord()
      ? injectIntoRecord(boundObject, parameterType)
      : injectIntoClass(boundObject, parameterType);
  }

  private static Object injectIntoRecord(Object boundObject, Class<?> recordClass)
    throws ReflectiveOperationException {
    RecordComponent[] components = recordClass.getRecordComponents();
    Object[] args = buildConstructorArguments(boundObject, components);
    Constructor<?> constructor = getCanonicalConstructor(recordClass, components);
    boolean accessible = constructor.canAccess(null);
    constructor.setAccessible(true);
    try {
      return constructor.newInstance(args);
    } finally {
      constructor.setAccessible(accessible);
    }
  }

  private static Object[] buildConstructorArguments(Object boundObject, RecordComponent[] components)
    throws InvocationTargetException, IllegalAccessException {
    Object[] args = new Object[components.length];
    for (int i = 0; i < components.length; i++) {
      RecordComponent component = components[i];
      Method accessor = component.getAccessor();
      boolean accessible = accessor.canAccess(boundObject);
      accessor.setAccessible(true);
      Object currentValue;
      try {
        currentValue = accessor.invoke(boundObject);
      } finally {
        accessor.setAccessible(accessible);
      }
      args[i] = getValueOrDefault(component, currentValue, component.getType(), component.getGenericType());
    }
    return args;
  }

  private static Constructor<?> getCanonicalConstructor(Class<?> recordClass, RecordComponent[] components)
    throws NoSuchMethodException {
    Class<?>[] parameterTypes = Arrays.stream(components)
      .map(RecordComponent::getType)
      .toArray(Class<?>[]::new);
    return recordClass.getDeclaredConstructor(parameterTypes);
  }

  private static Object injectIntoClass(Object boundObject, Class<?> classType) throws IllegalAccessException {
    // Walk the class hierarchy so an annotated field declared on a DTO superclass is found too.
    for (Field field : FieldUtils.getAllFieldsList(classType)) {
      String defaultValue = getDefault(field);
      if (defaultValue != null) {
        boolean accessible = field.canAccess(boundObject);
        field.setAccessible(true);
        try {
          field.set(boundObject, resolveValue(defaultValue, field.getType(), field.getGenericType()));
        } finally {
          field.setAccessible(accessible);
        }
      }
    }
    return boundObject;
  }

  private static Object getValueOrDefault(AnnotatedElement element, @Nullable Object currentValue, Class<?> targetType, Type genericType) {
    String defaultValue = getDefault(element);
    return defaultValue != null ? resolveValue(defaultValue, targetType, genericType) : currentValue;
  }

  /**
   * Uses {@link MethodParameter}'s own annotation lookup so that annotations declared on an
   * interface method's parameter are found (a raw {@link java.lang.reflect.Parameter} only sees
   * the concrete overriding method).
   */
  @CheckForNull
  static String getDefault(MethodParameter parameter) {
    return getDefault(parameter::hasParameterAnnotation);
  }

  @CheckForNull
  static String getDefault(AnnotatedElement element) {
    return getDefault(element::isAnnotationPresent);
  }

  @CheckForNull
  private static String getDefault(Predicate<Class<? extends Annotation>> hasAnnotation) {
    return ANNOTATIONS.entrySet().stream()
      .filter(entry -> hasAnnotation.test(entry.getKey()))
      .map(Map.Entry::getValue)
      .findFirst()
      .orElse(null);
  }

  static boolean hasAnnotation(MethodParameter parameter) {
    return ANNOTATIONS.keySet().stream().anyMatch(parameter::hasParameterAnnotation);
  }

  static boolean hasAnnotation(AnnotatedElement element) {
    return ANNOTATIONS.keySet().stream().anyMatch(element::isAnnotationPresent);
  }

  static Object resolveValue(String defaultValue, Class<?> targetType, Type genericType) {
    if (!isCollectionOrArray(targetType)) {
      return convert(defaultValue, targetType);
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
