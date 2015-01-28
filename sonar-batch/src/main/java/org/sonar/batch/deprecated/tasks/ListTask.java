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
package org.sonar.batch.deprecated.tasks;

import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;

public class ListTask implements Task {

  public static final String KEY = "list";

  public static final TaskDefinition DEFINITION = TaskDefinition.builder()
    .key(KEY)
    .description("List available tasks")
    .taskClass(ListTask.class)
    .build();

  private final Tasks tasks;

  public ListTask(Tasks tasks) {
    this.tasks = tasks;
  }

  @Override
  public void execute() {
    logBlankLine();
    log("Available tasks:");
    logBlankLine();
    for (TaskDefinition def : tasks.definitions()) {
      log("  - " + def.key() + ": " + def.description());
    }
    logBlankLine();
  }

  void log(String s) {
    System.out.println(s);
  }

  void logBlankLine() {
    System.out.println();
  }
}
