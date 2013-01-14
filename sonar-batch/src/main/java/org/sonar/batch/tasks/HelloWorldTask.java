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

import org.sonar.api.batch.TaskDefinition;
import org.sonar.api.batch.TaskDescriptor;
import org.sonar.api.batch.TaskExecutor;

public class HelloWorldTask implements TaskExecutor, TaskDefinition {

  public static final String COMMAND = "hello-world";

  public HelloWorldTask() {
  }

  public void execute() {
    System.out.println("HELLO WORLD");
  }

  public TaskDescriptor getTaskDescriptor() {
    return TaskDescriptor.create()
        .setDescription("Hello World")
        .setName("Hello")
        .setCommand(COMMAND);
  }

  public Class<? extends TaskExecutor> getExecutor() {
    return HelloWorldTask.class;
  }

}
