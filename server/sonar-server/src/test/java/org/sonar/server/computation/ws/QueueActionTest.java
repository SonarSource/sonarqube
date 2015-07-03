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

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.compute.AnalysisReportDto;
import org.sonar.server.computation.ReportQueue;
import org.sonar.server.ws.WsTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.compute.AnalysisReportDto.Status.PENDING;

public class QueueActionTest {

  WsTester tester;
  private ReportQueue queue;

  @Before
  public void setup() {
    queue = mock(ReportQueue.class);
    tester = new WsTester(new ComputationWs(new QueueAction(queue)));
  }

  @Test
  public void list_active_reports() throws Exception {
    AnalysisReportDto report = AnalysisReportDto
      .newForTests(1L)
      .setProjectKey("project-key")
      .setProjectName("Project name")
      .setStatus(PENDING)
      .setUuid("PROJECT_UUID")
      .setCreatedAt(DateUtils.parseDateTime("2014-10-13T00:00:00+0200").getTime())
      .setStartedAt(DateUtils.parseDateTime("2014-10-13T00:00:00+0200").getTime())
      .setFinishedAt(DateUtils.parseDateTime("2014-10-13T00:00:00+0200").getTime());
    List<AnalysisReportDto> reports = Lists.newArrayList(report);
    when(queue.all()).thenReturn(reports);

    WsTester.TestRequest request = tester.newGetRequest(ComputationWs.ENDPOINT, "queue");
    request.execute().assertJson(getClass(), "list_queue_reports.json");
  }

  @Test
  public void define() {
    assertThat(tester.controller(ComputationWs.ENDPOINT).action("queue")).isNotNull();
  }
}
