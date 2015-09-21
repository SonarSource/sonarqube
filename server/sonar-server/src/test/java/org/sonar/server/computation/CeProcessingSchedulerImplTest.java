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

import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.sonar.server.computation.log.CeLogging;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class CeProcessingSchedulerImplTest {
  private CeProcessingSchedulerExecutorService processingExecutorService = mock(CeProcessingSchedulerExecutorService.class);
  private CeQueue ceQueue = mock(CeQueue.class);
  private ReportTaskProcessor reportTaskProcessor = mock(ReportTaskProcessor.class);
  private CeLogging ceLogging = mock(CeLogging.class);
  private CeProcessingSchedulerImpl underTest = new CeProcessingSchedulerImpl(processingExecutorService, ceQueue, reportTaskProcessor, ceLogging);

  @Test
  public void startScheduling_schedules_CeWorkerRunnable_at_fixed_rate_run_head_of_queue() {
    underTest.startScheduling();

    verify(processingExecutorService).scheduleAtFixedRate(any(CeWorkerRunnable.class), eq(0L), eq(10L), eq(TimeUnit.SECONDS));
    verifyNoMoreInteractions(processingExecutorService);
  }

}
