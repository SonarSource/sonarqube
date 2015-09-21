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
package org.sonar.server.computation.ws;

import java.io.InputStream;
import org.junit.Test;
import org.mockito.Matchers;
import org.sonar.core.util.Protobuf;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.server.computation.queue.CeTask;
import org.sonar.server.computation.queue.report.ReportSubmitter;
import org.sonar.server.plugins.MimeTypes;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.WsCe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SubmitWsActionTest {

  ReportSubmitter reportSubmitter = mock(ReportSubmitter.class);
  SubmitWsAction underTest = new SubmitWsAction(reportSubmitter);
  WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void submit_task_to_the_queue_and_ask_for_immediate_processing() {
    CeTask task = new CeTask.Builder().setUuid("TASK_1").setType(CeTaskTypes.REPORT).setComponentUuid("PROJECT_1").setSubmitterLogin("robert").build();
    when(reportSubmitter.submit(eq("my_project"), Matchers.isNull(String.class), eq("My Project"), any(InputStream.class))).thenReturn(task);

    TestResponse wsResponse = tester.newRequest()
      .setParam("projectKey", "my_project")
      .setParam("projectName", "My Project")
      .setParam("report", "{binary}")
      .setMediaType(MimeTypes.PROTOBUF)
      .setMethod("POST")
      .execute();

    verify(reportSubmitter).submit(eq("my_project"), Matchers.isNull(String.class), eq("My Project"), any(InputStream.class));

    WsCe.SubmitResponse submitResponse = Protobuf.read(wsResponse.getInputStream(), WsCe.SubmitResponse.PARSER);
    assertThat(submitResponse.getTaskId()).isEqualTo("TASK_1");
    assertThat(submitResponse.getProjectId()).isEqualTo("PROJECT_1");
  }

  @Test
  public void test_example_json_response() {
    CeTask task = new CeTask.Builder().setUuid("TASK_1").setType(CeTaskTypes.REPORT).setComponentUuid("PROJECT_1").setSubmitterLogin("robert").build();
    when(reportSubmitter.submit(eq("my_project"), Matchers.isNull(String.class), eq("My Project"), any(InputStream.class))).thenReturn(task);

    TestResponse wsResponse = tester.newRequest()
      .setParam("projectKey", "my_project")
      .setParam("projectName", "My Project")
      .setParam("report", "{binary}")
      .setMediaType(MimeTypes.JSON)
      .setMethod("POST")
      .execute();

    JsonAssert.assertJson(tester.getDef().responseExampleAsString()).isSimilarTo(wsResponse.getInput());
  }

  /**
   * If project name is not specified, then name is the project key
   */
  @Test
  public void project_name_is_optional() {
    CeTask task = new CeTask.Builder().setUuid("TASK_1").setType(CeTaskTypes.REPORT).setComponentUuid("PROJECT_1").setSubmitterLogin("robert").build();
    when(reportSubmitter.submit(eq("my_project"), Matchers.isNull(String.class), eq("my_project"), any(InputStream.class))).thenReturn(task);

    tester.newRequest()
      .setParam("projectKey", "my_project")
      .setParam("report", "{binary}")
      .setMediaType(MimeTypes.PROTOBUF)
      .setMethod("POST")
      .execute();

    verify(reportSubmitter).submit(eq("my_project"), Matchers.isNull(String.class), eq("my_project"), any(InputStream.class));

  }
}
