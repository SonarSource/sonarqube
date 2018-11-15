/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.scanner.mediumtest.tasks;

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.Plugin;
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.LogTester;
import org.sonar.scanner.mediumtest.ScannerMediumTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class TasksMediumTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("faketask", new FakeTaskPlugin());

  @Test
  public void failWhenCallingTask() throws Exception {
    try {
      tester.newAnalysis()
        .properties(ImmutableMap.<String, String>builder()
          .put("sonar.task", "fake").build())
        .execute();
      fail("Expected exception");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(MessageException.class).hasMessage("Tasks support was removed in SonarQube 7.6.");
    }
  }

  @Test
  public void failWhenCallingViews() throws Exception {
    try {
      tester.newAnalysis()
        .properties(ImmutableMap.<String, String>builder()
          .put("sonar.task", "views").build())
        .execute();
      fail("Expected exception");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(MessageException.class).hasMessage("The task 'views' was removed with SonarQube 7.1. You can safely remove this call since portfolios and applications are automatically re-calculated.");
    }
  }

  private static class FakeTaskPlugin implements Plugin {

    @Override
    public void define(Context context) {
      context.addExtensions(FakeTask.DEF, FakeTask.class);
    }
  }

  private static class FakeTask implements Task {

    public static final TaskDefinition DEF = TaskDefinition.builder().key("fake").description("Fake description").taskClass(FakeTask.class).build();

    @Override
    public void execute() {
      // TODO Auto-generated method stub

    }

  }

}
