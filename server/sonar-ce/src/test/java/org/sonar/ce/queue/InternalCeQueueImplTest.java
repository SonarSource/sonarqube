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

import com.google.common.collect.ImmutableSet;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.utils.System2;
import org.sonar.ce.container.ComputeEngineStatus;
import org.sonar.ce.monitoring.CEQueueStatus;
import org.sonar.ce.monitoring.CEQueueStatusImpl;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.CeTaskResult;
import org.sonar.ce.task.TypedException;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeQueueTesting;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.organization.DefaultOrganization;
import org.sonar.server.organization.DefaultOrganizationProvider;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.ce.container.ComputeEngineStatus.Status.STARTED;
import static org.sonar.ce.container.ComputeEngineStatus.Status.STOPPING;

public class InternalCeQueueImplTest {

  private static final String AN_ANALYSIS_UUID = "U1";
  private static final String WORKER_UUID_1 = "worker uuid 1";
  private static final String WORKER_UUID_2 = "worker uuid 2";

  private System2 system2 = new AlwaysIncreasingSystem2();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(system2);

  private DbSession session = db.getSession();

  private UuidFactory uuidFactory = UuidFactoryImpl.INSTANCE;
  private CEQueueStatus queueStatus = new CEQueueStatusImpl(db.getDbClient(), mock(System2.class));
  private DefaultOrganizationProvider defaultOrganizationProvider = mock(DefaultOrganizationProvider.class);
  private ComputeEngineStatus computeEngineStatus = mock(ComputeEngineStatus.class);
  private InternalCeQueue underTest = new InternalCeQueueImpl(system2, db.getDbClient(), uuidFactory, queueStatus, defaultOrganizationProvider, computeEngineStatus);

  @Before
  public void setUp() {
    OrganizationDto defaultOrganization = db.getDefaultOrganization();
    when(defaultOrganizationProvider.get()).thenReturn(DefaultOrganization.newBuilder()
      .setUuid(defaultOrganization.getUuid())
      .setKey(defaultOrganization.getKey())
      .setName(defaultOrganization.getName())
      .setCreatedAt(defaultOrganization.getCreatedAt())
      .setUpdatedAt(defaultOrganization.getUpdatedAt())
      .build());
    when(computeEngineStatus.getStatus()).thenReturn(STARTED);
  }

  @Test
  public void submit_returns_task_populated_from_CeTaskSubmit_and_creates_CeQueue_row() {
    CeTaskSubmit taskSubmit = createTaskSubmit(CeTaskTypes.REPORT, newProjectDto("PROJECT_1"), "rob");
    CeTask task = underTest.submit(taskSubmit);
    UserDto userDto = db.getDbClient().userDao().selectByUuid(db.getSession(), taskSubmit.getSubmitterUuid());
    verifyCeTask(taskSubmit, task, null, userDto);
    verifyCeQueueDtoForTaskSubmit(taskSubmit);
  }

  @Test
  public void submit_populates_component_name_and_key_of_CeTask_if_component_exists() {
    ComponentDto componentDto = insertComponent(newProjectDto("PROJECT_1"));
    CeTaskSubmit taskSubmit = createTaskSubmit(CeTaskTypes.REPORT, componentDto, null);

    CeTask task = underTest.submit(taskSubmit);

    verifyCeTask(taskSubmit, task, componentDto, null);
  }

  @Test
  public void submit_returns_task_without_component_info_when_submit_has_none() {
    CeTaskSubmit taskSubmit = createTaskSubmit("not cpt related");

    CeTask task = underTest.submit(taskSubmit);

    verifyCeTask(taskSubmit, task, null, null);
  }

  @Test
  public void massSubmit_returns_tasks_for_each_CeTaskSubmit_populated_from_CeTaskSubmit_and_creates_CeQueue_row_for_each() {
    CeTaskSubmit taskSubmit1 = createTaskSubmit(CeTaskTypes.REPORT, newProjectDto("PROJECT_1"), "rob");
    CeTaskSubmit taskSubmit2 = createTaskSubmit("some type");

    List<CeTask> tasks = underTest.massSubmit(asList(taskSubmit1, taskSubmit2));

    UserDto userDto1 = db.getDbClient().userDao().selectByUuid(db.getSession(), taskSubmit1.getSubmitterUuid());
    assertThat(tasks).hasSize(2);
    verifyCeTask(taskSubmit1, tasks.get(0), null, userDto1);
    verifyCeTask(taskSubmit2, tasks.get(1), null, null);
    verifyCeQueueDtoForTaskSubmit(taskSubmit1);
    verifyCeQueueDtoForTaskSubmit(taskSubmit2);
  }

  @Test
  public void massSubmit_populates_component_name_and_key_of_CeTask_if_component_exists() {
    ComponentDto componentDto1 = insertComponent(newProjectDto("PROJECT_1"));
    CeTaskSubmit taskSubmit1 = createTaskSubmit(CeTaskTypes.REPORT, componentDto1, null);
    CeTaskSubmit taskSubmit2 = createTaskSubmit("something", newProjectDto("non existing component uuid"), null);

    List<CeTask> tasks = underTest.massSubmit(asList(taskSubmit1, taskSubmit2));

    assertThat(tasks).hasSize(2);
    verifyCeTask(taskSubmit1, tasks.get(0), componentDto1, null);
    verifyCeTask(taskSubmit2, tasks.get(1), null, null);
  }

  @Test
  public void peek_throws_NPE_if_workerUUid_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("workerUuid can't be null");

    underTest.peek(null);
  }

  @Test
  public void test_remove() {
    CeTask task = submit(CeTaskTypes.REPORT, newProjectDto("PROJECT_1"));
    Optional<CeTask> peek = underTest.peek(WORKER_UUID_1);
    underTest.remove(peek.get(), CeActivityDto.Status.SUCCESS, null, null);

    // queue is empty
    assertThat(db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), task.getUuid()).isPresent()).isFalse();
    assertThat(underTest.peek(WORKER_UUID_2).isPresent()).isFalse();

    // available in history
    Optional<CeActivityDto> history = db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), task.getUuid());
    assertThat(history.isPresent()).isTrue();
    assertThat(history.get().getStatus()).isEqualTo(CeActivityDto.Status.SUCCESS);
    assertThat(history.get().getIsLast()).isTrue();
    assertThat(history.get().getAnalysisUuid()).isNull();
  }

  @Test
  public void remove_throws_IAE_if_exception_is_provided_but_status_is_SUCCESS() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Error can be provided only when status is FAILED");

    underTest.remove(mock(CeTask.class), CeActivityDto.Status.SUCCESS, null, new RuntimeException("Some error"));
  }

  @Test
  public void remove_throws_IAE_if_exception_is_provided_but_status_is_CANCELED() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Error can be provided only when status is FAILED");

    underTest.remove(mock(CeTask.class), CeActivityDto.Status.CANCELED, null, new RuntimeException("Some error"));
  }

  @Test
  public void remove_does_not_set_analysisUuid_in_CeActivity_when_CeTaskResult_has_no_analysis_uuid() {
    CeTask task = submit(CeTaskTypes.REPORT, newProjectDto("PROJECT_1"));
    Optional<CeTask> peek = underTest.peek(WORKER_UUID_1);
    underTest.remove(peek.get(), CeActivityDto.Status.SUCCESS, newTaskResult(null), null);

    // available in history
    Optional<CeActivityDto> history = db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), task.getUuid());
    assertThat(history.isPresent()).isTrue();
    assertThat(history.get().getAnalysisUuid()).isNull();
  }

  @Test
  public void remove_sets_analysisUuid_in_CeActivity_when_CeTaskResult_has_analysis_uuid() {
    CeTask task = submit(CeTaskTypes.REPORT, newProjectDto("PROJECT_1"));

    Optional<CeTask> peek = underTest.peek(WORKER_UUID_2);
    underTest.remove(peek.get(), CeActivityDto.Status.SUCCESS, newTaskResult(AN_ANALYSIS_UUID), null);

    // available in history
    Optional<CeActivityDto> history = db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), task.getUuid());
    assertThat(history.isPresent()).isTrue();
    assertThat(history.get().getAnalysisUuid()).isEqualTo("U1");
  }

  @Test
  public void remove_saves_error_message_and_stacktrace_when_exception_is_provided() {
    Throwable error = new NullPointerException("Fake NPE to test persistence to DB");

    CeTask task = submit(CeTaskTypes.REPORT, newProjectDto("PROJECT_1"));
    Optional<CeTask> peek = underTest.peek(WORKER_UUID_1);
    underTest.remove(peek.get(), CeActivityDto.Status.FAILED, null, error);

    Optional<CeActivityDto> activityDto = db.getDbClient().ceActivityDao().selectByUuid(session, task.getUuid());
    assertThat(activityDto).isPresent();

    assertThat(activityDto.get().getErrorMessage()).isEqualTo(error.getMessage());
    assertThat(activityDto.get().getErrorStacktrace()).isEqualToIgnoringWhitespace(stacktraceToString(error));
    assertThat(activityDto.get().getErrorType()).isNull();
  }

  @Test
  public void remove_saves_error_when_TypedMessageException_is_provided() {
    Throwable error = new TypedExceptionImpl("aType", "aMessage");

    CeTask task = submit(CeTaskTypes.REPORT, newProjectDto("PROJECT_1"));
    Optional<CeTask> peek = underTest.peek(WORKER_UUID_1);
    underTest.remove(peek.get(), CeActivityDto.Status.FAILED, null, error);

    CeActivityDto activityDto = db.getDbClient().ceActivityDao().selectByUuid(session, task.getUuid()).get();
    assertThat(activityDto.getErrorType()).isEqualTo("aType");
    assertThat(activityDto.getErrorMessage()).isEqualTo("aMessage");
    assertThat(activityDto.getErrorStacktrace()).isEqualToIgnoringWhitespace(stacktraceToString(error));
  }

  @Test
  public void remove_updates_queueStatus_success_even_if_task_does_not_exist_in_DB() {
    CEQueueStatus queueStatus = mock(CEQueueStatus.class);

    CeTask task = submit(CeTaskTypes.REPORT, newProjectDto("PROJECT_1"));
    db.getDbClient().ceQueueDao().deleteByUuid(db.getSession(), task.getUuid());
    db.commit();

    InternalCeQueueImpl underTest = new InternalCeQueueImpl(system2, db.getDbClient(), null, queueStatus, null, null);

    try {
      underTest.remove(task, CeActivityDto.Status.SUCCESS, null, null);
      fail("remove should have thrown a IllegalStateException");
    } catch (IllegalStateException e) {
      verify(queueStatus).addSuccess(anyLong());
    }
  }

  @Test
  public void remove_updates_queueStatus_failure_even_if_task_does_not_exist_in_DB() {
    CEQueueStatus queueStatusMock = mock(CEQueueStatus.class);

    CeTask task = submit(CeTaskTypes.REPORT, newProjectDto("PROJECT_1"));
    db.getDbClient().ceQueueDao().deleteByUuid(db.getSession(), task.getUuid());
    db.commit();
    InternalCeQueueImpl underTest = new InternalCeQueueImpl(system2, db.getDbClient(), null, queueStatusMock, null, null);

    try {
      underTest.remove(task, CeActivityDto.Status.FAILED, null, null);
      fail("remove should have thrown a IllegalStateException");
    } catch (IllegalStateException e) {
      verify(queueStatusMock).addError(anyLong());
    }
  }

  @Test
  public void cancelWornOuts_does_not_update_queueStatus() {
    CEQueueStatus queueStatusMock = mock(CEQueueStatus.class);

    CeTask task = submit(CeTaskTypes.REPORT, newProjectDto("PROJECT_1"));
    db.executeUpdateSql("update ce_queue set status = 'PENDING', started_at = 123 where uuid = '" + task.getUuid() + "'");
    db.commit();
    InternalCeQueueImpl underTest = new InternalCeQueueImpl(system2, db.getDbClient(), null, queueStatusMock, null, null);

    underTest.cancelWornOuts();

    assertThat(db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), task.getUuid())).isPresent();
    verifyZeroInteractions(queueStatusMock);
  }

  private static class TypedExceptionImpl extends RuntimeException implements TypedException {
    private final String type;

    private TypedExceptionImpl(String type, String message) {
      super(message);
      this.type = type;
    }

    @Override
    public String getType() {
      return type;
    }
  }

  @Test
  public void remove_copies_workerUuid() {
    CeQueueDto ceQueueDto = db.getDbClient().ceQueueDao().insert(session, new CeQueueDto()
      .setUuid("uuid")
      .setTaskType("foo")
      .setStatus(CeQueueDto.Status.PENDING));
    makeInProgress(ceQueueDto, "Dustin");
    db.commit();

    underTest.remove(new CeTask.Builder()
      .setOrganizationUuid("foo")
      .setUuid("uuid")
      .setType("bar")
      .build(), CeActivityDto.Status.SUCCESS, null, null);

    CeActivityDto dto = db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), "uuid").get();
    assertThat(dto.getWorkerUuid()).isEqualTo("Dustin");
  }

  @Test
  public void fail_to_remove_if_not_in_queue() {
    CeTask task = submit(CeTaskTypes.REPORT, newProjectDto("PROJECT_1"));
    underTest.remove(task, CeActivityDto.Status.SUCCESS, null, null);

    expectedException.expect(IllegalStateException.class);

    underTest.remove(task, CeActivityDto.Status.SUCCESS, null, null);
  }

  @Test
  public void test_peek() {
    CeTask task = submit(CeTaskTypes.REPORT, newProjectDto("PROJECT_1"));

    Optional<CeTask> peek = underTest.peek(WORKER_UUID_1);
    assertThat(peek.isPresent()).isTrue();
    assertThat(peek.get().getUuid()).isEqualTo(task.getUuid());
    assertThat(peek.get().getType()).isEqualTo(CeTaskTypes.REPORT);
    assertThat(peek.get().getComponent()).contains(new CeTask.Component("PROJECT_1", null, null));
    assertThat(peek.get().getMainComponent()).contains(peek.get().getComponent().get());

    // no more pending tasks
    peek = underTest.peek(WORKER_UUID_2);
    assertThat(peek.isPresent()).isFalse();
  }

  @Test
  public void peek_populates_name_and_key_for_existing_component_and_main_component() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project);
    CeTask task = submit(CeTaskTypes.REPORT, branch);

    Optional<CeTask> peek = underTest.peek(WORKER_UUID_1);
    assertThat(peek.isPresent()).isTrue();
    assertThat(peek.get().getUuid()).isEqualTo(task.getUuid());
    assertThat(peek.get().getType()).isEqualTo(CeTaskTypes.REPORT);
    assertThat(peek.get().getComponent()).contains(new CeTask.Component(branch.uuid(), branch.getDbKey(), branch.name()));
    assertThat(peek.get().getMainComponent()).contains(new CeTask.Component(project.uuid(), project.getDbKey(), project.name()));

    // no more pending tasks
    peek = underTest.peek(WORKER_UUID_2);
    assertThat(peek.isPresent()).isFalse();
  }

  @Test
  public void peek_is_paused_then_resumed() {
    CeTask task = submit(CeTaskTypes.REPORT, newProjectDto("PROJECT_1"));
    underTest.pauseWorkers();

    Optional<CeTask> peek = underTest.peek(WORKER_UUID_1);
    assertThat(peek).isEmpty();

    underTest.resumeWorkers();
    peek = underTest.peek(WORKER_UUID_1);
    assertThat(peek).isPresent();
    assertThat(peek.get().getUuid()).isEqualTo(task.getUuid());
  }

  @Test
  public void peek_ignores_in_progress_tasks() {
    CeQueueDto dto = db.getDbClient().ceQueueDao().insert(session, new CeQueueDto()
      .setUuid("uuid")
      .setTaskType("foo")
      .setStatus(CeQueueDto.Status.PENDING));
    makeInProgress(dto, "foo");
    db.commit();

    assertThat(underTest.peek(WORKER_UUID_1)).isEmpty();
  }

  @Test
  public void peek_nothing_if_application_status_stopping() {
    submit(CeTaskTypes.REPORT, newProjectDto("PROJECT_1"));
    when(computeEngineStatus.getStatus()).thenReturn(STOPPING);

    Optional<CeTask> peek = underTest.peek(WORKER_UUID_1);
    assertThat(peek.isPresent()).isFalse();
  }

  @Test
  public void peek_peeks_pending_task() {
    db.getDbClient().ceQueueDao().insert(session, new CeQueueDto()
      .setUuid("uuid")
      .setTaskType("foo")
      .setStatus(CeQueueDto.Status.PENDING));
    db.commit();

    assertThat(underTest.peek(WORKER_UUID_1).get().getUuid()).isEqualTo("uuid");
  }

  @Test
  public void peek_resets_to_pending_any_task_in_progress_for_specified_worker_uuid_and_updates_updatedAt() {
    insertPending("u0"); // add a pending one that will be picked so that u1 isn't peek and status reset is visible in DB
    CeQueueDto u1 = insertPending("u1");// will be picked-because older than any of the reset ones
    CeQueueDto u2 = insertInProgress("u2", WORKER_UUID_1);// will be reset

    assertThat(underTest.peek(WORKER_UUID_1).get().getUuid()).isEqualTo("u0");

    verifyUnmodifiedTask(u1);
    verifyResetTask(u2);
  }

  @Test
  public void peek_resets_to_pending_any_task_in_progress_for_specified_worker_uuid_and_only_this_uuid() {
    insertPending("u0"); // add a pending one that will be picked so that u1 isn't peek and status reset is visible in DB
    CeQueueDto u1 = insertInProgress("u1", WORKER_UUID_1);
    CeQueueDto u2 = insertInProgress("u2", WORKER_UUID_2);
    CeQueueDto u3 = insertInProgress("u3", WORKER_UUID_1);
    CeQueueDto u4 = insertInProgress("u4", WORKER_UUID_2);

    assertThat(underTest.peek(WORKER_UUID_1).get().getUuid()).isEqualTo("u0");

    verifyResetTask(u1);
    verifyUnmodifiedTask(u2);
    verifyResetTask(u3);
    verifyUnmodifiedTask(u4);
  }

  private void verifyResetTask(CeQueueDto originalDto) {
    CeQueueDto dto = db.getDbClient().ceQueueDao().selectByUuid(session, originalDto.getUuid()).get();
    assertThat(dto.getStatus()).isEqualTo(CeQueueDto.Status.PENDING);
    assertThat(dto.getCreatedAt()).isEqualTo(originalDto.getCreatedAt());
    assertThat(dto.getUpdatedAt()).isGreaterThan(originalDto.getUpdatedAt());
  }

  private void verifyUnmodifiedTask(CeQueueDto originalDto) {
    CeQueueDto dto = db.getDbClient().ceQueueDao().selectByUuid(session, originalDto.getUuid()).get();
    assertThat(dto.getStatus()).isEqualTo(originalDto.getStatus());
    assertThat(dto.getCreatedAt()).isEqualTo(originalDto.getCreatedAt());
    assertThat(dto.getUpdatedAt()).isEqualTo(originalDto.getUpdatedAt());
  }

  private CeQueueDto insertInProgress(String uuid, String workerUuid) {
    CeQueueDto dto = new CeQueueDto()
      .setUuid(uuid)
      .setTaskType("foo")
      .setStatus(CeQueueDto.Status.PENDING);
    db.getDbClient().ceQueueDao().insert(session, dto);
    makeInProgress(dto, workerUuid);
    db.commit();
    return db.getDbClient().ceQueueDao().selectByUuid(session, uuid).get();
  }

  private CeQueueDto insertPending(String uuid) {
    CeQueueDto dto = new CeQueueDto()
      .setUuid(uuid)
      .setTaskType("foo")
      .setStatus(CeQueueDto.Status.PENDING);
    db.getDbClient().ceQueueDao().insert(session, dto);
    db.commit();
    return dto;
  }

  @Test
  public void cancel_pending() {
    CeTask task = submit(CeTaskTypes.REPORT, newProjectDto("PROJECT_1"));
    CeQueueDto queueDto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), task.getUuid()).get();

    underTest.cancel(db.getSession(), queueDto);

    Optional<CeActivityDto> activity = db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), task.getUuid());
    assertThat(activity.isPresent()).isTrue();
    assertThat(activity.get().getStatus()).isEqualTo(CeActivityDto.Status.CANCELED);
  }

  @Test
  public void fail_to_cancel_if_in_progress() {
    CeTask task = submit(CeTaskTypes.REPORT, newProjectDto("PROJECT_1"));
    underTest.peek(WORKER_UUID_2);
    CeQueueDto queueDto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), task.getUuid()).get();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Task is in progress and can't be canceled");

    underTest.cancel(db.getSession(), queueDto);
  }

  @Test
  public void cancelAll_pendings_but_not_in_progress() {
    CeTask inProgressTask = submit(CeTaskTypes.REPORT, newProjectDto("PROJECT_1"));
    CeTask pendingTask1 = submit(CeTaskTypes.REPORT, newProjectDto("PROJECT_2"));
    CeTask pendingTask2 = submit(CeTaskTypes.REPORT, newProjectDto("PROJECT_3"));
    underTest.peek(WORKER_UUID_2);

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
  public void resetTasksWithUnknownWorkerUUIDs_reset_only_in_progress_tasks() {
    CeQueueDto u1 = insertCeQueueDto("u1");
    CeQueueDto u2 = insertCeQueueDto("u2");
    CeQueueDto u6 = insertInProgress("u6", "worker1");
    CeQueueDto u7 = insertInProgress("u7", "worker2");
    CeQueueDto u8 = insertInProgress("u8", "worker3");

    underTest.resetTasksWithUnknownWorkerUUIDs(ImmutableSet.of("worker2", "worker3"));

    // Pending tasks must not be modified even if a workerUUID is not present
    verifyUnmodified(u1);
    verifyUnmodified(u2);

    // Unknown worker : null, "worker1"
    verifyReset(u6);

    // Known workers : "worker2", "worker3"
    verifyUnmodified(u7);
    verifyUnmodified(u8);
  }

  @Test
  public void resetTasksWithUnknownWorkerUUIDs_with_empty_set_will_reset_all_in_progress_tasks() {
    CeQueueDto u1 = insertCeQueueDto("u1");
    CeQueueDto u2 = insertCeQueueDto("u2");
    CeQueueDto u6 = insertInProgress("u6", "worker1");
    CeQueueDto u7 = insertInProgress("u7", "worker2");
    CeQueueDto u8 = insertInProgress("u8", "worker3");

    underTest.resetTasksWithUnknownWorkerUUIDs(ImmutableSet.of());

    // Pending tasks must not be modified even if a workerUUID is not present
    verifyUnmodified(u1);
    verifyUnmodified(u2);

    // Unknown worker : null, "worker1"
    verifyReset(u6);
    verifyReset(u7);
    verifyReset(u8);
  }

  @Test
  public void resetTasksWithUnknownWorkerUUIDs_with_worker_without_tasks_will_reset_all_in_progress_tasks() {
    CeQueueDto u1 = insertCeQueueDto("u1");
    CeQueueDto u2 = insertCeQueueDto("u2");
    CeQueueDto u6 = insertInProgress("u6", "worker1");
    CeQueueDto u7 = insertInProgress("u7", "worker2");
    CeQueueDto u8 = insertInProgress("u8", "worker3");

    underTest.resetTasksWithUnknownWorkerUUIDs(ImmutableSet.of("worker1000", "worker1001"));

    // Pending tasks must not be modified even if a workerUUID is not present
    verifyUnmodified(u1);
    verifyUnmodified(u2);

    // Unknown worker : null, "worker1"
    verifyReset(u6);
    verifyReset(u7);
    verifyReset(u8);
  }

  private void verifyReset(CeQueueDto original) {
    CeQueueDto dto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), original.getUuid()).get();
    // We do not touch CreatedAt
    assertThat(dto.getCreatedAt()).isEqualTo(original.getCreatedAt());

    // Status must have changed to PENDING and must not be equal to previous status
    assertThat(dto.getStatus()).isEqualTo(CeQueueDto.Status.PENDING).isNotEqualTo(original.getStatus());
    // UpdatedAt must have been updated
    assertThat(dto.getUpdatedAt()).isNotEqualTo(original.getUpdatedAt());
    assertThat(dto.getStartedAt()).isEqualTo(original.getStartedAt());
    // WorkerUuid must be null
    assertThat(dto.getWorkerUuid()).isNull();
  }

  private void verifyUnmodified(CeQueueDto original) {
    CeQueueDto dto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), original.getUuid()).get();
    assertThat(dto.getStatus()).isEqualTo(original.getStatus());
    assertThat(dto.getCreatedAt()).isEqualTo(original.getCreatedAt());
    assertThat(dto.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
  }

  private CeQueueDto insertCeQueueDto(String uuid) {
    CeQueueDto dto = new CeQueueDto()
      .setUuid(uuid)
      .setTaskType("foo")
      .setStatus(CeQueueDto.Status.PENDING);
    db.getDbClient().ceQueueDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  private void verifyCeTask(CeTaskSubmit taskSubmit, CeTask task, @Nullable ComponentDto componentDto, @Nullable UserDto userDto) {
    if (componentDto == null) {
      assertThat(task.getOrganizationUuid()).isEqualTo(defaultOrganizationProvider.get().getUuid());
    } else {
      assertThat(task.getOrganizationUuid()).isEqualTo(componentDto.getOrganizationUuid());
    }
    assertThat(task.getUuid()).isEqualTo(taskSubmit.getUuid());
    assertThat(task.getType()).isEqualTo(taskSubmit.getType());
    if (componentDto != null) {
      CeTask.Component component = task.getComponent().get();
      assertThat(component.getUuid()).isEqualTo(componentDto.uuid());
      assertThat(component.getKey()).contains(componentDto.getDbKey());
      assertThat(component.getName()).contains(componentDto.name());
    } else if (taskSubmit.getComponent().isPresent()) {
      assertThat(task.getComponent()).contains(new CeTask.Component(taskSubmit.getComponent().get().getUuid(), null, null));
    } else {
      assertThat(task.getComponent()).isEmpty();
    }
    if (taskSubmit.getSubmitterUuid() != null) {
      if (userDto == null) {
        assertThat(task.getSubmitter().getUuid()).isEqualTo(taskSubmit.getSubmitterUuid());
        assertThat(task.getSubmitter().getLogin()).isNull();
      } else {
        assertThat(task.getSubmitter().getUuid()).isEqualTo(userDto.getUuid()).isEqualTo(taskSubmit.getSubmitterUuid());
        assertThat(task.getSubmitter().getUuid()).isEqualTo(userDto.getLogin());
      }
    }
  }

  private void verifyCeQueueDtoForTaskSubmit(CeTaskSubmit taskSubmit) {
    Optional<CeQueueDto> queueDto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), taskSubmit.getUuid());
    assertThat(queueDto.isPresent()).isTrue();
    CeQueueDto dto = queueDto.get();
    assertThat(dto.getTaskType()).isEqualTo(taskSubmit.getType());
    Optional<CeTaskSubmit.Component> component = taskSubmit.getComponent();
    if (component.isPresent()) {
      assertThat(dto.getMainComponentUuid()).isEqualTo(component.get().getMainComponentUuid());
      assertThat(dto.getComponentUuid()).isEqualTo(component.get().getUuid());
    } else {
      assertThat(dto.getMainComponentUuid()).isNull();
      assertThat(dto.getComponentUuid()).isNull();
    }
    assertThat(dto.getSubmitterUuid()).isEqualTo(taskSubmit.getSubmitterUuid());
    assertThat(dto.getCreatedAt()).isEqualTo(dto.getUpdatedAt()).isNotNull();
  }

  private ComponentDto newProjectDto(String uuid) {
    return ComponentTesting.newPublicProjectDto(db.getDefaultOrganization(), uuid).setName("name_" + uuid).setDbKey("key_" + uuid);
  }

  private CeTask submit(String reportType, ComponentDto componentDto) {
    return underTest.submit(createTaskSubmit(reportType, componentDto, null));
  }

  private CeTaskSubmit createTaskSubmit(String type) {
    return createTaskSubmit(type, null, null);
  }

  private CeTaskSubmit createTaskSubmit(String type, @Nullable ComponentDto componentDto, @Nullable String submitterUuid) {
    CeTaskSubmit.Builder builder = underTest.prepareSubmit()
      .setType(type)
      .setSubmitterUuid(submitterUuid)
      .setCharacteristics(emptyMap());
    if (componentDto != null) {
      builder.setComponent(CeTaskSubmit.Component.fromDto(componentDto));
    }
    return builder.build();
  }

  private CeTaskResult newTaskResult(@Nullable String analysisUuid) {
    CeTaskResult taskResult = mock(CeTaskResult.class);
    when(taskResult.getAnalysisUuid()).thenReturn(java.util.Optional.ofNullable(analysisUuid));
    return taskResult;
  }

  private ComponentDto insertComponent(ComponentDto componentDto) {
    db.getDbClient().componentDao().insert(session, componentDto);
    session.commit();
    return componentDto;
  }

  private CeQueueDto makeInProgress(CeQueueDto ceQueueDto, String workerUuid) {
    CeQueueTesting.makeInProgress(session, workerUuid, system2.now(), ceQueueDto);
    return db.getDbClient().ceQueueDao().selectByUuid(session, ceQueueDto.getUuid()).get();
  }

  private static String stacktraceToString(Throwable error) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    error.printStackTrace(new PrintStream(out));
    return out.toString();
  }
}
