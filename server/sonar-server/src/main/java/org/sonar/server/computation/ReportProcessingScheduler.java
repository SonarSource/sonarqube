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
import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerStartHandler;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.computation.container.ContainerFactory;
import org.sonar.server.computation.container.ContainerFactoryImpl;

/**
 * Adds tasks to the Compute Engine to process batch reports.
 */
public class ReportProcessingScheduler implements ServerStartHandler {

  private final ReportProcessingSchedulerExecutorService reportProcessingSchedulerExecutorService;
  private final ComputeEngineProcessingQueue processingQueue;
  private final ReportQueue queue;
  private final ComponentContainer sqContainer;
  private final ContainerFactory containerFactory;

  private final long delayBetweenTasks;
  private final long delayForFirstStart;
  private final TimeUnit timeUnit;

  public ReportProcessingScheduler(ReportProcessingSchedulerExecutorService reportProcessingSchedulerExecutorService,
    ComputeEngineProcessingQueue processingQueue,
    ReportQueue queue, ComponentContainer sqContainer) {
    this.reportProcessingSchedulerExecutorService = reportProcessingSchedulerExecutorService;
    this.processingQueue = processingQueue;
    this.queue = queue;
    this.sqContainer = sqContainer;
    this.containerFactory = new ContainerFactoryImpl();

    this.delayBetweenTasks = 10;
    this.delayForFirstStart = 0;
    this.timeUnit = TimeUnit.SECONDS;
  }

  public void startAnalysisTaskNow() {
    reportProcessingSchedulerExecutorService.execute(new AddReportProcessingToCEProcessingQueue());
  }

  @Override
  public void onServerStart(Server server) {
    reportProcessingSchedulerExecutorService.scheduleAtFixedRate(new AddReportProcessingToCEProcessingQueue(), delayForFirstStart, delayBetweenTasks, timeUnit);
  }

  private class AddReportProcessingToCEProcessingQueue implements Runnable {
    @Override
    public void run() {
      processingQueue.addTask(new ReportProcessingTask(queue, sqContainer, containerFactory));
    }
  }
}
