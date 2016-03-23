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
package org.sonar.ce.queue;

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
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.ComponentDto;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sonar.db.component.ComponentDtoFunctions.toUuid;

@ComputeEngineSide
public class CeQueueImpl implements CeQueue {

  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final CeQueueListener[] listeners;

  // state
  private AtomicBoolean submitPaused = new AtomicBoolean(false);

  /**
   * Constructor in case there is no CeQueueListener
   */
  public CeQueueImpl(DbClient dbClient, UuidFactory uuidFactory) {
    this(dbClient, uuidFactory, new CeQueueListener[] {});
  }

  public CeQueueImpl(DbClient dbClient, UuidFactory uuidFactory, CeQueueListener[] listeners) {
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
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
      return tasks;

    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  protected CeTask loadTask(DbSession dbSession, CeQueueDto dto) {
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
        cancelImpl(dbSession, queueDto.get());
        return true;
      }
      return false;
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  protected void cancelImpl(DbSession dbSession, CeQueueDto q) {
    CeTask task = loadTask(dbSession, q);
    CeActivityDto activityDto = new CeActivityDto(q);
    activityDto.setStatus(CeActivityDto.Status.CANCELED);
    remove(dbSession, task, q, activityDto);
  }

  @Override
  public int cancelAll() {
    return cancelAll(false);
  }

  protected int cancelAll(boolean includeInProgress) {
    int count = 0;
    DbSession dbSession = dbClient.openSession(false);
    try {
      for (CeQueueDto queueDto : dbClient.ceQueueDao().selectAllInAscOrder(dbSession)) {
        if (includeInProgress || !queueDto.getStatus().equals(CeQueueDto.Status.IN_PROGRESS)) {
          cancelImpl(dbSession, queueDto);
          count++;
        }
      }
      return count;
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  protected void remove(DbSession dbSession, CeTask task, CeQueueDto queueDto, CeActivityDto activityDto) {
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
