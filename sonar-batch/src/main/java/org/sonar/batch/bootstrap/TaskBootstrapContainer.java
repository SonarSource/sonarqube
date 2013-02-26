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

import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.task.TaskDefinition;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.scan.ScanTask;
import org.sonar.batch.tasks.ListTasksTask;
import org.sonar.batch.tasks.Tasks;

import javax.annotation.Nullable;

/**
 * Level-2 components. Collect tasks definitions.
 */
public class TaskBootstrapContainer extends Container {

  private String taskCommand;
  private ProjectReactor reactor;

  public TaskBootstrapContainer(@Nullable String taskCommand, @Nullable ProjectReactor reactor) {
    this.taskCommand = taskCommand;
    this.reactor = reactor;
  }

  @Override
  protected void configure() {
    registerCoreTaskDefinitions();
    registerTaskDefinitionExtensions();
    container.addSingleton(Tasks.class);
  }

  private void registerCoreTaskDefinitions() {
    container.addSingleton(ScanTask.DEFINITION);
    container.addSingleton(ListTasksTask.DEFINITION);
  }

  private void registerTaskDefinitionExtensions() {
    ExtensionInstaller installer = container.getComponentByType(ExtensionInstaller.class);
    installer.installTaskDefinitionExtensions(container);
  }

  @Override
  protected void doStart() {
    Tasks tasks = container.getComponentByType(Tasks.class);
    executeTask(tasks.getTaskDefinition(taskCommand));
  }

  private void executeTask(TaskDefinition taskDefinition) {
    boolean projectPresent = (reactor != null);
    if (ExtensionUtils.requiresProject(taskDefinition.getTask()) && !projectPresent) {
      throw new SonarException("Task '" + taskDefinition.getName() + "' requires to be run on a project");
    }
    Container childModule = new ProjectLessTaskContainer(taskDefinition, reactor);
    try {
      installChild(childModule);
      childModule.start();
    } finally {
      childModule.stop();
      uninstallChild();
    }
  }
}
