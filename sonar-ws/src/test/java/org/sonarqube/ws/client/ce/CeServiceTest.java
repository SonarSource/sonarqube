/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.ce;

import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.WsCe;
import org.sonarqube.ws.WsCe.ActivityResponse;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.ServiceTester;
import org.sonarqube.ws.client.WsConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.mock;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_COMPONENT_ID;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_MAX_EXECUTED_AT;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_MIN_SUBMITTED_AT;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_ONLY_CURRENTS;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_STATUS;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_TYPE;

public class CeServiceTest {
  private static final String VALUE_COMPONENT_ID = "component-uuid";
  private static final String VALUE_QUERY = "component-query";
  private static final String VALUE_TASK_STATUS_1 = "task-status";
  private static final String VALUE_TASK_STATUS_2 = "task-status-2";
  private static final String VALUE_TASK_TYPE = "task-type";
  private static final int VALUE_PAGE = 1;
  private static final int VALUE_PAGE_SIZE = 10;
  private static final String VALUE_MAX_EXECUTED_AT = "2015-09-17T23:34:59+0200";
  private static final String VALUE_MIN_SUBMITTED_AT = "2015-09-17T23:34:59+0200";
  private static final boolean VALUE_ONLY_CURRENTS = true;

  @Rule
  public ServiceTester<CeService> serviceTester = new ServiceTester<>(new CeService(mock(WsConnector.class)));

  CeService underTest = serviceTester.getInstanceUnderTest();

  @Test
  public void activity() {
    ActivityWsRequest request = new ActivityWsRequest()
      .setComponentId(VALUE_COMPONENT_ID)
      .setQuery(VALUE_QUERY)
      .setStatus(ImmutableList.of(VALUE_TASK_STATUS_1, VALUE_TASK_STATUS_2))
      .setType(VALUE_TASK_TYPE)
      .setPage(VALUE_PAGE)
      .setPageSize(VALUE_PAGE_SIZE)
      .setMaxExecutedAt(VALUE_MAX_EXECUTED_AT)
      .setMinSubmittedAt(VALUE_MIN_SUBMITTED_AT)
      .setOnlyCurrents(VALUE_ONLY_CURRENTS)
      .setPage(1)
      .setPageSize(1);

    underTest.activity(request);
    GetRequest result = serviceTester.getGetRequest();

    assertThat(serviceTester.getGetParser()).isSameAs(ActivityResponse.parser());
    serviceTester.assertThat(result)
      .hasPath("activity")
      .hasParam(PARAM_COMPONENT_ID, VALUE_COMPONENT_ID)
      .hasParam("q", VALUE_QUERY)
      .hasParam(PARAM_STATUS, VALUE_TASK_STATUS_1 + "," + VALUE_TASK_STATUS_2)
      .hasParam(PARAM_TYPE, VALUE_TASK_TYPE)
      .hasParam(PARAM_MAX_EXECUTED_AT, VALUE_MAX_EXECUTED_AT)
      .hasParam(PARAM_MIN_SUBMITTED_AT, VALUE_MIN_SUBMITTED_AT)
      .hasParam(PARAM_ONLY_CURRENTS, VALUE_ONLY_CURRENTS)
      .hasParam("p", 1)
      .hasParam("ps", 1)
      .andNoOtherParam();
  }

  @Test
  public void task_types() {
    underTest.taskTypes();

    assertThat(serviceTester.getGetParser()).isSameAs(WsCe.TaskTypesWsResponse.parser());
  }

  @Test
  public void task_by_id_only() {
    underTest.task("task_id");

    assertThat(serviceTester.getGetRequest().getPath()).isEqualTo("api/ce/task");
    assertThat(serviceTester.getGetRequest().getParams()).containsOnly(entry("id", "task_id"));
  }

  @Test
  public void task_with_stacktrace_and_scanner_context() {
    underTest.task(TaskWsRequest.newBuilder("task_id")
        .withErrorStacktrace()
        .withScannerContext()
        .build());

    assertThat(serviceTester.getGetRequest().getPath()).isEqualTo("api/ce/task");
    assertThat(serviceTester.getGetRequest().getParams()).containsOnly(entry("id", "task_id"), entry("additionalFields", "stacktrace,scannerContext"));
  }

  @Test
  public void task_with_scanner_context_only() {
    underTest.task(TaskWsRequest.newBuilder("task_id")
        .withScannerContext()
        .build());

    assertThat(serviceTester.getGetRequest().getPath()).isEqualTo("api/ce/task");
    assertThat(serviceTester.getGetRequest().getParams()).containsOnly(entry("id", "task_id"), entry("additionalFields", "scannerContext"));
  }

  @Test
  public void task_with_stacktrace_only() {
    underTest.task(TaskWsRequest.newBuilder("task_id")
        .withErrorStacktrace()
        .build());

    assertThat(serviceTester.getGetRequest().getPath()).isEqualTo("api/ce/task");
    assertThat(serviceTester.getGetRequest().getParams()).containsOnly(entry("id", "task_id"), entry("additionalFields", "stacktrace"));
  }
}
