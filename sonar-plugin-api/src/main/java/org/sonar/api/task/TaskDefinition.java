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
package org.sonar.api.task;

/**
 * Implement this interface to provide a new task.
 * @since 3.5
 */
public class TaskDefinition implements TaskComponent {

  private String name;
  private String description;
  private String command;
  private Class<? extends Task> task;

  private TaskDefinition() {

  }

  public static TaskDefinition create() {
    return new TaskDefinition();
  }

  public String getName() {
    return name;
  }

  public TaskDefinition setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public TaskDefinition setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getCommand() {
    return command;
  }

  public TaskDefinition setCommand(String command) {
    this.command = command;
    return this;
  }

  public Class<? extends Task> getTask() {
    return task;
  }

  public TaskDefinition setTask(Class<? extends Task> task) {
    this.task = task;
    return this;
  }

  @Override
  public String toString() {
    return "Definition of task " + task + " with command " + command;
  }

}
