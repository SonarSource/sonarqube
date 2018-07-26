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

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.ce.task.CeTask;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.sonar.ce.queue.CeQueue.SubmitOption.UNIQUE_QUEUE_PER_COMPONENT;

public class CeQueueImplTest {

  private static final String WORKER_UUID = "workerUuid";

  private System2 system2 = new TestSystem2().setNow(1_450_000_000_000L);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(system2);

  private DbSession session = db.getSession();

  private UuidFactory uuidFactory = UuidFactoryImpl.INSTANCE;
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);

  private CeQueue underTest = new CeQueueImpl(db.getDbClient(), uuidFactory, defaultOrganizationProvider);

  @Test
  public void submit_returns_task_populated_from_CeTaskSubmit_and_creates_CeQueue_row() {
    CeTaskSubmit taskSubmit = createTaskSubmit(CeTaskTypes.REPORT, "PROJECT_1", "submitter uuid");

    CeTask task = underTest.submit(taskSubmit);

    verifyCeTask(taskSubmit, task, null);
    verifyCeQueueDtoForTaskSubmit(taskSubmit);
  }

  @Test
  public void submit_populates_component_name_and_key_of_CeTask_if_component_exists() {
    ComponentDto componentDto = insertComponent(ComponentTesting.newPrivateProjectDto(db.organizations().insert(), "PROJECT_1"));
    CeTaskSubmit taskSubmit = createTaskSubmit(CeTaskTypes.REPORT, componentDto.uuid(), null);

    CeTask task = underTest.submit(taskSubmit);

    verifyCeTask(taskSubmit, task, componentDto);
  }

  @Test
  public void submit_returns_task_without_component_info_when_submit_has_none() {
    CeTaskSubmit taskSubmit = createTaskSubmit("not cpt related");

    CeTask task = underTest.submit(taskSubmit);

    verifyCeTask(taskSubmit, task, null);
  }

  @Test
  public void submit_with_UNIQUE_QUEUE_PER_COMPONENT_creates_task_without_component_when_there_is_a_pending_task_without_component() {
    CeTaskSubmit taskSubmit = createTaskSubmit("no_component");
    CeQueueDto dto = insertPendingInQueue(null);

    Optional<CeTask> task = underTest.submit(taskSubmit, UNIQUE_QUEUE_PER_COMPONENT);

    assertThat(task).isNotEmpty();
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(dto.getUuid(), task.get().getUuid());
  }

  @Test
  public void submit_with_UNIQUE_QUEUE_PER_COMPONENT_creates_task_when_there_is_a_pending_task_for_another_component() {
    String componentUuid = randomAlphabetic(5);
    String otherComponentUuid = randomAlphabetic(6);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", componentUuid, null);
    CeQueueDto dto = insertPendingInQueue(otherComponentUuid);

    Optional<CeTask> task = underTest.submit(taskSubmit, UNIQUE_QUEUE_PER_COMPONENT);

    assertThat(task).isNotEmpty();
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(dto.getUuid(), task.get().getUuid());
  }

  @Test
  public void submit_with_UNIQUE_QUEUE_PER_COMPONENT_does_not_create_task_when_there_is_one_pending_task_for_component() {
    String componentUuid = randomAlphabetic(5);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", componentUuid, null);
    CeQueueDto dto = insertPendingInQueue(componentUuid);

    Optional<CeTask> task = underTest.submit(taskSubmit, UNIQUE_QUEUE_PER_COMPONENT);

    assertThat(task).isEmpty();
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(dto.getUuid());
  }

  @Test
  public void submit_with_UNIQUE_QUEUE_PER_COMPONENT_does_not_create_task_when_there_is_many_pending_task_for_component() {
    String componentUuid = randomAlphabetic(5);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", componentUuid, null);
    String[] uuids = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(i -> insertPendingInQueue(componentUuid))
      .map(CeQueueDto::getUuid)
      .toArray(String[]::new);

    Optional<CeTask> task = underTest.submit(taskSubmit, UNIQUE_QUEUE_PER_COMPONENT);

    assertThat(task).isEmpty();
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(uuids);
  }

  @Test
  public void submit_without_UNIQUE_QUEUE_PER_COMPONENT_creates_task_when_there_is_one_pending_task_for_component() {
    String componentUuid = randomAlphabetic(5);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", componentUuid, null);
    CeQueueDto dto = insertPendingInQueue(componentUuid);

    CeTask task = underTest.submit(taskSubmit);

    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(dto.getUuid(), task.getUuid());
  }

  @Test
  public void submit_without_UNIQUE_QUEUE_PER_COMPONENT_creates_task_when_there_is_many_pending_task_for_component() {
    String componentUuid = randomAlphabetic(5);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", componentUuid, null);
    String[] uuids = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(i -> insertPendingInQueue(componentUuid))
      .map(CeQueueDto::getUuid)
      .toArray(String[]::new);

    CeTask task = underTest.submit(taskSubmit);

    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .hasSize(uuids.length + 1)
      .contains(uuids)
      .contains(task.getUuid());
  }

  @Test
  public void massSubmit_returns_tasks_for_each_CeTaskSubmit_populated_from_CeTaskSubmit_and_creates_CeQueue_row_for_each() {
    CeTaskSubmit taskSubmit1 = createTaskSubmit(CeTaskTypes.REPORT, "PROJECT_1", "submitter uuid");
    CeTaskSubmit taskSubmit2 = createTaskSubmit("some type");

    List<CeTask> tasks = underTest.massSubmit(asList(taskSubmit1, taskSubmit2));

    assertThat(tasks).hasSize(2);
    verifyCeTask(taskSubmit1, tasks.get(0), null);
    verifyCeTask(taskSubmit2, tasks.get(1), null);
    verifyCeQueueDtoForTaskSubmit(taskSubmit1);
    verifyCeQueueDtoForTaskSubmit(taskSubmit2);
  }

  @Test
  public void massSubmit_populates_component_name_and_key_of_CeTask_if_component_exists() {
    ComponentDto componentDto1 = insertComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization(), "PROJECT_1"));
    CeTaskSubmit taskSubmit1 = createTaskSubmit(CeTaskTypes.REPORT, componentDto1.uuid(), null);
    CeTaskSubmit taskSubmit2 = createTaskSubmit("something", "non existing component uuid", null);

    List<CeTask> tasks = underTest.massSubmit(asList(taskSubmit1, taskSubmit2));

    assertThat(tasks).hasSize(2);
    verifyCeTask(taskSubmit1, tasks.get(0), componentDto1);
    verifyCeTask(taskSubmit2, tasks.get(1), null);
  }

  @Test
  public void massSubmit_with_UNIQUE_QUEUE_PER_COMPONENT_creates_task_without_component_when_there_is_a_pending_task_without_component() {
    CeTaskSubmit taskSubmit = createTaskSubmit("no_component");
    CeQueueDto dto = insertPendingInQueue(null);

    List<CeTask> tasks = underTest.massSubmit(of(taskSubmit), UNIQUE_QUEUE_PER_COMPONENT);

    assertThat(tasks).hasSize(1);
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(dto.getUuid(), tasks.iterator().next().getUuid());
  }

  @Test
  public void massSubmit_with_UNIQUE_QUEUE_PER_COMPONENT_creates_task_when_there_is_a_pending_task_for_another_component() {
    String componentUuid = randomAlphabetic(5);
    String otherComponentUuid = randomAlphabetic(6);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", componentUuid, null);
    CeQueueDto dto = insertPendingInQueue(otherComponentUuid);

    List<CeTask> tasks = underTest.massSubmit(of(taskSubmit), UNIQUE_QUEUE_PER_COMPONENT);

    assertThat(tasks).hasSize(1);
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(dto.getUuid(), tasks.iterator().next().getUuid());
  }

  @Test
  public void massSubmit_with_UNIQUE_QUEUE_PER_COMPONENT_does_not_create_task_when_there_is_one_pending_task_for_component() {
    String componentUuid = randomAlphabetic(5);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", componentUuid, null);
    CeQueueDto dto = insertPendingInQueue(componentUuid);

    List<CeTask> tasks = underTest.massSubmit(of(taskSubmit), UNIQUE_QUEUE_PER_COMPONENT);

    assertThat(tasks).isEmpty();
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(dto.getUuid());
  }

  @Test
  public void massSubmit_with_UNIQUE_QUEUE_PER_COMPONENT_does_not_create_task_when_there_is_many_pending_task_for_component() {
    String componentUuid = randomAlphabetic(5);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", componentUuid, null);
    String[] uuids = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(i -> insertPendingInQueue(componentUuid))
      .map(CeQueueDto::getUuid)
      .toArray(String[]::new);

    List<CeTask> tasks = underTest.massSubmit(of(taskSubmit), UNIQUE_QUEUE_PER_COMPONENT);

    assertThat(tasks).isEmpty();
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(uuids);
  }

  @Test
  public void massSubmit_without_UNIQUE_QUEUE_PER_COMPONENT_creates_task_when_there_is_one_pending_task_for_component() {
    String componentUuid = randomAlphabetic(5);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", componentUuid, null);
    CeQueueDto dto = insertPendingInQueue(componentUuid);

    List<CeTask> tasks = underTest.massSubmit(of(taskSubmit));

    assertThat(tasks).hasSize(1);
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(dto.getUuid(), tasks.iterator().next().getUuid());
  }

  @Test
  public void massSubmit_without_UNIQUE_QUEUE_PER_COMPONENT_creates_task_when_there_is_many_pending_task_for_component() {
    String componentUuid = randomAlphabetic(5);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", componentUuid, null);
    String[] uuids = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(i -> insertPendingInQueue(componentUuid))
      .map(CeQueueDto::getUuid)
      .toArray(String[]::new);

    List<CeTask> tasks = underTest.massSubmit(of(taskSubmit));

    assertThat(tasks).hasSize(1);
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .hasSize(uuids.length + 1)
      .contains(uuids)
      .contains(tasks.iterator().next().getUuid());
  }

  @Test
  public void massSubmit_with_UNIQUE_QUEUE_PER_COMPONENT_creates_tasks_depending_on_whether_there_is_pending_task_for_component() {
    String componentUuid1 = randomAlphabetic(5);
    String componentUuid2 = randomAlphabetic(6);
    String componentUuid3 = randomAlphabetic(7);
    String componentUuid4 = randomAlphabetic(8);
    String componentUuid5 = randomAlphabetic(9);
    CeTaskSubmit taskSubmit1 = createTaskSubmit("with_one_pending", componentUuid1, null);
    CeQueueDto dto1 = insertPendingInQueue(componentUuid1);
    CeTaskSubmit taskSubmit2 = createTaskSubmit("no_pending", componentUuid2, null);
    CeTaskSubmit taskSubmit3 = createTaskSubmit("with_many_pending", componentUuid3, null);
    String[] uuids3 = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(i -> insertPendingInQueue(componentUuid3))
      .map(CeQueueDto::getUuid)
      .toArray(String[]::new);
    CeTaskSubmit taskSubmit4 = createTaskSubmit("no_pending_2", componentUuid4, null);
    CeTaskSubmit taskSubmit5 = createTaskSubmit("with_pending_2", componentUuid5, null);
    CeQueueDto dto5 = insertPendingInQueue(componentUuid5);

    List<CeTask> tasks = underTest.massSubmit(of(taskSubmit1, taskSubmit2, taskSubmit3, taskSubmit4, taskSubmit5), UNIQUE_QUEUE_PER_COMPONENT);

    assertThat(tasks)
      .hasSize(2)
      .extracting(CeTask::getComponentUuid)
      .containsOnly(componentUuid2, componentUuid4);
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .hasSize(1 + uuids3.length + 1 + tasks.size())
      .contains(dto1.getUuid())
      .contains(uuids3)
      .contains(dto5.getUuid())
      .containsAll(tasks.stream().map(CeTask::getUuid).collect(Collectors.toList()));
  }

  @Test
  public void cancel_pending() {
    CeTask task = submit(CeTaskTypes.REPORT, "PROJECT_1");
    CeQueueDto queueDto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), task.getUuid()).get();

    underTest.cancel(db.getSession(), queueDto);

    Optional<CeActivityDto> activity = db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), task.getUuid());
    assertThat(activity.isPresent()).isTrue();
    assertThat(activity.get().getStatus()).isEqualTo(CeActivityDto.Status.CANCELED);
  }

  @Test
  public void fail_to_cancel_if_in_progress() {
    submit(CeTaskTypes.REPORT, "PROJECT_1");
    CeQueueDto ceQueueDto = db.getDbClient().ceQueueDao().peek(session, WORKER_UUID).get();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(startsWith("Task is in progress and can't be canceled"));

    underTest.cancel(db.getSession(), ceQueueDto);
  }

  @Test
  public void cancelAll_pendings_but_not_in_progress() {
    CeTask inProgressTask = submit(CeTaskTypes.REPORT, "PROJECT_1");
    CeTask pendingTask1 = submit(CeTaskTypes.REPORT, "PROJECT_2");
    CeTask pendingTask2 = submit(CeTaskTypes.REPORT, "PROJECT_3");

    db.getDbClient().ceQueueDao().peek(session, WORKER_UUID);

    int canceledCount = underTest.cancelAll();
    assertThat(canceledCount).isEqualTo(2);

    Optional<CeActivityDto> history = db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), pendingTask1.getUuid());
    assertThat(history.get().getStatus()).isEqualTo(CeActivityDto.Status.CANCELED);
    history = db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), pendingTask2.getUuid());
    assertThat(history.get().getStatus()).isEqualTo(CeActivityDto.Status.CANCELED);
    history = db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), inProgressTask.getUuid());
    assertThat(history.isPresent()).isFalse();
  }

  @Test
  public void pauseWorkers_marks_workers_as_paused_if_zero_tasks_in_progress() {
    submit(CeTaskTypes.REPORT, "PROJECT_1");
    // task is pending

    assertThat(underTest.getWorkersPauseStatus()).isEqualTo(CeQueue.WorkersPauseStatus.RESUMED);

    underTest.pauseWorkers();
    assertThat(underTest.getWorkersPauseStatus()).isEqualTo(CeQueue.WorkersPauseStatus.PAUSED);
  }

  @Test
  public void pauseWorkers_marks_workers_as_pausing_if_some_tasks_in_progress() {
    submit(CeTaskTypes.REPORT, "PROJECT_1");
    db.getDbClient().ceQueueDao().peek(session, WORKER_UUID);
    // task is in-progress

    assertThat(underTest.getWorkersPauseStatus()).isEqualTo(CeQueue.WorkersPauseStatus.RESUMED);

    underTest.pauseWorkers();
    assertThat(underTest.getWorkersPauseStatus()).isEqualTo(CeQueue.WorkersPauseStatus.PAUSING);
  }

  @Test
  public void resumeWorkers_does_nothing_if_not_paused() {
    assertThat(underTest.getWorkersPauseStatus()).isEqualTo(CeQueue.WorkersPauseStatus.RESUMED);

    underTest.resumeWorkers();

    assertThat(underTest.getWorkersPauseStatus()).isEqualTo(CeQueue.WorkersPauseStatus.RESUMED);
  }

  @Test
  public void resumeWorkers_resumes_pausing_workers() {
    submit(CeTaskTypes.REPORT, "PROJECT_1");
    db.getDbClient().ceQueueDao().peek(session, WORKER_UUID);
    // task is in-progress

    underTest.pauseWorkers();
    assertThat(underTest.getWorkersPauseStatus()).isEqualTo(CeQueue.WorkersPauseStatus.PAUSING);

    underTest.resumeWorkers();
    assertThat(underTest.getWorkersPauseStatus()).isEqualTo(CeQueue.WorkersPauseStatus.RESUMED);
  }

  @Test
  public void resumeWorkers_resumes_paused_workers() {
    underTest.pauseWorkers();
    assertThat(underTest.getWorkersPauseStatus()).isEqualTo(CeQueue.WorkersPauseStatus.PAUSED);

    underTest.resumeWorkers();
    assertThat(underTest.getWorkersPauseStatus()).isEqualTo(CeQueue.WorkersPauseStatus.RESUMED);
  }

  private void verifyCeTask(CeTaskSubmit taskSubmit, CeTask task, @Nullable ComponentDto componentDto) {
    if (componentDto == null) {
      assertThat(task.getOrganizationUuid()).isEqualTo(defaultOrganizationProvider.get().getUuid());
    } else {
      assertThat(task.getOrganizationUuid()).isEqualTo(componentDto.getOrganizationUuid());
    }
    assertThat(task.getUuid()).isEqualTo(taskSubmit.getUuid());
    assertThat(task.getComponentUuid()).isEqualTo(task.getComponentUuid());
    assertThat(task.getType()).isEqualTo(taskSubmit.getType());
    if (componentDto == null) {
      assertThat(task.getComponentKey()).isNull();
      assertThat(task.getComponentName()).isNull();
    } else {
      assertThat(task.getComponentKey()).isEqualTo(componentDto.getDbKey());
      assertThat(task.getComponentName()).isEqualTo(componentDto.name());
    }
    assertThat(task.getSubmitterUuid()).isEqualTo(taskSubmit.getSubmitterUuid());
  }

  private void verifyCeQueueDtoForTaskSubmit(CeTaskSubmit taskSubmit) {
    Optional<CeQueueDto> queueDto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), taskSubmit.getUuid());
    assertThat(queueDto.isPresent()).isTrue();
    assertThat(queueDto.get().getTaskType()).isEqualTo(taskSubmit.getType());
    assertThat(queueDto.get().getComponentUuid()).isEqualTo(taskSubmit.getComponentUuid());
    assertThat(queueDto.get().getSubmitterUuid()).isEqualTo(taskSubmit.getSubmitterUuid());
    assertThat(queueDto.get().getCreatedAt()).isEqualTo(1_450_000_000_000L);
  }

  private CeTask submit(String reportType, String componentUuid) {
    return underTest.submit(createTaskSubmit(reportType, componentUuid, null));
  }

  private CeTaskSubmit createTaskSubmit(String type) {
    return createTaskSubmit(type, null, null);
  }

  private CeTaskSubmit createTaskSubmit(String type, @Nullable String componentUuid, @Nullable String submitterUuid) {
    return underTest.prepareSubmit()
      .setType(type)
      .setComponentUuid(componentUuid)
      .setSubmitterUuid(submitterUuid)
      .setCharacteristics(emptyMap())
      .build();
  }

  private ComponentDto insertComponent(ComponentDto componentDto) {
    db.getDbClient().componentDao().insert(session, componentDto);
    session.commit();
    return componentDto;
  }

  private CeQueueDto insertPendingInQueue(@Nullable String componentUuid) {
    CeQueueDto dto = new CeQueueDto()
      .setUuid(UuidFactoryFast.getInstance().create())
      .setTaskType("some type")
      .setComponentUuid(componentUuid)
      .setStatus(CeQueueDto.Status.PENDING);
    db.getDbClient().ceQueueDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }
}
