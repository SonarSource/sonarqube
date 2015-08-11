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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.compute.AnalysisReportDto;
import org.sonar.server.computation.ReportProcessingScheduler;
import org.sonar.server.computation.ReportQueue;
import org.sonar.server.computation.monitoring.CEQueueStatus;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SubmitReportActionTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  ReportProcessingScheduler workerLauncher = mock(ReportProcessingScheduler.class);
  CEQueueStatus queueStatus = mock(CEQueueStatus.class);
  ReportQueue queue = mock(ReportQueue.class);
  WsTester wsTester;
  SubmitReportAction underTest;

  @Before
  public void before() {
    underTest = new SubmitReportAction(queue, workerLauncher, userSessionRule, queueStatus);
    wsTester = new WsTester(new ComputationWs(underTest));
  }

  @Test
  public void define_metadata() {
    WebService.Context context = new WebService.Context();
    WebService.NewController controller = context.createController("api/computation");
    underTest.define(controller);
    controller.done();

    WebService.Action action = context.controller("api/computation").action("submit_report");
    assertThat(action).isNotNull();
    assertThat(action.params()).hasSize(3);
  }

  @Test
  public void add_element_to_queue_and_launch_analysis_task() throws Exception {
    userSessionRule.setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    AnalysisReportDto dto = mock(AnalysisReportDto.class);
    when(dto.getId()).thenReturn(42L);
    when(queue.add(any(String.class), any(String.class), any(InputStream.class))).thenReturn(new ReportQueue.Item(dto, null));

    WsTester.TestRequest request = wsTester
      .newPostRequest(ComputationWs.ENDPOINT, "submit_report")
      .setParam(SubmitReportAction.PARAM_PROJECT_KEY, "P1")
      .setParam(SubmitReportAction.PARAM_PROJECT_NAME, "Project 1")
      .setParam(SubmitReportAction.PARAM_REPORT_DATA, null);
    WsTester.Result response = request.execute();

    verify(queue).add(eq("P1"), eq("Project 1"), any(InputStream.class));
    verify(workerLauncher).startAnalysisTaskNow();
    verify(queueStatus).addReceived();
    assertThat(response.outputAsString()).isEqualTo("{\"key\":\"42\"}");
  }

  @Test(expected = ForbiddenException.class)
  public void requires_scan_permission() throws Exception {
    userSessionRule.setGlobalPermissions(GlobalPermissions.DASHBOARD_SHARING);

    WsTester.TestRequest request = wsTester
      .newPostRequest(ComputationWs.ENDPOINT, "submit_report")
      .setParam(SubmitReportAction.PARAM_PROJECT_KEY, "P1")
      .setParam(SubmitReportAction.PARAM_PROJECT_NAME, "Project 1")
      .setParam(SubmitReportAction.PARAM_REPORT_DATA, null);
    request.execute();

  }
}
