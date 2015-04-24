/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.bootstrap;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.apache.commons.lang.ClassUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.CheckProject;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.dag.DirectAcyclicGraph;
import org.sonar.batch.postjob.PostJobOptimizer;
import org.sonar.batch.postjob.PostJobWrapper;
import org.sonar.batch.sensor.DefaultSensorContext;
import org.sonar.batch.sensor.SensorOptimizer;
import org.sonar.batch.sensor.SensorWrapper;

import javax.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @since 2.6
 */
public class BatchExtensionDictionnary {

  private final ComponentContainer componentContainer;
  private final SensorContext sensorContext;
  private final SensorOptimizer sensorOptimizer;
  private final PostJobContext postJobContext;
  private final PostJobOptimizer postJobOptimizer;

  public BatchExtensionDictionnary(ComponentContainer componentContainer, DefaultSensorContext sensorContext, SensorOptimizer sensorOptimizer, PostJobContext postJobContext,
    PostJobOptimizer postJobOptimizer) {
    this.componentContainer = componentContainer;
    this.sensorContext = sensorContext;
    this.sensorOptimizer = sensorOptimizer;
    this.postJobContext = postJobContext;
    this.postJobOptimizer = postJobOptimizer;
  }

  public <T> Collection<T> select(Class<T> type, @Nullable Project project, boolean sort, @Nullable ExtensionMatcher matcher) {
    List<T> result = getFilteredExtensions(type, project, matcher);
    if (sort) {
      return sort(result);
    }
    return result;
  }

  private Phase.Name evaluatePhase(Object extension) {
    Object extensionToEvaluate;
    if (extension instanceof SensorWrapper) {
      extensionToEvaluate = ((SensorWrapper) extension).wrappedSensor();
    } else {
      extensionToEvaluate = extension;
    }
    Phase phaseAnnotation = AnnotationUtils.getAnnotation(extensionToEvaluate, Phase.class);
    if (phaseAnnotation != null) {
      return phaseAnnotation.name();
    }
    return Phase.Name.DEFAULT;
  }

  private <T> List<T> getFilteredExtensions(Class<T> type, @Nullable Project project, @Nullable ExtensionMatcher matcher) {
    List<T> result = Lists.newArrayList();
    for (Object extension : getExtensions(type)) {
      if (org.sonar.api.batch.Sensor.class.equals(type) && extension instanceof Sensor) {
        extension = new SensorWrapper((Sensor) extension, sensorContext, sensorOptimizer);
      }
      if (shouldKeep(type, extension, project, matcher)) {
        result.add((T) extension);
      }
    }
    if (org.sonar.api.batch.Sensor.class.equals(type)) {
      // Retrieve new Sensors and wrap then in SensorWrapper
      for (Object extension : getExtensions(Sensor.class)) {
        extension = new SensorWrapper((Sensor) extension, sensorContext, sensorOptimizer);
        if (shouldKeep(type, extension, project, matcher)) {
          result.add((T) extension);
        }
      }
    }
    if (org.sonar.api.batch.PostJob.class.equals(type)) {
      // Retrieve new PostJob and wrap then in PostJobWrapper
      for (Object extension : getExtensions(PostJob.class)) {
        extension = new PostJobWrapper((PostJob) extension, postJobContext, postJobOptimizer);
        if (shouldKeep(type, extension, project, matcher)) {
          result.add((T) extension);
        }
      }
    }
    return result;
  }

  protected List<Object> getExtensions(@Nullable Class type) {
    List<Object> extensions = Lists.newArrayList();
    completeBatchExtensions(componentContainer, extensions, type);
    return extensions;
  }

  private static void completeBatchExtensions(ComponentContainer container, List<Object> extensions, @Nullable Class type) {
    if (container != null) {
      extensions.addAll(container.getComponentsByType(type != null ? type : BatchExtension.class));
      completeBatchExtensions(container.getParent(), extensions, type);
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
    List sortedList = dag.sort();

    return Collections2.filter(sortedList, Predicates.in(extensions));
  }

  /**
   * Extension dependencies
   */
  private <T> List<Object> getDependencies(T extension) {
    List<Object> result = new ArrayList<Object>();
    result.addAll(evaluateAnnotatedClasses(extension, DependsUpon.class));
    return result;
  }

  /**
   * Objects that depend upon this extension.
   */
  public <T> List<Object> getDependents(T extension) {
    List<Object> result = new ArrayList<Object>();
    result.addAll(evaluateAnnotatedClasses(extension, DependedUpon.class));
    return result;
  }

  private void completePhaseDependencies(DirectAcyclicGraph dag, Object extension) {
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
    List<Object> results = Lists.newArrayList();
    Class aClass = extension.getClass();
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

  private void evaluateClass(Class extensionClass, Class annotationClass, List<Object> results) {
    Annotation annotation = extensionClass.getAnnotation(annotationClass);
    if (annotation != null) {
      if (annotation.annotationType().isAssignableFrom(DependsUpon.class)) {
        results.addAll(Arrays.asList(((DependsUpon) annotation).value()));

      } else if (annotation.annotationType().isAssignableFrom(DependedUpon.class)) {
        results.addAll(Arrays.asList(((DependedUpon) annotation).value()));
      }
    }

    Class[] interfaces = extensionClass.getInterfaces();
    for (Class anInterface : interfaces) {
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

  private void checkAnnotatedMethod(Method method) {
    if (!Modifier.isPublic(method.getModifiers())) {
      throw new IllegalStateException("Annotated method must be public:" + method);
    }
    if (method.getParameterTypes().length > 0) {
      throw new IllegalStateException("Annotated method must not have parameters:" + method);
    }
  }

  private boolean shouldKeep(Class type, Object extension, @Nullable Project project, @Nullable ExtensionMatcher matcher) {
    boolean keep = (ClassUtils.isAssignable(extension.getClass(), type)
      || (org.sonar.api.batch.Sensor.class.equals(type) && ClassUtils.isAssignable(extension.getClass(), Sensor.class)))
      && (matcher == null || matcher.accept(extension));
    if (keep && project != null && ClassUtils.isAssignable(extension.getClass(), CheckProject.class)) {
      keep = ((CheckProject) extension).shouldExecuteOnProject(project);
    }
    return keep;
  }
}
