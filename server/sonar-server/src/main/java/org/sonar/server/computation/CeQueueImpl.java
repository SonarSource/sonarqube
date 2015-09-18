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
import com.google.common.base.Strings;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.server.computation.monitoring.CEQueueStatus;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

@ServerSide
public class CeQueueImpl implements CeQueue {

  private final System2 system2;
  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final CEQueueStatus queueStatus;
  private final CeQueueListener[] listeners;

  // state
  private AtomicBoolean submitPaused = new AtomicBoolean(false);
  private AtomicBoolean peekPaused = new AtomicBoolean(false);

  public CeQueueImpl(System2 system2, DbClient dbClient, UuidFactory uuidFactory,
    CEQueueStatus queueStatus, CeQueueListener[] listeners) {
    this.system2 = system2;
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
    this.queueStatus = queueStatus;
    this.listeners = listeners;
  }

  @Override
  public TaskSubmission prepareSubmit() {
    return new TaskSubmissionImpl(uuidFactory.create());
  }

  @Override
  public CeTask submit(TaskSubmission submission) {
    checkArgument(!Strings.isNullOrEmpty(submission.getUuid()));
    checkArgument(!Strings.isNullOrEmpty(submission.getType()));
    checkArgument(submission instanceof TaskSubmissionImpl);
    checkState(!submitPaused.get(), "Compute Engine does not currently accept new tasks");

    CeTask task = new CeTask(submission);
    DbSession dbSession = dbClient.openSession(false);
    try {
      CeQueueDto dto = createQueueDtoForSubmit(task);
      dbClient.ceQueueDao().insert(dbSession, dto);
      dbSession.commit();
      queueStatus.addReceived();
      return task;
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private CeQueueDto createQueueDtoForSubmit(CeTask task) {
    CeQueueDto dto = new CeQueueDto();
    dto.setUuid(task.getUuid());
    dto.setTaskType(task.getType());
    dto.setComponentUuid(task.getComponentUuid());
    dto.setStatus(CeQueueDto.Status.PENDING);
    dto.setSubmitterLogin(task.getSubmitterLogin());
    dto.setStartedAt(null);
    return dto;
  }

  @Override
  public Optional<CeTask> peek() {
    if (peekPaused.get()) {
      return Optional.absent();
    }
    DbSession dbSession = dbClient.openSession(false);
    try {
      Optional<CeQueueDto> dto = dbClient.ceQueueDao().peek(dbSession);
      CeTask task = null;
      if (dto.isPresent()) {
        queueStatus.addInProgress();
        task = new CeTask(dto.get());
      }
      return Optional.fromNullable(task);

    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  @Override
  public boolean cancel(String taskUuid) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Optional<CeQueueDto> queueDto = dbClient.ceQueueDao().selectByUuid(dbSession, taskUuid);
      if (queueDto.isPresent()) {
        checkState(CeQueueDto.Status.PENDING.equals(queueDto.get().getStatus()), "Task is in progress and can't be canceled [uuid=%s]", taskUuid);
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

  @Override
  public int clear() {
    return cancelAll(true);
  }

  @Override
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

  @Override
  public void remove(CeTask task, CeActivityDto.Status status) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Optional<CeQueueDto> queueDto = dbClient.ceQueueDao().selectByUuid(dbSession, task.getUuid());
      if (!queueDto.isPresent()) {
        throw new IllegalStateException(format("Task does not exist anymore: %s", task));
      }
      CeActivityDto activityDto = new CeActivityDto(queueDto.get());
      activityDto.setStatus(status);
      updateQueueStatus(status, activityDto);
      remove(dbSession, task, queueDto.get(), activityDto);

    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private void updateQueueStatus(CeActivityDto.Status status, CeActivityDto activityDto) {
    Long startedAt = activityDto.getStartedAt();
    if (startedAt == null) {
      return;
    }
    activityDto.setFinishedAt(system2.now());
    long executionTime = activityDto.getFinishedAt() - startedAt;
    activityDto.setExecutionTimeMs(executionTime);
    if (status == CeActivityDto.Status.SUCCESS) {
      queueStatus.addSuccess(executionTime);
    } else {
      queueStatus.addError(executionTime);
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

  @Override
  public void pauseSubmit() {
    this.submitPaused.set(true);
  }

  @Override
  public void resumeSubmit() {
    this.submitPaused.set(false);
  }

  @Override
  public boolean isSubmitPaused() {
    return submitPaused.get();
  }

  @Override
  public void pausePeek() {
    this.peekPaused.set(true);
  }

  @Override
  public void resumePeek() {
    this.peekPaused.set(false);
  }

  @Override
  public boolean isPeekPaused() {
    return peekPaused.get();
  }

  private static class TaskSubmissionImpl implements TaskSubmission {
    private final String uuid;
    private String type;
    private String componentUuid;
    private String submitterLogin;

    private TaskSubmissionImpl(String uuid) {
      this.uuid = uuid;
    }

    @Override
    public String getUuid() {
      return uuid;
    }

    @Override
    public String getType() {
      return type;
    }

    @Override
    public TaskSubmission setType(@Nullable String s) {
      this.type = s;
      return this;
    }

    @Override
    public String getComponentUuid() {
      return componentUuid;
    }

    @Override
    public TaskSubmission setComponentUuid(@Nullable String s) {
      this.componentUuid = s;
      return this;
    }

    @Override
    public String getSubmitterLogin() {
      return submitterLogin;
    }

    @Override
    public TaskSubmission setSubmitterLogin(@Nullable String s) {
      this.submitterLogin = s;
      return this;
    }
  }
}
