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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.organization.DefaultOrganizationProvider;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static org.sonar.ce.queue.CeQueue.SubmitOption.UNIQUE_QUEUE_PER_COMPONENT;
import static org.sonar.core.util.stream.MoreCollectors.toEnumSet;
import static org.sonar.db.ce.CeQueueDto.Status.PENDING;

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
    return submit(submission, EnumSet.noneOf(SubmitOption.class)).get();
  }

  @Override
  public java.util.Optional<CeTask> submit(CeTaskSubmit submission, SubmitOption... options) {
    return submit(submission, toSet(options));
  }

  private java.util.Optional<CeTask> submit(CeTaskSubmit submission, EnumSet<SubmitOption> submitOptions) {
    checkState(!submitPaused.get(), "Compute Engine does not currently accept new tasks");
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (submitOptions.contains(UNIQUE_QUEUE_PER_COMPONENT)
        && submission.getComponentUuid() != null
        && dbClient.ceQueueDao().countByStatusAndComponentUuid(dbSession, PENDING, submission.getComponentUuid()) > 0) {
        return java.util.Optional.empty();
      }
      CeQueueDto dto = addToQueueInDb(dbSession, submission);
      CeTask task = loadTask(dbSession, dto);
      dbSession.commit();
      return java.util.Optional.of(task);
    }
  }

  @Override
  public List<CeTask> massSubmit(Collection<CeTaskSubmit> submissions, SubmitOption... options) {
    checkState(!submitPaused.get(), "Compute Engine does not currently accept new tasks");
    if (submissions.isEmpty()) {
      return Collections.emptyList();
    }

    try (DbSession dbSession = dbClient.openSession(true)) {
      List<CeQueueDto> ceQueueDtos = submissions.stream()
        .filter(filterBySubmitOptions(options, submissions, dbSession))
        .map(submission -> addToQueueInDb(dbSession, submission))
        .collect(Collectors.toList());
      List<CeTask> tasks = loadTasks(dbSession, ceQueueDtos);
      dbSession.commit();
      return tasks;
    }
  }

  private Predicate<CeTaskSubmit> filterBySubmitOptions(SubmitOption[] options, Collection<CeTaskSubmit> submissions, DbSession dbSession) {
    EnumSet<SubmitOption> submitOptions = toSet(options);

    if (submitOptions.contains(UNIQUE_QUEUE_PER_COMPONENT)) {
      Set<String> componentUuids = submissions.stream()
        .map(CeTaskSubmit::getComponentUuid)
        .filter(Objects::nonNull)
        .collect(MoreCollectors.toSet(submissions.size()));
      if (componentUuids.isEmpty()) {
        return t -> true;
      }
      return new NoPendingTaskFilter(dbSession, componentUuids);
    }

    return t -> true;
  }

  private class NoPendingTaskFilter implements Predicate<CeTaskSubmit> {
    private final Map<String, Integer> queuedItemsByComponentUuid;

    private NoPendingTaskFilter(DbSession dbSession, Set<String> componentUuids) {
      queuedItemsByComponentUuid = dbClient.ceQueueDao().countByStatusAndComponentUuids(dbSession, PENDING, componentUuids);
    }

    @Override
    public boolean test(CeTaskSubmit ceTaskSubmit) {
      String componentUuid = ceTaskSubmit.getComponentUuid();
      return componentUuid == null || queuedItemsByComponentUuid.getOrDefault(componentUuid, 0) == 0;
    }
  }

  private static EnumSet<SubmitOption> toSet(SubmitOption[] options) {
    return Arrays.stream(options).collect(toEnumSet(SubmitOption.class));
  }

  private CeQueueDto addToQueueInDb(DbSession dbSession, CeTaskSubmit submission) {
    CeQueueDto dto = new CeQueueDto();
    dto.setUuid(submission.getUuid());
    dto.setTaskType(submission.getType());
    dto.setComponentUuid(submission.getComponentUuid());
    dto.setStatus(PENDING);
    dto.setSubmitterLogin(submission.getSubmitterLogin());
    dto.setStartedAt(null);
    dbClient.ceQueueDao().insert(dbSession, dto);
    return dto;
  }

  protected CeTask loadTask(DbSession dbSession, CeQueueDto dto) {
    String componentUuid = dto.getComponentUuid();
    if (componentUuid == null) {
      return new CeQueueDtoToCeTask(defaultOrganizationProvider.get().getUuid()).apply(dto);
    }
    Optional<ComponentDto> componentDto = dbClient.componentDao().selectByUuid(dbSession, componentUuid);
    if (componentDto.isPresent()) {
      return new CeQueueDtoToCeTask(defaultOrganizationProvider.get().getUuid(), ImmutableMap.of(componentUuid, componentDto.get())).apply(dto);
    }
    return new CeQueueDtoToCeTask(defaultOrganizationProvider.get().getUuid()).apply(dto);
  }

  private List<CeTask> loadTasks(DbSession dbSession, List<CeQueueDto> dtos) {
    Set<String> componentUuids = dtos.stream()
      .map(CeQueueDto::getComponentUuid)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    Map<String, ComponentDto> componentDtoByUuid = from(dbClient.componentDao()
      .selectByUuids(dbSession, componentUuids))
      .uniqueIndex(ComponentDto::uuid);

    return dtos.stream()
      .map(new CeQueueDtoToCeTask(defaultOrganizationProvider.get().getUuid(), componentDtoByUuid)::apply)
      .collect(MoreCollectors.toList(dtos.size()));
  }

  @Override
  public void cancel(DbSession dbSession, CeQueueDto ceQueueDto) {
    checkState(PENDING.equals(ceQueueDto.getStatus()), "Task is in progress and can't be canceled [uuid=%s]", ceQueueDto.getUuid());
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

    private CeQueueDtoToCeTask(String defaultOrganizationUuid) {
      this(defaultOrganizationUuid, Collections.emptyMap());
    }

    private CeQueueDtoToCeTask(String defaultOrganizationUuid, Map<String, ComponentDto> componentDtoByUuid) {
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

}
