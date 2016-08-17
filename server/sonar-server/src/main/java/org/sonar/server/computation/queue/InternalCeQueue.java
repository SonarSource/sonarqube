/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.queue;

import com.google.common.base.Optional;
import javax.annotation.Nullable;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeTask;
import org.sonar.ce.queue.CeTaskResult;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto.Status;
import org.sonar.db.ce.CeQueueDto;

/**
 * Queue of pending Compute Engine tasks. Both producer and consumer actions
 * are implemented.
 * <p>
 *   This class is decoupled from the regular task type {@link org.sonar.db.ce.CeTaskTypes#REPORT}.
 * </p>
 */
public interface InternalCeQueue extends CeQueue {

  /**
   * Peek the oldest task in status {@link org.sonar.db.ce.CeQueueDto.Status#PENDING}.
   * The task status is changed to {@link org.sonar.db.ce.CeQueueDto.Status#IN_PROGRESS}.
   * Does not return anything if the queue is paused (see {@link #isPeekPaused()}.
   *
   * <p>Only a single task can be peeked by project.</p>
   *
   * <p>An unchecked exception may be thrown on technical errors (db connection, ...).</p>
   */
  Optional<CeTask> peek();

  /**
   * Removes all the tasks from the queue, whatever their status. They are marked
   * as {@link Status#CANCELED} in past activity.
   * This method can NOT be called when  workers are being executed, as in progress
   * tasks can't be killed.
   *
   * @return the number of canceled tasks
   */
  int clear();

  /**
   * Removes a task from the queue and registers it to past activities. This method
   * is called by Compute Engine workers when task is processed and can include an option {@link CeTaskResult} object.
   *
   * @throws IllegalStateException if the task does not exist in the queue
   * @throws IllegalArgumentException if {@code error} is non {@code null} but {@code status} is not {@link Status#FAILED}
   */
  void remove(CeTask task, Status status, @Nullable CeTaskResult taskResult, @Nullable Throwable error);

  void cancel(DbSession dbSession, CeQueueDto ceQueueDto);

  void pausePeek();

  void resumePeek();

  boolean isPeekPaused();
}
