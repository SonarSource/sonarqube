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
import org.sonar.server.computation.log.CeLogging;

public class CeProcessingSchedulerImpl implements CeProcessingScheduler {
  private final CeProcessingSchedulerExecutorService executorService;
  private final CeQueue ceQueue;
  private final ReportTaskProcessor reportTaskProcessor;
  private final CeLogging ceLogging;

  private final long delayBetweenTasks;
  private final long delayForFirstStart;
  private final TimeUnit timeUnit;

  public CeProcessingSchedulerImpl(CeProcessingSchedulerExecutorService processingExecutorService, CeQueue ceQueue, ReportTaskProcessor reportTaskProcessor, CeLogging ceLogging) {
    this.executorService = processingExecutorService;
    this.ceQueue = ceQueue;
    this.reportTaskProcessor = reportTaskProcessor;
    this.ceLogging = ceLogging;

    this.delayBetweenTasks = 10;
    this.delayForFirstStart = 0;
    this.timeUnit = TimeUnit.SECONDS;
  }

  @Override
  public void startScheduling() {
    executorService.scheduleAtFixedRate(new CeWorkerRunnable(ceQueue, reportTaskProcessor, ceLogging), delayForFirstStart, delayBetweenTasks, timeUnit);
  }

}
