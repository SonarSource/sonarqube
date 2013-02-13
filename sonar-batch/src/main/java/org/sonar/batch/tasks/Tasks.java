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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskComponent;
import org.sonar.api.task.TaskDefinition;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.scan.ScanTask;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Tasks implements TaskComponent {

  private static final Logger LOG = LoggerFactory.getLogger(Tasks.class);
  private static final String COMMAND_PATTERN = "[a-zA-Z0-9\\-\\_]+";

  private final TaskDefinition[] taskDefinitions;
  private final Settings settings;

  private final Map<String, TaskDefinition> taskDefByCommand = new HashMap<String, TaskDefinition>();
  private final Map<Class<? extends Task>, TaskDefinition> taskDefByTask = new HashMap<Class<? extends Task>, TaskDefinition>();

  public Tasks(Settings settings, TaskDefinition[] taskDefinitions) {
    this.settings = settings;
    this.taskDefinitions = taskDefinitions;
  }

  public TaskDefinition getTaskDefinition(@Nullable String command) {
    String finalCommand = command;
    if (StringUtils.isBlank(finalCommand)) {
      // Try with a property
      finalCommand = settings.getString(CoreProperties.TASK);
    }
    // Default to inspection task
    finalCommand = StringUtils.isNotBlank(finalCommand) ? finalCommand : ScanTask.COMMAND;
    if (taskDefByCommand.containsKey(finalCommand)) {
      return taskDefByCommand.get(finalCommand);
    }
    throw new SonarException("No task found for command: " + finalCommand);
  }

  public TaskDefinition[] getTaskDefinitions() {
    return taskDefinitions;
  }

  /**
   * Perform validation of tasks definitions
   */
  public void start() {
    for (TaskDefinition def : taskDefinitions) {
      validateTask(def);
      validateName(def);
      validateCommand(def);
      validateDescription(def);
    }
  }

  private void validateName(TaskDefinition def) {
    if (StringUtils.isBlank(def.getName())) {
      throw new SonarException("Task definition for task '" + def.getTask().getName() + "' doesn't define task name");
    }

  }

  private void validateCommand(TaskDefinition def) {
    String command = def.getCommand();
    if (StringUtils.isBlank(command)) {
      throw new SonarException("Task definition '" + def.getName() + "' doesn't define task command");
    }
    if (!Pattern.matches(COMMAND_PATTERN, command)) {
      throw new SonarException("Command '" + command + "' for task definition '" + def.getName() + "' is not valid and should match " + COMMAND_PATTERN);
    }
    if (taskDefByCommand.containsKey(command)) {
      throw new SonarException("Task '" + def.getName() + "' uses the same command than task '" + taskDefByCommand.get(command).getName() + "'");
    }
    taskDefByCommand.put(command, def);
  }

  private void validateDescription(TaskDefinition def) {
    if (StringUtils.isBlank(def.getDescription())) {
      LOG.warn("Task definition {} doesn't define a description. Using name as description.", def.getName());
      def.setDescription(def.getName());
    }
  }

  private void validateTask(TaskDefinition def) {
    Class<? extends Task> taskClass = def.getTask();
    if (taskClass == null) {
      throw new SonarException("Task definition '" + def.getName() + "' doesn't define the associated task class");
    }
    if (taskDefByTask.containsKey(taskClass)) {
      throw new SonarException("Task '" + def.getTask().getName() + "' is defined twice: first by '" + taskDefByTask.get(taskClass).getName() + "' and then by '" + def.getName()
        + "'");
    }
    taskDefByTask.put(taskClass, def);
  }

}
