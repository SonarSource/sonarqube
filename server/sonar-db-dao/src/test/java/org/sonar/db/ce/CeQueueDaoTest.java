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
package org.sonar.db.ce;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbTester;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.ce.CeQueueDto.Status.IN_PROGRESS;
import static org.sonar.db.ce.CeQueueDto.Status.PENDING;
import static org.sonar.db.ce.CeQueueTesting.newCeQueueDto;

public class CeQueueDaoTest {
  private static final long INIT_TIME = 1_450_000_000_000L;
  private static final String TASK_UUID_1 = "TASK_1";
  private static final String TASK_UUID_2 = "TASK_2";
  private static final String COMPONENT_UUID_1 = "PROJECT_1";
  private static final String COMPONENT_UUID_2 = "PROJECT_2";
  private static final String TASK_UUID_3 = "TASK_3";
  private static final String SELECT_QUEUE_UUID_AND_STATUS_QUERY = "select uuid,status from ce_queue";
  private static final String SUBMITTER_LOGIN = "henri";
  private static final String WORKER_UUID_1 = "worker uuid 1";
  private static final String WORKER_UUID_2 = "worker uuid 2";
  private static final int EXECUTION_COUNT = 42;
  private static final int MAX_EXECUTION_COUNT = 2;

  private TestSystem2 system2 = new TestSystem2().setNow(INIT_TIME);

  @Rule
  public DbTester db = DbTester.create(system2);

  private System2 mockedSystem2 = mock(System2.class);

  private CeQueueDao underTest = new CeQueueDao(system2);
  private CeQueueDao underTestWithSystem2Mock = new CeQueueDao(mockedSystem2);
  private CeQueueDao underTestAlwaysIncreasingSystem2 = new CeQueueDao(new AlwaysIncreasingSystem2());

  @Test
  public void insert_populates_createdAt_and_updateAt_from_System2_with_same_value_if_any_is_not_set() {
    long now = 1_334_333L;
    CeQueueDto dto = new CeQueueDto()
      .setTaskType(CeTaskTypes.REPORT)
      .setComponentUuid(COMPONENT_UUID_1)
      .setStatus(PENDING)
      .setSubmitterLogin(SUBMITTER_LOGIN)
      .setWorkerUuid(WORKER_UUID_1)
      .setExecutionCount(EXECUTION_COUNT);

    mockSystem2ForSingleCall(now);
    underTestWithSystem2Mock.insert(db.getSession(), dto.setUuid(TASK_UUID_1));
    mockSystem2ForSingleCall(now);
    underTestWithSystem2Mock.insert(db.getSession(), dto.setUuid(TASK_UUID_2).setCreatedAt(8_000_999L).setUpdatedAt(0));
    mockSystem2ForSingleCall(now);
    underTestWithSystem2Mock.insert(db.getSession(), dto.setUuid(TASK_UUID_3).setCreatedAt(0).setUpdatedAt(8_000_999L));
    mockSystem2ForSingleCall(now);
    String uuid4 = "uuid 4";
    underTestWithSystem2Mock.insert(db.getSession(), dto.setUuid(uuid4).setCreatedAt(6_888_777L).setUpdatedAt(8_000_999L));
    db.getSession().commit();

    Stream.of(TASK_UUID_1, TASK_UUID_2, TASK_UUID_3)
      .forEach(uuid -> {
        CeQueueDto saved = underTest.selectByUuid(db.getSession(), uuid).get();
        assertThat(saved.getUuid()).isEqualTo(uuid);
        assertThat(saved.getTaskType()).isEqualTo(CeTaskTypes.REPORT);
        assertThat(saved.getComponentUuid()).isEqualTo(COMPONENT_UUID_1);
        assertThat(saved.getStatus()).isEqualTo(PENDING);
        assertThat(saved.getSubmitterLogin()).isEqualTo(SUBMITTER_LOGIN);
        assertThat(saved.getWorkerUuid()).isEqualTo(WORKER_UUID_1);
        assertThat(saved.getExecutionCount()).isEqualTo(EXECUTION_COUNT);
        assertThat(saved.getCreatedAt()).isEqualTo(now);
        assertThat(saved.getUpdatedAt()).isEqualTo(now);
        assertThat(saved.getStartedAt()).isNull();
      });
    CeQueueDto saved = underTest.selectByUuid(db.getSession(), uuid4).get();
    assertThat(saved.getUuid()).isEqualTo(uuid4);
    assertThat(saved.getTaskType()).isEqualTo(CeTaskTypes.REPORT);
    assertThat(saved.getComponentUuid()).isEqualTo(COMPONENT_UUID_1);
    assertThat(saved.getStatus()).isEqualTo(PENDING);
    assertThat(saved.getSubmitterLogin()).isEqualTo(SUBMITTER_LOGIN);
    assertThat(saved.getWorkerUuid()).isEqualTo(WORKER_UUID_1);
    assertThat(saved.getExecutionCount()).isEqualTo(EXECUTION_COUNT);
    assertThat(saved.getCreatedAt()).isEqualTo(6_888_777L);
    assertThat(saved.getUpdatedAt()).isEqualTo(8_000_999L);
    assertThat(saved.getStartedAt()).isNull();
  }

  @Test
  public void test_selectByUuid() {
    insert(TASK_UUID_1, COMPONENT_UUID_1, PENDING);

    assertThat(underTest.selectByUuid(db.getSession(), "TASK_UNKNOWN").isPresent()).isFalse();
    CeQueueDto saved = underTest.selectByUuid(db.getSession(), TASK_UUID_1).get();
    assertThat(saved.getUuid()).isEqualTo(TASK_UUID_1);
    assertThat(saved.getTaskType()).isEqualTo(CeTaskTypes.REPORT);
    assertThat(saved.getComponentUuid()).isEqualTo(COMPONENT_UUID_1);
    assertThat(saved.getStatus()).isEqualTo(PENDING);
    assertThat(saved.getSubmitterLogin()).isEqualTo("henri");
    assertThat(saved.getWorkerUuid()).isNull();
    assertThat(saved.getExecutionCount()).isEqualTo(0);
    assertThat(saved.getCreatedAt()).isEqualTo(INIT_TIME);
    assertThat(saved.getUpdatedAt()).isEqualTo(INIT_TIME);
    assertThat(saved.getStartedAt()).isNull();
  }

  @Test
  public void test_selectByComponentUuid() {
    insert(TASK_UUID_1, COMPONENT_UUID_1, PENDING);
    insert(TASK_UUID_2, COMPONENT_UUID_1, PENDING);
    insert(TASK_UUID_3, "PROJECT_2", PENDING);

    assertThat(underTest.selectByComponentUuid(db.getSession(), "UNKNOWN")).isEmpty();
    assertThat(underTest.selectByComponentUuid(db.getSession(), COMPONENT_UUID_1)).extracting("uuid").containsOnly(TASK_UUID_1, TASK_UUID_2);
    assertThat(underTest.selectByComponentUuid(db.getSession(), "PROJECT_2")).extracting("uuid").containsOnly(TASK_UUID_3);
  }

  @Test
  public void test_selectAllInAscOrder() {
    insert(TASK_UUID_1, COMPONENT_UUID_1, PENDING);
    insert(TASK_UUID_2, COMPONENT_UUID_1, PENDING);
    insert(TASK_UUID_3, "PROJECT_2", PENDING);

    assertThat(underTest.selectAllInAscOrder(db.getSession())).extracting("uuid").containsOnly(TASK_UUID_1, TASK_UUID_2, TASK_UUID_3);
  }

  @Test
  public void selectPendingByMinimumExecutionCount_returns_pending_tasks_with_executionCount_greater_or_equal_to_argument() {
    insert("p1", CeQueueDto.Status.PENDING, 0);
    insert("p2", CeQueueDto.Status.PENDING, 1);
    insert("p3", CeQueueDto.Status.PENDING, 2);
    insert("i1", CeQueueDto.Status.IN_PROGRESS, 0);
    insert("i2", CeQueueDto.Status.IN_PROGRESS, 1);
    insert("i3", CeQueueDto.Status.IN_PROGRESS, 2);

    assertThat(underTest.selectPendingByMinimumExecutionCount(db.getSession(), 0))
      .extracting(CeQueueDto::getUuid)
      .containsOnly("p1", "p2", "p3");
    assertThat(underTest.selectPendingByMinimumExecutionCount(db.getSession(), 1))
      .extracting(CeQueueDto::getUuid)
      .containsOnly("p2", "p3");
    assertThat(underTest.selectPendingByMinimumExecutionCount(db.getSession(), 2))
      .extracting(CeQueueDto::getUuid)
      .containsOnly("p3");
    assertThat(underTest.selectPendingByMinimumExecutionCount(db.getSession(), 3))
      .isEmpty();
    assertThat(underTest.selectPendingByMinimumExecutionCount(db.getSession(), 3 + Math.abs(new Random().nextInt(20))))
      .isEmpty();
  }

  @Test
  public void selectPendingByMinimumExecutionCount_does_not_return_non_pending_tasks() {

  }

  @Test
  public void test_delete() {
    insert(TASK_UUID_1, COMPONENT_UUID_1, PENDING);

    underTest.deleteByUuid(db.getSession(), "UNKNOWN");
    assertThat(underTest.selectByUuid(db.getSession(), TASK_UUID_1)).isPresent();

    underTest.deleteByUuid(db.getSession(), TASK_UUID_1);
    assertThat(underTest.selectByUuid(db.getSession(), TASK_UUID_1).isPresent()).isFalse();
  }

  @Test
  public void test_resetAllToPendingStatus() {
    insert(TASK_UUID_1, COMPONENT_UUID_1, PENDING);
    insert(TASK_UUID_2, COMPONENT_UUID_1, IN_PROGRESS);
    insert(TASK_UUID_3, COMPONENT_UUID_1, IN_PROGRESS);
    verifyCeQueueStatuses(TASK_UUID_1, PENDING, TASK_UUID_2, IN_PROGRESS, TASK_UUID_3, IN_PROGRESS);

    underTest.resetAllToPendingStatus(db.getSession());
    db.getSession().commit();

    verifyCeQueueStatuses(TASK_UUID_1, PENDING, TASK_UUID_2, PENDING, TASK_UUID_3, PENDING);
  }

  @Test
  public void resetAllToPendingStatus_updates_updatedAt() {
    long now = 1_334_333L;
    insert(TASK_UUID_1, COMPONENT_UUID_1, IN_PROGRESS);
    insert(TASK_UUID_2, COMPONENT_UUID_1, IN_PROGRESS);
    mockSystem2ForSingleCall(now);

    underTestWithSystem2Mock.resetAllToPendingStatus(db.getSession());

    assertThat(underTest.selectByUuid(db.getSession(), TASK_UUID_1).get().getUpdatedAt()).isEqualTo(now);
    assertThat(underTest.selectByUuid(db.getSession(), TASK_UUID_2).get().getUpdatedAt()).isEqualTo(now);
  }

  @Test
  public void resetAllToPendingStatus_resets_startedAt() {
    assertThat(insert(TASK_UUID_1, COMPONENT_UUID_1, PENDING).getStartedAt()).isNull();
    assertThat(underTest.peek(db.getSession(), WORKER_UUID_1, MAX_EXECUTION_COUNT).get().getUuid()).isEqualTo(TASK_UUID_1);
    assertThat(underTest.selectByUuid(db.getSession(), TASK_UUID_1).get().getStartedAt()).isNotNull();

    underTest.resetAllToPendingStatus(db.getSession());

    assertThat(underTest.selectByUuid(db.getSession(), TASK_UUID_1).get().getStartedAt()).isNull();
  }

  @Test
  public void resetAllToPendingStatus_does_not_reset_workerUuid_nor_executionCount() {
    CeQueueDto dto = new CeQueueDto()
      .setUuid(TASK_UUID_1)
      .setTaskType(CeTaskTypes.REPORT)
      .setComponentUuid(COMPONENT_UUID_1)
      .setStatus(IN_PROGRESS)
      .setSubmitterLogin(SUBMITTER_LOGIN)
      .setWorkerUuid(WORKER_UUID_1)
      .setExecutionCount(EXECUTION_COUNT);
    underTest.insert(db.getSession(), dto);
    db.commit();

    underTest.resetAllToPendingStatus(db.getSession());

    CeQueueDto saved = underTest.selectByUuid(db.getSession(), TASK_UUID_1).get();
    assertThat(saved.getWorkerUuid()).isEqualTo(WORKER_UUID_1);
    assertThat(saved.getExecutionCount()).isEqualTo(EXECUTION_COUNT);
  }

  @Test
  public void resetToPendingForWorker_resets_status_of_non_pending_tasks_only_for_specified_workerUuid() {
    long startedAt = 2_099_888L;
    CeQueueDto u1 = insert("u1", CeQueueDto.Status.IN_PROGRESS, 1, WORKER_UUID_1, startedAt);
    CeQueueDto u2 = insert("u2", CeQueueDto.Status.PENDING, 1, WORKER_UUID_1, startedAt);
    CeQueueDto u3 = insert("u3", CeQueueDto.Status.PENDING, 0, WORKER_UUID_1, startedAt);
    CeQueueDto u4 = insert("u4", CeQueueDto.Status.IN_PROGRESS, 2, WORKER_UUID_1, startedAt);
    CeQueueDto o1 = insert("o1", CeQueueDto.Status.IN_PROGRESS, 1, WORKER_UUID_2, startedAt);
    CeQueueDto o2 = insert("o2", CeQueueDto.Status.PENDING, 1, WORKER_UUID_2, startedAt);
    CeQueueDto o3 = insert("o3", CeQueueDto.Status.PENDING, 0, WORKER_UUID_2, startedAt);
    CeQueueDto o4 = insert("o4", CeQueueDto.Status.IN_PROGRESS, 2, WORKER_UUID_2, startedAt);

    underTestAlwaysIncreasingSystem2.resetToPendingForWorker(db.getSession(), WORKER_UUID_1);

    verifyResetToPendingForWorker(u1);
    verifyUnchangedByResetToPendingForWorker(u2);
    verifyUnchangedByResetToPendingForWorker(u3);
    verifyResetToPendingForWorker(u4);
    verifyUnchangedByResetToPendingForWorker(o1);
    verifyUnchangedByResetToPendingForWorker(o2);
    verifyUnchangedByResetToPendingForWorker(o3);
    verifyUnchangedByResetToPendingForWorker(o4);
  }


  @Test
  public void resetTasksWithUnknownWorkerUUIDs_with_empty_set_resets_status_of_all_pending_tasks() {
    long startedAt = 2_099_888L;
    CeQueueDto u1 = insert("u1", CeQueueDto.Status.IN_PROGRESS, 1, WORKER_UUID_1, startedAt);
    CeQueueDto u2 = insert("u2", CeQueueDto.Status.PENDING, 1, WORKER_UUID_1, startedAt);
    CeQueueDto u3 = insert("u3", CeQueueDto.Status.PENDING, 0, WORKER_UUID_1, startedAt);
    CeQueueDto u4 = insert("u4", CeQueueDto.Status.IN_PROGRESS, 2, WORKER_UUID_1, startedAt);
    CeQueueDto o1 = insert("o1", CeQueueDto.Status.IN_PROGRESS, 1, WORKER_UUID_2, startedAt);
    CeQueueDto o2 = insert("o2", CeQueueDto.Status.PENDING, 1, WORKER_UUID_2, startedAt);
    CeQueueDto o3 = insert("o3", CeQueueDto.Status.PENDING, 0, WORKER_UUID_2, startedAt);
    CeQueueDto o4 = insert("o4", CeQueueDto.Status.IN_PROGRESS, 2, WORKER_UUID_2, startedAt);

    underTestAlwaysIncreasingSystem2.resetTasksWithUnknownWorkerUUIDs(db.getSession(), ImmutableSet.of());

    verifyResetByResetTasks(u1);
    verifyUnchangedByResetToPendingForWorker(u2);
    verifyUnchangedByResetToPendingForWorker(u3);
    verifyResetByResetTasks(u4);
    verifyResetByResetTasks(o1);
    verifyUnchangedByResetToPendingForWorker(o2);
    verifyUnchangedByResetToPendingForWorker(o3);
    verifyResetByResetTasks(o4);
  }

  @Test
  public void resetTasksWithUnknownWorkerUUIDs_set_resets_status_of_all_pending_tasks_with_unknown_workers() {
    long startedAt = 2_099_888L;
    CeQueueDto u1 = insert("u1", CeQueueDto.Status.IN_PROGRESS, 1, WORKER_UUID_1, startedAt);
    CeQueueDto u2 = insert("u2", CeQueueDto.Status.PENDING, 1, WORKER_UUID_1, startedAt);
    CeQueueDto u3 = insert("u3", CeQueueDto.Status.PENDING, 0, WORKER_UUID_1, startedAt);
    CeQueueDto u4 = insert("u4", CeQueueDto.Status.IN_PROGRESS, 2, WORKER_UUID_1, startedAt);
    CeQueueDto o1 = insert("o1", CeQueueDto.Status.IN_PROGRESS, 1, WORKER_UUID_2, startedAt);
    CeQueueDto o2 = insert("o2", CeQueueDto.Status.PENDING, 1, WORKER_UUID_2, startedAt);
    CeQueueDto o3 = insert("o3", CeQueueDto.Status.PENDING, 0, WORKER_UUID_2, startedAt);
    CeQueueDto o4 = insert("o4", CeQueueDto.Status.IN_PROGRESS, 2, WORKER_UUID_2, startedAt);

    underTestAlwaysIncreasingSystem2.resetTasksWithUnknownWorkerUUIDs(db.getSession(), ImmutableSet.of(WORKER_UUID_1, "unknown"));

    verifyUnchangedByResetToPendingForWorker(u1);
    verifyUnchangedByResetToPendingForWorker(u2);
    verifyUnchangedByResetToPendingForWorker(u3);
    verifyUnchangedByResetToPendingForWorker(u4);
    verifyResetByResetTasks(o1);
    verifyUnchangedByResetToPendingForWorker(o2);
    verifyUnchangedByResetToPendingForWorker(o3);
    verifyResetByResetTasks(o4);
  }

  private void verifyResetByResetTasks(CeQueueDto original) {
    CeQueueDto dto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), original.getUuid()).get();
    assertThat(dto.getStatus()).isEqualTo(CeQueueDto.Status.PENDING).isNotEqualTo(original.getStatus());
    assertThat(dto.getExecutionCount()).isEqualTo(original.getExecutionCount());
    assertThat(dto.getStartedAt()).isNull();
    assertThat(dto.getCreatedAt()).isEqualTo(original.getCreatedAt());
    assertThat(dto.getUpdatedAt()).isGreaterThan(original.getUpdatedAt());
    assertThat(dto.getWorkerUuid()).isNull();
  }

  private void verifyResetToPendingForWorker(CeQueueDto original) {
    CeQueueDto dto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), original.getUuid()).get();
    assertThat(dto.getStatus()).isEqualTo(CeQueueDto.Status.PENDING);
    assertThat(dto.getExecutionCount()).isEqualTo(original.getExecutionCount());
    assertThat(dto.getStartedAt()).isNull();
    assertThat(dto.getCreatedAt()).isEqualTo(original.getCreatedAt());
    assertThat(dto.getUpdatedAt()).isGreaterThan(original.getUpdatedAt());
    assertThat(dto.getWorkerUuid()).isEqualTo(original.getWorkerUuid());
  }

  private void verifyUnchangedByResetToPendingForWorker(CeQueueDto original) {
    CeQueueDto dto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), original.getUuid()).get();
    assertThat(dto.getStatus()).isEqualTo(original.getStatus());
    assertThat(dto.getExecutionCount()).isEqualTo(original.getExecutionCount());
    assertThat(dto.getStartedAt()).isEqualTo(original.getStartedAt());
    assertThat(dto.getCreatedAt()).isEqualTo(original.getCreatedAt());
    assertThat(dto.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
    assertThat(dto.getWorkerUuid()).isEqualTo(original.getWorkerUuid());
  }

  @Test
  public void peek_none_if_no_pendings() {
    assertThat(underTest.peek(db.getSession(), WORKER_UUID_1, MAX_EXECUTION_COUNT).isPresent()).isFalse();

    // not pending, but in progress
    insert(TASK_UUID_1, COMPONENT_UUID_1, IN_PROGRESS);
    assertThat(underTest.peek(db.getSession(), WORKER_UUID_1, MAX_EXECUTION_COUNT).isPresent()).isFalse();
  }

  @Test
  public void peek_oldest_pending() {
    insert(TASK_UUID_1, COMPONENT_UUID_1, PENDING);
    system2.setNow(INIT_TIME + 3_000_000);
    insert(TASK_UUID_2, COMPONENT_UUID_2, PENDING);

    assertThat(db.countRowsOfTable("ce_queue")).isEqualTo(2);
    verifyCeQueueStatuses(TASK_UUID_1, PENDING, TASK_UUID_2, PENDING);

    // peek first one
    Optional<CeQueueDto> peek = underTest.peek(db.getSession(), WORKER_UUID_1, MAX_EXECUTION_COUNT);
    assertThat(peek).isPresent();
    assertThat(peek.get().getUuid()).isEqualTo(TASK_UUID_1);
    assertThat(peek.get().getStatus()).isEqualTo(IN_PROGRESS);
    assertThat(peek.get().getWorkerUuid()).isEqualTo(WORKER_UUID_1);
    assertThat(peek.get().getExecutionCount()).isEqualTo(1);
    verifyCeQueueStatuses(TASK_UUID_1, IN_PROGRESS, TASK_UUID_2, PENDING);

    // peek second one
    peek = underTest.peek(db.getSession(), WORKER_UUID_2, MAX_EXECUTION_COUNT);
    assertThat(peek).isPresent();
    assertThat(peek.get().getUuid()).isEqualTo(TASK_UUID_2);
    assertThat(peek.get().getStatus()).isEqualTo(IN_PROGRESS);
    assertThat(peek.get().getWorkerUuid()).isEqualTo(WORKER_UUID_2);
    assertThat(peek.get().getExecutionCount()).isEqualTo(1);
    verifyCeQueueStatuses(TASK_UUID_1, IN_PROGRESS, TASK_UUID_2, IN_PROGRESS);

    // no more pendings
    assertThat(underTest.peek(db.getSession(), WORKER_UUID_1, MAX_EXECUTION_COUNT).isPresent()).isFalse();
  }

  @Test
  public void do_not_peek_multiple_tasks_on_same_project_at_the_same_time() {
    // two pending tasks on the same project
    insert(TASK_UUID_1, COMPONENT_UUID_1, PENDING);
    system2.setNow(INIT_TIME + 3_000_000);
    insert(TASK_UUID_2, COMPONENT_UUID_1, PENDING);

    Optional<CeQueueDto> peek = underTest.peek(db.getSession(), WORKER_UUID_1, MAX_EXECUTION_COUNT);
    assertThat(peek).isPresent();
    assertThat(peek.get().getUuid()).isEqualTo(TASK_UUID_1);
    assertThat(peek.get().getWorkerUuid()).isEqualTo(WORKER_UUID_1);
    assertThat(peek.get().getExecutionCount()).isEqualTo(1);
    verifyCeQueueStatuses(TASK_UUID_1, IN_PROGRESS, TASK_UUID_2, PENDING);

    // do not peek second task as long as the first one is in progress
    peek = underTest.peek(db.getSession(), WORKER_UUID_1, MAX_EXECUTION_COUNT);
    assertThat(peek.isPresent()).isFalse();

    // first one is finished
    underTest.deleteByUuid(db.getSession(), TASK_UUID_1);
    peek = underTest.peek(db.getSession(), WORKER_UUID_2, MAX_EXECUTION_COUNT);
    assertThat(peek.get().getUuid()).isEqualTo(TASK_UUID_2);
    assertThat(peek.get().getWorkerUuid()).isEqualTo(WORKER_UUID_2);
    assertThat(peek.get().getExecutionCount()).isEqualTo(1);
  }

  @Test
  public void peek_ignores_rows_with_executionCount_greater_or_equal_to_specified_maxExecutionCount_0() {
    peek_ignores_rows_with_executionCount_greater_or_equal_to_specified_maxExecutionCount(0, null);
  }

  @Test
  public void peek_ignores_rows_with_executionCount_greater_or_equal_to_specified_maxExecutionCount_1() {
    peek_ignores_rows_with_executionCount_greater_or_equal_to_specified_maxExecutionCount(1, "u0");
  }

  @Test
  public void peek_ignores_rows_with_executionCount_greater_or_equal_to_specified_maxExecutionCount_2() {
    peek_ignores_rows_with_executionCount_greater_or_equal_to_specified_maxExecutionCount(2, "u1");
  }

  @Test
  public void peek_ignores_rows_with_executionCount_greater_or_equal_to_specified_maxExecutionCount_3() {
    peek_ignores_rows_with_executionCount_greater_or_equal_to_specified_maxExecutionCount(3, "u2");
  }

  @Test
  public void peek_ignores_rows_with_executionCount_greater_or_equal_to_specified_maxExecutionCount_4() {
    peek_ignores_rows_with_executionCount_greater_or_equal_to_specified_maxExecutionCount(4, "u3");
  }

  @Test
  public void peek_ignores_rows_with_executionCount_greater_or_equal_to_specified_maxExecutionCount_more_then_4() {
    peek_ignores_rows_with_executionCount_greater_or_equal_to_specified_maxExecutionCount(4 + Math.abs(new Random().nextInt(100)), "u3");
  }

  private void peek_ignores_rows_with_executionCount_greater_or_equal_to_specified_maxExecutionCount(int maxExecutionCount, @Nullable String expected) {
    insert("u3", CeQueueDto.Status.PENDING, 3);
    insert("u2", CeQueueDto.Status.PENDING, 2);
    insert("u1", CeQueueDto.Status.PENDING, 1);
    insert("u0", CeQueueDto.Status.PENDING, 0);

    Optional<CeQueueDto> dto = underTest.peek(db.getSession(), WORKER_UUID_1, maxExecutionCount);
    if (expected == null) {
      assertThat(dto.isPresent()).isFalse();
    } else {
      assertThat(dto.get().getUuid()).isEqualTo(expected);
    }
  }

  @Test
  public void select_by_query() {
    // task status not in query
    insert(newCeQueueDto(TASK_UUID_1)
      .setComponentUuid(COMPONENT_UUID_1)
      .setStatus(IN_PROGRESS)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(100_000L));

    // too early
    insert(newCeQueueDto(TASK_UUID_3)
      .setComponentUuid(COMPONENT_UUID_1)
      .setStatus(PENDING)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(90_000L));

    // task type not in query
    insert(newCeQueueDto("TASK_4")
      .setComponentUuid("PROJECT_2")
      .setStatus(PENDING)
      .setTaskType("ANOTHER_TYPE")
      .setCreatedAt(100_000L));

    // correct
    insert(newCeQueueDto(TASK_UUID_2)
      .setComponentUuid(COMPONENT_UUID_1)
      .setStatus(PENDING)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(100_000L));

    // correct submitted later
    insert(newCeQueueDto("TASK_5")
      .setComponentUuid(COMPONENT_UUID_1)
      .setStatus(PENDING)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(120_000L));

    // select by component uuid, status, task type and minimum submitted at
    CeTaskQuery query = new CeTaskQuery()
      .setComponentUuids(newArrayList(COMPONENT_UUID_1, "PROJECT_2"))
      .setStatuses(singletonList(PENDING.name()))
      .setType(CeTaskTypes.REPORT)
      .setMinSubmittedAt(100_000L);

    List<CeQueueDto> result = underTest.selectByQueryInDescOrder(db.getSession(), query, 1_000);
    int total = underTest.countByQuery(db.getSession(), query);

    assertThat(result).extracting("uuid").containsExactly("TASK_5", TASK_UUID_2);
    assertThat(total).isEqualTo(2);
  }

  @Test
  public void select_by_query_returns_empty_list_when_only_current() {
    insert(newCeQueueDto(TASK_UUID_1)
      .setComponentUuid(COMPONENT_UUID_1)
      .setStatus(IN_PROGRESS)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(100_000L));

    CeTaskQuery query = new CeTaskQuery().setOnlyCurrents(true);

    List<CeQueueDto> result = underTest.selectByQueryInDescOrder(db.getSession(), query, 1_000);
    int total = underTest.countByQuery(db.getSession(), query);

    assertThat(result).isEmpty();
    assertThat(total).isEqualTo(0);
  }

  @Test
  public void select_by_query_returns_empty_list_when_max_submitted_at() {
    insert(newCeQueueDto(TASK_UUID_1)
      .setComponentUuid(COMPONENT_UUID_1)
      .setStatus(IN_PROGRESS)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(100_000L));

    CeTaskQuery query = new CeTaskQuery().setMaxExecutedAt(1_000_000_000_000L);

    List<CeQueueDto> result = underTest.selectByQueryInDescOrder(db.getSession(), query, 1_000);
    int total = underTest.countByQuery(db.getSession(), query);

    assertThat(result).isEmpty();
    assertThat(total).isEqualTo(0);
  }

  @Test
  public void select_by_query_returns_empty_list_when_empty_list_of_component_uuid() {
    insert(newCeQueueDto(TASK_UUID_1)
      .setComponentUuid(COMPONENT_UUID_1)
      .setStatus(IN_PROGRESS)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(100_000L));

    CeTaskQuery query = new CeTaskQuery().setComponentUuids(Collections.emptyList());

    List<CeQueueDto> result = underTest.selectByQueryInDescOrder(db.getSession(), query, 1_000);
    int total = underTest.countByQuery(db.getSession(), query);

    assertThat(result).isEmpty();
    assertThat(total).isEqualTo(0);
  }

  @Test
  public void count_by_status_and_component_uuid() {
    // task retrieved in the queue
    insert(newCeQueueDto(TASK_UUID_1)
      .setComponentUuid(COMPONENT_UUID_1)
      .setStatus(IN_PROGRESS)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(100_000L));
    // on component uuid 2, not returned
    insert(newCeQueueDto(TASK_UUID_2)
      .setComponentUuid(COMPONENT_UUID_2)
      .setStatus(IN_PROGRESS)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(100_000L));
    // pending status, not returned
    insert(newCeQueueDto(TASK_UUID_3)
      .setComponentUuid(COMPONENT_UUID_1)
      .setStatus(PENDING)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(100_000L));

    assertThat(underTest.countByStatusAndComponentUuid(db.getSession(), IN_PROGRESS, COMPONENT_UUID_1)).isEqualTo(1);
    assertThat(underTest.countByStatus(db.getSession(), IN_PROGRESS)).isEqualTo(2);
  }

  private void insert(CeQueueDto dto) {
    underTest.insert(db.getSession(), dto);
    db.commit();
  }

  private CeQueueDto insert(String uuid, CeQueueDto.Status status, int executionCount) {
    CeQueueDto dto = new CeQueueDto();
    dto.setUuid(uuid);
    dto.setTaskType(CeTaskTypes.REPORT);
    dto.setStatus(status);
    dto.setSubmitterLogin("henri");
    dto.setExecutionCount(executionCount);
    underTestAlwaysIncreasingSystem2.insert(db.getSession(), dto);
    db.getSession().commit();
    return dto;
  }

  private CeQueueDto insert(String uuid, CeQueueDto.Status status, int executionCount, String workerUuid, Long startedAt) {
    CeQueueDto dto = new CeQueueDto();
    dto.setUuid(uuid);
    dto.setTaskType(CeTaskTypes.REPORT);
    dto.setStatus(status);
    dto.setSubmitterLogin("henri");
    dto.setExecutionCount(executionCount);
    dto.setWorkerUuid(workerUuid);
    dto.setStartedAt(startedAt);
    underTestAlwaysIncreasingSystem2.insert(db.getSession(), dto);
    db.getSession().commit();
    return dto;
  }

  private CeQueueDto insert(String uuid, String componentUuid, CeQueueDto.Status status) {
    CeQueueDto dto = new CeQueueDto();
    dto.setUuid(uuid);
    dto.setTaskType(CeTaskTypes.REPORT);
    dto.setComponentUuid(componentUuid);
    dto.setStatus(status);
    dto.setSubmitterLogin("henri");
    underTest.insert(db.getSession(), dto);
    db.getSession().commit();
    return dto;
  }

  private static Iterable<Map<String, Object>> upperizeKeys(List<Map<String, Object>> select) {
    return from(select).transform(new Function<Map<String, Object>, Map<String, Object>>() {
      @Nullable
      @Override
      public Map<String, Object> apply(Map<String, Object> input) {
        Map<String, Object> res = new HashMap<>(input.size());
        for (Map.Entry<String, Object> entry : input.entrySet()) {
          res.put(entry.getKey().toUpperCase(), entry.getValue());
        }
        return res;
      }
    });
  }

  private void verifyCeQueueStatuses(String taskUuid1, CeQueueDto.Status taskStatus1, String taskUuid2, CeQueueDto.Status taskStatus2, String taskUuid3,
    CeQueueDto.Status taskStatus3) {
    verifyCeQueueStatuses(new String[] {taskUuid1, taskUuid2, taskUuid3}, new CeQueueDto.Status[] {taskStatus1, taskStatus2, taskStatus3});
  }

  private void verifyCeQueueStatuses(String taskUuid1, CeQueueDto.Status taskStatus1, String taskUuid2, CeQueueDto.Status taskStatus2) {
    verifyCeQueueStatuses(new String[] {taskUuid1, taskUuid2}, new CeQueueDto.Status[] {taskStatus1, taskStatus2});
  }

  private void verifyCeQueueStatuses(String[] taskUuids, CeQueueDto.Status[] statuses) {
    Map<String, Object>[] rows = new Map[taskUuids.length];
    for (int i = 0; i < taskUuids.length; i++) {
      rows[i] = rowMap(taskUuids[i], statuses[i]);
    }
    assertThat(upperizeKeys(db.select(SELECT_QUEUE_UUID_AND_STATUS_QUERY))).containsOnly(rows);
  }

  private static Map<String, Object> rowMap(String uuid, CeQueueDto.Status status) {
    return ImmutableMap.of("UUID", uuid, "STATUS", status.name());
  }

  private void mockSystem2ForSingleCall(long now) {
    Mockito.reset(mockedSystem2);
    when(mockedSystem2.now())
      .thenReturn(now)
      .thenThrow(new IllegalStateException("now should be called only once"));
  }
}
