/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.task;

import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ListTaskTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void should_list_available_tasks() {
    Tasks tasks = mock(Tasks.class);
    when(tasks.definitions()).thenReturn(Arrays.asList(
      TaskDefinition.builder().key("foo").description("Foo").taskClass(FooTask.class).build(),
      TaskDefinition.builder().key("purge").description("Purge database").taskClass(FakePurgeTask.class).build()));

    ListTask task = new ListTask(tasks);

    task.execute();

    assertThat(logTester.logs(LoggerLevel.INFO)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.INFO).get(0)).contains("Available tasks:", "  - foo: Foo", "  - purge: Purge database");
  }

  private static class FakePurgeTask implements Task {
    public void execute() {
    }
  }

  private static class FooTask implements Task {
    public void execute() {
    }
  }
}
