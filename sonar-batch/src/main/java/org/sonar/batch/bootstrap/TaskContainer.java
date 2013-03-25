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

import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;
import org.sonar.api.task.TaskExtension;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.scan.ScanTask;
import org.sonar.batch.tasks.ListTask;
import org.sonar.batch.tasks.Tasks;
import org.sonar.core.resource.DefaultResourcePermissions;

public class TaskContainer extends ComponentContainer {

  public TaskContainer(ComponentContainer parent) {
    super(parent);
  }

  @Override
  protected void doBeforeStart() {
    installCoreTasks();
    installTaskExtensions();
    installComponentsUsingTaskExtensions();
  }

  private void installCoreTasks() {
    add(
        ScanTask.DEFINITION, ScanTask.class,
        ListTask.DEFINITION, ListTask.class);
  }

  private void installTaskExtensions() {
    getComponentByType(ExtensionInstaller.class).install(this, new ExtensionInstaller.ComponentFilter() {
      public boolean accept(Object extension) {
        return ExtensionUtils.isType(extension, TaskExtension.class);
      }
    });
  }

  private void installComponentsUsingTaskExtensions() {
    add(
        ResourceTypes.class,
        DefaultResourcePermissions.class,
        Tasks.class);
  }

  @Override
  public void doAfterStart() {
    // default value is declared in CorePlugin
    String taskKey = getComponentByType(Settings.class).getString(CoreProperties.TASK);

    TaskDefinition def = getComponentByType(Tasks.class).definition(taskKey);
    if (def == null) {
      throw new SonarException("Task " + taskKey + " does not exist");
    }
    Task task = getComponentByType(def.taskClass());
    if (task != null) {
      task.execute();
    } else {
      throw new IllegalStateException("Task " + taskKey + " is badly defined");
    }
  }
}
