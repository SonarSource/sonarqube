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

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.computation.db.AnalysisReportDto;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ComputationWorkerTest {

  private ComputationWorker sut;
  private ComputationService service;
  private AnalysisReportQueue queue;

  @Before
  public void before() {
    this.service = mock(ComputationService.class);
    this.queue = mock(AnalysisReportQueue.class);
    this.sut = new ComputationWorker(queue, service);
  }

  @Test
  public void call_findAndBook_and_no_call_to_analyze_if_no_report_found() {
    sut.run();

    verify(queue).pop();
    verify(service, never()).process(any(AnalysisReportDto.class));
  }

  @Test
  public void call_findAndBook_and_then_analyze_if_there_is_a_report() {
    AnalysisReportDto report = AnalysisReportDto.newForTests(1L);
    when(queue.pop()).thenReturn(report);

    sut.run();

    verify(queue).pop();
    verify(service).process(report);
  }

  @Test
  public void when_the_analysis_throws_an_exception_it_does_not_break_the_task() throws Exception {
    AnalysisReportDto report = AnalysisReportDto.newForTests(1L);
    when(queue.bookNextAvailable()).thenReturn(report);
    doThrow(IllegalStateException.class).when(service).analyzeReport(report);

    sut.run();
  }

  @Test
  public void when_the_queue_returns_an_exception_it_does_not_break_the_task() throws Exception {
    AnalysisReportDto report = AnalysisReportDto.newForTests(1L);
    when(queue.bookNextAvailable()).thenThrow(IllegalStateException.class);

    sut.run();
  }
}
