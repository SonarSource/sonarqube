/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.batch;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.apache.commons.lang.ClassUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.dag.DirectAcyclicGraph;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @since 1.11
 */
public class BatchExtensionDictionnary {

  private ComponentContainer componentContainer;

  public BatchExtensionDictionnary(ComponentContainer componentContainer) {
    this.componentContainer = componentContainer;
  }

  public <T> Collection<T> select(Class<T> type) {
    return select(type, null, false);
  }

  public <T> Collection<T> select(Class<T> type, Project project, boolean sort) {
    List<T> result = getFilteredExtensions(type, project);
    if (sort) {
      return sort(result);
    }
    return result;
  }

  public Collection<MavenPluginHandler> selectMavenPluginHandlers(Project project) {
    Collection<DependsUponMavenPlugin> selectedExtensions = select(DependsUponMavenPlugin.class, project, true);
    List<MavenPluginHandler> handlers = Lists.newArrayList();
    for (DependsUponMavenPlugin extension : selectedExtensions) {
      MavenPluginHandler handler = extension.getMavenPluginHandler(project);
      if (handler != null) {
        boolean ok = true;
        if (handler instanceof CheckProject) {
          ok = ((CheckProject) handler).shouldExecuteOnProject(project);
        }
        if (ok) {
          handlers.add(handler);
        }
      }

    }
    return handlers;
  }

  private List<BatchExtension> getExtensions() {
    List<BatchExtension> extensions = Lists.newArrayList();
    completeBatchExtensions(componentContainer, extensions);
    return extensions;
  }

  private static void completeBatchExtensions(ComponentContainer container, List<BatchExtension> extensions) {
    if (container != null) {
      extensions.addAll(container.getComponentsByType(BatchExtension.class));
      completeBatchExtensions(container.getParent(), extensions);
    }
  }

  private <T> List<T> getFilteredExtensions(Class<T> type, Project project) {
    List<T> result = Lists.newArrayList();
    for (BatchExtension extension : getExtensions()) {
      if (shouldKeep(type, extension, project)) {
        result.add((T) extension);
      }
    }
    return result;
  }

  private boolean shouldKeep(Class type, Object extension, Project project) {
    boolean keep = ClassUtils.isAssignable(extension.getClass(), type);
    if (keep && project != null && ClassUtils.isAssignable(extension.getClass(), CheckProject.class)) {
      keep = ((CheckProject) extension).shouldExecuteOnProject(project);
    }
    return keep;
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
  private <T> List getDependencies(T extension) {
    return evaluateAnnotatedClasses(extension, DependsUpon.class);
  }

  /**
   * Objects that depend upon this extension.
   */
  public <T> List getDependents(T extension) {
    return evaluateAnnotatedClasses(extension, DependedUpon.class);
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


  protected List evaluateAnnotatedClasses(Object extension, Class<? extends Annotation> annotation) {
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

  protected Phase.Name evaluatePhase(Object extension) {
    Phase phaseAnnotation = AnnotationUtils.getAnnotation(extension, Phase.class);
    if (phaseAnnotation != null) {
      return phaseAnnotation.name();
    }
    return Phase.Name.DEFAULT;
  }

  private void evaluateMethod(Object extension, Method method, List<Object> results) {
    try {
      Object result = method.invoke(extension);
      if (result != null) {
        //TODO add arrays/collections of objects/classes
        if (result instanceof Class<?>) {
          results.addAll(componentContainer.getComponentsByType((Class<?>) result));

        } else if (result instanceof Collection<?>) {
          results.addAll((Collection<?>) result);

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
      throw new IllegalStateException("Annotated method must be public :" + method);
    }
    if (method.getParameterTypes().length > 0) {
      throw new IllegalStateException("Annotated method must not have parameters :" + method);
    }
  }
}
