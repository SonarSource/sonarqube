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
package org.sonar.batch.mediumtest.tasks;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.List;
import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.SonarPlugin;
import org.sonar.api.issue.action.Actions;
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.LogTester;
import org.sonar.batch.bootstrap.MockHttpServer;
import org.sonar.batch.mediumtest.BatchMediumTester;

import static org.assertj.core.api.Assertions.assertThat;

public class TasksMediumTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public LogTester logTester = new LogTester();

  public BatchMediumTester tester = BatchMediumTester.builder()
    .registerPlugin("faketask", new FakeTaskPlugin())
    .build();

  private MockHttpServer server = null;

  @After
  public void stopServer() {
    if (server != null) {
      server.stop();
    }
  }

  @After
  public void stop() {
    tester.stop();
  }

  @Test
  public void listTasksIncludingBroken() throws Exception {
    tester.start();
    tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "list").build())
      .start();

    assertThat(logTester.logs()).haveExactly(1, new Condition<String>() {

      @Override
      public boolean matches(String value) {
        return value.contains("Available tasks:") && value.contains("fake: Fake description") && value.contains("broken: Broken description");
      }
    });
  }

  @Test
  public void runBroken() throws Exception {
    tester.start();

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage(
      "Unable to load component class org.sonar.batch.mediumtest.tasks.TasksMediumTest$BrokenTask");

    tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "broken").build())
      .start();
  }

  @Test(expected = MessageException.class)
  public void unsupportedTask() throws Exception {
    tester = BatchMediumTester.builder()
      .build();
    tester.start();
    tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "foo").build())
      .start();
  }

  private void startServer(Integer responseStatus, String responseData) throws Exception {
    server = new MockHttpServer();
    server.start();

    if (responseStatus != null) {
      server.setMockResponseStatus(responseStatus);
    }
    if (responseData != null) {
      server.setMockResponseData(responseData);
    }
  }

  private static class FakeTaskPlugin extends SonarPlugin {

    @Override
    public List getExtensions() {
      return Arrays.asList(FakeTask.DEF, FakeTask.class, BrokenTask.DEF, BrokenTask.class);
    }

  }

  private static class FakeTask implements Task {

    public static final TaskDefinition DEF = TaskDefinition.builder().key("fake").description("Fake description").taskClass(FakeTask.class).build();

    @Override
    public void execute() {
      // TODO Auto-generated method stub

    }

  }

  private static class BrokenTask implements Task {

    public static final TaskDefinition DEF = TaskDefinition.builder().key("broken").description("Broken description").taskClass(BrokenTask.class).build();
    private final Actions serverSideComponent;

    public BrokenTask(Actions serverSideComponent) {
      this.serverSideComponent = serverSideComponent;
    }

    @Override
    public void execute() {
      System.out.println(serverSideComponent.list());
      ;

    }

  }

}
