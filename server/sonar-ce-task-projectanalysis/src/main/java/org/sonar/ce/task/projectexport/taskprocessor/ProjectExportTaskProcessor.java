/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectexport.taskprocessor;

import java.util.Set;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.CeTaskResult;
import org.sonar.ce.task.container.TaskContainer;
import org.sonar.ce.task.container.TaskContainerImpl;
import org.sonar.ce.task.projectexport.ProjectExportContainerPopulator;
import org.sonar.ce.task.projectexport.ProjectExportProcessor;
import org.sonar.ce.task.taskprocessor.CeTaskProcessor;
import org.sonar.core.platform.SpringComponentContainer;

import static org.sonar.db.ce.CeTaskTypes.PROJECT_EXPORT;

public class ProjectExportTaskProcessor implements CeTaskProcessor {

  private final SpringComponentContainer componentContainer;

  public ProjectExportTaskProcessor(SpringComponentContainer componentContainer) {
    this.componentContainer = componentContainer;
  }

  @Override
  public Set<String> getHandledCeTaskTypes() {
    return Set.of(PROJECT_EXPORT);
  }

  @Override
  public CeTaskResult process(CeTask task) {
    processProjectExport(task);
    return null;
  }

  private void processProjectExport(CeTask task) {
    CeTask.Component exportComponent = mandatoryComponent(task, PROJECT_EXPORT);
    failIfNotMain(exportComponent, task);
    ProjectDescriptor projectExportDescriptor = new ProjectDescriptor(exportComponent.getUuid(),
      mandatoryKey(exportComponent), mandatoryName(exportComponent));

    try (TaskContainer taskContainer = new TaskContainerImpl(componentContainer,
      new ProjectExportContainerPopulator(projectExportDescriptor))) {
      taskContainer.bootup();
      taskContainer.getComponentByType(ProjectExportProcessor.class).process();
    }

  }

  private static void failIfNotMain(CeTask.Component exportComponent, CeTask task) {
    task.getMainComponent().filter(mainComponent -> mainComponent.equals(exportComponent))
      .orElseThrow(() -> new IllegalStateException("Component of task must be the same as main component"));
  }

  private static CeTask.Component mandatoryComponent(CeTask task, String type) {
    return task.getComponent().orElseThrow(() -> new IllegalStateException(String.format("Task with type %s must have a component", type)));
  }

  private static String mandatoryKey(CeTask.Component component) {
    return component.getKey().orElseThrow(() -> new IllegalStateException("Task component must have a key"));
  }

  private static String mandatoryName(CeTask.Component component) {
    return component.getName().orElseThrow(() -> new IllegalStateException("Task component must have a name"));
  }
}
