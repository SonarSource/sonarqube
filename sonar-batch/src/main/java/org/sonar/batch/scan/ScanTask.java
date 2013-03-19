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
package org.sonar.batch.scan;

import org.sonar.api.CoreProperties;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;
import org.sonar.batch.DefaultProfileLoader;
import org.sonar.batch.DefaultProjectTree;
import org.sonar.batch.bootstrap.TaskContainer;
import org.sonar.batch.phases.Phases;

public class ScanTask implements Task {
  public static final TaskDefinition DEFINITION = TaskDefinition.builder()
      .description("Scan project")
      .key(CoreProperties.SCAN_TASK)
      .taskClass(ScanTask.class)
      .build();

  private final ComponentContainer taskContainer;

  public ScanTask(TaskContainer taskContainer) {
    this.taskContainer = taskContainer;
  }

  public void execute() {
    scan(new ProjectScanContainer(taskContainer));
  }

  // Add components specific to project scan (views will use different ones)
  void scan(ComponentContainer scanContainer) {
    scanContainer.add(
        new Phases().enable(Phases.Phase.values()),
        DefaultProjectTree.class,
        ProjectExclusions.class,
        ProjectReactorReady.class,
        DefaultProfileLoader.class);
    scanContainer.execute();
  }
}
