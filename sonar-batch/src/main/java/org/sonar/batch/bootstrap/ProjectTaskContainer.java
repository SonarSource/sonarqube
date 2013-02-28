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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.DefaultFileLinesContextFactory;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.scan.ScanTask;
import org.sonar.core.component.ScanGraph;
import org.sonar.core.component.ScanGraphStore;
import org.sonar.core.component.ScanPerspectives;
import org.sonar.core.test.TestPlanBuilder;
import org.sonar.core.test.TestableBuilder;

/**
 * Level-4 components. Task-level components that depends on project.
 */
public class ProjectTaskContainer extends Container {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectTaskContainer.class);

  private TaskDefinition taskDefinition;

  public ProjectTaskContainer(TaskDefinition task) {
    this.taskDefinition = task;
  }

  @Override
  protected void configure() {
    registerCoreProjectTasks();
    registerCoreComponentsRequiringProject();
    registerProjectTaskExtensions();
    registerOverrideAbleComponents();
  }

  private void registerCoreProjectTasks() {
    container.addSingleton(ScanTask.class);
  }

  private void registerProjectTaskExtensions() {
    ExtensionInstaller installer = container.getComponentByType(ExtensionInstaller.class);
    installer.installTaskExtensions(container, true);
  }

  private void registerCoreComponentsRequiringProject() {
    container.addSingleton(ProjectTree.class);
    container.addSingleton(DefaultFileLinesContextFactory.class);
    container.addSingleton(ProjectLock.class);

    // graphs
    container.addSingleton(ScanGraph.create());
    container.addSingleton(TestPlanBuilder.class);
    container.addSingleton(TestableBuilder.class);
    container.addSingleton(ScanPerspectives.class);
    container.addSingleton(ScanGraphStore.class);
  }

  /**
   * In order for instance for the plugin Views to override some components
   */
  protected void registerOverrideAbleComponents(){
    container.addSingleton(DefaultIndex.class);
    container.addSingleton(ProjectLock.class);
  }

  private void logSettings() {
    LOG.info("-------------  Executing {}", taskDefinition.getName());
  }

  /**
   * Execute task
   */
  @Override
  protected void doStart() {
    Task task = container.getComponentByType(taskDefinition.getTask());
    if (task != null) {
      logSettings();
      task.execute();
    } else {
      throw new SonarException("Extension " + taskDefinition.getTask() + " was not found in declared extensions.");
    }
  }

}
