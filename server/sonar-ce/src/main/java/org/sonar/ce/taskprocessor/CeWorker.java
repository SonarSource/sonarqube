/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.ce.taskprocessor;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.CeTaskResult;
import org.sonar.db.ce.CeActivityDto;

/**
 * Marker interface of the runnable in charge of polling the {@link CeQueue} and executing {@link CeTask}.
 * {@link Callable#call()} returns a Boolean which is {@code true} when some a {@link CeTask} was processed,
 * {@code false} otherwise.
 */
public interface CeWorker extends Callable<CeWorker.Result> {

  enum Result {
    /** Worker is disabled */
    DISABLED,
    /** Worker found no task to process */
    NO_TASK,
    /** Worker found a task and processed it (either successfully or not) */
    TASK_PROCESSED
  }

  /**
   * Position of the current CeWorker among all the running workers, starts with 0.
   */
  int getOrdinal();

  /**
   * UUID of the current CeWorker.
   */
  String getUUID();

  /**
   * @return {@code true} if this CeWorker currently being executed by the specified {@link Thread}.
   */
  boolean isExecutedBy(Thread thread);

  /**
   * @return the {@link CeTask} currently being executed by this worker, if any.
   */
  Optional<CeTask> getCurrentTask();

  /**
   * Classes implementing will be called a task start and finishes executing.
   * All classes implementing this interface are guaranted to be called for each event, even if another implementation
   * failed when called.
   */
  @ComputeEngineSide
  interface ExecutionListener {
    /**
     * Called when starting executing a {@link CeTask} (which means: after it's been picked for processing, but before
     * the execution of the task by the {@link org.sonar.ce.task.taskprocessor.CeTaskProcessor#process(CeTask)}).
     */
    void onStart(CeTask ceTask);

    /**
     * Called when the processing of the task is finished (which means: after it's been moved to history).
     */
    void onEnd(CeTask ceTask, CeActivityDto.Status status, Duration duration, @Nullable CeTaskResult taskResult, @Nullable Throwable error);
  }
}
