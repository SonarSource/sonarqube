/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.server.computation.monitoring.CEQueueStatus;
import org.sonar.server.computation.monitoring.CEQueueStatusImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class CeQueueImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  System2 system2 = new TestSystem2().setNow(1_450_000_000_000L);

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  UuidFactory uuidFactory = UuidFactoryImpl.INSTANCE;
  CEQueueStatus queueStatus = new CEQueueStatusImpl();
  CeQueueListener listener = mock(CeQueueListener.class);
  CeQueue underTest = new CeQueueImpl(system2, dbTester.getDbClient(), uuidFactory, queueStatus, new CeQueueListener[] {listener});

  @Test
  public void test_submit() {
    CeTaskSubmit.Builder submission = underTest.prepareSubmit();
    submission.setComponentUuid("PROJECT_1");
    submission.setType(CeTaskTypes.REPORT);
    submission.setSubmitterLogin("rob");

    CeTask task = underTest.submit(submission.build());
    assertThat(task.getUuid()).isEqualTo(submission.getUuid());
    assertThat(task.getComponentUuid()).isEqualTo("PROJECT_1");
    assertThat(task.getSubmitterLogin()).isEqualTo("rob");

    Optional<CeQueueDto> queueDto = dbTester.getDbClient().ceQueueDao().selectByUuid(dbTester.getSession(), submission.getUuid());
    assertThat(queueDto.isPresent()).isTrue();
    assertThat(queueDto.get().getTaskType()).isEqualTo(CeTaskTypes.REPORT);
    assertThat(queueDto.get().getComponentUuid()).isEqualTo("PROJECT_1");
    assertThat(queueDto.get().getSubmitterLogin()).isEqualTo("rob");
    assertThat(queueDto.get().getCreatedAt()).isEqualTo(1_450_000_000_000L);
    assertThat(queueStatus.getReceivedCount()).isEqualTo(1L);
  }

  @Test
  public void fail_to_submit_if_paused() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Compute Engine does not currently accept new tasks");
    underTest.pauseSubmit();

    submit(CeTaskTypes.REPORT, "PROJECT_1");
  }

  @Test
  public void test_remove() {
    CeTask task = submit(CeTaskTypes.REPORT, "PROJECT_1");
    Optional<CeTask> peek = underTest.peek();
    underTest.remove(peek.get(), CeActivityDto.Status.SUCCESS);

    // queue is empty
    assertThat(dbTester.getDbClient().ceQueueDao().selectByUuid(dbTester.getSession(), task.getUuid()).isPresent()).isFalse();
    assertThat(underTest.peek().isPresent()).isFalse();

    // available in history
    Optional<CeActivityDto> history = dbTester.getDbClient().ceActivityDao().selectByUuid(dbTester.getSession(), task.getUuid());
    assertThat(history.isPresent()).isTrue();
    assertThat(history.get().getStatus()).isEqualTo(CeActivityDto.Status.SUCCESS);
    assertThat(history.get().getIsLast()).isTrue();

    verify(listener).onRemoved(task, CeActivityDto.Status.SUCCESS);
  }

  @Test
  public void fail_to_remove_if_not_in_queue() throws Exception {
    expectedException.expect(IllegalStateException.class);
    CeTask task = submit(CeTaskTypes.REPORT, "PROJECT_1");
    underTest.remove(task, CeActivityDto.Status.SUCCESS);

    // fail
    underTest.remove(task, CeActivityDto.Status.SUCCESS);
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

  private CeTask submit(String reportType, String componentUuid) {
    CeTaskSubmit.Builder submission = underTest.prepareSubmit();
    submission.setType(reportType);
    submission.setComponentUuid(componentUuid);
    return underTest.submit(submission.build());
  }
}
