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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.MockitoAnnotations;
import org.sonar.ce.queue.CeTask;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.server.computation.queue.ReportSubmitter;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Ce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SubmitActionTest {

  private static final CeTask A_CE_TASK = new CeTask.Builder()
    .setOrganizationUuid("org1")
    .setUuid("TASK_1")
    .setType(CeTaskTypes.REPORT)
    .setComponentUuid("PROJECT_1").setSubmitterLogin("robert")
    .build();

  @Captor
  ArgumentCaptor<Map<String, String>> map;

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.fromUuid("org1");
  private String organizationKey = defaultOrganizationProvider.get().getKey();
  private ReportSubmitter reportSubmitter = mock(ReportSubmitter.class);
  private SubmitAction underTest = new SubmitAction(reportSubmitter, defaultOrganizationProvider);
  private WsActionTester tester = new WsActionTester(underTest);

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void submit_task_to_the_queue_and_ask_for_immediate_processing() {
    when(reportSubmitter.submit(eq(organizationKey), eq("my_project"), Matchers.isNull(String.class), eq("My Project"),
      anyMapOf(String.class, String.class), any(InputStream.class))).thenReturn(A_CE_TASK);

    Ce.SubmitResponse submitResponse = tester.newRequest()
      .setParam("projectKey", "my_project")
      .setParam("projectName", "My Project")
      .setPart("report", new ByteArrayInputStream("{binary}".getBytes()), "foo.bar")
      .setMethod("POST")
      .executeProtobuf(Ce.SubmitResponse.class);

    verify(reportSubmitter).submit(eq(organizationKey), eq("my_project"), Matchers.isNull(String.class), eq("My Project"),
      anyMapOf(String.class, String.class), any(InputStream.class));

    assertThat(submitResponse.getTaskId()).isEqualTo("TASK_1");
    assertThat(submitResponse.getProjectId()).isEqualTo("PROJECT_1");
  }

  @Test
  public void submit_task_with_multiple_characteristics() {
    when(reportSubmitter.submit(eq(organizationKey), eq("my_project"), Matchers.isNull(String.class), eq("My Project"),
      anyMapOf(String.class, String.class), any(InputStream.class))).thenReturn(A_CE_TASK);

    String[] characteristics = {"branch=branch1", "key=value1=value2"};
    Ce.SubmitResponse submitResponse = tester.newRequest()
      .setParam("projectKey", "my_project")
      .setParam("projectName", "My Project")
      .setMultiParam("characteristic", Arrays.asList(characteristics))
      .setPart("report", new ByteArrayInputStream("{binary}".getBytes()), "foo.bar")
      .setMethod("POST")
      .executeProtobuf(Ce.SubmitResponse.class);

    assertThat(submitResponse.getTaskId()).isEqualTo("TASK_1");
    verify(reportSubmitter).submit(eq(organizationKey), eq("my_project"), Matchers.isNull(String.class), eq("My Project"),
      map.capture(), any(InputStream.class));

    assertThat(map.getValue()).containsOnly(entry("branch", "branch1"), entry("key", "value1=value2"));
  }

  @Test
  public void test_example_json_response() {
    when(reportSubmitter.submit(eq(organizationKey), eq("my_project"), Matchers.isNull(String.class), eq("My Project"),
      anyMapOf(String.class, String.class), any(InputStream.class))).thenReturn(A_CE_TASK);

    TestResponse wsResponse = tester.newRequest()
      .setParam("projectKey", "my_project")
      .setParam("projectName", "My Project")
      .setPart("report", new ByteArrayInputStream("{binary}".getBytes()), "foo.bar")
      .setMediaType(MediaTypes.JSON)
      .setMethod("POST")
      .execute();

    JsonAssert.assertJson(tester.getDef().responseExampleAsString()).isSimilarTo(wsResponse.getInput());
  }

  /**
   * If project name is not specified, then name is the project key
   */
  @Test
  public void project_name_is_optional() {
    when(reportSubmitter.submit(eq(organizationKey), eq("my_project"), Matchers.isNull(String.class), eq("my_project"),
      anyMapOf(String.class, String.class), any(InputStream.class))).thenReturn(A_CE_TASK);

    tester.newRequest()
      .setParam("projectKey", "my_project")
      .setPart("report", new ByteArrayInputStream("{binary}".getBytes()), "foo.bar")
      .setMediaType(MediaTypes.PROTOBUF)
      .setMethod("POST")
      .execute();

    verify(reportSubmitter).submit(eq(organizationKey), eq("my_project"), Matchers.isNull(String.class), eq("my_project"),
      anyMapOf(String.class, String.class), any(InputStream.class));

  }
}
