/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.ce.queue;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;

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
import org.sonar.server.organization.DefaultOrganizationProvider;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

@ComputeEngineSide
public class CeQueueImpl implements CeQueue {

  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  // state
  private AtomicBoolean submitPaused = new AtomicBoolean(false);

  public CeQueueImpl(DbClient dbClient, UuidFactory uuidFactory, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  public CeTaskSubmit.Builder prepareSubmit() {
    return new CeTaskSubmit.Builder(uuidFactory.create());
  }

  @Override
  public CeTask submit(CeTaskSubmit submission) {
    checkState(!submitPaused.get(), "Compute Engine does not currently accept new tasks");

    try (DbSession dbSession = dbClient.openSession(false)) {
      CeQueueDto dto = new CeTaskSubmitToInsertedCeQueueDto(dbSession, dbClient).apply(submission);
      CeTask task = loadTask(dbSession, dto);
      dbSession.commit();
      return task;
    }
  }

  @Override
  public List<CeTask> massSubmit(Collection<CeTaskSubmit> submissions) {
    checkState(!submitPaused.get(), "Compute Engine does not currently accept new tasks");
    if (submissions.isEmpty()) {
      return Collections.emptyList();
    }

    try (DbSession dbSession = dbClient.openSession(true)) {
      List<CeQueueDto> ceQueueDtos = from(submissions)
        .transform(new CeTaskSubmitToInsertedCeQueueDto(dbSession, dbClient))
        .toList();
      List<CeTask> tasks = loadTasks(dbSession, ceQueueDtos);
      dbSession.commit();
      return tasks;
    }
  }

  protected CeTask loadTask(DbSession dbSession, CeQueueDto dto) {
    if (dto.getComponentUuid() == null) {
      return new CeQueueDtoToCeTask(defaultOrganizationProvider.get().getUuid()).apply(dto);
    }
    com.google.common.base.Optional<ComponentDto> componentDto = dbClient.componentDao().selectByUuid(dbSession, dto.getComponentUuid());
    if (componentDto.isPresent()) {
      return new CeQueueDtoToCeTask(defaultOrganizationProvider.get().getUuid(), ImmutableMap.of(dto.getComponentUuid(), componentDto.get())).apply(dto);
    }
    return new CeQueueDtoToCeTask(defaultOrganizationProvider.get().getUuid()).apply(dto);
  }

  private List<CeTask> loadTasks(DbSession dbSession, List<CeQueueDto> dtos) {
    Set<String> componentUuids = from(dtos)
      .transform(CeQueueDtoToComponentUuid.INSTANCE)
      .filter(notNull())
      .toSet();
    Map<String, ComponentDto> componentDtoByUuid = from(dbClient.componentDao()
      .selectByUuids(dbSession, componentUuids))
        .uniqueIndex(ComponentDto::uuid);

    return from(dtos)
      .transform(new CeQueueDtoToCeTask(defaultOrganizationProvider.get().getUuid(), componentDtoByUuid))
      .toList();
  }

  @Override
  public void cancel(DbSession dbSession, CeQueueDto ceQueueDto) {
    checkState(CeQueueDto.Status.PENDING.equals(ceQueueDto.getStatus()), "Task is in progress and can't be canceled [uuid=%s]", ceQueueDto.getUuid());
    cancelImpl(dbSession, ceQueueDto);
  }

  private void cancelImpl(DbSession dbSession, CeQueueDto q) {
    CeActivityDto activityDto = new CeActivityDto(q);
    activityDto.setStatus(CeActivityDto.Status.CANCELED);
    remove(dbSession, q, activityDto);
  }

  @Override
  public int cancelAll() {
    return cancelAll(false);
  }

  protected int cancelAll(boolean includeInProgress) {
    int count = 0;
    try (DbSession dbSession = dbClient.openSession(false)) {
      for (CeQueueDto queueDto : dbClient.ceQueueDao().selectAllInAscOrder(dbSession)) {
        if (includeInProgress || !queueDto.getStatus().equals(CeQueueDto.Status.IN_PROGRESS)) {
          cancelImpl(dbSession, queueDto);
          count++;
        }
      }
      return count;
    }
  }

  protected void remove(DbSession dbSession, CeQueueDto queueDto, CeActivityDto activityDto) {
    dbClient.ceActivityDao().insert(dbSession, activityDto);
    dbClient.ceQueueDao().deleteByUuid(dbSession, queueDto.getUuid());
    dbClient.ceTaskInputDao().deleteByUuids(dbSession, singleton(queueDto.getUuid()));
    dbSession.commit();
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
    private final String defaultOrganizationUuid;
    private final Map<String, ComponentDto> componentDtoByUuid;

    public CeQueueDtoToCeTask(String defaultOrganizationUuid) {
      this(defaultOrganizationUuid, Collections.emptyMap());
    }

    public CeQueueDtoToCeTask(String defaultOrganizationUuid, Map<String, ComponentDto> componentDtoByUuid) {
      this.defaultOrganizationUuid = requireNonNull(defaultOrganizationUuid, "defaultOrganizationUuid can't be null");
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
          builder.setOrganizationUuid(component.getOrganizationUuid());
          builder.setComponentKey(component.getDbKey());
          builder.setComponentName(component.name());
        }
      }
      // fixme this should be set from the CeQueueDto
      if (!builder.hasOrganizationUuid()) {
        builder.setOrganizationUuid(defaultOrganizationUuid);
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
