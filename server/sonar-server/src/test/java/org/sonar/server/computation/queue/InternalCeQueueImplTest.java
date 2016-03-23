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

import com.google.common.base.Optional;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.ce.monitoring.CEQueueStatus;
import org.sonar.ce.queue.CeQueueListener;
import org.sonar.ce.queue.CeTask;
import org.sonar.ce.queue.CeTaskResult;
import org.sonar.ce.queue.CeTaskSubmit;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.computation.monitoring.CEQueueStatusImpl;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class InternalCeQueueImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  System2 system2 = new TestSystem2().setNow(1_450_000_000_000L);

  @Rule
  public DbTester dbTester = DbTester.create(system2);
  DbSession session = dbTester.getSession();

  UuidFactory uuidFactory = UuidFactoryImpl.INSTANCE;
  CEQueueStatus queueStatus = new CEQueueStatusImpl(dbTester.getDbClient());
  CeQueueListener listener = mock(CeQueueListener.class);
  InternalCeQueue underTest = new InternalCeQueueImpl(system2, dbTester.getDbClient(), uuidFactory, queueStatus, new CeQueueListener[] {listener});

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
  public void test_remove() {
    CeTask task = submit(CeTaskTypes.REPORT, "PROJECT_1");
    Optional<CeTask> peek = underTest.peek();
    underTest.remove(peek.get(), CeActivityDto.Status.SUCCESS, null);

    // queue is empty
    assertThat(dbTester.getDbClient().ceQueueDao().selectByUuid(dbTester.getSession(), task.getUuid()).isPresent()).isFalse();
    assertThat(underTest.peek().isPresent()).isFalse();

    // available in history
    Optional<CeActivityDto> history = dbTester.getDbClient().ceActivityDao().selectByUuid(dbTester.getSession(), task.getUuid());
    assertThat(history.isPresent()).isTrue();
    assertThat(history.get().getStatus()).isEqualTo(CeActivityDto.Status.SUCCESS);
    assertThat(history.get().getIsLast()).isTrue();
    assertThat(history.get().getSnapshotId()).isNull();

    verify(listener).onRemoved(task, CeActivityDto.Status.SUCCESS);
  }

  @Test
  public void remove_does_not_set_snapshotId_in_CeActivity_when_CeTaskResult_has_no_snapshot_id() {
    CeTask task = submit(CeTaskTypes.REPORT, "PROJECT_1");
    Optional<CeTask> peek = underTest.peek();
    underTest.remove(peek.get(), CeActivityDto.Status.SUCCESS, newTaskResult(null));

    // available in history
    Optional<CeActivityDto> history = dbTester.getDbClient().ceActivityDao().selectByUuid(dbTester.getSession(), task.getUuid());
    assertThat(history.isPresent()).isTrue();
    assertThat(history.get().getSnapshotId()).isNull();
  }

  @Test
  public void remove_sets_snapshotId_in_CeActivity_when_CeTaskResult_has_no_snapshot_id() {
    CeTask task = submit(CeTaskTypes.REPORT, "PROJECT_1");
    long snapshotId = 663L;

    Optional<CeTask> peek = underTest.peek();
    underTest.remove(peek.get(), CeActivityDto.Status.SUCCESS, newTaskResult(snapshotId));

    // available in history
    Optional<CeActivityDto> history = dbTester.getDbClient().ceActivityDao().selectByUuid(dbTester.getSession(), task.getUuid());
    assertThat(history.isPresent()).isTrue();
    assertThat(history.get().getSnapshotId()).isEqualTo(snapshotId);
  }

  @Test
  public void fail_to_remove_if_not_in_queue() throws Exception {
    CeTask task = submit(CeTaskTypes.REPORT, "PROJECT_1");
    underTest.remove(task, CeActivityDto.Status.SUCCESS, null);

    expectedException.expect(IllegalStateException.class);

    underTest.remove(task, CeActivityDto.Status.SUCCESS, null);
  }

  @Test
  public void test_peek() throws Exception {
    CeTask task = submit(CeTaskTypes.REPORT, "PROJECT_1");

    Optional<CeTask> peek = underTest.peek();
    assertThat(peek.isPresent()).isTrue();
    assertThat(peek.get().getUuid()).isEqualTo(task.getUuid());
    assertThat(peek.get().getType()).isEqualTo(CeTaskTypes.REPORT);
    assertThat(peek.get().getComponentUuid()).isEqualTo("PROJECT_1");

    // no more pending tasks
    peek = underTest.peek();
    assertThat(peek.isPresent()).isFalse();

    verify(listener, never()).onRemoved(eq(task), any(CeActivityDto.Status.class));
  }

  @Test
  public void peek_nothing_if_paused() throws Exception {
    submit(CeTaskTypes.REPORT, "PROJECT_1");
    underTest.pausePeek();

    Optional<CeTask> peek = underTest.peek();
    assertThat(peek.isPresent()).isFalse();
  }

  @Test
  public void cancel_pending() throws Exception {
    CeTask task = submit(CeTaskTypes.REPORT, "PROJECT_1");

    // ignore
    boolean canceled = underTest.cancel("UNKNOWN");
    assertThat(canceled).isFalse();
    verifyZeroInteractions(listener);

    canceled = underTest.cancel(task.getUuid());
    assertThat(canceled).isTrue();
    Optional<CeActivityDto> activity = dbTester.getDbClient().ceActivityDao().selectByUuid(dbTester.getSession(), task.getUuid());
    assertThat(activity.isPresent()).isTrue();
    assertThat(activity.get().getStatus()).isEqualTo(CeActivityDto.Status.CANCELED);
    verify(listener).onRemoved(task, CeActivityDto.Status.CANCELED);
  }

  @Test
  public void fail_to_cancel_if_in_progress() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(startsWith("Task is in progress and can't be canceled"));

    CeTask task = submit(CeTaskTypes.REPORT, "PROJECT_1");
    underTest.peek();

    underTest.cancel(task.getUuid());
  }

  @Test
  public void cancelAll_pendings_but_not_in_progress() throws Exception {
    CeTask inProgressTask = submit(CeTaskTypes.REPORT, "PROJECT_1");
    CeTask pendingTask1 = submit(CeTaskTypes.REPORT, "PROJECT_2");
    CeTask pendingTask2 = submit(CeTaskTypes.REPORT, "PROJECT_3");
    underTest.peek();

    int canceledCount = underTest.cancelAll();
    assertThat(canceledCount).isEqualTo(2);

    Optional<CeActivityDto> history = dbTester.getDbClient().ceActivityDao().selectByUuid(dbTester.getSession(), pendingTask1.getUuid());
    assertThat(history.get().getStatus()).isEqualTo(CeActivityDto.Status.CANCELED);
    history = dbTester.getDbClient().ceActivityDao().selectByUuid(dbTester.getSession(), pendingTask2.getUuid());
    assertThat(history.get().getStatus()).isEqualTo(CeActivityDto.Status.CANCELED);
    history = dbTester.getDbClient().ceActivityDao().selectByUuid(dbTester.getSession(), inProgressTask.getUuid());
    assertThat(history.isPresent()).isFalse();

    verify(listener).onRemoved(pendingTask1, CeActivityDto.Status.CANCELED);
    verify(listener).onRemoved(pendingTask2, CeActivityDto.Status.CANCELED);
  }

  @Test
  public void pause_and_resume_submits() throws Exception {
    assertThat(underTest.isSubmitPaused()).isFalse();
    underTest.pauseSubmit();
    assertThat(underTest.isSubmitPaused()).isTrue();
    underTest.resumeSubmit();
    assertThat(underTest.isSubmitPaused()).isFalse();
  }

  @Test
  public void pause_and_resume_peeks() throws Exception {
    assertThat(underTest.isPeekPaused()).isFalse();
    underTest.pausePeek();
    assertThat(underTest.isPeekPaused()).isTrue();
    underTest.resumePeek();
    assertThat(underTest.isPeekPaused()).isFalse();
  }

  private void verifyCeTask(CeTaskSubmit taskSubmit, CeTask task, @Nullable ComponentDto componentDto) {
    assertThat(task.getUuid()).isEqualTo(taskSubmit.getUuid());
    assertThat(task.getComponentUuid()).isEqualTo(task.getComponentUuid());
    assertThat(task.getType()).isEqualTo(taskSubmit.getType());
    if (componentDto == null) {
      assertThat(task.getComponentKey()).isNull();
      assertThat(task.getComponentName()).isNull();
    } else {
      assertThat(task.getComponentKey()).isEqualTo(componentDto.key());
      assertThat(task.getComponentName()).isEqualTo(componentDto.name());
    }
    assertThat(task.getSubmitterLogin()).isEqualTo(taskSubmit.getSubmitterLogin());
  }

  private void verifyCeQueueDtoForTaskSubmit(CeTaskSubmit taskSubmit) {
    Optional<CeQueueDto> queueDto = dbTester.getDbClient().ceQueueDao().selectByUuid(dbTester.getSession(), taskSubmit.getUuid());
    assertThat(queueDto.isPresent()).isTrue();
    assertThat(queueDto.get().getTaskType()).isEqualTo(taskSubmit.getType());
    assertThat(queueDto.get().getComponentUuid()).isEqualTo(taskSubmit.getComponentUuid());
    assertThat(queueDto.get().getSubmitterLogin()).isEqualTo(taskSubmit.getSubmitterLogin());
    assertThat(queueDto.get().getCreatedAt()).isEqualTo(1_450_000_000_000L);
  }

  private static ComponentDto newComponentDto(String uuid) {
    return new ComponentDto().setUuid(uuid).setName("name_" + uuid).setKey("key_" + uuid);
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

  private CeTaskResult newTaskResult(Long snapshotId) {
    CeTaskResult taskResult = mock(CeTaskResult.class);
    when(taskResult.getSnapshotId()).thenReturn(snapshotId);
    return taskResult;
  }

  private ComponentDto insertComponent(ComponentDto componentDto) {
    dbTester.getDbClient().componentDao().insert(session, componentDto);
    session.commit();
    return componentDto;
  }
}
