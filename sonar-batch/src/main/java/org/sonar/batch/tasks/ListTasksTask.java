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

import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;

public class ListTasksTask implements Task {

  public static final String COMMAND = "list-tasks";

  public static final TaskDefinition DEFINITION = TaskDefinition.create()
      .setDescription("List available tasks")
      .setName("List Tasks")
      .setCommand(COMMAND)
      .setTask(ListTasksTask.class);

  private final Tasks taskManager;

  public ListTasksTask(Tasks taskManager) {
    this.taskManager = taskManager;
  }

  public void execute() {
    System.out.println();
    System.out.println("Available tasks:");
    System.out.println();
    for (TaskDefinition taskDef : taskManager.getTaskDefinitions()) {
      System.out.println("  - " + taskDef.getCommand() + ": " + taskDef.getDescription());
    }
    System.out.println();
  }

}
