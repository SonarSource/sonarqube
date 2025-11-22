/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.ce.queue.CeTaskSubmit.Component;
import org.sonar.ce.task.CeTask;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.platform.NodeInformation;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.queue.CeQueue.SubmitOption.UNIQUE_QUEUE_PER_ENTITY;
import static org.sonar.ce.queue.CeQueue.SubmitOption.UNIQUE_QUEUE_PER_TASK_TYPE;

public class CeQueueImplIT {

  private static final String WORKER_UUID = "workerUuid";
  private static final long NOW = 1_450_000_000_000L;
  private static final String NODE_NAME = "nodeName1";

  private System2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public DbTester db = DbTester.create(system2);

  private DbSession session = db.getSession();

  private UuidFactory uuidFactory = new SequenceUuidFactory();

  private NodeInformation nodeInformation = mock(NodeInformation.class);

  private CeQueue underTest = new CeQueueImpl(system2, db.getDbClient(), uuidFactory, nodeInformation);

  @Test
  public void submit_returns_task_populated_from_CeTaskSubmit_and_creates_CeQueue_row() {
    String componentUuid = secure().nextAlphabetic(3);
    String mainComponentUuid = secure().nextAlphabetic(4);
    CeTaskSubmit taskSubmit = createTaskSubmit(CeTaskTypes.REPORT, new Component(componentUuid, mainComponentUuid), "submitter uuid");
    UserDto userDto = db.getDbClient().userDao().selectByUuid(db.getSession(), taskSubmit.getSubmitterUuid());

    CeTask task = underTest.submit(taskSubmit);

    verifyCeTask(taskSubmit, task, null, null, userDto);
    verifyCeQueueDtoForTaskSubmit(taskSubmit);
  }

  @Test
  public void submit_populates_component_name_and_key_of_CeTask_if_component_exists() {
    ProjectData projectData = db.components().insertPrivateProject("PROJECT_1");
    CeTaskSubmit taskSubmit = createTaskSubmit(CeTaskTypes.REPORT, Component.fromDto(projectData.getMainBranchDto()), null);

    CeTask task = underTest.submit(taskSubmit);

    verifyCeTask(taskSubmit, task, projectData.getProjectDto(), projectData.getMainBranchDto(), null);
  }

  @Test
  public void submit_returns_task_without_component_info_when_submit_has_none() {
    CeTaskSubmit taskSubmit = createTaskSubmit("not cpt related");

    CeTask task = underTest.submit(taskSubmit);

    verifyCeTask(taskSubmit, task, null, null, null);
  }

  @Test
  public void submit_populates_submitter_login_of_CeTask_if_submitter_exists() {
    UserDto userDto = insertUser(UserTesting.newUserDto());
    CeTaskSubmit taskSubmit = createTaskSubmit(CeTaskTypes.REPORT, null, userDto.getUuid());

    CeTask task = underTest.submit(taskSubmit);

    verifyCeTask(taskSubmit, task, null, null, userDto);
  }

  @Test
  public void submit_with_UNIQUE_QUEUE_PER_MAIN_COMPONENT_creates_task_without_component_when_there_is_a_pending_task_without_component() {
    CeTaskSubmit taskSubmit = createTaskSubmit("no_component");
    CeQueueDto dto = insertPendingInQueue(null);

    Optional<CeTask> task = underTest.submit(taskSubmit, UNIQUE_QUEUE_PER_ENTITY);

    assertThat(task).isNotEmpty();
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(dto.getUuid(), task.get().getUuid());
  }

  @Test
  public void submit_with_UNIQUE_QUEUE_PER_MAIN_COMPONENT_creates_task_when_there_is_a_pending_task_for_another_main_component() {
    String mainComponentUuid = secure().nextAlphabetic(5);
    String otherMainComponentUuid = secure().nextAlphabetic(6);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", newComponent(mainComponentUuid), null);
    CeQueueDto dto = insertPendingInQueue(newComponent(otherMainComponentUuid));

    Optional<CeTask> task = underTest.submit(taskSubmit, UNIQUE_QUEUE_PER_ENTITY);

    assertThat(task).isNotEmpty();
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(dto.getUuid(), task.get().getUuid());
  }

  @Test
  public void submit_with_UNIQUE_QUEUE_PER_MAIN_COMPONENT_does_not_create_task_when_there_is_one_pending_task_for_same_main_component() {
    String mainComponentUuid = secure().nextAlphabetic(5);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", newComponent(mainComponentUuid), null);
    CeQueueDto dto = insertPendingInQueue(newComponent(mainComponentUuid));

    Optional<CeTask> task = underTest.submit(taskSubmit, UNIQUE_QUEUE_PER_ENTITY);

    assertThat(task).isEmpty();
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(dto.getUuid());
  }

  @Test
  public void submit_with_UNIQUE_QUEUE_PER_MAIN_COMPONENT_does_not_create_task_when_there_is_many_pending_task_for_same_main_component() {
    String mainComponentUuid = secure().nextAlphabetic(5);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", newComponent(mainComponentUuid), null);
    String[] uuids = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(i -> insertPendingInQueue(newComponent(mainComponentUuid)))
      .map(CeQueueDto::getUuid)
      .toArray(String[]::new);

    Optional<CeTask> task = underTest.submit(taskSubmit, UNIQUE_QUEUE_PER_ENTITY);

    assertThat(task).isEmpty();
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(uuids);
  }

  @Test
  public void submit_without_UNIQUE_QUEUE_PER_MAIN_COMPONENT_creates_task_when_there_is_one_pending_task_for_same_main_component() {
    String mainComponentUuid = secure().nextAlphabetic(5);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", newComponent(mainComponentUuid), null);
    CeQueueDto dto = insertPendingInQueue(newComponent(mainComponentUuid));

    CeTask task = underTest.submit(taskSubmit);

    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(dto.getUuid(), task.getUuid());
  }

  @Test
  public void submit_without_UNIQUE_QUEUE_PER_MAIN_COMPONENT_creates_task_when_there_is_many_pending_task_for_same_main_component() {
    String mainComponentUuid = secure().nextAlphabetic(5);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", newComponent(mainComponentUuid), null);
    String[] uuids = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(i -> insertPendingInQueue(newComponent(mainComponentUuid)))
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
  public void submit_with_UNIQUE_QUEUE_PER_TASK_TYPE_does_not_create_task_when_there_is_a_task_with_the_same_type() {
    String mainComponentUuid = secure().nextAlphabetic(5);
    CeTaskSubmit taskSubmit = createTaskSubmit("some type", newComponent(mainComponentUuid), null);
    String[] uuids = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(i -> insertPendingInQueue(newComponent(mainComponentUuid)))
      .map(CeQueueDto::getUuid)
      .toArray(String[]::new);
    Optional<CeTask> task = underTest.submit(taskSubmit, UNIQUE_QUEUE_PER_TASK_TYPE);

    assertThat(task).isEmpty();
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(uuids);
  }

  @Test
  public void massSubmit_returns_tasks_for_each_CeTaskSubmit_populated_from_CeTaskSubmit_and_creates_CeQueue_row_for_each() {
    String mainComponentUuid = secure().nextAlphabetic(10);
    CeTaskSubmit taskSubmit1 = createTaskSubmit(CeTaskTypes.REPORT, newComponent(mainComponentUuid), "submitter uuid");
    CeTaskSubmit taskSubmit2 = createTaskSubmit("some type");
    UserDto userDto1 = db.getDbClient().userDao().selectByUuid(db.getSession(), taskSubmit1.getSubmitterUuid());

    List<CeTask> tasks = underTest.massSubmit(asList(taskSubmit1, taskSubmit2));

    assertThat(tasks).hasSize(2);
    verifyCeTask(taskSubmit1, tasks.get(0), null, null, userDto1);
    verifyCeTask(taskSubmit2, tasks.get(1), null, null, null);
    verifyCeQueueDtoForTaskSubmit(taskSubmit1);
    verifyCeQueueDtoForTaskSubmit(taskSubmit2);
  }

  @Test
  public void massSubmit_populates_component_name_and_key_of_CeTask_if_project_exists() {
    ProjectData projectData = db.components().insertPrivateProject("PROJECT_1");
    CeTaskSubmit taskSubmit1 = createTaskSubmit(CeTaskTypes.REPORT, Component.fromDto(projectData.getMainBranchDto()), null);
    CeTaskSubmit taskSubmit2 = createTaskSubmit("something", newComponent(secure().nextAlphabetic(12)), null);

    List<CeTask> tasks = underTest.massSubmit(asList(taskSubmit1, taskSubmit2));

    assertThat(tasks).hasSize(2);
    verifyCeTask(taskSubmit1, tasks.get(0), projectData.getProjectDto(), projectData.getMainBranchDto(), null);
    verifyCeTask(taskSubmit2, tasks.get(1), null, null, null);
  }

  @Test
  public void massSubmit_populates_component_name_and_key_of_CeTask_if_project_and_branch_exists() {
    ProjectDto project = db.components().insertPublicProject(p -> p.setUuid("PROJECT_1").setBranchUuid("PROJECT_1")).getProjectDto();
    BranchDto branch1 = db.components().insertProjectBranch(project);
    BranchDto branch2 = db.components().insertProjectBranch(project);
    CeTaskSubmit taskSubmit1 = createTaskSubmit(CeTaskTypes.REPORT, Component.fromDto(branch1.getUuid(), project.getUuid()), null);
    CeTaskSubmit taskSubmit2 = createTaskSubmit("something", Component.fromDto(branch2.getUuid(), project.getUuid()), null);

    List<CeTask> tasks = underTest.massSubmit(asList(taskSubmit1, taskSubmit2));

    assertThat(tasks).hasSize(2);
    verifyCeTask(taskSubmit1, tasks.get(0), project, branch1, null);
    verifyCeTask(taskSubmit2, tasks.get(1), project, branch2, null);
  }

  @Test
  public void massSubmit_with_UNIQUE_QUEUE_PER_MAIN_COMPONENT_creates_task_without_component_when_there_is_a_pending_task_without_component() {
    CeTaskSubmit taskSubmit = createTaskSubmit("no_component");
    CeQueueDto dto = insertPendingInQueue(null);

    List<CeTask> tasks = underTest.massSubmit(of(taskSubmit), UNIQUE_QUEUE_PER_ENTITY);

    assertThat(tasks).hasSize(1);
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(dto.getUuid(), tasks.iterator().next().getUuid());
  }

  @Test
  public void massSubmit_with_UNIQUE_QUEUE_PER_MAIN_COMPONENT_creates_task_when_there_is_a_pending_task_for_another_main_component() {
    String mainComponentUuid = secure().nextAlphabetic(5);
    String otherMainComponentUuid = secure().nextAlphabetic(6);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", newComponent(mainComponentUuid), null);
    CeQueueDto dto = insertPendingInQueue(newComponent(otherMainComponentUuid));

    List<CeTask> tasks = underTest.massSubmit(of(taskSubmit), UNIQUE_QUEUE_PER_ENTITY);

    assertThat(tasks).hasSize(1);
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(dto.getUuid(), tasks.iterator().next().getUuid());
  }

  @Test
  public void massSubmit_with_UNIQUE_QUEUE_PER_MAIN_COMPONENT_does_not_create_task_when_there_is_one_pending_task_for_same_main_component() {
    String mainComponentUuid = secure().nextAlphabetic(5);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", newComponent(mainComponentUuid), null);
    CeQueueDto dto = insertPendingInQueue(newComponent(mainComponentUuid));

    List<CeTask> tasks = underTest.massSubmit(of(taskSubmit), UNIQUE_QUEUE_PER_ENTITY);

    assertThat(tasks).isEmpty();
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(dto.getUuid());
  }

  @Test
  public void massSubmit_with_UNIQUE_QUEUE_PER_MAIN_COMPONENT_does_not_create_task_when_there_is_many_pending_task_for_same_main_component() {
    String mainComponentUuid = secure().nextAlphabetic(5);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", newComponent(mainComponentUuid), null);
    String[] uuids = IntStream.range(0, 7)
      .mapToObj(i -> insertPendingInQueue(newComponent(mainComponentUuid)))
      .map(CeQueueDto::getUuid)
      .toArray(String[]::new);

    List<CeTask> tasks = underTest.massSubmit(of(taskSubmit), UNIQUE_QUEUE_PER_ENTITY);

    assertThat(tasks).isEmpty();
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(uuids);
  }

  @Test
  public void massSubmit_without_UNIQUE_QUEUE_PER_MAIN_COMPONENT_creates_task_when_there_is_one_pending_task_for_other_main_component() {
    String mainComponentUuid = secure().nextAlphabetic(5);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", newComponent(mainComponentUuid), null);
    CeQueueDto dto = insertPendingInQueue(newComponent(mainComponentUuid));

    List<CeTask> tasks = underTest.massSubmit(of(taskSubmit));

    assertThat(tasks).hasSize(1);
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly(dto.getUuid(), tasks.iterator().next().getUuid());
  }

  @Test
  public void massSubmit_without_UNIQUE_QUEUE_PER_MAIN_COMPONENT_creates_task_when_there_is_many_pending_task_for_other_main_component() {
    String mainComponentUuid = secure().nextAlphabetic(5);
    CeTaskSubmit taskSubmit = createTaskSubmit("with_component", newComponent(mainComponentUuid), null);
    String[] uuids = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(i -> insertPendingInQueue(newComponent(mainComponentUuid)))
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
  public void massSubmit_with_UNIQUE_QUEUE_PER_MAIN_COMPONENT_creates_tasks_depending_on_whether_there_is_pending_task_for_same_main_component() {
    String mainComponentUuid1 = secure().nextAlphabetic(5);
    String mainComponentUuid2 = secure().nextAlphabetic(6);
    String mainComponentUuid3 = secure().nextAlphabetic(7);
    String mainComponentUuid4 = secure().nextAlphabetic(8);
    String mainComponentUuid5 = secure().nextAlphabetic(9);
    CeTaskSubmit taskSubmit1 = createTaskSubmit("with_one_pending", newComponent(mainComponentUuid1), null);
    CeQueueDto dto1 = insertPendingInQueue(newComponent(mainComponentUuid1));
    Component componentForMainComponentUuid2 = newComponent(mainComponentUuid2);
    CeTaskSubmit taskSubmit2 = createTaskSubmit("no_pending", componentForMainComponentUuid2, null);
    CeTaskSubmit taskSubmit3 = createTaskSubmit("with_many_pending", newComponent(mainComponentUuid3), null);
    String[] uuids3 = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(i -> insertPendingInQueue(newComponent(mainComponentUuid3)))
      .map(CeQueueDto::getUuid)
      .toArray(String[]::new);
    Component componentForMainComponentUuid4 = newComponent(mainComponentUuid4);
    CeTaskSubmit taskSubmit4 = createTaskSubmit("no_pending_2", componentForMainComponentUuid4, null);
    CeTaskSubmit taskSubmit5 = createTaskSubmit("with_pending_2", newComponent(mainComponentUuid5), null);
    CeQueueDto dto5 = insertPendingInQueue(newComponent(mainComponentUuid5));

    List<CeTask> tasks = underTest.massSubmit(of(taskSubmit1, taskSubmit2, taskSubmit3, taskSubmit4, taskSubmit5), UNIQUE_QUEUE_PER_ENTITY);

    assertThat(tasks)
      .hasSize(2)
      .extracting(task -> task.getComponent().get().getUuid(), task -> task.getEntity().get().getUuid())
      .containsOnly(tuple(componentForMainComponentUuid2.getUuid(), componentForMainComponentUuid2.getEntityUuid()),
        tuple(componentForMainComponentUuid4.getUuid(), componentForMainComponentUuid4.getEntityUuid()));
    assertThat(db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .hasSize(1 + uuids3.length + 1 + tasks.size())
      .contains(dto1.getUuid())
      .contains(uuids3)
      .contains(dto5.getUuid())
      .containsAll(tasks.stream().map(CeTask::getUuid).toList());
  }

  @Test
  public void cancel_pending() {
    CeTask task = submit(CeTaskTypes.REPORT, newComponent(secure().nextAlphabetic(12)));
    CeQueueDto queueDto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), task.getUuid()).get();

    underTest.cancel(db.getSession(), queueDto);

    Optional<CeActivityDto> activity = findCeActivityDtoInDb(task);
    assertThat(activity).isPresent();
    assertThat(activity.get().getStatus()).isEqualTo(CeActivityDto.Status.CANCELED);
  }

  @Test
  public void cancel_pending_whenNodeNameProvided_setItInCeActivity() {
    when(nodeInformation.getNodeName()).thenReturn(Optional.of(NODE_NAME));
    CeTask task = submit(CeTaskTypes.REPORT, newComponent(secure().nextAlphabetic(12)));
    CeQueueDto queueDto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), task.getUuid()).get();

    underTest.cancel(db.getSession(), queueDto);

    Optional<CeActivityDto> activity = findCeActivityDtoInDb(task);
    assertThat(activity).isPresent();
    assertThat(activity.get().getNodeName()).isEqualTo(NODE_NAME);
  }

  @Test
  public void cancel_pending_whenNodeNameNOtProvided_setNulInCeActivity() {
    when(nodeInformation.getNodeName()).thenReturn(Optional.empty());
    CeTask task = submit(CeTaskTypes.REPORT, newComponent(secure().nextAlphabetic(12)));
    CeQueueDto queueDto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), task.getUuid()).get();

    underTest.cancel(db.getSession(), queueDto);

    Optional<CeActivityDto> activity = findCeActivityDtoInDb(task);
    assertThat(activity).isPresent();
    assertThat(activity.get().getNodeName()).isNull();
  }

  @Test
  public void fail_to_cancel_if_in_progress() {
    CeTask task = submit(CeTaskTypes.REPORT, newComponent(secure().nextAlphabetic(11)));
    CeQueueDto ceQueueDto = db.getDbClient().ceQueueDao().tryToPeek(session, task.getUuid(), WORKER_UUID).get();

    assertThatThrownBy(() -> underTest.cancel(db.getSession(), ceQueueDto))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageStartingWith("Task is in progress and can't be canceled");
  }

  @Test
  public void cancelAll_pendings_but_not_in_progress() {
    CeTask inProgressTask = submit(CeTaskTypes.REPORT, newComponent(secure().nextAlphabetic(12)));
    CeTask pendingTask1 = submit(CeTaskTypes.REPORT, newComponent(secure().nextAlphabetic(12)));
    CeTask pendingTask2 = submit(CeTaskTypes.REPORT, newComponent(secure().nextAlphabetic(12)));

    db.getDbClient().ceQueueDao().tryToPeek(session, inProgressTask.getUuid(), WORKER_UUID);

    int canceledCount = underTest.cancelAll();
    assertThat(canceledCount).isEqualTo(2);

    Optional<CeActivityDto> ceActivityInProgress = findCeActivityDtoInDb(pendingTask1);
    assertThat(ceActivityInProgress.get().getStatus()).isEqualTo(CeActivityDto.Status.CANCELED);
    Optional<CeActivityDto> ceActivityPending1 = findCeActivityDtoInDb(pendingTask2);
    assertThat(ceActivityPending1.get().getStatus()).isEqualTo(CeActivityDto.Status.CANCELED);
    Optional<CeActivityDto> ceActivityPending2 = findCeActivityDtoInDb(inProgressTask);
    assertThat(ceActivityPending2).isNotPresent();
  }

  @Test
  public void pauseWorkers_marks_workers_as_paused_if_zero_tasks_in_progress() {
    submit(CeTaskTypes.REPORT, newComponent(secure().nextAlphabetic(12)));
    // task is pending

    assertThat(underTest.getWorkersPauseStatus()).isEqualTo(CeQueue.WorkersPauseStatus.RESUMED);

    underTest.pauseWorkers();
    assertThat(underTest.getWorkersPauseStatus()).isEqualTo(CeQueue.WorkersPauseStatus.PAUSED);
  }

  @Test
  public void pauseWorkers_marks_workers_as_pausing_if_some_tasks_in_progress() {
    CeTask task = submit(CeTaskTypes.REPORT, newComponent(secure().nextAlphabetic(12)));
    db.getDbClient().ceQueueDao().tryToPeek(session, task.getUuid(), WORKER_UUID);
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
    CeTask task = submit(CeTaskTypes.REPORT, newComponent(secure().nextAlphabetic(12)));
    db.getDbClient().ceQueueDao().tryToPeek(session, task.getUuid(), WORKER_UUID);
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

  @Test
  public void fail_in_progress_task() {
    CeTask task = submit(CeTaskTypes.REPORT, newComponent(secure().nextAlphabetic(12)));
    CeQueueDto queueDto = db.getDbClient().ceQueueDao().tryToPeek(db.getSession(), task.getUuid(), WORKER_UUID).get();

    underTest.fail(db.getSession(), queueDto, "TIMEOUT", "Failed on timeout");

    Optional<CeActivityDto> activity = findCeActivityDtoInDb(task);
    assertThat(activity).isPresent();
    assertThat(activity.get().getStatus()).isEqualTo(CeActivityDto.Status.FAILED);
    assertThat(activity.get().getErrorType()).isEqualTo("TIMEOUT");
    assertThat(activity.get().getErrorMessage()).isEqualTo("Failed on timeout");
    assertThat(activity.get().getExecutedAt()).isEqualTo(NOW);
    assertThat(activity.get().getWorkerUuid()).isEqualTo(WORKER_UUID);
    assertThat(activity.get().getNodeName()).isNull();
  }

  @Test
  public void fail_in_progress_task_whenNodeNameProvided_setsItInCeActivityDto() {
    when(nodeInformation.getNodeName()).thenReturn(Optional.of(NODE_NAME));
    CeTask task = submit(CeTaskTypes.REPORT, newComponent(secure().nextAlphabetic(12)));
    CeQueueDto queueDto = db.getDbClient().ceQueueDao().tryToPeek(db.getSession(), task.getUuid(), WORKER_UUID).get();

    underTest.fail(db.getSession(), queueDto, "TIMEOUT", "Failed on timeout");

    Optional<CeActivityDto> activity = findCeActivityDtoInDb(task);
    assertThat(activity).isPresent();
    assertThat(activity.get().getNodeName()).isEqualTo(NODE_NAME);
  }

  private Optional<CeActivityDto> findCeActivityDtoInDb(CeTask task) {
    return db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), task.getUuid());
  }

  @Test
  public void fail_throws_exception_if_task_is_pending() {
    CeTask task = submit(CeTaskTypes.REPORT, newComponent(secure().nextAlphabetic(12)));
    CeQueueDto queueDto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), task.getUuid()).get();

    Throwable thrown = catchThrowable(() -> underTest.fail(db.getSession(), queueDto, "TIMEOUT", "Failed on timeout"));

    assertThat(thrown)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Task is not in-progress and can't be marked as failed [uuid=" + task.getUuid() + "]");
  }

  private void verifyCeTask(CeTaskSubmit taskSubmit, CeTask task, @Nullable EntityDto entityDto, @Nullable BranchDto branch, @Nullable UserDto userDto) {
    assertThat(task.getUuid()).isEqualTo(taskSubmit.getUuid());
    if (branch != null) {
      CeTask.Component component = task.getComponent().get();
      assertThat(component.getUuid()).isEqualTo(branch.getUuid());
      assertThat(component.getKey()).contains(entityDto.getKey());
      assertThat(component.getName()).contains(entityDto.getName());
    } else if (taskSubmit.getComponent().isPresent()) {
      assertThat(task.getComponent()).contains(new CeTask.Component(taskSubmit.getComponent().get().getUuid(), null, null));
    } else {
      assertThat(task.getComponent()).isEmpty();
    }
    if (entityDto != null) {
      CeTask.Component component = task.getEntity().get();
      assertThat(component.getUuid()).isEqualTo(entityDto.getUuid());
      assertThat(component.getKey()).contains(entityDto.getKey());
      assertThat(component.getName()).contains(entityDto.getName());
    } else if (taskSubmit.getComponent().isPresent()) {
      assertThat(task.getEntity()).contains(new CeTask.Component(taskSubmit.getComponent().get().getEntityUuid(), null, null));
    } else {
      assertThat(task.getEntity()).isEmpty();
    }
    assertThat(task.getType()).isEqualTo(taskSubmit.getType());
    if (taskSubmit.getSubmitterUuid() != null) {
      if (userDto == null) {
        assertThat(task.getSubmitter().uuid()).isEqualTo(taskSubmit.getSubmitterUuid());
        assertThat(task.getSubmitter().login()).isNull();
      } else {
        assertThat(task.getSubmitter().uuid()).isEqualTo(userDto.getUuid()).isEqualTo(taskSubmit.getSubmitterUuid());
        assertThat(task.getSubmitter().login()).isEqualTo(userDto.getLogin());
      }
    }
  }

  private void verifyCeQueueDtoForTaskSubmit(CeTaskSubmit taskSubmit) {
    Optional<CeQueueDto> queueDto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), taskSubmit.getUuid());
    assertThat(queueDto).isPresent();
    assertThat(queueDto.get().getTaskType()).isEqualTo(taskSubmit.getType());
    Optional<Component> component = taskSubmit.getComponent();
    if (component.isPresent()) {
      assertThat(queueDto.get().getComponentUuid()).isEqualTo(component.get().getUuid());
      assertThat(queueDto.get().getEntityUuid()).isEqualTo(component.get().getEntityUuid());
    } else {
      assertThat(queueDto.get().getComponentUuid()).isNull();
      assertThat(queueDto.get().getComponentUuid()).isNull();
    }
    assertThat(queueDto.get().getSubmitterUuid()).isEqualTo(taskSubmit.getSubmitterUuid());
    assertThat(queueDto.get().getCreatedAt()).isEqualTo(1_450_000_000_000L);
  }

  private CeTask submit(String reportType, Component component) {
    return underTest.submit(createTaskSubmit(reportType, component, null));
  }

  private CeTaskSubmit createTaskSubmit(String type) {
    return createTaskSubmit(type, null, null);
  }

  private CeTaskSubmit createTaskSubmit(String type, @Nullable Component component, @Nullable String submitterUuid) {
    return underTest.prepareSubmit()
      .setType(type)
      .setComponent(component)
      .setSubmitterUuid(submitterUuid)
      .setCharacteristics(emptyMap())
      .build();
  }

  private UserDto insertUser(UserDto userDto) {
    db.getDbClient().userDao().insert(session, userDto);
    session.commit();
    return userDto;
  }

  private CeQueueDto insertPendingInQueue(@Nullable Component component) {
    CeQueueDto dto = new CeQueueDto()
      .setUuid(UuidFactoryFast.getInstance().create())
      .setTaskType("some type")
      .setStatus(CeQueueDto.Status.PENDING);
    if (component != null) {
      dto.setComponentUuid(component.getUuid())
        .setEntityUuid(component.getEntityUuid());
    }
    db.getDbClient().ceQueueDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  private static int newComponentIdGenerator = new Random().nextInt(8_999_333);

  private static Component newComponent(String mainComponentUuid) {
    return new Component("uuid_" + newComponentIdGenerator++, mainComponentUuid);
  }
}
