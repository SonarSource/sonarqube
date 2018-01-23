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

import com.google.common.collect.ImmutableSet;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.ce.container.ComputeEngineStatus;
import org.sonar.ce.monitoring.CEQueueStatus;
import org.sonar.ce.monitoring.CEQueueStatusImpl;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.computation.task.step.TypedException;
import org.sonar.server.organization.DefaultOrganization;
import org.sonar.server.organization.DefaultOrganizationProvider;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
  private CEQueueStatus queueStatus = new CEQueueStatusImpl(db.getDbClient());
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
    CeTaskSubmit taskSubmit = createTaskSubmit(CeTaskTypes.REPORT, "PROJECT_1", "rob");
    CeTask task = underTest.submit(taskSubmit);

    verifyCeTask(taskSubmit, task, null);
    verifyCeQueueDtoForTaskSubmit(taskSubmit);
  }

  @Test
  public void submit_populates_component_name_and_key_of_CeTask_if_component_exists() {
    ComponentDto componentDto = insertComponent(newComponentDto("PROJECT_1"));
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
  public void submit_fails_with_ISE_if_paused() {
    underTest.pauseSubmit();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Compute Engine does not currently accept new tasks");

    submit(CeTaskTypes.REPORT, "PROJECT_1");
  }

  @Test
  public void massSubmit_returns_tasks_for_each_CeTaskSubmit_populated_from_CeTaskSubmit_and_creates_CeQueue_row_for_each() {
    CeTaskSubmit taskSubmit1 = createTaskSubmit(CeTaskTypes.REPORT, "PROJECT_1", "rob");
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
    ComponentDto componentDto1 = insertComponent(newComponentDto("PROJECT_1"));
    CeTaskSubmit taskSubmit1 = createTaskSubmit(CeTaskTypes.REPORT, componentDto1.uuid(), null);
    CeTaskSubmit taskSubmit2 = createTaskSubmit("something", "non existing component uuid", null);

    List<CeTask> tasks = underTest.massSubmit(asList(taskSubmit1, taskSubmit2));

    assertThat(tasks).hasSize(2);
    verifyCeTask(taskSubmit1, tasks.get(0), componentDto1);
    verifyCeTask(taskSubmit2, tasks.get(1), null);
  }

  @Test
  public void peek_throws_NPE_if_workerUUid_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("workerUuid can't be null");

    underTest.peek(null);
  }

  @Test
  public void test_remove() {
    CeTask task = submit(CeTaskTypes.REPORT, "PROJECT_1");
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
    CeTask task = submit(CeTaskTypes.REPORT, "PROJECT_1");
    Optional<CeTask> peek = underTest.peek(WORKER_UUID_1);
    underTest.remove(peek.get(), CeActivityDto.Status.SUCCESS, newTaskResult(null), null);

    // available in history
    Optional<CeActivityDto> history = db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), task.getUuid());
    assertThat(history.isPresent()).isTrue();
    assertThat(history.get().getAnalysisUuid()).isNull();
  }

  @Test
  public void remove_sets_analysisUuid_in_CeActivity_when_CeTaskResult_has_analysis_uuid() {
    CeTask task = submit(CeTaskTypes.REPORT, "PROJECT_1");

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

    CeTask task = submit(CeTaskTypes.REPORT, "PROJECT_1");
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

    CeTask task = submit(CeTaskTypes.REPORT, "PROJECT_1");
    Optional<CeTask> peek = underTest.peek(WORKER_UUID_1);
    underTest.remove(peek.get(), CeActivityDto.Status.FAILED, null, error);

    CeActivityDto activityDto = db.getDbClient().ceActivityDao().selectByUuid(session, task.getUuid()).get();
    assertThat(activityDto.getErrorType()).isEqualTo("aType");
    assertThat(activityDto.getErrorMessage()).isEqualTo("aMessage");
    assertThat(activityDto.getErrorStacktrace()).isEqualToIgnoringWhitespace(stacktraceToString(error));
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
  public void remove_copies_executionCount_and_workerUuid() {
    db.getDbClient().ceQueueDao().insert(session, new CeQueueDto()
      .setUuid("uuid")
      .setTaskType("foo")
      .setStatus(CeQueueDto.Status.PENDING)
      .setWorkerUuid("Dustin")
      .setExecutionCount(2));
    db.commit();

    underTest.remove(new CeTask.Builder()
      .setOrganizationUuid("foo")
      .setUuid("uuid")
      .setType("bar")
      .build(), CeActivityDto.Status.SUCCESS, null, null);

    CeActivityDto dto = db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), "uuid").get();
    assertThat(dto.getExecutionCount()).isEqualTo(2);
    assertThat(dto.getWorkerUuid()).isEqualTo("Dustin");
  }

  @Test
  public void fail_to_remove_if_not_in_queue() {
    CeTask task = submit(CeTaskTypes.REPORT, "PROJECT_1");
    underTest.remove(task, CeActivityDto.Status.SUCCESS, null, null);

    expectedException.expect(IllegalStateException.class);

    underTest.remove(task, CeActivityDto.Status.SUCCESS, null, null);
  }

  @Test
  public void test_peek() {
    CeTask task = submit(CeTaskTypes.REPORT, "PROJECT_1");

    Optional<CeTask> peek = underTest.peek(WORKER_UUID_1);
    assertThat(peek.isPresent()).isTrue();
    assertThat(peek.get().getUuid()).isEqualTo(task.getUuid());
    assertThat(peek.get().getType()).isEqualTo(CeTaskTypes.REPORT);
    assertThat(peek.get().getComponentUuid()).isEqualTo("PROJECT_1");

    // no more pending tasks
    peek = underTest.peek(WORKER_UUID_2);
    assertThat(peek.isPresent()).isFalse();
  }

  @Test
  public void peek_overrides_workerUuid_to_argument() {
    db.getDbClient().ceQueueDao().insert(session, new CeQueueDto()
      .setUuid("uuid")
      .setTaskType("foo")
      .setStatus(CeQueueDto.Status.PENDING)
      .setWorkerUuid("must be overriden"));
    db.commit();

    underTest.peek(WORKER_UUID_1);

    CeQueueDto ceQueueDto = db.getDbClient().ceQueueDao().selectByUuid(session, "uuid").get();
    assertThat(ceQueueDto.getWorkerUuid()).isEqualTo(WORKER_UUID_1);
  }

  @Test
  public void peek_nothing_if_application_status_stopping() {
    submit(CeTaskTypes.REPORT, "PROJECT_1");
    when(computeEngineStatus.getStatus()).thenReturn(STOPPING);

    Optional<CeTask> peek = underTest.peek(WORKER_UUID_1);
    assertThat(peek.isPresent()).isFalse();
  }

  @Test
  public void peek_peeks_pending_tasks_with_executionCount_equal_to_0_and_increases_it() {
    db.getDbClient().ceQueueDao().insert(session, new CeQueueDto()
      .setUuid("uuid")
      .setTaskType("foo")
      .setStatus(CeQueueDto.Status.PENDING)
      .setExecutionCount(0));
    db.commit();

    assertThat(underTest.peek(WORKER_UUID_1).get().getUuid()).isEqualTo("uuid");
    assertThat(db.getDbClient().ceQueueDao().selectByUuid(session, "uuid").get().getExecutionCount()).isEqualTo(1);
  }

  @Test
  public void peek_ignores_pending_tasks_with_executionCount_equal_to_1() {
    db.getDbClient().ceQueueDao().insert(session, new CeQueueDto()
      .setUuid("uuid")
      .setTaskType("foo")
      .setStatus(CeQueueDto.Status.PENDING)
      .setExecutionCount(1));
    db.commit();

    assertThat(underTest.peek(WORKER_UUID_1).isPresent()).isFalse();
  }

  @Test
  public void peek_ignores_pending_tasks_with_executionCount_equal_to_2() {
    db.getDbClient().ceQueueDao().insert(session, new CeQueueDto()
      .setUuid("uuid")
      .setTaskType("foo")
      .setStatus(CeQueueDto.Status.PENDING)
      .setExecutionCount(2));
    db.commit();

    assertThat(underTest.peek(WORKER_UUID_1).isPresent()).isFalse();
  }

  @Test
  public void peek_ignores_pending_tasks_with_executionCount_greater_than_2() {
    db.getDbClient().ceQueueDao().insert(session, new CeQueueDto()
      .setUuid("uuid")
      .setTaskType("foo")
      .setStatus(CeQueueDto.Status.PENDING)
      .setExecutionCount(2 + Math.abs(new Random().nextInt(100))));
    db.commit();

    assertThat(underTest.peek(WORKER_UUID_1).isPresent()).isFalse();
  }

  @Test
  public void peek_resets_to_pending_any_task_in_progress_for_specified_worker_uuid_and_updates_updatedAt_no_matter_execution_count() {
    insertPending("u0", "doesn't matter", 0); // add a pending one that will be picked so that u1 isn't peek and status reset is visible in DB
    CeQueueDto u1 = insertPending("u1", WORKER_UUID_1, 2);// won't be peeked because it's worn-out
    CeQueueDto u2 = insertInProgress("u2", WORKER_UUID_1, 3);// will be reset but won't be picked because it's worn-out
    CeQueueDto u3 = insertPending("u3", WORKER_UUID_1, 1);// will be picked-because older than any of the reset ones
    CeQueueDto u4 = insertInProgress("u4", WORKER_UUID_1, 1);// will be reset

    assertThat(underTest.peek(WORKER_UUID_1).get().getUuid()).isEqualTo("u0");

    verifyUnmodifiedTask(u1);
    verifyResetTask(u2);
    verifyUnmodifiedTask(u3);
    verifyResetTask(u4);
  }

  @Test
  public void peek_resets_to_pending_any_task_in_progress_for_specified_worker_uuid_and_only_this_uuid() {
    insertPending("u0", "doesn't matter", 0); // add a pending one that will be picked so that u1 isn't peek and status reset is visible in DB
    CeQueueDto u1 = insertInProgress("u1", WORKER_UUID_1, 3);
    CeQueueDto u2 = insertInProgress("u2", WORKER_UUID_2, 3);
    CeQueueDto u3 = insertInProgress("u3", WORKER_UUID_1, 3);
    CeQueueDto u4 = insertInProgress("u4", WORKER_UUID_2, 1);

    assertThat(underTest.peek(WORKER_UUID_1).get().getUuid()).isEqualTo("u0");

    verifyResetTask(u1);
    verifyUnmodifiedTask(u2);
    verifyResetTask(u3);
    verifyUnmodifiedTask(u4);
  }

  @Test
  public void peek_resets_to_pending_any_task_in_progress_for_specified_worker_uuid_and_peeks_the_oldest_non_worn_out_no_matter_if_it_has_been_reset_or_not() {
    insertPending("u1", WORKER_UUID_1, 3); // won't be picked because worn out
    insertInProgress("u2", WORKER_UUID_1, 3); // will be reset but won't be picked because worn out
    insertInProgress("u3", WORKER_UUID_1, 1); // will be reset but won't be picked because worn out
    insertPending("u4", WORKER_UUID_1, 0); // will be picked

    Optional<CeTask> ceTask = underTest.peek(WORKER_UUID_1);
    assertThat(ceTask.get().getUuid()).isEqualTo("u4");
  }

  @Test
  public void peek_resets_to_pending_any_task_in_progress_for_specified_worker_uuid_and_peeks_reset_tasks_if_is_the_oldest_non_worn_out() {
    insertPending("u1", WORKER_UUID_1, 3); // won't be picked because worn out
    insertInProgress("u2", WORKER_UUID_1, 3); // will be reset but won't be picked because worn out
    insertInProgress("u3", WORKER_UUID_1, 1); // won't be picked because worn out
    insertPending("u4", WORKER_UUID_1, 0); // will be picked second

    Optional<CeTask> ceTask = underTest.peek(WORKER_UUID_1);
    assertThat(ceTask.get().getUuid()).isEqualTo("u4");
  }

  private void verifyResetTask(CeQueueDto originalDto) {
    CeQueueDto dto = db.getDbClient().ceQueueDao().selectByUuid(session, originalDto.getUuid()).get();
    assertThat(dto.getStatus()).isEqualTo(CeQueueDto.Status.PENDING);
    assertThat(dto.getExecutionCount()).isEqualTo(originalDto.getExecutionCount());
    assertThat(dto.getCreatedAt()).isEqualTo(originalDto.getCreatedAt());
    assertThat(dto.getUpdatedAt()).isGreaterThan(originalDto.getUpdatedAt());
  }

  private void verifyUnmodifiedTask(CeQueueDto originalDto) {
    CeQueueDto dto = db.getDbClient().ceQueueDao().selectByUuid(session, originalDto.getUuid()).get();
    assertThat(dto.getStatus()).isEqualTo(originalDto.getStatus());
    assertThat(dto.getExecutionCount()).isEqualTo(originalDto.getExecutionCount());
    assertThat(dto.getCreatedAt()).isEqualTo(originalDto.getCreatedAt());
    assertThat(dto.getUpdatedAt()).isEqualTo(originalDto.getUpdatedAt());
  }

  private CeQueueDto insertInProgress(String uuid, String workerUuid, int executionCount) {
    checkArgument(executionCount > 0, "execution count less than 1 does not make sense for an in progress task");
    CeQueueDto dto = new CeQueueDto()
      .setUuid(uuid)
      .setTaskType("foo")
      .setStatus(CeQueueDto.Status.IN_PROGRESS)
      .setWorkerUuid(workerUuid)
      .setExecutionCount(executionCount);
    db.getDbClient().ceQueueDao().insert(session, dto);
    db.commit();
    return dto;
  }

  private CeQueueDto insertPending(String uuid, String workerUuid, int executionCount) {
    checkArgument(executionCount > -1, "execution count less than 0 does not make sense for a pending task");
    CeQueueDto dto = new CeQueueDto()
      .setUuid(uuid)
      .setTaskType("foo")
      .setStatus(CeQueueDto.Status.PENDING)
      .setWorkerUuid(workerUuid)
      .setExecutionCount(executionCount);
    db.getDbClient().ceQueueDao().insert(session, dto);
    db.commit();
    return dto;
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
  public void cancel_copies_executionCount_and_workerUuid() {
    CeQueueDto ceQueueDto = db.getDbClient().ceQueueDao().insert(session, new CeQueueDto()
      .setUuid("uuid")
      .setTaskType("foo")
      .setStatus(CeQueueDto.Status.PENDING)
      .setWorkerUuid("Dustin")
      .setExecutionCount(2));
    db.commit();

    underTest.cancel(db.getSession(), ceQueueDto);

    CeActivityDto dto = db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), "uuid").get();
    assertThat(dto.getExecutionCount()).isEqualTo(2);
    assertThat(dto.getWorkerUuid()).isEqualTo("Dustin");
  }

  @Test
  public void fail_to_cancel_if_in_progress() {
    CeTask task = submit(CeTaskTypes.REPORT, "PROJECT_1");
    underTest.peek(WORKER_UUID_2);
    CeQueueDto queueDto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), task.getUuid()).get();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Task is in progress and can't be canceled");

    underTest.cancel(db.getSession(), queueDto);
  }

  @Test
  public void cancelAll_pendings_but_not_in_progress() {
    CeTask inProgressTask = submit(CeTaskTypes.REPORT, "PROJECT_1");
    CeTask pendingTask1 = submit(CeTaskTypes.REPORT, "PROJECT_2");
    CeTask pendingTask2 = submit(CeTaskTypes.REPORT, "PROJECT_3");
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
  public void cancelWornOuts_cancels_pending_tasks_with_executionCount_greater_or_equal_to_1() {
    CeQueueDto u1 = insertCeQueueDto("u1", CeQueueDto.Status.PENDING, 0, "worker1");
    CeQueueDto u2 = insertCeQueueDto("u2", CeQueueDto.Status.PENDING, 1, "worker1");
    CeQueueDto u3 = insertCeQueueDto("u3", CeQueueDto.Status.PENDING, 2, "worker1");
    CeQueueDto u4 = insertCeQueueDto("u4", CeQueueDto.Status.PENDING, 3, "worker1");
    CeQueueDto u5 = insertCeQueueDto("u5", CeQueueDto.Status.IN_PROGRESS, 0, "worker1");
    CeQueueDto u6 = insertCeQueueDto("u6", CeQueueDto.Status.IN_PROGRESS, 1, "worker1");
    CeQueueDto u7 = insertCeQueueDto("u7", CeQueueDto.Status.IN_PROGRESS, 2, "worker1");
    CeQueueDto u8 = insertCeQueueDto("u8", CeQueueDto.Status.IN_PROGRESS, 3, "worker1");

    underTest.cancelWornOuts();

    verifyUnmodified(u1);
    verifyCanceled(u2);
    verifyCanceled(u3);
    verifyCanceled(u4);
    verifyUnmodified(u5);
    verifyUnmodified(u6);
    verifyUnmodified(u7);
    verifyUnmodified(u8);
  }

  @Test
  public void resetTasksWithUnknownWorkerUUIDs_reset_only_in_progress_tasks() {
    CeQueueDto u1 = insertCeQueueDto("u1", CeQueueDto.Status.PENDING, 0, null);
    CeQueueDto u2 = insertCeQueueDto("u2", CeQueueDto.Status.PENDING, 1, "worker1");
    CeQueueDto u3 = insertCeQueueDto("u3", CeQueueDto.Status.PENDING, 2, null);
    CeQueueDto u4 = insertCeQueueDto("u4", CeQueueDto.Status.PENDING, 3, "worker2");
    CeQueueDto u5 = insertCeQueueDto("u5", CeQueueDto.Status.IN_PROGRESS, 0, null);
    CeQueueDto u6 = insertCeQueueDto("u6", CeQueueDto.Status.IN_PROGRESS, 1, "worker1");
    CeQueueDto u7 = insertCeQueueDto("u7", CeQueueDto.Status.IN_PROGRESS, 2, "worker2");
    CeQueueDto u8 = insertCeQueueDto("u8", CeQueueDto.Status.IN_PROGRESS, 3, "worker3");

    underTest.resetTasksWithUnknownWorkerUUIDs(ImmutableSet.of("worker2", "worker3"));

    // Pending tasks must not be modified even if a workerUUID is not present
    verifyUnmodified(u1);
    verifyUnmodified(u2);
    verifyUnmodified(u3);
    verifyUnmodified(u4);

    // Unknown worker : null, "worker1"
    verifyReset(u5);
    verifyReset(u6);

    // Known workers : "worker2", "worker3"
    verifyUnmodified(u7);
    verifyUnmodified(u8);
  }

  @Test
  public void resetTasksWithUnknownWorkerUUIDs_with_empty_set_will_reset_all_in_progress_tasks() {
    CeQueueDto u1 = insertCeQueueDto("u1", CeQueueDto.Status.PENDING, 0, null);
    CeQueueDto u2 = insertCeQueueDto("u2", CeQueueDto.Status.PENDING, 1, "worker1");
    CeQueueDto u3 = insertCeQueueDto("u3", CeQueueDto.Status.PENDING, 2, null);
    CeQueueDto u4 = insertCeQueueDto("u4", CeQueueDto.Status.PENDING, 3, "worker2");
    CeQueueDto u5 = insertCeQueueDto("u5", CeQueueDto.Status.IN_PROGRESS, 0, null);
    CeQueueDto u6 = insertCeQueueDto("u6", CeQueueDto.Status.IN_PROGRESS, 1, "worker1");
    CeQueueDto u7 = insertCeQueueDto("u7", CeQueueDto.Status.IN_PROGRESS, 2, "worker2");
    CeQueueDto u8 = insertCeQueueDto("u8", CeQueueDto.Status.IN_PROGRESS, 3, "worker3");

    underTest.resetTasksWithUnknownWorkerUUIDs(ImmutableSet.of());

    // Pending tasks must not be modified even if a workerUUID is not present
    verifyUnmodified(u1);
    verifyUnmodified(u2);
    verifyUnmodified(u3);
    verifyUnmodified(u4);

    // Unknown worker : null, "worker1"
    verifyReset(u5);
    verifyReset(u6);
    verifyReset(u7);
    verifyReset(u8);
  }

  @Test
  public void resetTasksWithUnknownWorkerUUIDs_with_worker_without_tasks_will_reset_all_in_progress_tasks() {
    CeQueueDto u1 = insertCeQueueDto("u1", CeQueueDto.Status.PENDING, 0, null);
    CeQueueDto u2 = insertCeQueueDto("u2", CeQueueDto.Status.PENDING, 1, "worker1");
    CeQueueDto u3 = insertCeQueueDto("u3", CeQueueDto.Status.PENDING, 2, null);
    CeQueueDto u4 = insertCeQueueDto("u4", CeQueueDto.Status.PENDING, 3, "worker2");
    CeQueueDto u5 = insertCeQueueDto("u5", CeQueueDto.Status.IN_PROGRESS, 0, null);
    CeQueueDto u6 = insertCeQueueDto("u6", CeQueueDto.Status.IN_PROGRESS, 1, "worker1");
    CeQueueDto u7 = insertCeQueueDto("u7", CeQueueDto.Status.IN_PROGRESS, 2, "worker2");
    CeQueueDto u8 = insertCeQueueDto("u8", CeQueueDto.Status.IN_PROGRESS, 3, "worker3");

    underTest.resetTasksWithUnknownWorkerUUIDs(ImmutableSet.of("worker1000", "worker1001"));

    // Pending tasks must not be modified even if a workerUUID is not present
    verifyUnmodified(u1);
    verifyUnmodified(u2);
    verifyUnmodified(u3);
    verifyUnmodified(u4);

    // Unknown worker : null, "worker1"
    verifyReset(u5);
    verifyReset(u6);
    verifyReset(u7);
    verifyReset(u8);
  }

  private void verifyReset(CeQueueDto original) {
    CeQueueDto dto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), original.getUuid()).get();
    // We do not touch ExecutionCount nor CreatedAt
    assertThat(dto.getExecutionCount()).isEqualTo(original.getExecutionCount());
    assertThat(dto.getCreatedAt()).isEqualTo(original.getCreatedAt());

    // Status must have changed to PENDING and must not be equal to previous status
    assertThat(dto.getStatus()).isEqualTo(CeQueueDto.Status.PENDING).isNotEqualTo(original.getStatus());
    // UpdatedAt must have been updated
    assertThat(dto.getUpdatedAt()).isNotEqualTo(original.getUpdatedAt());
    // StartedAt must be null
    assertThat(dto.getStartedAt()).isNull();
    // WorkerUuid must be null
    assertThat(dto.getWorkerUuid()).isNull();
  }

  private void verifyUnmodified(CeQueueDto original) {
    CeQueueDto dto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), original.getUuid()).get();
    assertThat(dto.getStatus()).isEqualTo(original.getStatus());
    assertThat(dto.getExecutionCount()).isEqualTo(original.getExecutionCount());
    assertThat(dto.getCreatedAt()).isEqualTo(original.getCreatedAt());
    assertThat(dto.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
  }

  private void verifyCanceled(CeQueueDto original) {
    assertThat(db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), original.getUuid())).isEmpty();
    CeActivityDto dto = db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), original.getUuid()).get();
    assertThat(dto.getStatus()).isEqualTo(CeActivityDto.Status.CANCELED);
    assertThat(dto.getExecutionCount()).isEqualTo(original.getExecutionCount());
    assertThat(dto.getWorkerUuid()).isEqualTo(original.getWorkerUuid());
  }

  private CeQueueDto insertCeQueueDto(String uuid, CeQueueDto.Status status, int executionCount, String workerUuid) {
    CeQueueDto dto = new CeQueueDto()
      .setUuid(uuid)
      .setTaskType("foo")
      .setStatus(status)
      .setExecutionCount(executionCount)
      .setWorkerUuid(workerUuid);
    db.getDbClient().ceQueueDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  @Test
  public void pause_and_resume_submits() {
    assertThat(underTest.isSubmitPaused()).isFalse();
    underTest.pauseSubmit();
    assertThat(underTest.isSubmitPaused()).isTrue();
    underTest.resumeSubmit();
    assertThat(underTest.isSubmitPaused()).isFalse();
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
    assertThat(task.getSubmitterLogin()).isEqualTo(taskSubmit.getSubmitterLogin());
  }

  private void verifyCeQueueDtoForTaskSubmit(CeTaskSubmit taskSubmit) {
    Optional<CeQueueDto> queueDto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), taskSubmit.getUuid());
    assertThat(queueDto.isPresent()).isTrue();
    CeQueueDto dto = queueDto.get();
    assertThat(dto.getTaskType()).isEqualTo(taskSubmit.getType());
    assertThat(dto.getComponentUuid()).isEqualTo(taskSubmit.getComponentUuid());
    assertThat(dto.getSubmitterLogin()).isEqualTo(taskSubmit.getSubmitterLogin());
    assertThat(dto.getCreatedAt()).isEqualTo(dto.getUpdatedAt()).isNotNull();
  }

  private ComponentDto newComponentDto(String uuid) {
    return ComponentTesting.newPublicProjectDto(db.getDefaultOrganization(), uuid).setName("name_" + uuid).setDbKey("key_" + uuid);
  }

  private CeTask submit(String reportType, String componentUuid) {
    return underTest.submit(createTaskSubmit(reportType, componentUuid, null));
  }

  private CeTaskSubmit createTaskSubmit(String type) {
    return createTaskSubmit(type, null, null);
  }

  private CeTaskSubmit createTaskSubmit(String type, @Nullable String componentUuid, @Nullable String submitterLogin) {
    CeTaskSubmit.Builder submission = underTest.prepareSubmit();
    submission.setType(type);
    submission.setComponentUuid(componentUuid);
    submission.setSubmitterLogin(submitterLogin);
    return submission.build();
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

  private static String stacktraceToString(Throwable error) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    error.printStackTrace(new PrintStream(out));
    return out.toString();
  }
}
