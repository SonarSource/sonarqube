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
package org.sonar.batch.tasks;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.task.TaskDefinition;
import org.sonar.api.utils.SonarException;

public class Tasks {

  private final TaskDefinition[] taskDefinitions;
  private final Settings settings;

  public Tasks(Settings settings, TaskDefinition[] taskDefinitions) {
    this.settings = settings;
    this.taskDefinitions = taskDefinitions;
  }

  public TaskDefinition getTaskDefinition(String command) {
    String finalCommand = command;
    if (StringUtils.isBlank(finalCommand)) {
      // Try with a property
      finalCommand = settings.getString(CoreProperties.TASK);
    }
    // Default to inspection task
    finalCommand = StringUtils.isNotBlank(finalCommand) ? finalCommand : InspectionTask.COMMAND;
    for (TaskDefinition taskDef : taskDefinitions) {
      if (finalCommand.equals(taskDef.getCommand())) {
        return taskDef;
      }
    }
    throw new SonarException("No task found for command: " + finalCommand);
  }

  public TaskDefinition[] getTaskDefinitions() {
    return taskDefinitions;
  }

}
