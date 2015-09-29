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

package org.sonar.server.computation.taskprocessor;

import com.google.common.base.Optional;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.logs.Profiler;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.server.computation.log.CeLogging;
import org.sonar.server.computation.queue.CeQueue;
import org.sonar.server.computation.queue.CeTask;

import static java.lang.String.format;

public class CeWorkerRunnableImpl implements CeWorkerRunnable {

  private static final Logger LOG = Loggers.get(CeWorkerRunnableImpl.class);

  private final CeQueue queue;
  private final CeLogging ceLogging;
  private final CeTaskProcessorRepository taskProcessorRepository;

  public CeWorkerRunnableImpl(CeQueue queue, CeLogging ceLogging, CeTaskProcessorRepository taskProcessorRepository) {
    this.queue = queue;
    this.ceLogging = ceLogging;
    this.taskProcessorRepository = taskProcessorRepository;
  }

  @Override
  public void run() {
    Optional<CeTask> ceTask = tryAndFindTaskToExecute();
    if (!ceTask.isPresent()) {
      return;
    }

    executeTask(ceTask.get());
  }

  private Optional<CeTask> tryAndFindTaskToExecute() {
    try {
      return queue.peek();
    } catch (Exception e) {
      LOG.error("Failed to pop the queue of analysis reports", e);
    }
    return Optional.absent();
  }

  private void executeTask(CeTask task) {
    // logging twice: once in sonar.log and once in CE appender
    Profiler regularProfiler = startProfiler(task);
    ceLogging.initForTask(task);
    Profiler ceProfiler = startProfiler(task);

    CeActivityDto.Status status = CeActivityDto.Status.FAILED;
    try {
      // TODO delegate the message to the related task processor, according to task type
      Optional<CeTaskProcessor> taskProcessor = taskProcessorRepository.getForCeTask(task);
      if (taskProcessor.isPresent()) {
        taskProcessor.get().process(task);
        status = CeActivityDto.Status.SUCCESS;
      } else {
        LOG.error("No CeTaskProcessor is defined for task of type {}. Plugin configuration may have changed", task.getType());
        status = CeActivityDto.Status.FAILED;
      }
      queue.remove(task, status);
    } catch (Throwable e) {
      LOG.error(format("Failed to execute task %s", task.getUuid()), e);
      queue.remove(task, status);
    } finally {
      // logging twice: once in sonar.log and once in CE appender
      stopProfiler(ceProfiler, task, status);
      ceLogging.clearForTask();
      stopProfiler(regularProfiler, task, status);
    }
  }

  private static Profiler startProfiler(CeTask task) {
    return Profiler.create(LOG).startInfo("Execute task | project={} | id={}", task.getComponentKey(), task.getUuid());
  }

  private static void stopProfiler(Profiler profiler, CeTask task, CeActivityDto.Status status) {
    if (status == CeActivityDto.Status.FAILED) {
      profiler.stopError("Executed task | project={} | id={}", task.getComponentKey(), task.getUuid());
    } else {
      profiler.stopInfo("Executed task | project={} | id={}", task.getComponentKey(), task.getUuid());
    }
  }
}
