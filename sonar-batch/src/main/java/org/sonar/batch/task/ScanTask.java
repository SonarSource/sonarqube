/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.task;

import javax.annotation.CheckForNull;
import org.sonar.api.CoreProperties;
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;
import org.sonar.batch.analysis.AnalysisProperties;
import org.sonar.batch.analysis.DefaultAnalysisMode;
import org.sonar.batch.bootstrap.GlobalProperties;
import org.sonar.batch.cache.ProjectSyncContainer;
import org.sonar.batch.scan.ProjectScanContainer;
import org.sonar.core.platform.ComponentContainer;

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
    if (isIssuesMode(props)) {
      String projectKey = getProjectKeyWithBranch(props);
      new ProjectSyncContainer(taskContainer, projectKey, false).execute();
    }
    new ProjectScanContainer(taskContainer, props).execute();
  }

  @CheckForNull
  private static String getProjectKeyWithBranch(AnalysisProperties props) {
    String projectKey = props.property(CoreProperties.PROJECT_KEY_PROPERTY);
    if (projectKey != null && props.property(CoreProperties.PROJECT_BRANCH_PROPERTY) != null) {
      projectKey = projectKey + ":" + props.property(CoreProperties.PROJECT_BRANCH_PROPERTY);
    }
    return projectKey;
  }

  private boolean isIssuesMode(AnalysisProperties props) {
    DefaultAnalysisMode mode = new DefaultAnalysisMode(taskContainer.getComponentByType(GlobalProperties.class), props);
    return mode.isIssues();
  }

}
