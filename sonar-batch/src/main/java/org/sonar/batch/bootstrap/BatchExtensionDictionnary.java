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

import com.google.common.collect.Lists;
import org.apache.commons.lang.ClassUtils;
import org.sonar.api.batch.CheckProject;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.analyzer.Analyzer;
import org.sonar.api.batch.analyzer.AnalyzerContext;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.resources.Project;
import org.sonar.batch.scan.SensorWrapper;
import org.sonar.batch.scan2.AnalyzerOptimizer;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @since 2.6
 */
public class BatchExtensionDictionnary extends org.sonar.api.batch.BatchExtensionDictionnary {

  private AnalyzerContext context;
  private AnalyzerOptimizer analyzerOptimizer;

  public BatchExtensionDictionnary(ComponentContainer componentContainer, AnalyzerContext context, AnalyzerOptimizer analyzerOptimizer) {
    super(componentContainer);
    this.context = context;
    this.analyzerOptimizer = analyzerOptimizer;
  }

  public <T> Collection<T> select(Class<T> type, @Nullable Project project, boolean sort, @Nullable ExtensionMatcher matcher) {
    List<T> result = getFilteredExtensions(type, project, matcher);
    if (sort) {
      return sort(result);
    }
    return result;
  }

  private <T> List<T> getFilteredExtensions(Class<T> type, @Nullable Project project, @Nullable ExtensionMatcher matcher) {
    List<T> result = Lists.newArrayList();
    for (Object extension : getExtensions(type)) {
      if (Sensor.class.equals(type) && extension instanceof Analyzer) {
        extension = new SensorWrapper((Analyzer) extension, context, analyzerOptimizer);
      }
      if (shouldKeep(type, extension, project, matcher)) {
        result.add((T) extension);
      }
    }
    if (Sensor.class.equals(type)) {
      // Retrieve Analyzer and wrap then in SensorWrapper
      for (Object extension : getExtensions(Analyzer.class)) {
        extension = new SensorWrapper((Analyzer) extension, context, analyzerOptimizer);
        if (shouldKeep(type, extension, project, matcher)) {
          result.add((T) extension);
        }
      }
    }
    return result;
  }

  private boolean shouldKeep(Class type, Object extension, @Nullable Project project, @Nullable ExtensionMatcher matcher) {
    boolean keep = (ClassUtils.isAssignable(extension.getClass(), type)
      || (Sensor.class.equals(type) && ClassUtils.isAssignable(extension.getClass(), Analyzer.class)))
      && (matcher == null || matcher.accept(extension));
    if (keep && project != null && ClassUtils.isAssignable(extension.getClass(), CheckProject.class)) {
      keep = ((CheckProject) extension).shouldExecuteOnProject(project);
    }
    return keep;
  }
}
