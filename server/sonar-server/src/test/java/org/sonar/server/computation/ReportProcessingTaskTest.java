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

import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.db.compute.AnalysisReportDto;
import org.sonar.server.computation.container.ComputeEngineContainer;
import org.sonar.server.computation.container.ContainerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReportProcessingTaskTest {

  private static final long ANALYSIS_REPORT_DTO_ID = 663l;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public LogTester logTester = new LogTester();

  ReportQueue queue = mock(ReportQueue.class);
  ComponentContainer componentContainer = mock(ComponentContainer.class);
  ContainerFactory containerFactory = mock(ContainerFactory.class);
  ReportQueue.Item item = new ReportQueue.Item(createAnalysisReportDto(), new File("Don't care"));

  private static AnalysisReportDto createAnalysisReportDto() {
    AnalysisReportDto res = new AnalysisReportDto();
    res.setProjectKey("P1").setId(ANALYSIS_REPORT_DTO_ID);
    return res;
  }

  ReportProcessingTask underTest = new ReportProcessingTask(queue, item, componentContainer, containerFactory);

  @Test
  public void creates_container_for_item_run_its_ReportProcessor_and_remove_from_queue() throws IOException {
    ComputeEngineContainer computeEngineContainer = mock(ComputeEngineContainer.class);
    when(containerFactory.create(componentContainer, item)).thenReturn(computeEngineContainer);

    ReportProcessor reportProcessor = mock(ReportProcessor.class);
    when(computeEngineContainer.getComponentByType(ReportProcessor.class)).thenReturn(reportProcessor);

    underTest.run();

    verify(containerFactory).create(componentContainer, item);
    verify(reportProcessor).process();
    verify(computeEngineContainer).cleanup();
    verify(queue).remove(item);
  }

  @Test
  public void remove_from_queue_even_if_process_failed() throws IOException {
    ComputeEngineContainer computeEngineContainer = mock(ComputeEngineContainer.class);
    when(containerFactory.create(componentContainer, item)).thenReturn(computeEngineContainer);

    ReportProcessor reportProcessor = mock(ReportProcessor.class);
    when(computeEngineContainer.getComponentByType(ReportProcessor.class)).thenReturn(reportProcessor);
    doThrow(new IllegalArgumentException("This exception must be silently logged by ReportProcessingTask"))
      .when(reportProcessor)
      .process();

    underTest.run();

    verify(containerFactory).create(componentContainer, item);
    verify(reportProcessor).process();
    verify(computeEngineContainer).cleanup();
    verify(queue).remove(item);
  }

  @Test
  public void handle_error_during_removal_from_queue() throws Exception {
    ComputeEngineContainer computeEngineContainer = mock(ComputeEngineContainer.class);
    when(containerFactory.create(componentContainer, item)).thenReturn(computeEngineContainer);

    ReportProcessor reportProcessor = mock(ReportProcessor.class);
    when(computeEngineContainer.getComponentByType(ReportProcessor.class)).thenReturn(reportProcessor);

    doThrow(new IllegalStateException("pb")).when(queue).remove(item);

    underTest.run();

    assertThat(logTester.logs()).contains("Failed to remove analysis report " + ANALYSIS_REPORT_DTO_ID + " from queue");
  }
}
