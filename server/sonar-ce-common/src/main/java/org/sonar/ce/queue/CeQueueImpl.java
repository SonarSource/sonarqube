/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.CeTask;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.db.ce.CeTaskQuery;
import org.sonar.db.ce.DeleteIf;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.platform.NodeInformation;
import org.sonar.server.property.InternalProperties;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singleton;
import static java.util.Optional.ofNullable;
import static org.sonar.ce.queue.CeQueue.SubmitOption.UNIQUE_QUEUE_PER_ENTITY;
import static org.sonar.db.ce.CeQueueDto.Status.IN_PROGRESS;
import static org.sonar.db.ce.CeQueueDto.Status.PENDING;

@ServerSide
public class CeQueueImpl implements CeQueue {

  private final System2 system2;
  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  protected final NodeInformation nodeInformation;

  public CeQueueImpl(System2 system2, DbClient dbClient, UuidFactory uuidFactory, NodeInformation nodeInformation) {
    this.system2 = system2;
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
    this.nodeInformation = nodeInformation;
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
  public Optional<CeTask> submit(CeTaskSubmit submission, SubmitOption... options) {
    return submit(submission, toSet(options));
  }

  @Override
  public Optional<CeTask> submit(DbSession dbSession, CeTaskSubmit submission, SubmitOption... options) {
    return submit(dbSession, submission, toSet(options));
  }

  private Optional<CeTask> submit(CeTaskSubmit submission, Set<SubmitOption> submitOptions) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<CeTask> ceTask = submit(dbSession, submission, submitOptions);
      dbSession.commit();
      return ceTask;
    }
  }

  private Optional<CeTask> submit(DbSession dbSession, CeTaskSubmit submission, Set<SubmitOption> submitOptions) {
    CeTaskQuery query = new CeTaskQuery();
    for (SubmitOption option : submitOptions) {
      switch (option) {
        case UNIQUE_QUEUE_PER_ENTITY -> submission.getComponent()
          .flatMap(component -> Optional.ofNullable(component.getEntityUuid()))
          .ifPresent(entityUuid -> query.setEntityUuid(entityUuid).setStatuses(List.of(PENDING.name())));
        case UNIQUE_QUEUE_PER_TASK_TYPE -> query.setType(submission.getType());
      }
    }

    boolean queryNonEmpty = query.getEntityUuids() != null || query.getStatuses() != null || query.getType() != null;
    if (queryNonEmpty && dbClient.ceQueueDao().countByQuery(dbSession, query) > 0) {
      return Optional.empty();
    }
    CeQueueDto inserted = addToQueueInDb(dbSession, submission);
    return Optional.of(convertQueueDtoToTask(dbSession, inserted, submission));
  }

  private CeTask convertQueueDtoToTask(DbSession dbSession, CeQueueDto queueDto, CeTaskSubmit submission) {
    return convertToTask(dbSession, queueDto, submission.getCharacteristics());
  }

  protected CeTask convertToTask(DbSession dbSession, CeQueueDto queueDto, Map<String, String> characteristicDto) {
    ComponentDto component = null;
    EntityDto entity = null;

    if (queueDto.getComponentUuid() != null) {
      component = dbClient.componentDao().selectByUuid(dbSession, queueDto.getComponentUuid()).orElse(null);
    }
    if (queueDto.getEntityUuid() != null) {
      entity = dbClient.entityDao().selectByUuid(dbSession, queueDto.getEntityUuid()).orElse(null);
    }

    return convertToTask(dbSession, queueDto, characteristicDto, component, entity);

  }

  @Override
  public List<CeTask> massSubmit(Collection<CeTaskSubmit> submissions, SubmitOption... options) {
    if (submissions.isEmpty()) {
      return Collections.emptyList();
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      List<CeQueueDto> taskDtos = submissions.stream()
        .filter(filterBySubmitOptions(options, submissions, dbSession))
        .map(submission -> addToQueueInDb(dbSession, submission))
        .toList();
      List<CeTask> tasks = loadTasks(dbSession, taskDtos);
      dbSession.commit();
      return tasks;
    }
  }

  private Predicate<CeTaskSubmit> filterBySubmitOptions(SubmitOption[] options, Collection<CeTaskSubmit> submissions, DbSession dbSession) {
    Set<SubmitOption> submitOptions = toSet(options);

    if (submitOptions.contains(UNIQUE_QUEUE_PER_ENTITY)) {
      Set<String> mainComponentUuids = submissions.stream()
        .map(CeTaskSubmit::getComponent)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(CeTaskSubmit.Component::getEntityUuid)
        .collect(Collectors.toSet());
      if (mainComponentUuids.isEmpty()) {
        return t -> true;
      }
      return new NoPendingTaskFilter(dbSession, mainComponentUuids);
    }

    return t -> true;
  }

  private class NoPendingTaskFilter implements Predicate<CeTaskSubmit> {
    private final Map<String, Integer> queuedItemsByMainComponentUuid;

    private NoPendingTaskFilter(DbSession dbSession, Set<String> projectUuids) {
      queuedItemsByMainComponentUuid = dbClient.ceQueueDao().countByStatusAndEntityUuids(dbSession, PENDING, projectUuids);
    }

    @Override
    public boolean test(CeTaskSubmit ceTaskSubmit) {
      return ceTaskSubmit.getComponent()
        .map(component -> queuedItemsByMainComponentUuid.getOrDefault(component.getEntityUuid(), 0) == 0)
        .orElse(true);
    }
  }

  private static Set<SubmitOption> toSet(SubmitOption[] options) {
    return Arrays.stream(options).collect(Collectors.toSet());
  }

  private void insertCharacteristics(DbSession dbSession, CeTaskSubmit submission) {
    for (Map.Entry<String, String> characteristic : submission.getCharacteristics().entrySet()) {
      CeTaskCharacteristicDto characteristicDto = new CeTaskCharacteristicDto();
      characteristicDto.setUuid(uuidFactory.create());
      characteristicDto.setTaskUuid(submission.getUuid());
      characteristicDto.setKey(characteristic.getKey());
      characteristicDto.setValue(characteristic.getValue());
      dbClient.ceTaskCharacteristicsDao().insert(dbSession, characteristicDto);
    }
  }

  private CeQueueDto addToQueueInDb(DbSession dbSession, CeTaskSubmit submission) {
    insertCharacteristics(dbSession, submission);

    CeQueueDto dto = new CeQueueDto();
    dto.setUuid(submission.getUuid());
    dto.setTaskType(submission.getType());
    submission.getComponent().ifPresent(component -> dto
      .setComponentUuid(component.getUuid())
      .setEntityUuid(component.getEntityUuid()));
    dto.setStatus(PENDING);
    dto.setSubmitterUuid(submission.getSubmitterUuid());
    dbClient.ceQueueDao().insert(dbSession, dto);

    return dto;
  }

  private List<CeTask> loadTasks(DbSession dbSession, List<CeQueueDto> dtos) {
    // load components, if defined
    Set<String> componentUuids = dtos.stream()
      .map(CeQueueDto::getComponentUuid)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    // these components correspond to a branch or a portfolio (analysis target)
    Map<String, ComponentDto> componentsByUuid = dbClient.componentDao()
      .selectByUuids(dbSession, componentUuids).stream()
      .collect(Collectors.toMap(ComponentDto::uuid, Function.identity()));
    Set<String> entityUuids = dtos.stream().map(CeQueueDto::getEntityUuid).filter(Objects::nonNull).collect(Collectors.toSet());
    Map<String, EntityDto> entitiesByUuid = dbClient.entityDao().selectByUuids(dbSession, entityUuids).stream()
      .collect(Collectors.toMap(EntityDto::getUuid, e -> e));

    // load characteristics
    // TODO could be avoided, characteristics are already present in submissions
    Set<String> taskUuids = dtos.stream().map(CeQueueDto::getUuid).collect(Collectors.toSet());
    Multimap<String, CeTaskCharacteristicDto> characteristicsByTaskUuid = dbClient.ceTaskCharacteristicsDao()
      .selectByTaskUuids(dbSession, taskUuids).stream()
      .collect(MoreCollectors.index(CeTaskCharacteristicDto::getTaskUuid));

    List<CeTask> result = new ArrayList<>();
    for (CeQueueDto dto : dtos) {
      ComponentDto component = ofNullable(dto.getComponentUuid())
        .map(componentsByUuid::get)
        .orElse(null);
      EntityDto entity = ofNullable(dto.getEntityUuid())
        .map(entitiesByUuid::get)
        .orElse(null);
      Map<String, String> characteristics = characteristicsByTaskUuid.get(dto.getUuid()).stream()
        .collect(Collectors.toMap(CeTaskCharacteristicDto::getKey, CeTaskCharacteristicDto::getValue));
      result.add(convertToTask(dbSession, dto, characteristics, component, entity));
    }
    return result;
  }

  @Override
  public void cancel(DbSession dbSession, CeQueueDto ceQueueDto) {
    checkState(PENDING.equals(ceQueueDto.getStatus()), "Task is in progress and can't be canceled [uuid=%s]", ceQueueDto.getUuid());
    cancelImpl(dbSession, ceQueueDto);
  }

  private void cancelImpl(DbSession dbSession, CeQueueDto q) {
    CeActivityDto activityDto = new CeActivityDto(q);
    activityDto.setNodeName(nodeInformation.getNodeName().orElse(null));
    activityDto.setStatus(CeActivityDto.Status.CANCELED);
    remove(dbSession, q, activityDto);
  }

  @Override
  public void fail(DbSession dbSession, CeQueueDto task, @Nullable String errorType, @Nullable String errorMessage) {
    checkState(IN_PROGRESS.equals(task.getStatus()), "Task is not in-progress and can't be marked as failed [uuid=%s]", task.getUuid());
    CeActivityDto activityDto = new CeActivityDto(task);
    activityDto.setNodeName(nodeInformation.getNodeName().orElse(null));
    activityDto.setStatus(CeActivityDto.Status.FAILED);
    activityDto.setErrorType(errorType);
    activityDto.setErrorMessage(errorMessage);
    updateExecutionFields(activityDto);
    remove(dbSession, task, activityDto);
  }

  protected long updateExecutionFields(CeActivityDto activityDto) {
    Long startedAt = activityDto.getStartedAt();
    if (startedAt == null) {
      return 0L;
    }
    long now = system2.now();
    long executionTimeInMs = now - startedAt;
    activityDto.setExecutedAt(now);
    activityDto.setExecutionTimeMs(executionTimeInMs);
    return executionTimeInMs;
  }

  protected void remove(DbSession dbSession, CeQueueDto queueDto, CeActivityDto activityDto) {
    String taskUuid = queueDto.getUuid();
    CeQueueDto.Status expectedQueueDtoStatus = queueDto.getStatus();

    dbClient.ceActivityDao().insert(dbSession, activityDto);
    dbClient.ceTaskInputDao().deleteByUuids(dbSession, singleton(taskUuid));
    int deletedTasks = dbClient.ceQueueDao().deleteByUuid(dbSession, taskUuid, new DeleteIf(expectedQueueDtoStatus));

    if (deletedTasks == 1) {
      dbSession.commit();
    } else {
      LoggerFactory.getLogger(CeQueueImpl.class).debug(
        "Remove rolled back because task in queue with uuid {} and status {} could not be deleted",
        taskUuid, expectedQueueDtoStatus);
      dbSession.rollback();
    }
  }

  @Override
  public int cancelAll() {
    return cancelAll(false);
  }

  int cancelAll(boolean includeInProgress) {
    int count = 0;
    try (DbSession dbSession = dbClient.openSession(false)) {
      for (CeQueueDto queueDto : dbClient.ceQueueDao().selectAllInAscOrder(dbSession)) {
        if (includeInProgress || !queueDto.getStatus().equals(IN_PROGRESS)) {
          cancelImpl(dbSession, queueDto);
          count++;
        }
      }
      return count;
    }
  }

  @Override
  public void pauseWorkers() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.internalPropertiesDao().save(dbSession, InternalProperties.COMPUTE_ENGINE_PAUSE, "true");
      dbSession.commit();
    }
  }

  @Override
  public void resumeWorkers() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.internalPropertiesDao().delete(dbSession, InternalProperties.COMPUTE_ENGINE_PAUSE);
      dbSession.commit();
    }
  }

  @Override
  public WorkersPauseStatus getWorkersPauseStatus() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<String> propValue = dbClient.internalPropertiesDao().selectByKey(dbSession, InternalProperties.COMPUTE_ENGINE_PAUSE);
      if (!propValue.isPresent() || !propValue.get().equals("true")) {
        return WorkersPauseStatus.RESUMED;
      }
      int countInProgress = dbClient.ceQueueDao().countByStatus(dbSession, IN_PROGRESS);
      if (countInProgress > 0) {
        return WorkersPauseStatus.PAUSING;
      }
      return WorkersPauseStatus.PAUSED;
    }
  }

  @Override
  public int clear() {
    return cancelAll(true);
  }

  CeTask convertToTask(DbSession dbSession, CeQueueDto taskDto, Map<String, String> characteristics, @Nullable ComponentDto component, @Nullable EntityDto entity) {
    CeTask.Builder builder = new CeTask.Builder()
      .setUuid(taskDto.getUuid())
      .setType(taskDto.getTaskType())
      .setCharacteristics(characteristics)
      .setSubmitter(resolveSubmitter(dbSession, taskDto.getSubmitterUuid()));

    String componentUuid = taskDto.getComponentUuid();
    if (component != null) {
      builder.setComponent(new CeTask.Component(component.uuid(), component.getKey(), component.name()));
    } else if (componentUuid != null) {
      builder.setComponent(new CeTask.Component(componentUuid, null, null));
    }

    String entityUuid = taskDto.getEntityUuid();
    if (entity != null) {
      builder.setEntity(new CeTask.Component(entity.getUuid(), entity.getKey(), entity.getName()));
    } else if (entityUuid != null) {
      builder.setEntity(new CeTask.Component(entityUuid, null, null));
    }

    return builder.build();
  }

  @CheckForNull
  private CeTask.User resolveSubmitter(DbSession dbSession, @Nullable String submitterUuid) {
    if (submitterUuid == null) {
      return null;
    }
    UserDto submitterDto = dbClient.userDao().selectByUuid(dbSession, submitterUuid);
    if (submitterDto != null) {
      return new CeTask.User(submitterUuid, submitterDto.getLogin());
    } else {
      return new CeTask.User(submitterUuid, null);
    }
  }

}
