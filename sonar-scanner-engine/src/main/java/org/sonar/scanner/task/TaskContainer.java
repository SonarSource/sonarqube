/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.task;

import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;
import org.sonar.api.utils.MessageException;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.scanner.bootstrap.ExtensionInstaller;
import org.sonar.scanner.bootstrap.ExtensionMatcher;
import org.sonar.scanner.bootstrap.ExtensionUtils;
import org.sonar.scanner.bootstrap.GlobalProperties;

public class TaskContainer extends ComponentContainer {

  private final Map<String, String> taskProperties;
  private final Object[] components;

  public TaskContainer(ComponentContainer parent, Map<String, String> taskProperties, Object... components) {
    super(parent);
    this.taskProperties = taskProperties;
    this.components = components;
  }

  @Override
  protected void doBeforeStart() {
    addTaskExtensions();
    addCoreComponents();
    for (Object component : components) {
      add(component);
    }
  }

  private void addCoreComponents() {
    add(new TaskProperties(taskProperties, getParent().getComponentByType(GlobalProperties.class).property(CoreProperties.ENCRYPTION_SECRET_KEY_PATH)));
  }

  private void addTaskExtensions() {
    getComponentByType(ExtensionInstaller.class).install(this, new TaskExtensionFilter());
  }

  static class TaskExtensionFilter implements ExtensionMatcher {
    @Override
    public boolean accept(Object extension) {
      return ExtensionUtils.isScannerSide(extension)
        && ExtensionUtils.isInstantiationStrategy(extension, InstantiationStrategy.PER_TASK);
    }
  }

  @Override
  public void doAfterStart() {
    // default value is declared in CorePlugin
    String taskKey = StringUtils.defaultIfEmpty(taskProperties.get(CoreProperties.TASK), CoreProperties.SCAN_TASK);
    // Release memory
    taskProperties.clear();

    TaskDefinition def = getComponentByType(Tasks.class).definition(taskKey);
    if (def == null) {
      throw MessageException.of("Task '" + taskKey + "' does not exist. Please use '" + ListTask.KEY + "' task to see all available tasks.");
    }
    Task task = getComponentByType(def.taskClass());
    if (task != null) {
      task.execute();
    } else {
      throw new IllegalStateException("Task " + taskKey + " is badly defined");
    }
  }
}
