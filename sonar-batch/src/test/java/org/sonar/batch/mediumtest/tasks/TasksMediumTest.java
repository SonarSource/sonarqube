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
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
  public void triggerViews() throws Exception {
    startServer(200, "OK");
    tester = BatchMediumTester.builder()
      .bootstrapProperties(ImmutableMap.of("sonar.host.url", "http://localhost:" + server.getPort()))
      .build();
    tester.start();
    tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "views").build())
      .start();

    assertThat(logTester.logs()).contains("Trigger views update");
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

}
