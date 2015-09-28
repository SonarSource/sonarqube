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
package org.sonar.server.computation.queue;

import java.util.concurrent.TimeUnit;

public class CeProcessingSchedulerImpl implements CeProcessingScheduler {
  private final CeProcessingSchedulerExecutorService executorService;
  private final CeWorkerRunnable workerRunnable;

  private final long delayBetweenTasks;
  private final long delayForFirstStart;
  private final TimeUnit timeUnit;

  public CeProcessingSchedulerImpl(CeProcessingSchedulerExecutorService processingExecutorService, CeWorkerRunnable workerRunnable) {
    this.executorService = processingExecutorService;
    this.workerRunnable = workerRunnable;

    this.delayBetweenTasks = 10;
    this.delayForFirstStart = 0;
    this.timeUnit = TimeUnit.SECONDS;
  }

  @Override
  public void startScheduling() {
    executorService.scheduleAtFixedRate(workerRunnable, delayForFirstStart, delayBetweenTasks, timeUnit);
  }

}
