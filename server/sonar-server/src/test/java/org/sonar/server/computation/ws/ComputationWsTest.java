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

import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.activity.index.ActivityIndex;
import org.sonar.server.computation.ReportProcessingScheduler;
import org.sonar.server.computation.ReportQueue;
import org.sonar.server.computation.monitoring.CEQueueStatus;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ComputationWsTest {

  WsTester ws = new WsTester(new ComputationWs(
    new QueueAction(mock(ReportQueue.class)),
    new SubmitReportAction(mock(ReportQueue.class), mock(ReportProcessingScheduler.class), mock(UserSession.class), mock(CEQueueStatus.class)),
    new HistoryAction(mock(ActivityIndex.class), mock(UserSession.class))));

  @Test
  public void define() {
    WebService.Controller controller = ws.controller("api/computation");

    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(3);
  }
}
