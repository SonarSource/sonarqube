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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.scan.ScanTask;

import static org.assertj.core.api.Assertions.assertThat;

public class TasksTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_get_definitions() {
    Tasks tasks = new Tasks(new TaskDefinition[] {ScanTask.DEFINITION, ListTask.DEFINITION});
    assertThat(tasks.definitions()).hasSize(2);
  }

  @Test
  public void should_get_definition_by_key() {
    Tasks tasks = new Tasks(new TaskDefinition[] {ScanTask.DEFINITION, ListTask.DEFINITION});
    tasks.start();
    assertThat(tasks.definition(ListTask.DEFINITION.key())).isEqualTo(ListTask.DEFINITION);
  }

  @Test
  public void should_return_null_if_task_not_found() {
    Tasks tasks = new Tasks(new TaskDefinition[] {ScanTask.DEFINITION, ListTask.DEFINITION});

    assertThat(tasks.definition("not-exists")).isNull();
  }

  @Test
  public void should_fail_on_duplicated_keys() {
    thrown.expect(SonarException.class);
    thrown.expectMessage("Task 'foo' is declared twice");

    new Tasks(new TaskDefinition[] {
      TaskDefinition.builder().key("foo").taskClass(FakeTask1.class).description("foo1").build(),
      TaskDefinition.builder().key("foo").taskClass(FakeTask2.class).description("foo2").build()
    });
  }

  @Test
  public void should_fail_on_duplicated_class() {
    Tasks tasks = new Tasks(new TaskDefinition[] {
      TaskDefinition.builder().key("foo1").taskClass(FakeTask1.class).description("foo1").build(),
      TaskDefinition.builder().key("foo2").taskClass(FakeTask1.class).description("foo1").build()
    });

    thrown.expect(SonarException.class);
    thrown.expectMessage("Task 'org.sonar.batch.deprecated.tasks.TasksTest$FakeTask1' is defined twice: first by 'foo1' and then by 'foo2'");

    tasks.start();
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
