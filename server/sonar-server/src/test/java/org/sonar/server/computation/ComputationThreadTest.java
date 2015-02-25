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

package org.sonar.server.computation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.core.computation.db.AnalysisReportDto;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ComputationThreadTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  ComputationContainer container = mock(ComputationContainer.class);
  ReportQueue queue = mock(ReportQueue.class);
  ComputationThread sut = new ComputationThread(queue, container);

  @Test
  public void do_nothing_if_queue_empty() {
    when(queue.pop()).thenReturn(null);

    sut.run();

    verify(queue).pop();
    verifyZeroInteractions(container);
  }

  @Test
  public void pop_queue_and_integrate_report() throws IOException {
    AnalysisReportDto report = AnalysisReportDto.newForTests(1L);
    ReportQueue.Item item = new ReportQueue.Item(report, temp.newFile());
    when(queue.pop()).thenReturn(item);

    sut.run();

    verify(queue).pop();
    verify(container).execute(item);
  }

  @Test
  public void handle_error_during_queue_pop() throws Exception {
    when(queue.pop()).thenThrow(new IllegalStateException());

    sut.run();

    assertThat(logTester.logs()).contains("Failed to pop the queue of analysis reports");
  }

  @Test
  public void handle_error_during_integration() throws Exception {
    AnalysisReportDto report = AnalysisReportDto.newForTests(1L).setProjectKey("P1");
    ReportQueue.Item item = new ReportQueue.Item(report, temp.newFile());
    when(queue.pop()).thenReturn(item);
    doThrow(new IllegalStateException("pb")).when(container).execute(item);

    sut.run();

    assertThat(logTester.logs()).contains("Failed to process analysis report 1 of project P1");
  }

  @Test
  public void handle_error_during_removal_from_queue() throws Exception {
    AnalysisReportDto report = AnalysisReportDto.newForTests(1L).setProjectKey("P1");
    ReportQueue.Item item = new ReportQueue.Item(report, temp.newFile());
    when(queue.pop()).thenReturn(item);
    doThrow(new IllegalStateException("pb")).when(queue).remove(item);

    sut.run();

    assertThat(logTester.logs()).contains("Failed to remove analysis report 1 from queue");
  }
}
