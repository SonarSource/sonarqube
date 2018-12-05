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
package org.sonar.scanner.bootstrap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang.ClassUtils;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.Phase;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.dag.DirectAcyclicGraph;
import org.sonar.core.platform.ComponentContainer;

public abstract class AbstractExtensionDictionnary {

  private final ComponentContainer componentContainer;

  public AbstractExtensionDictionnary(ComponentContainer componentContainer) {
    this.componentContainer = componentContainer;
  }

  public <T> Collection<T> select(Class<T> type, boolean sort, @Nullable ExtensionMatcher matcher) {
    List<T> result = getFilteredExtensions(type, matcher);
    if (sort) {
      return sort(result);
    }
    return result;
  }

  private static Phase.Name evaluatePhase(Object extension) {
    Phase phaseAnnotation = AnnotationUtils.getAnnotation(extension, Phase.class);
    if (phaseAnnotation != null) {
      return phaseAnnotation.name();
    }
    return Phase.Name.DEFAULT;
  }

  protected <T> List<T> getFilteredExtensions(Class<T> type, @Nullable ExtensionMatcher matcher) {
    List<T> result = new ArrayList<>();

    for (T extension : getExtensions(type)) {
      if (shouldKeep(type, extension, matcher)) {
        result.add(extension);
      }
    }
    return result;
  }

  private <T> List<T> getExtensions(Class<T> type) {
    List<T> extensions = new ArrayList<>();
    completeScannerExtensions(componentContainer, extensions, type);
    return extensions;
  }

  private static <T> void completeScannerExtensions(ComponentContainer container, List<T> extensions, Class<T> type) {
    extensions.addAll(container.getComponentsByType(type));
    ComponentContainer parentContainer = container.getParent();
    if (parentContainer != null) {
      completeScannerExtensions(parentContainer, extensions, type);
    }
  }

  protected <T> Collection<T> sort(Collection<T> extensions) {
    DirectAcyclicGraph dag = new DirectAcyclicGraph();

    for (T extension : extensions) {
      dag.add(extension);
      for (Object dependency : getDependencies(extension)) {
        dag.add(extension, dependency);
      }
      for (Object generates : getDependents(extension)) {
        dag.add(generates, extension);
      }
      completePhaseDependencies(dag, extension);
    }
    List<?> sortedList = dag.sort();

    return (Collection<T>) sortedList.stream()
      .filter(extensions::contains)
      .collect(Collectors.toList());
  }

  /**
   * Extension dependencies
   */
  private <T> List<Object> getDependencies(T extension) {
    return new ArrayList<>(evaluateAnnotatedClasses(extension, DependsUpon.class));
  }

  /**
   * Objects that depend upon this extension.
   */
  private <T> List<Object> getDependents(T extension) {
    return new ArrayList<>(evaluateAnnotatedClasses(extension, DependedUpon.class));
  }

  private static void completePhaseDependencies(DirectAcyclicGraph dag, Object extension) {
    Phase.Name phase = evaluatePhase(extension);
    dag.add(extension, phase);
    for (Phase.Name name : Phase.Name.values()) {
      if (phase.compareTo(name) < 0) {
        dag.add(name, extension);
      } else if (phase.compareTo(name) > 0) {
        dag.add(extension, name);
      }
    }
  }

  public List<Object> evaluateAnnotatedClasses(Object extension, Class<? extends Annotation> annotation) {
    List<Object> results = new ArrayList<>();
    Class<?> aClass = extension.getClass();
    while (aClass != null) {
      evaluateClass(aClass, annotation, results);

      for (Method method : aClass.getDeclaredMethods()) {
        if (method.getAnnotation(annotation) != null) {
          checkAnnotatedMethod(method);
          evaluateMethod(extension, method, results);
        }
      }
      aClass = aClass.getSuperclass();
    }

    return results;
  }

  private static void evaluateClass(Class<?> extensionClass, Class<? extends Annotation> annotationClass, List<Object> results) {
    Annotation annotation = extensionClass.getAnnotation(annotationClass);
    if (annotation != null) {
      if (annotation.annotationType().isAssignableFrom(DependsUpon.class)) {
        results.addAll(Arrays.asList(((DependsUpon) annotation).value()));

      } else if (annotation.annotationType().isAssignableFrom(DependedUpon.class)) {
        results.addAll(Arrays.asList(((DependedUpon) annotation).value()));
      }
    }

    Class<?>[] interfaces = extensionClass.getInterfaces();
    for (Class<?> anInterface : interfaces) {
      evaluateClass(anInterface, annotationClass, results);
    }
  }

  private void evaluateMethod(Object extension, Method method, List<Object> results) {
    try {
      Object result = method.invoke(extension);
      if (result != null) {
        if (result instanceof Class<?>) {
          results.addAll(componentContainer.getComponentsByType((Class<?>) result));

        } else if (result instanceof Collection<?>) {
          results.addAll((Collection<?>) result);

        } else if (result.getClass().isArray()) {
          for (int i = 0; i < Array.getLength(result); i++) {
            results.add(Array.get(result, i));
          }

        } else {
          results.add(result);
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Can not invoke method " + method, e);
    }
  }

  private static void checkAnnotatedMethod(Method method) {
    if (!Modifier.isPublic(method.getModifiers())) {
      throw new IllegalStateException("Annotated method must be public:" + method);
    }
    if (method.getParameterTypes().length > 0) {
      throw new IllegalStateException("Annotated method must not have parameters:" + method);
    }
  }

  private static boolean shouldKeep(Class<?> type, Object extension, @Nullable ExtensionMatcher matcher) {
    return ClassUtils.isAssignable(extension.getClass(), type) && (matcher == null || matcher.accept(extension));
  }
}
