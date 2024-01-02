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
package org.sonar.ce.task.projectexport;

import java.util.List;
import org.sonar.ce.task.container.TaskContainer;
import org.sonar.ce.task.projectexport.component.ComponentRepositoryImpl;
import org.sonar.ce.task.projectexport.rule.RuleRepositoryImpl;
import org.sonar.ce.task.projectexport.steps.DumpWriterImpl;
import org.sonar.ce.task.projectexport.steps.MutableMetricRepositoryImpl;
import org.sonar.ce.task.projectexport.steps.MutableProjectHolderImpl;
import org.sonar.ce.task.projectexport.taskprocessor.ProjectDescriptor;
import org.sonar.ce.task.projectexport.util.ProjectExportDumpFSImpl;
import org.sonar.ce.task.setting.SettingsLoader;
import org.sonar.core.platform.ContainerPopulator;

public class ProjectExportContainerPopulator implements ContainerPopulator<TaskContainer> {
  private static final List<Class<?>> COMPONENT_CLASSES = List.of(
    SettingsLoader.class,
    ProjectExportProcessor.class,
    MutableMetricRepositoryImpl.class,
    MutableProjectHolderImpl.class,
    ProjectExportDumpFSImpl.class,
    DumpWriterImpl.class,
    RuleRepositoryImpl.class,
    ComponentRepositoryImpl.class);

  private final ProjectDescriptor projectDescriptor;

  public ProjectExportContainerPopulator(ProjectDescriptor descriptor) {
    this.projectDescriptor = descriptor;
  }

  @Override
  public void populateContainer(TaskContainer container) {
    ProjectExportComputationSteps steps = new ProjectExportComputationSteps(container);

    container.add(projectDescriptor);
    container.add(steps);
    container.add(COMPONENT_CLASSES);
    container.add(steps.orderedStepClasses());
  }

}
