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
package org.sonar.batch.bootstrap;

import com.google.common.collect.Lists;
import org.apache.commons.lang.ClassUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.CheckProject;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.resources.Project;
import org.sonar.batch.scan.ExtensionMatcher;

import java.util.Collection;
import java.util.List;

/**
 * @since 2.6
 */
public class BatchExtensionDictionnary extends org.sonar.api.batch.BatchExtensionDictionnary {

  public BatchExtensionDictionnary(ComponentContainer componentContainer) {
    super(componentContainer);
  }

  public <T> Collection<T> select(Class<T> type, Project project, boolean sort, ExtensionMatcher matcher) {
    List<T> result = getFilteredExtensions(type, project, matcher);
    if (sort) {
      return sort(result);
    }
    return result;
  }

  private <T> List<T> getFilteredExtensions(Class<T> type, Project project, ExtensionMatcher matcher) {
    List<T> result = Lists.newArrayList();
    for (BatchExtension extension : getExtensions()) {
      if (shouldKeep(type, extension, project, matcher)) {
        result.add((T) extension);
      }
    }
    return result;
  }

  private boolean shouldKeep(Class type, Object extension, Project project, ExtensionMatcher matcher) {
    boolean keep = ClassUtils.isAssignable(extension.getClass(), type) && (matcher == null || matcher.accept(extension));
    if (keep && project != null && ClassUtils.isAssignable(extension.getClass(), CheckProject.class)) {
      keep = ((CheckProject) extension).shouldExecuteOnProject(project);
    }
    return keep;
  }
}
