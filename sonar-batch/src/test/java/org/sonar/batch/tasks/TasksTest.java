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
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.scan.ScanTask;

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
    Tasks tasks = new Tasks(settings, new TaskDefinition[] {ScanTask.DEFINITION, ListTasksTask.DEFINITION});
    assertThat(tasks.getTaskDefinitions().length).isEqualTo(2);
  }

  @Test
  public void shouldReturnInspectionTask() {
    Tasks tasks = new Tasks(settings, new TaskDefinition[] {ScanTask.DEFINITION, ListTasksTask.DEFINITION});
    tasks.start();
    assertThat(tasks.getTaskDefinition(ScanTask.COMMAND)).isEqualTo(ScanTask.DEFINITION);
  }

  @Test
  public void shouldReturnInspectionTaskByDefault() {
    Tasks tasks = new Tasks(settings, new TaskDefinition[] {ScanTask.DEFINITION, ListTasksTask.DEFINITION});
    tasks.start();
    assertThat(tasks.getTaskDefinition(null)).isEqualTo(ScanTask.DEFINITION);
  }

  @Test
  public void shouldReturnUsePropertyWhenNoCommand() {
    Tasks tasks = new Tasks(settings, new TaskDefinition[] {ScanTask.DEFINITION, ListTasksTask.DEFINITION});
    tasks.start();
    assertThat(tasks.getTaskDefinition(ListTasksTask.COMMAND)).isEqualTo(ListTasksTask.DEFINITION);
    assertThat(tasks.getTaskDefinition(null)).isEqualTo(ScanTask.DEFINITION);

    settings.setProperty(CoreProperties.TASK, ListTasksTask.COMMAND);
    assertThat(tasks.getTaskDefinition(null)).isEqualTo(ListTasksTask.DEFINITION);
    assertThat(tasks.getTaskDefinition(ScanTask.COMMAND)).isEqualTo(ScanTask.DEFINITION);
  }

  @Test
  public void shouldThrowWhenCommandNotFound() {
    Tasks tasks = new Tasks(settings, new TaskDefinition[] {ScanTask.DEFINITION, ListTasksTask.DEFINITION});

    thrown.expect(SonarException.class);
    thrown.expectMessage("No task found for command: not-exists");

    tasks.getTaskDefinition("not-exists");
  }

  @Test
  public void shouldThrowWhenCommandMissing() {
    Tasks tasks = new Tasks(settings, new TaskDefinition[] {TaskDefinition.create().setName("foo").setTask(FakeTask1.class)});

    thrown.expect(SonarException.class);
    thrown.expectMessage("Task definition 'foo' doesn't define task command");

    tasks.start();
  }

  @Test
  public void shouldThrowWhenCommandInvalid() {
    Tasks tasks = new Tasks(settings, new TaskDefinition[] {TaskDefinition.create().setName("foo").setTask(FakeTask1.class).setCommand("Azc-12_bbC")});
    tasks.start();

    tasks = new Tasks(settings, new TaskDefinition[] {TaskDefinition.create().setName("foo").setTask(FakeTask1.class).setCommand("with space")});

    thrown.expect(SonarException.class);
    thrown.expectMessage("Command 'with space' for task definition 'foo' is not valid and should match [a-zA-Z0-9\\-\\_]+");

    tasks.start();
  }

  @Test
  public void shouldThrowWhenDuplicateCommand() {
    Tasks tasks = new Tasks(settings, new TaskDefinition[] {
      TaskDefinition.create().setName("foo1").setTask(FakeTask1.class).setCommand("cmd"),
      TaskDefinition.create().setName("foo2").setTask(FakeTask2.class).setCommand("cmd")});

    thrown.expect(SonarException.class);
    thrown.expectMessage("Task 'foo2' uses the same command than task 'foo1'");

    tasks.start();
  }

  @Test
  public void shouldThrowWhenNameMissing() {
    Tasks tasks = new Tasks(settings, new TaskDefinition[] {TaskDefinition.create().setCommand("foo").setTask(FakeTask1.class)});

    thrown.expect(SonarException.class);
    thrown.expectMessage("Task definition for task 'org.sonar.batch.tasks.TasksTest$FakeTask1' doesn't define task name");

    tasks.start();
  }

  @Test
  public void shouldThrowWhenTaskMissing() {
    Tasks tasks = new Tasks(settings, new TaskDefinition[] {TaskDefinition.create().setCommand("foo").setName("bar")});

    thrown.expect(SonarException.class);
    thrown.expectMessage("Task definition 'bar' doesn't define the associated task class");

    tasks.start();
  }

  @Test
  public void shouldThrowWhenDuplicateTask() {
    Tasks tasks = new Tasks(settings, new TaskDefinition[] {
      TaskDefinition.create().setName("foo1").setTask(FakeTask1.class).setCommand("cmd1"),
      TaskDefinition.create().setName("foo2").setTask(FakeTask1.class).setCommand("cmd2")});

    thrown.expect(SonarException.class);
    thrown.expectMessage("Task 'org.sonar.batch.tasks.TasksTest$FakeTask1' is defined twice: first by 'foo1' and then by 'foo2'");

    tasks.start();
  }

  @Test
  public void shouldUseNameWhenDescriptionIsMissing() {
    Tasks tasks = new Tasks(settings, new TaskDefinition[] {TaskDefinition.create().setName("foo").setCommand("cmd").setTask(FakeTask1.class)});
    tasks.start();

    assertThat(tasks.getTaskDefinition("cmd").getDescription()).isEqualTo("foo");
  }

  private static class FakeTask1 implements Task {

    public void execute() {
    }

  }

  private static class FakeTask2 implements Task {

    public void execute() {
    }

  }

}
