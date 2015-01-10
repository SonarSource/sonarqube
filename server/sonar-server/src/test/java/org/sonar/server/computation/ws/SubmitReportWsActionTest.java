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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.computation.AnalysisReportQueue;
import org.sonar.server.computation.ComputationWorkerLauncher;
import org.sonar.server.ws.WsTester;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SubmitReportWsActionTest {

  private static final String DEFAULT_PROJECT_KEY = "123456789-987654321";
  private SubmitReportWsAction sut;

  private WsTester wsTester;
  private ComputationWorkerLauncher workerLauncher = mock(ComputationWorkerLauncher.class);
  private AnalysisReportQueue queue;

  @Before
  public void before() {
    queue = mock(AnalysisReportQueue.class);
    sut = new SubmitReportWsAction(queue, workerLauncher);
    wsTester = new WsTester(new ComputationWebService(sut));
  }

  @Test
  public void define_metadata() throws Exception {
    WebService.Context context = new WebService.Context();
    WebService.NewController controller = context.createController("api/computation");
    sut.define(controller);
    controller.done();

    WebService.Action action = context.controller("api/computation").action("submit_report");
    assertThat(action).isNotNull();
    assertThat(action.params()).hasSize(3);
  }

  @Test
  public void add_element_to_queue_and_launch_analysis_task() throws Exception {
    when(queue.add(any(String.class), anyLong(), any(InputStream.class))).thenReturn("P1");

    WsTester.TestRequest request = wsTester
      .newGetRequest(ComputationWebService.API_ENDPOINT, "submit_report")
      .setParam(SubmitReportWsAction.PARAM_PROJECT_KEY, "P1")
      .setParam(SubmitReportWsAction.PARAM_SNAPSHOT, "456")
      .setParam(SubmitReportWsAction.PARAM_REPORT_DATA, null);
    request.execute();

    verify(queue).add(eq("P1"), eq(456L), any(InputStream.class));
    verify(workerLauncher).startAnalysisTaskNow();
  }

  @Test
  public void return_report_key() throws Exception {
    when(queue.add(any(String.class), anyLong(), any(InputStream.class))).thenReturn("P1");

    WsTester.TestRequest request = wsTester
      .newPostRequest(ComputationWebService.API_ENDPOINT, "submit_report")
      .setParam(SubmitReportWsAction.PARAM_PROJECT_KEY, "P1")
      .setParam(SubmitReportWsAction.PARAM_SNAPSHOT, "456")
      .setParam(SubmitReportWsAction.PARAM_REPORT_DATA, null);
    request.execute().assertJson(getClass(), "submit_report.json", false);
  }
}
