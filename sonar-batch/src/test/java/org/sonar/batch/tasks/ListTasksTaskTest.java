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

import org.junit.Test;
import org.sonar.api.task.TaskDefinition;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ListTasksTaskTest {
  @Test
  public void should_list_available_tasks() {
    TaskDefinition def = TaskDefinition.create().setCommand("purge").setName("Purge").setDescription("Purge database");
    Tasks tasks = mock(Tasks.class);
    when(tasks.getTaskDefinitions()).thenReturn(new TaskDefinition[]{def});
    ListTasksTask task = spy(new ListTasksTask(tasks));

    task.execute();

    verify(task, times(1)).log("Available tasks:");
    verify(task, times(1)).log("  - purge: Purge database");
  }
}
