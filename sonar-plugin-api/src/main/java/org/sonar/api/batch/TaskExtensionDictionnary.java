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

import com.google.common.collect.Lists;
import org.apache.commons.lang.ClassUtils;
import org.sonar.api.TaskExtension;
import org.sonar.api.platform.ComponentContainer;

import java.util.Collection;
import java.util.List;

/**
 * @since 3.5
 */
public class TaskExtensionDictionnary {

  private ComponentContainer componentContainer;

  public TaskExtensionDictionnary(ComponentContainer componentContainer) {
    this.componentContainer = componentContainer;
  }

  public <T> Collection<T> select(Class<T> type) {
    List<T> result = getFilteredExtensions(type);
    return result;
  }

  private List<TaskExtension> getExtensions() {
    List<TaskExtension> extensions = Lists.newArrayList();
    completeTaskExtensions(componentContainer, extensions);
    return extensions;
  }

  private static void completeTaskExtensions(ComponentContainer container, List<TaskExtension> extensions) {
    if (container != null) {
      extensions.addAll(container.getComponentsByType(TaskExtension.class));
      completeTaskExtensions(container.getParent(), extensions);
    }
  }

  private <T> List<T> getFilteredExtensions(Class<T> type) {
    List<T> result = Lists.newArrayList();
    for (TaskExtension extension : getExtensions()) {
      if (shouldKeep(type, extension)) {
        result.add((T) extension);
      }
    }
    return result;
  }

  private boolean shouldKeep(Class type, Object extension) {
    return ClassUtils.isAssignable(extension.getClass(), type);
  }
}
