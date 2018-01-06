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

import org.sonar.api.CoreProperties;
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.scanner.analysis.AnalysisProperties;
import org.sonar.scanner.scan.ProjectScanContainer;

public class ScanTask implements Task {
  public static final TaskDefinition DEFINITION = TaskDefinition.builder()
    .description("Scan project")
    .key(CoreProperties.SCAN_TASK)
    .taskClass(ScanTask.class)
    .build();

  private final ComponentContainer taskContainer;
  private final TaskProperties taskProps;

  public ScanTask(TaskContainer taskContainer, TaskProperties taskProps) {
    this.taskContainer = taskContainer;
    this.taskProps = taskProps;
  }

  @Override
  public void execute() {
    AnalysisProperties props = new AnalysisProperties(taskProps.properties(), taskProps.property(CoreProperties.ENCRYPTION_SECRET_KEY_PATH));
    ProjectScanContainer scanContainer = new ProjectScanContainer(taskContainer, props);
    scanContainer.execute();
  }
}
