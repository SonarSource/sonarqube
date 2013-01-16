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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.task.TaskDefinition;
import org.sonar.api.utils.SonarException;

import static org.fest.assertions.Assertions.assertThat;

public class TasksTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private Settings settings;

  @Before
  public void prepare() {
    settings = new Settings();
  }

  @Test
  public void shouldReturnTaskDefinitions() {
    Tasks tasks = new Tasks(settings, new TaskDefinition[] {InspectionTask.DEFINITION, ListTasksTask.DEFINITION});
    assertThat(tasks.getTaskDefinitions().length).isEqualTo(2);
  }

  @Test
  public void shouldReturnInspectionTask() {
    Tasks tasks = new Tasks(settings, new TaskDefinition[] {InspectionTask.DEFINITION, ListTasksTask.DEFINITION});
    assertThat(tasks.getTaskDefinition(InspectionTask.COMMAND)).isEqualTo(InspectionTask.DEFINITION);
  }

  @Test
  public void shouldReturnInspectionTaskByDefault() {
    Tasks tasks = new Tasks(settings, new TaskDefinition[] {InspectionTask.DEFINITION, ListTasksTask.DEFINITION});
    assertThat(tasks.getTaskDefinition(null)).isEqualTo(InspectionTask.DEFINITION);
  }

  @Test
  public void shouldReturnUsePropertyWhenNoCommand() {
    Tasks tasks = new Tasks(settings, new TaskDefinition[] {InspectionTask.DEFINITION, ListTasksTask.DEFINITION});
    assertThat(tasks.getTaskDefinition(ListTasksTask.COMMAND)).isEqualTo(ListTasksTask.DEFINITION);
    assertThat(tasks.getTaskDefinition(null)).isEqualTo(InspectionTask.DEFINITION);

    settings.setProperty(CoreProperties.TASK, ListTasksTask.COMMAND);
    assertThat(tasks.getTaskDefinition(null)).isEqualTo(ListTasksTask.DEFINITION);
    assertThat(tasks.getTaskDefinition(InspectionTask.COMMAND)).isEqualTo(InspectionTask.DEFINITION);
  }

  @Test
  public void shouldThrowWhenCommandNotFound() {
    Tasks tasks = new Tasks(settings, new TaskDefinition[] {InspectionTask.DEFINITION, ListTasksTask.DEFINITION});

    thrown.expect(SonarException.class);
    thrown.expectMessage("No task found for command: not-exists");

    tasks.getTaskDefinition("not-exists");
  }
}
