/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang.ClassUtils;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.internal.DefaultPostJobDescriptor;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.dag.DirectAcyclicGraph;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.scanner.postjob.PostJobOptimizer;
import org.sonar.scanner.sensor.SensorOptimizer;

/**
 * @since 2.6
 */
public class ScannerExtensionDictionnary {

  private final ComponentContainer componentContainer;
  private final SensorOptimizer sensorOptimizer;
  private final PostJobOptimizer postJobOptimizer;

  public ScannerExtensionDictionnary(ComponentContainer componentContainer, SensorOptimizer sensorOptimizer, PostJobOptimizer postJobOptimizer) {
    this.componentContainer = componentContainer;
    this.sensorOptimizer = sensorOptimizer;
    this.postJobOptimizer = postJobOptimizer;
  }

  public <T> Collection<T> select(Class<T> type, boolean sort, @Nullable ExtensionMatcher matcher) {
    List<T> result = getFilteredExtensions(type, matcher);
    if (sort) {
      return sort(result);
    }
    return result;
  }

  public Collection<PostJob> selectPostJobs() {
    List<PostJob> result = getFilteredExtensions(PostJob.class, null);

    Iterator<PostJob> iterator = result.iterator();
    while (iterator.hasNext()) {
      PostJob postJob = iterator.next();
      DefaultPostJobDescriptor descriptor = new DefaultPostJobDescriptor();
      postJob.describe(descriptor);
      if (!postJobOptimizer.shouldExecute(descriptor)) {
        iterator.remove();
      }
    }

    return sort(result);
  }

  public Collection<Sensor> selectSensors(boolean global) {
    List<Sensor> result = getFilteredExtensions(Sensor.class, null);

    Iterator<Sensor> iterator = result.iterator();
    while (iterator.hasNext()) {
      Sensor sensor = iterator.next();
      DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
      sensor.describe(descriptor);
      if (!sensorOptimizer.shouldExecute(descriptor) || global != descriptor.isGlobal()) {
        iterator.remove();
      }
    }

    return sort(result);
  }

  private static Phase.Name evaluatePhase(Object extension) {
    Phase phaseAnnotation = AnnotationUtils.getAnnotation(extension, Phase.class);
    if (phaseAnnotation != null) {
      return phaseAnnotation.name();
    }
    return Phase.Name.DEFAULT;
  }

  private <T> List<T> getFilteredExtensions(Class<T> type, @Nullable ExtensionMatcher matcher) {
    List<T> result = new ArrayList<>();
    List<Object> candidates = new ArrayList<>();
    candidates.addAll(getExtensions(type));

    for (Object extension : candidates) {
      if (shouldKeep(type, extension, matcher)) {
        result.add((T) extension);
      }
    }
    return result;
  }

  protected <T> List<T> getExtensions(Class<T> type) {
    List<T> extensions = new ArrayList<>();
    completeBatchExtensions(componentContainer, extensions, type);
    return extensions;
  }

  private static <T> void completeBatchExtensions(ComponentContainer container, List<T> extensions, Class<T> type) {
    extensions.addAll(container.getComponentsByType(type));
    ComponentContainer parentContainer = container.getParent();
    if (parentContainer != null) {
      completeBatchExtensions(parentContainer, extensions, type);
    }
  }

  public <T> Collection<T> sort(Collection<T> extensions) {
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
    List<Object> result = new ArrayList<>();
    result.addAll(evaluateAnnotatedClasses(extension, DependsUpon.class));
    return result;
  }

  /**
   * Objects that depend upon this extension.
   */
  public <T> List<Object> getDependents(T extension) {
    List<Object> result = new ArrayList<>();
    result.addAll(evaluateAnnotatedClasses(extension, DependedUpon.class));
    return result;
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

  protected List<Object> evaluateAnnotatedClasses(Object extension, Class<? extends Annotation> annotation) {
    List<Object> results = new ArrayList<>();
    Class<? extends Object> aClass = extension.getClass();
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
