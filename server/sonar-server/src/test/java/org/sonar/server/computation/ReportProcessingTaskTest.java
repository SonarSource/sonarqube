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

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.db.compute.AnalysisReportDto;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.computation.container.ComputeEngineContainer;
import org.sonar.server.computation.container.ContainerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ReportProcessingTaskTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public LogTester logTester = new LogTester();

  ReportQueue queue = mock(ReportQueue.class);
  ComponentContainer componentContainer = mock(ComponentContainer.class);
  ContainerFactory containerFactory = mock(ContainerFactory.class);
  ReportProcessingTask underTest = new ReportProcessingTask(queue, componentContainer, containerFactory);

  @Test
  public void do_nothing_if_queue_empty() {
    when(queue.pop()).thenReturn(null);

    underTest.run();

    verify(queue).pop();
    verifyZeroInteractions(containerFactory);
  }

  @Test
  public void pop_queue_and_integrate_report() throws IOException {
    AnalysisReportDto report = AnalysisReportDto.newForTests(1L);
    ReportQueue.Item item = new ReportQueue.Item(report, temp.newFile());

    when(queue.pop()).thenReturn(item);
    when(containerFactory.create(componentContainer, item)).thenReturn(mock(ComputeEngineContainer.class));

    underTest.run();

    verify(queue).pop();
    verify(containerFactory).create(componentContainer, item);
  }

  @Test
  public void handle_error_during_queue_pop() {
    when(queue.pop()).thenThrow(new IllegalStateException());

    underTest.run();

    assertThat(logTester.logs()).contains("Failed to pop the queue of analysis reports");
  }

  @Test
  public void handle_error_during_removal_from_queue() throws Exception {
    when(containerFactory.create(any(ComponentContainer.class), any(ReportQueue.Item.class))).thenReturn(mock(ComputeEngineContainer.class));

    AnalysisReportDto report = AnalysisReportDto.newForTests(1L).setProjectKey("P1");
    ReportQueue.Item item = new ReportQueue.Item(report, temp.newFile());
    when(queue.pop()).thenReturn(item);
    doThrow(new IllegalStateException("pb")).when(queue).remove(item);

    underTest.run();

    assertThat(logTester.logs()).contains("Failed to remove analysis report 1 from queue");
  }
}
