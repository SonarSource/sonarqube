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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.TaskDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.tasks.AnalyseProjectTaskDefinition;
import org.sonar.batch.tasks.TaskManager;

/**
 * Level-2 components. Collect tasks definitions.
 */
public class TaskBootstrapModule extends Module {

  private String taskCommand;

  public TaskBootstrapModule(String taskCommand) {
    this.taskCommand = taskCommand;
  }

  @Override
  protected void configure() {
    registerCoreTaskDefinitions();
    registerTaskDefinitionExtensions();
    container.addSingleton(TaskManager.class);
  }

  private void registerCoreTaskDefinitions() {
    container.addSingleton(AnalyseProjectTaskDefinition.class);
  }

  private void registerTaskDefinitionExtensions() {
    ExtensionInstaller installer = container.getComponentByType(ExtensionInstaller.class);
    installer.installTaskDefinitionExtensions(container);
  }

  @Override
  protected void doStart() {
    String command = StringUtils.isNotBlank(taskCommand) ? taskCommand : AnalyseProjectTaskDefinition.COMMAND;
    TaskManager manager = container.getComponentByType(TaskManager.class);
    executeTask(manager.getTask(command));
  }

  private void executeTask(TaskDefinition task) {
    if (task.getTaskDescriptor().isRequiresProject() && container.getComponentByType(ProjectReactor.class) == null) {
      throw new SonarException("Task " + task.getTaskDescriptor().getName() + " requires to be run on a project");
    }
    Module childModule;
    if (task.getTaskDescriptor().isRequiresProject()) {
      childModule = new ProjectTaskModule(task);
    }
    else {
      childModule = new ProjectLessTaskModule(task);
    }
    try {
      installChild(childModule);
      childModule.start();
    } finally {
      childModule.stop();
      uninstallChild();
    }
  }
}
