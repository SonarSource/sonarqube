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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Queues;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerStartHandler;

import static java.util.Objects.requireNonNull;

public class ComputeEngineProcessingQueueImpl implements ComputeEngineProcessingQueue, ServerStartHandler {
  private final ComputeEngineProcessingExecutorService processingService;
  private final ConcurrentLinkedQueue<ComputeEngineTask> queue = Queues.newConcurrentLinkedQueue();

  private final long delayBetweenTasks;
  private final long delayForFirstStart;
  private final TimeUnit timeUnit;

  public ComputeEngineProcessingQueueImpl(ComputeEngineProcessingExecutorService processingExecutorService) {
    this.processingService = processingExecutorService;

    this.delayBetweenTasks = 10;
    this.delayForFirstStart = 0;
    this.timeUnit = TimeUnit.SECONDS;
  }

  @VisibleForTesting
  ComputeEngineProcessingQueueImpl(ComputeEngineProcessingExecutorService processingExecutorService,
    long delayForFirstStart, long delayBetweenTasks, TimeUnit timeUnit) {
    this.processingService = processingExecutorService;

    this.delayBetweenTasks = delayBetweenTasks;
    this.delayForFirstStart = delayForFirstStart;
    this.timeUnit = timeUnit;
  }

  @Override
  public void addTask(ComputeEngineTask task) {
    requireNonNull(task, "a ComputeEngineTask can not be null");

    queue.add(task);
  }

  @Override
  public void onServerStart(Server server) {
    processingService.scheduleAtFixedRate(new ProcessHeadOfQueueRunnable(), delayForFirstStart, delayBetweenTasks, timeUnit);
  }

  private class ProcessHeadOfQueueRunnable implements Runnable {
    @Override
    public void run() {
      ComputeEngineTask task = queue.poll();
      if (task != null) {
        task.run();
      }
    }
  }
}
