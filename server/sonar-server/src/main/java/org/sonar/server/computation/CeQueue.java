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

import com.google.common.base.Optional;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.server.computation.monitoring.CEQueueStatus;

import static java.lang.String.format;

/**
 * Queue of pending Compute Engine tasks. Both producer and consumer actions
 * are implemented.
 * <p>
 *   This class is decoupled from the regular task type {@link org.sonar.db.ce.CeTaskTypes#REPORT}.
 * </p>
 */
@ServerSide
public class CeQueue {

  private final System2 system2;
  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final CEQueueStatus queueStatus;
  private final CeQueueListener[] listeners;

  // state
  private boolean submitPaused = false;
  private boolean peekPaused = false;

  public CeQueue(System2 system2, DbClient dbClient, UuidFactory uuidFactory,
    CEQueueStatus queueStatus, CeQueueListener[] listeners) {
    this.system2 = system2;
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
    this.queueStatus = queueStatus;
    this.listeners = listeners;
  }

  public CeTaskSubmit prepareSubmit() {
    return new CeTaskSubmit(uuidFactory.create());
  }

  public CeTask submit(CeTaskSubmit submit) {
    if (submitPaused) {
      throw new IllegalStateException("Compute Engine does not currently accept new tasks");
    }
    CeTask task = new CeTask(submit);
    DbSession dbSession = dbClient.openSession(false);
    try {
      CeQueueDto dto = new CeQueueDto();
      dto.setUuid(task.getUuid());
      dto.setTaskType(task.getType());
      dto.setComponentUuid(task.getComponentUuid());
      dto.setStatus(CeQueueDto.Status.PENDING);
      dto.setSubmitterLogin(task.getSubmitterLogin());
      dto.setStartedAt(null);
      dbClient.ceQueueDao().insert(dbSession, dto);
      dbSession.commit();
      queueStatus.addReceived();
      return task;
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  public Optional<CeTask> peek() {
    if (peekPaused) {
      return Optional.absent();
    }
    DbSession dbSession = dbClient.openSession(false);
    try {
      Optional<CeQueueDto> dto = dbClient.ceQueueDao().peek(dbSession);
      if (!dto.isPresent()) {
        return Optional.absent();
      }
      queueStatus.addInProgress();
      return Optional.of(new CeTask(dto.get()));

    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  public boolean cancel(String taskUuid) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Optional<CeQueueDto> queueDto = dbClient.ceQueueDao().selectByUuid(dbSession, taskUuid);
      if (queueDto.isPresent()) {
        if (!queueDto.get().getStatus().equals(CeQueueDto.Status.PENDING)) {
          throw new IllegalStateException(String.format("Task is in progress and can't be cancelled [uuid=%s]", taskUuid));
        }
        cancel(dbSession, queueDto.get());
        return true;
      }
      return false;
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  void cancel(DbSession dbSession, CeQueueDto q) {
    CeActivityDto activityDto = new CeActivityDto(q);
    activityDto.setStatus(CeActivityDto.Status.CANCELED);
    remove(dbSession, new CeTask(q), q, activityDto);
  }


  /**
   * Removes all the tasks from the queue, whatever their status. They are marked
   * as {@link org.sonar.db.ce.CeActivityDto.Status#CANCELED} in past activity.
   * This method can NOT be called when  workers are being executed, as in progress
   * tasks can't be killed.
   *
   * @return the number of canceled tasks
   */
  public int clear() {
    return cancelAll(true);
  }

  /**
   * Similar as {@link #clear()}, except that the tasks with status
   * {@link org.sonar.db.ce.CeQueueDto.Status#IN_PROGRESS} are ignored. This method
   * can be called at runtime, even if workers are being executed.
   *
   * @return the number of canceled tasks
   */
  public int cancelAll() {
    return cancelAll(false);
  }

  private int cancelAll(boolean includeInProgress) {
    int count = 0;
    DbSession dbSession = dbClient.openSession(false);
    try {
      for (CeQueueDto queueDto : dbClient.ceQueueDao().selectAllInAscOrder(dbSession)) {
        if (includeInProgress || !queueDto.getStatus().equals(CeQueueDto.Status.IN_PROGRESS)) {
          cancel(dbSession, queueDto);
          count++;
        }
      }
      return count;
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  public void remove(CeTask task, CeActivityDto.Status status) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Optional<CeQueueDto> queueDto = dbClient.ceQueueDao().selectByUuid(dbSession, task.getUuid());
      if (!queueDto.isPresent()) {
        throw new IllegalStateException(format("Task does not exist anymore: %s", task));
      }
      CeActivityDto activityDto = new CeActivityDto(queueDto.get());
      activityDto.setStatus(status);
      Long startedAt = activityDto.getStartedAt();
      if (startedAt != null) {
        activityDto.setFinishedAt(system2.now());
        long executionTime = activityDto.getFinishedAt() - startedAt;
        activityDto.setExecutionTimeMs(executionTime);
        if (status == CeActivityDto.Status.SUCCESS) {
          queueStatus.addSuccess(executionTime);
        } else {
          queueStatus.addError(executionTime);
        }
      }
      remove(dbSession, task, queueDto.get(), activityDto);

    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private void remove(DbSession dbSession, CeTask task, CeQueueDto queueDto, CeActivityDto activityDto) {
    dbClient.ceActivityDao().insert(dbSession, activityDto);
    dbClient.ceQueueDao().deleteByUuid(dbSession, queueDto.getUuid());
    dbSession.commit();
    for (CeQueueListener listener : listeners) {
      listener.onRemoved(task, activityDto.getStatus());
    }
  }

  public void pauseSubmit() {
    this.submitPaused = true;
  }

  public void resumeSubmit() {
    this.submitPaused = false;
  }

  public boolean isSubmitPaused() {
    return submitPaused;
  }

  public void pausePeek() {
    this.peekPaused = true;
  }

  public void resumePeek() {
    this.peekPaused = false;
  }

  public boolean isPeekPaused() {
    return peekPaused;
  }
}
