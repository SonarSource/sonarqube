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
package org.sonar.server.ce.ws;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.junit.Test;
import org.sonar.ce.queue.CeTask;
import org.sonar.ce.queue.CeTaskResult;
import org.sonar.ce.taskprocessor.CeTaskProcessor;
import org.sonar.server.ws.WsActionTester;

import static org.sonar.test.JsonAssert.assertJson;

public class TaskTypesActionTest {

  WsActionTester ws = new WsActionTester(new TaskTypesAction(new CeTaskProcessor[] {
    new FakeCeTaskProcessor("REPORT"),
    new FakeCeTaskProcessor("DEV_REFRESH", "DEV_PURGE"),
    new FakeCeTaskProcessor("VIEW_REFRESH")
  }));

  @Test
  public void json_example() {
    String response = ws.newRequest().execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("task_types-example.json"));
  }

  private static class FakeCeTaskProcessor implements CeTaskProcessor {
    private final Set<String> taskTypes;

    private FakeCeTaskProcessor(String... taskTypes) {
      this.taskTypes = ImmutableSet.copyOf(taskTypes);
    }

    @Override
    public Set<String> getHandledCeTaskTypes() {
      return taskTypes;
    }

    @Override
    public CeTaskResult process(CeTask task) {
      throw new UnsupportedOperationException();
    }
  }

}
