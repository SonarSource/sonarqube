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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.computation.monitoring.CEQueueStatus;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static java.lang.String.format;
import static org.sonar.db.component.ComponentDtoFunctions.toUuid;

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
  public CeTaskSubmit.Builder prepareSubmit() {
    return new CeTaskSubmit.Builder(uuidFactory.create());
  }

  @Override
  public CeTask submit(CeTaskSubmit submission) {
    checkState(!submitPaused.get(), "Compute Engine does not currently accept new tasks");

    DbSession dbSession = dbClient.openSession(false);
    try {
      CeQueueDto dto = new CeTaskSubmitToInsertedCeQueueDto(dbSession, dbClient).apply(submission);
      CeTask task = loadTask(dbSession, dto);
      dbSession.commit();
      queueStatus.addReceived();
      return task;

    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  @Override
  public List<CeTask> massSubmit(Collection<CeTaskSubmit> submissions) {
    checkState(!submitPaused.get(), "Compute Engine does not currently accept new tasks");
    if (submissions.isEmpty()) {
      return Collections.emptyList();
    }

    DbSession dbSession = dbClient.openSession(true);
    try {
      List<CeQueueDto> ceQueueDtos = from(submissions)
        .transform(new CeTaskSubmitToInsertedCeQueueDto(dbSession, dbClient))
        .toList();
      List<CeTask> tasks = loadTasks(dbSession, ceQueueDtos);
      dbSession.commit();
      queueStatus.addReceived(tasks.size());
      return tasks;

    } finally {
      dbClient.closeSession(dbSession);
    }
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
        task = loadTask(dbSession, dto.get());
        queueStatus.addInProgress();
      }
      return Optional.fromNullable(task);

    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private CeTask loadTask(DbSession dbSession, CeQueueDto dto) {
    if (dto.getComponentUuid() == null) {
      return new CeQueueDtoToCeTask().apply(dto);
    }
    Optional<ComponentDto> componentDto = dbClient.componentDao().selectByUuid(dbSession, dto.getComponentUuid());
    if (componentDto.isPresent()) {
      return new CeQueueDtoToCeTask(ImmutableMap.of(dto.getComponentUuid(), componentDto.get())).apply(dto);
    }
    return new CeQueueDtoToCeTask().apply(dto);
  }

  private List<CeTask> loadTasks(DbSession dbSession, List<CeQueueDto> dtos) {
    Set<String> componentUuids = from(dtos)
      .transform(CeQueueDtoToComponentUuid.INSTANCE)
      .filter(notNull())
      .toSet();
    Map<String, ComponentDto> componentDtoByUuid = from(dbClient.componentDao()
      .selectByUuids(dbSession, componentUuids))
      .uniqueIndex(toUuid());

    return from(dtos)
      .transform(new CeQueueDtoToCeTask(componentDtoByUuid))
      .toList();
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
    CeTask task = loadTask(dbSession, q);
    CeActivityDto activityDto = new CeActivityDto(q);
    activityDto.setStatus(CeActivityDto.Status.CANCELED);
    remove(dbSession, task, q, activityDto);
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
  public void remove(CeTask task, CeActivityDto.Status status, CeTaskResult taskResult) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Optional<CeQueueDto> queueDto = dbClient.ceQueueDao().selectByUuid(dbSession, task.getUuid());
      if (!queueDto.isPresent()) {
        throw new IllegalStateException(format("Task does not exist anymore: %s", task));
      }
      CeActivityDto activityDto = new CeActivityDto(queueDto.get());
      activityDto.setStatus(status);
      updateQueueStatus(status, activityDto);
      updateTaskResult(activityDto, taskResult);
      remove(dbSession, task, queueDto.get(), activityDto);

    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static void updateTaskResult(CeActivityDto activityDto, @Nullable CeTaskResult taskResult) {
    if (taskResult != null) {
      Long snapshotId = taskResult.getSnapshotId();
      if (snapshotId != null) {
        activityDto.setSnapshotId(snapshotId);
      }
    }
  }

  private void updateQueueStatus(CeActivityDto.Status status, CeActivityDto activityDto) {
    Long startedAt = activityDto.getStartedAt();
    if (startedAt == null) {
      return;
    }
    activityDto.setExecutedAt(system2.now());
    long executionTime = activityDto.getExecutedAt() - startedAt;
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

  private static class CeQueueDtoToCeTask implements Function<CeQueueDto, CeTask> {
    private final Map<String, ComponentDto> componentDtoByUuid;

    public CeQueueDtoToCeTask() {
      this.componentDtoByUuid = Collections.emptyMap();
    }

    public CeQueueDtoToCeTask(Map<String, ComponentDto> componentDtoByUuid) {
      this.componentDtoByUuid = componentDtoByUuid;
    }

    @Override
    @Nonnull
    public CeTask apply(@Nonnull CeQueueDto dto) {
      CeTask.Builder builder = new CeTask.Builder();
      builder.setUuid(dto.getUuid());
      builder.setType(dto.getTaskType());
      builder.setSubmitterLogin(dto.getSubmitterLogin());
      String componentUuid = dto.getComponentUuid();
      if (componentUuid != null) {
        builder.setComponentUuid(componentUuid);
        ComponentDto component = componentDtoByUuid.get(componentUuid);
        if (component != null) {
          builder.setComponentKey(component.getKey());
          builder.setComponentName(component.name());
        }
      }
      return builder.build();
    }
  }

  private static class CeTaskSubmitToInsertedCeQueueDto implements Function<CeTaskSubmit, CeQueueDto> {
    private final DbSession dbSession;
    private final DbClient dbClient;

    public CeTaskSubmitToInsertedCeQueueDto(DbSession dbSession, DbClient dbClient) {
      this.dbSession = dbSession;
      this.dbClient = dbClient;
    }

    @Override
    @Nonnull
    public CeQueueDto apply(@Nonnull CeTaskSubmit submission) {
      CeQueueDto dto = new CeQueueDto();
      dto.setUuid(submission.getUuid());
      dto.setTaskType(submission.getType());
      dto.setComponentUuid(submission.getComponentUuid());
      dto.setStatus(CeQueueDto.Status.PENDING);
      dto.setSubmitterLogin(submission.getSubmitterLogin());
      dto.setStartedAt(null);
      dbClient.ceQueueDao().insert(dbSession, dto);
      return dto;
    }
  }

  private enum CeQueueDtoToComponentUuid implements Function<CeQueueDto, String> {
    INSTANCE;

    @Override
    @Nullable
    public String apply(@Nonnull CeQueueDto input) {
      return input.getComponentUuid();
    }
  }
}
