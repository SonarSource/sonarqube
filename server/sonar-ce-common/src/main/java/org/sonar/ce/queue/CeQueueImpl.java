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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.CeTask;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.db.ce.DeleteIf;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.property.InternalProperties;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.sonar.ce.queue.CeQueue.SubmitOption.UNIQUE_QUEUE_PER_MAIN_COMPONENT;
import static org.sonar.core.util.stream.MoreCollectors.toEnumSet;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.ce.CeQueueDto.Status.IN_PROGRESS;
import static org.sonar.db.ce.CeQueueDto.Status.PENDING;

@ServerSide
public class CeQueueImpl implements CeQueue {

  private final System2 system2;
  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public CeQueueImpl(System2 system2, DbClient dbClient, UuidFactory uuidFactory, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.system2 = system2;
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
  public Optional<CeTask> submit(CeTaskSubmit submission, SubmitOption... options) {
    return submit(submission, toSet(options));
  }

  private Optional<CeTask> submit(CeTaskSubmit submission, EnumSet<SubmitOption> submitOptions) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (submitOptions.contains(UNIQUE_QUEUE_PER_MAIN_COMPONENT)
        && submission.getComponent()
          .map(component -> dbClient.ceQueueDao().countByStatusAndMainComponentUuid(dbSession, PENDING, component.getMainComponentUuid()) > 0)
          .orElse(false)) {
        return Optional.empty();
      }
      CeQueueDto taskDto = addToQueueInDb(dbSession, submission);
      dbSession.commit();

      Map<String, ComponentDto> componentsByUuid = loadComponentDtos(dbSession, taskDto);
      if (componentsByUuid.isEmpty()) {
        return of(convertToTask(dbSession, taskDto, submission.getCharacteristics(), null, null));
      }

      return of(convertToTask(dbSession, taskDto, submission.getCharacteristics(),
        ofNullable(taskDto.getComponentUuid()).map(componentsByUuid::get).orElse(null),
        ofNullable(taskDto.getMainComponentUuid()).map(componentsByUuid::get).orElse(null)));
    }
  }

  Map<String, ComponentDto> loadComponentDtos(DbSession dbSession, CeQueueDto taskDto) {
    Set<String> componentUuids = Stream.of(taskDto.getComponentUuid(), taskDto.getMainComponentUuid())
      .filter(Objects::nonNull)
      .collect(MoreCollectors.toSet(2));
    if (componentUuids.isEmpty()) {
      return emptyMap();
    }

    return dbClient.componentDao().selectByUuids(dbSession, componentUuids)
      .stream()
      .collect(uniqueIndex(ComponentDto::uuid, 2));
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
        .collect(Collectors.toList());
      List<CeTask> tasks = loadTasks(dbSession, taskDtos);
      dbSession.commit();
      return tasks;
    }
  }

  private Predicate<CeTaskSubmit> filterBySubmitOptions(SubmitOption[] options, Collection<CeTaskSubmit> submissions, DbSession dbSession) {
    EnumSet<SubmitOption> submitOptions = toSet(options);

    if (submitOptions.contains(UNIQUE_QUEUE_PER_MAIN_COMPONENT)) {
      Set<String> mainComponentUuids = submissions.stream()
        .map(CeTaskSubmit::getComponent)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(CeTaskSubmit.Component::getMainComponentUuid)
        .collect(MoreCollectors.toSet(submissions.size()));
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
      queuedItemsByMainComponentUuid = dbClient.ceQueueDao().countByStatusAndMainComponentUuids(dbSession, PENDING, projectUuids);
    }

    @Override
    public boolean test(CeTaskSubmit ceTaskSubmit) {
      return ceTaskSubmit.getComponent()
        .map(component -> queuedItemsByMainComponentUuid.getOrDefault(component.getMainComponentUuid(), 0) == 0)
        .orElse(true);
    }
  }

  private static EnumSet<SubmitOption> toSet(SubmitOption[] options) {
    return Arrays.stream(options).collect(toEnumSet(SubmitOption.class));
  }

  private CeQueueDto addToQueueInDb(DbSession dbSession, CeTaskSubmit submission) {
    for (Map.Entry<String, String> characteristic : submission.getCharacteristics().entrySet()) {
      CeTaskCharacteristicDto characteristicDto = new CeTaskCharacteristicDto();
      characteristicDto.setUuid(uuidFactory.create());
      characteristicDto.setTaskUuid(submission.getUuid());
      characteristicDto.setKey(characteristic.getKey());
      characteristicDto.setValue(characteristic.getValue());
      dbClient.ceTaskCharacteristicsDao().insert(dbSession, characteristicDto);
    }

    CeQueueDto dto = new CeQueueDto();
    dto.setUuid(submission.getUuid());
    dto.setTaskType(submission.getType());
    submission.getComponent().ifPresent(component -> dto
      .setComponentUuid(component.getUuid())
      .setMainComponentUuid(component.getMainComponentUuid()));
    dto.setStatus(PENDING);
    dto.setSubmitterUuid(submission.getSubmitterUuid());
    dbClient.ceQueueDao().insert(dbSession, dto);

    return dto;
  }

  private List<CeTask> loadTasks(DbSession dbSession, List<CeQueueDto> dtos) {
    // load components, if defined
    Set<String> componentUuids = dtos.stream()
      .flatMap(dto -> Stream.of(dto.getComponentUuid(), dto.getMainComponentUuid()))
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    Map<String, ComponentDto> componentsByUuid = dbClient.componentDao()
      .selectByUuids(dbSession, componentUuids).stream()
      .collect(uniqueIndex(ComponentDto::uuid));

    // load characteristics
    // TODO could be avoided, characteristics are already present in submissions
    Set<String> taskUuids = dtos.stream().map(CeQueueDto::getUuid).collect(MoreCollectors.toSet(dtos.size()));
    Multimap<String, CeTaskCharacteristicDto> characteristicsByTaskUuid = dbClient.ceTaskCharacteristicsDao()
      .selectByTaskUuids(dbSession, taskUuids).stream()
      .collect(MoreCollectors.index(CeTaskCharacteristicDto::getTaskUuid));

    List<CeTask> result = new ArrayList<>();
    for (CeQueueDto dto : dtos) {
      ComponentDto component = ofNullable(dto.getComponentUuid())
        .map(componentsByUuid::get)
        .orElse(null);
      ComponentDto mainComponent = ofNullable(dto.getMainComponentUuid())
        .map(componentsByUuid::get)
        .orElse(null);
      Map<String, String> characteristics = characteristicsByTaskUuid.get(dto.getUuid()).stream()
        .collect(uniqueIndex(CeTaskCharacteristicDto::getKey, CeTaskCharacteristicDto::getValue));
      result.add(convertToTask(dbSession, dto, characteristics, component, mainComponent));
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
    activityDto.setStatus(CeActivityDto.Status.CANCELED);
    remove(dbSession, q, activityDto);
  }

  @Override
  public void fail(DbSession dbSession, CeQueueDto task, @Nullable String errorType, @Nullable String errorMessage) {
    checkState(IN_PROGRESS.equals(task.getStatus()), "Task is not in-progress and can't be marked as failed [uuid=%s]", task.getUuid());
    CeActivityDto activityDto = new CeActivityDto(task);
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
      Loggers.get(CeQueueImpl.class).debug(
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

  CeTask convertToTask(DbSession dbSession, CeQueueDto taskDto, Map<String, String> characteristics, @Nullable ComponentDto component, @Nullable ComponentDto mainComponent) {
    CeTask.Builder builder = new CeTask.Builder()
      .setUuid(taskDto.getUuid())
      .setType(taskDto.getTaskType())
      .setCharacteristics(characteristics)
      .setSubmitter(resolveSubmitter(dbSession, taskDto.getSubmitterUuid()));


    String componentUuid = taskDto.getComponentUuid();
    if (component != null) {
      builder.setComponent(new CeTask.Component(component.uuid(), component.getDbKey(), component.name()));
      builder.setOrganizationUuid(component.getOrganizationUuid());
    } else if (componentUuid != null) {
      builder.setComponent(new CeTask.Component(componentUuid, null, null));
    }

    String mainComponentUuid = taskDto.getMainComponentUuid();
    if (mainComponent != null) {
      builder.setMainComponent(new CeTask.Component(mainComponent.uuid(), mainComponent.getDbKey(), mainComponent.name()));
    } else if (mainComponentUuid != null) {
      builder.setMainComponent(new CeTask.Component(mainComponentUuid, null, null));
    }

    // FIXME this should be set from the CeQueueDto
    if (!builder.hasOrganizationUuid()) {
      builder.setOrganizationUuid(defaultOrganizationProvider.get().getUuid());
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
