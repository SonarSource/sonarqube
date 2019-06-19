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
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.ce.CeQueueDto.Status.IN_PROGRESS;
import static org.sonar.db.ce.CeQueueDto.Status.PENDING;
import static org.sonar.db.ce.CeQueueTesting.newCeQueueDto;
import static org.sonar.db.ce.CeQueueTesting.reset;

public class CeQueueDaoTest {
  private static final long INIT_TIME = 1_450_000_000_000L;
  private static final String TASK_UUID_1 = "TASK_1";
  private static final String TASK_UUID_2 = "TASK_2";
  private static final String MAIN_COMPONENT_UUID_1 = "PROJECT_1";
  private static final String MAIN_COMPONENT_UUID_2 = "PROJECT_2";
  private static final String TASK_UUID_3 = "TASK_3";
  private static final String SELECT_QUEUE_UUID_AND_STATUS_QUERY = "select uuid,status from ce_queue";
  private static final String SUBMITTER_LOGIN = "submitter uuid";
  private static final String WORKER_UUID_1 = "worker uuid 1";
  private static final String WORKER_UUID_2 = "worker uuid 2";

  private TestSystem2 system2 = new TestSystem2().setNow(INIT_TIME);

  @Rule
  public DbTester db = DbTester.create(system2);

  private System2 mockedSystem2 = mock(System2.class);
  private System2 alwaysIncreasingSystem2 = new AlwaysIncreasingSystem2();

  private CeQueueDao underTest = new CeQueueDao(system2);
  private CeQueueDao underTestWithSystem2Mock = new CeQueueDao(mockedSystem2);
  private CeQueueDao underTestAlwaysIncreasingSystem2 = new CeQueueDao(alwaysIncreasingSystem2);

  @Test
  public void insert_populates_createdAt_and_updateAt_from_System2_with_same_value_if_any_is_not_set() {
    long now = 1_334_333L;
    CeQueueDto dto = new CeQueueDto()
      .setTaskType(CeTaskTypes.REPORT)
      .setComponentUuid(MAIN_COMPONENT_UUID_1)
      .setStatus(PENDING)
      .setSubmitterUuid(SUBMITTER_LOGIN);

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
        assertThat(saved.getComponentUuid()).isEqualTo(MAIN_COMPONENT_UUID_1);
        assertThat(saved.getStatus()).isEqualTo(PENDING);
        assertThat(saved.getSubmitterUuid()).isEqualTo(SUBMITTER_LOGIN);
        assertThat(saved.getWorkerUuid()).isNull();
        assertThat(saved.getCreatedAt()).isEqualTo(now);
        assertThat(saved.getUpdatedAt()).isEqualTo(now);
        assertThat(saved.getStartedAt()).isNull();
      });
    CeQueueDto saved = underTest.selectByUuid(db.getSession(), uuid4).get();
    assertThat(saved.getUuid()).isEqualTo(uuid4);
    assertThat(saved.getTaskType()).isEqualTo(CeTaskTypes.REPORT);
    assertThat(saved.getComponentUuid()).isEqualTo(MAIN_COMPONENT_UUID_1);
    assertThat(saved.getStatus()).isEqualTo(PENDING);
    assertThat(saved.getSubmitterUuid()).isEqualTo(SUBMITTER_LOGIN);
    assertThat(saved.getWorkerUuid()).isNull();
    assertThat(saved.getCreatedAt()).isEqualTo(6_888_777L);
    assertThat(saved.getUpdatedAt()).isEqualTo(8_000_999L);
    assertThat(saved.getStartedAt()).isNull();
  }

  @Test
  public void test_selectByUuid() {
    CeQueueDto ceQueueDto = insertPending(TASK_UUID_1, MAIN_COMPONENT_UUID_1);

    assertThat(underTest.selectByUuid(db.getSession(), "TASK_UNKNOWN").isPresent()).isFalse();
    CeQueueDto saved = underTest.selectByUuid(db.getSession(), TASK_UUID_1).get();
    assertThat(saved.getUuid()).isEqualTo(TASK_UUID_1);
    assertThat(saved.getTaskType()).isEqualTo(CeTaskTypes.REPORT);
    assertThat(saved.getMainComponentUuid()).isEqualTo(MAIN_COMPONENT_UUID_1);
    assertThat(saved.getComponentUuid()).isEqualTo(ceQueueDto.getComponentUuid());
    assertThat(saved.getStatus()).isEqualTo(PENDING);
    assertThat(saved.getSubmitterUuid()).isEqualTo("henri");
    assertThat(saved.getWorkerUuid()).isNull();
    assertThat(saved.getCreatedAt()).isEqualTo(INIT_TIME);
    assertThat(saved.getUpdatedAt()).isEqualTo(INIT_TIME);
    assertThat(saved.getStartedAt()).isNull();
  }

  @Test
  public void test_selectByMainComponentUuid() {
    insertPending(TASK_UUID_1, MAIN_COMPONENT_UUID_1);
    insertPending(TASK_UUID_2, MAIN_COMPONENT_UUID_1);
    insertPending(TASK_UUID_3, "PROJECT_2");

    assertThat(underTest.selectByMainComponentUuid(db.getSession(), "UNKNOWN")).isEmpty();
    assertThat(underTest.selectByMainComponentUuid(db.getSession(), MAIN_COMPONENT_UUID_1)).extracting("uuid").containsOnly(TASK_UUID_1, TASK_UUID_2);
    assertThat(underTest.selectByMainComponentUuid(db.getSession(), "PROJECT_2")).extracting("uuid").containsOnly(TASK_UUID_3);
  }

  @Test
  public void test_selectAllInAscOrder() {
    insertPending(TASK_UUID_1, MAIN_COMPONENT_UUID_1);
    insertPending(TASK_UUID_2, MAIN_COMPONENT_UUID_1);
    insertPending(TASK_UUID_3, "PROJECT_2");

    assertThat(underTest.selectAllInAscOrder(db.getSession())).extracting("uuid").containsOnly(TASK_UUID_1, TASK_UUID_2, TASK_UUID_3);
  }

  @Test
  public void selectPending_returns_pending_tasks() {
    insertPending("p1");
    insertPending("p2");
    insertPending("p3");
    makeInProgress("w1", alwaysIncreasingSystem2.now(), insertPending("i1"));
    makeInProgress("w1", alwaysIncreasingSystem2.now(), insertPending("i2"));
    makeInProgress("w1", alwaysIncreasingSystem2.now(), insertPending("i3"));

    assertThat(underTest.selectPending(db.getSession()))
      .extracting(CeQueueDto::getUuid)
      .containsOnly("p1", "p2", "p3");
  }

  @Test
  public void selectCreationDateOfOldestPendingByMainComponentUuid_on_any_component_returns_date() {
    long time = alwaysIncreasingSystem2.now() + 10_000;
    insertPending("p1", dto -> {
      dto.setCreatedAt(time);
      dto.setUpdatedAt(time + 500);
      dto.setMainComponentUuid("c1");
    });
    insertPending("p2", dto -> {
      dto.setCreatedAt(time + 1000);
      dto.setUpdatedAt(time + 2000);
      dto.setMainComponentUuid("c2");
    });

    makeInProgress("w1", alwaysIncreasingSystem2.now(), insertPending("i1", dto -> dto.setMainComponentUuid("c3")));

    assertThat(underTest.selectCreationDateOfOldestPendingByMainComponentUuid(db.getSession(), null))
      .isEqualTo(Optional.of(time));
  }

  @Test
  public void selectCreationDateOfOldestPendingByMainComponentUuid_on_specific_component_returns_date() {
    long time = alwaysIncreasingSystem2.now() + 10_000;
    insertPending("p1", dto -> {
      dto.setCreatedAt(time);
      dto.setUpdatedAt(time + 500);
      dto.setMainComponentUuid("c2");
    });
    insertPending("p2", dto -> {
      dto.setCreatedAt(time + 2000);
      dto.setUpdatedAt(time + 3000);
      dto.setMainComponentUuid("c1");
    });
    insertPending("p3", dto -> {
      dto.setCreatedAt(time + 4000);
      dto.setUpdatedAt(time + 5000);
      dto.setMainComponentUuid("c1");
    });

    makeInProgress("w1", alwaysIncreasingSystem2.now(), insertPending("i1", dto -> dto.setMainComponentUuid("c1")));

    assertThat(underTest.selectCreationDateOfOldestPendingByMainComponentUuid(db.getSession(), "c1"))
      .isEqualTo(Optional.of(time + 2000));
  }

  @Test
  public void selectCreationDateOfOldestPendingByMainComponentUuid_returns_empty_when_no_pending_tasks() {
    makeInProgress("w1", alwaysIncreasingSystem2.now(), insertPending("i1"));
    assertThat(underTest.selectCreationDateOfOldestPendingByMainComponentUuid(db.getSession(), null))
      .isEmpty();
  }

  @Test
  public void selectWornout_returns_task_pending_with_a_non_null_startedAt() {
    insertPending("p1");
    makeInProgress("w1", alwaysIncreasingSystem2.now(), insertPending("i1"));
    CeQueueDto resetDto = makeInProgress("w1", alwaysIncreasingSystem2.now(), insertPending("i2"));
    makeInProgress("w1", alwaysIncreasingSystem2.now(), insertPending("i3"));
    reset(db.getSession(), alwaysIncreasingSystem2.now(), resetDto);

    List<CeQueueDto> ceQueueDtos = underTest.selectWornout(db.getSession());
    assertThat(ceQueueDtos)
      .extracting(CeQueueDto::getStatus, CeQueueDto::getUuid)
      .containsOnly(tuple(PENDING, resetDto.getUuid()));
  }

  @Test
  public void test_delete() {
    insertPending(TASK_UUID_1, MAIN_COMPONENT_UUID_1);
    insertPending(TASK_UUID_2, MAIN_COMPONENT_UUID_1);

    int deletedCount = underTest.deleteByUuid(db.getSession(), "UNKNOWN");
    assertThat(deletedCount).isEqualTo(0);
    assertThat(underTest.selectByUuid(db.getSession(), TASK_UUID_1)).isPresent();

    deletedCount = underTest.deleteByUuid(db.getSession(), TASK_UUID_1);
    assertThat(deletedCount).isEqualTo(1);
    assertThat(underTest.selectByUuid(db.getSession(), TASK_UUID_1)).isEmpty();

    deletedCount = underTest.deleteByUuid(db.getSession(), TASK_UUID_2, null);
    assertThat(deletedCount).isEqualTo(1);
    assertThat(underTest.selectByUuid(db.getSession(), TASK_UUID_2)).isEmpty();
  }

  @Test
  public void test_delete_with_expected_status() {
    insertPending(TASK_UUID_1, MAIN_COMPONENT_UUID_1);
    insertInProgress(TASK_UUID_2, MAIN_COMPONENT_UUID_1);

    int deletedCount = underTest.deleteByUuid(db.getSession(), "UNKNOWN", null);
    assertThat(deletedCount).isEqualTo(0);
    assertThat(underTest.selectByUuid(db.getSession(), TASK_UUID_1)).isPresent();

    deletedCount = underTest.deleteByUuid(db.getSession(), TASK_UUID_1, new DeleteIf(IN_PROGRESS));
    assertThat(deletedCount).isEqualTo(0);
    assertThat(underTest.selectByUuid(db.getSession(), TASK_UUID_1)).isPresent();

    deletedCount = underTest.deleteByUuid(db.getSession(), TASK_UUID_2, new DeleteIf(PENDING));
    assertThat(deletedCount).isEqualTo(0);
    assertThat(underTest.selectByUuid(db.getSession(), TASK_UUID_2)).isPresent();

    deletedCount = underTest.deleteByUuid(db.getSession(), TASK_UUID_1, new DeleteIf(PENDING));
    assertThat(deletedCount).isEqualTo(1);
    assertThat(underTest.selectByUuid(db.getSession(), TASK_UUID_1)).isEmpty();

    deletedCount = underTest.deleteByUuid(db.getSession(), TASK_UUID_2, new DeleteIf(IN_PROGRESS));
    assertThat(deletedCount).isEqualTo(1);
    assertThat(underTest.selectByUuid(db.getSession(), TASK_UUID_2)).isEmpty();
  }

  @Test
  public void resetToPendingForWorker_resets_status_of_non_pending_tasks_only_for_specified_workerUuid() {
    CeQueueDto[] worker1 = {insertPending("u1"), insertPending("u2"), insertPending("u3"), insertPending("u4")};
    CeQueueDto[] worker2 = {insertPending("o1"), insertPending("o2"), insertPending("o3"), insertPending("o4")};
    long startedAt = alwaysIncreasingSystem2.now();
    makeInProgress(WORKER_UUID_1, startedAt, worker1[0]);
    makeInProgress(WORKER_UUID_1, startedAt, worker1[3]);
    makeInProgress(WORKER_UUID_2, startedAt, worker2[0]);
    makeInProgress(WORKER_UUID_2, startedAt, worker2[3]);

    underTestAlwaysIncreasingSystem2.resetToPendingForWorker(db.getSession(), WORKER_UUID_1);

    verifyResetToPendingForWorker(worker1[0], WORKER_UUID_1, startedAt);
    verifyUnchangedByResetToPendingForWorker(worker1[1]);
    verifyUnchangedByResetToPendingForWorker(worker1[2]);
    verifyResetToPendingForWorker(worker1[3], WORKER_UUID_1, startedAt);
    verifyInProgressUnchangedByResetToPendingForWorker(worker2[0], WORKER_UUID_2, startedAt);
    verifyUnchangedByResetToPendingForWorker(worker2[1]);
    verifyUnchangedByResetToPendingForWorker(worker2[2]);
    verifyInProgressUnchangedByResetToPendingForWorker(worker2[3], WORKER_UUID_2, startedAt);
  }

  @Test
  public void resetTasksWithUnknownWorkerUUIDs_with_empty_set_resets_status_of_all_pending_tasks() {
    CeQueueDto[] worker1 = {insertPending("u1"), insertPending("u2"), insertPending("u3"), insertPending("u4")};
    CeQueueDto[] worker2 = {insertPending("o1"), insertPending("o2"), insertPending("o3"), insertPending("o4")};
    long startedAt = alwaysIncreasingSystem2.now();
    makeInProgress(WORKER_UUID_1, startedAt, worker1[0]);
    makeInProgress(WORKER_UUID_1, startedAt, worker1[3]);
    makeInProgress(WORKER_UUID_2, startedAt, worker2[0]);
    makeInProgress(WORKER_UUID_2, startedAt, worker2[3]);

    underTestAlwaysIncreasingSystem2.resetTasksWithUnknownWorkerUUIDs(db.getSession(), ImmutableSet.of());

    verifyResetByResetTasks(worker1[0], startedAt);
    verifyUnchangedByResetToPendingForWorker(worker1[1]);
    verifyUnchangedByResetToPendingForWorker(worker1[2]);
    verifyResetByResetTasks(worker1[3], startedAt);
    verifyResetByResetTasks(worker2[0], startedAt);
    verifyUnchangedByResetToPendingForWorker(worker2[1]);
    verifyUnchangedByResetToPendingForWorker(worker2[2]);
    verifyResetByResetTasks(worker2[3], startedAt);
  }

  @Test
  public void resetTasksWithUnknownWorkerUUIDs_set_resets_status_of_all_pending_tasks_with_unknown_workers() {
    CeQueueDto[] worker1 = {insertPending("u1"), insertPending("u2"), insertPending("u3"), insertPending("u4")};
    CeQueueDto[] worker2 = {insertPending("o1"), insertPending("o2"), insertPending("o3"), insertPending("o4")};
    long startedAt = alwaysIncreasingSystem2.now();
    makeInProgress(WORKER_UUID_1, startedAt, worker1[0]);
    makeInProgress(WORKER_UUID_1, startedAt, worker1[3]);
    makeInProgress(WORKER_UUID_2, startedAt, worker2[0]);
    makeInProgress(WORKER_UUID_2, startedAt, worker2[3]);

    underTestAlwaysIncreasingSystem2.resetTasksWithUnknownWorkerUUIDs(db.getSession(), ImmutableSet.of(WORKER_UUID_1, "unknown"));

    verifyInProgressUnchangedByResetToPendingForWorker(worker1[0], WORKER_UUID_1, startedAt);
    verifyUnchangedByResetToPendingForWorker(worker1[1]);
    verifyUnchangedByResetToPendingForWorker(worker1[2]);
    verifyInProgressUnchangedByResetToPendingForWorker(worker1[3], WORKER_UUID_1, startedAt);
    verifyResetByResetTasks(worker2[0], startedAt);
    verifyUnchangedByResetToPendingForWorker(worker2[1]);
    verifyUnchangedByResetToPendingForWorker(worker2[2]);
    verifyResetByResetTasks(worker2[3], startedAt);
  }

  private CeQueueDto makeInProgress(String workerUuid, long startedAt, CeQueueDto ceQueueDto) {
    CeQueueTesting.makeInProgress(db.getSession(), workerUuid, startedAt, ceQueueDto);
    return underTestAlwaysIncreasingSystem2.selectByUuid(db.getSession(), ceQueueDto.getUuid()).get();
  }

  private void verifyResetByResetTasks(CeQueueDto original, long startedAt) {
    CeQueueDto dto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), original.getUuid()).get();
    assertThat(dto.getStatus()).isEqualTo(PENDING);
    assertThat(dto.getStartedAt()).isEqualTo(startedAt);
    assertThat(dto.getCreatedAt()).isEqualTo(original.getCreatedAt());
    assertThat(dto.getUpdatedAt()).isGreaterThan(startedAt);
    assertThat(dto.getWorkerUuid()).isNull();
  }

  private void verifyResetToPendingForWorker(CeQueueDto original, String workerUuid, long startedAt) {
    CeQueueDto dto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), original.getUuid()).get();
    assertThat(dto.getStatus()).isEqualTo(PENDING);
    assertThat(dto.getStartedAt()).isEqualTo(startedAt);
    assertThat(dto.getCreatedAt()).isEqualTo(original.getCreatedAt());
    assertThat(dto.getUpdatedAt()).isGreaterThan(startedAt);
    assertThat(dto.getWorkerUuid()).isEqualTo(workerUuid);
  }

  private void verifyUnchangedByResetToPendingForWorker(CeQueueDto original) {
    CeQueueDto dto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), original.getUuid()).get();
    assertThat(dto.getStatus()).isEqualTo(original.getStatus());
    assertThat(dto.getStartedAt()).isEqualTo(original.getStartedAt());
    assertThat(dto.getCreatedAt()).isEqualTo(original.getCreatedAt());
    assertThat(dto.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
    assertThat(dto.getWorkerUuid()).isEqualTo(original.getWorkerUuid());
  }

  private void verifyInProgressUnchangedByResetToPendingForWorker(CeQueueDto original, String workerUuid, long startedAt) {
    CeQueueDto dto = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), original.getUuid()).get();
    assertThat(dto.getStatus()).isEqualTo(IN_PROGRESS);
    assertThat(dto.getStartedAt()).isEqualTo(startedAt);
    assertThat(dto.getCreatedAt()).isEqualTo(original.getCreatedAt());
    assertThat(dto.getUpdatedAt()).isEqualTo(startedAt);
    assertThat(dto.getWorkerUuid()).isEqualTo(workerUuid);
  }

  @Test
  public void peek_none_if_no_pendings() {
    assertThat(underTest.peek(db.getSession(), WORKER_UUID_1).isPresent()).isFalse();

    // not pending, but in progress
    makeInProgress(WORKER_UUID_1, 2_232_222L, insertPending(TASK_UUID_1, MAIN_COMPONENT_UUID_1));
    assertThat(underTest.peek(db.getSession(), WORKER_UUID_1).isPresent()).isFalse();
  }

  @Test
  public void peek_oldest_pending() {
    insertPending(TASK_UUID_1, MAIN_COMPONENT_UUID_1);
    system2.setNow(INIT_TIME + 3_000_000);
    insertPending(TASK_UUID_2, MAIN_COMPONENT_UUID_2);

    assertThat(db.countRowsOfTable("ce_queue")).isEqualTo(2);
    verifyCeQueueStatuses(TASK_UUID_1, PENDING, TASK_UUID_2, PENDING);

    // peek first one
    Optional<CeQueueDto> peek = underTest.peek(db.getSession(), WORKER_UUID_1);
    assertThat(peek).isPresent();
    assertThat(peek.get().getUuid()).isEqualTo(TASK_UUID_1);
    assertThat(peek.get().getStatus()).isEqualTo(IN_PROGRESS);
    assertThat(peek.get().getWorkerUuid()).isEqualTo(WORKER_UUID_1);
    verifyCeQueueStatuses(TASK_UUID_1, IN_PROGRESS, TASK_UUID_2, PENDING);

    // peek second one
    peek = underTest.peek(db.getSession(), WORKER_UUID_2);
    assertThat(peek).isPresent();
    assertThat(peek.get().getUuid()).isEqualTo(TASK_UUID_2);
    assertThat(peek.get().getStatus()).isEqualTo(IN_PROGRESS);
    assertThat(peek.get().getWorkerUuid()).isEqualTo(WORKER_UUID_2);
    verifyCeQueueStatuses(TASK_UUID_1, IN_PROGRESS, TASK_UUID_2, IN_PROGRESS);

    // no more pendings
    assertThat(underTest.peek(db.getSession(), WORKER_UUID_1).isPresent()).isFalse();
  }

  @Test
  public void do_not_peek_multiple_tasks_on_same_main_component_at_the_same_time() {
    // two pending tasks on the same project
    insertPending(TASK_UUID_1, MAIN_COMPONENT_UUID_1);
    system2.setNow(INIT_TIME + 3_000_000);
    insertPending(TASK_UUID_2, MAIN_COMPONENT_UUID_1);

    Optional<CeQueueDto> peek = underTest.peek(db.getSession(), WORKER_UUID_1);
    assertThat(peek).isPresent();
    assertThat(peek.get().getUuid()).isEqualTo(TASK_UUID_1);
    assertThat(peek.get().getMainComponentUuid()).isEqualTo(MAIN_COMPONENT_UUID_1);
    assertThat(peek.get().getWorkerUuid()).isEqualTo(WORKER_UUID_1);
    verifyCeQueueStatuses(TASK_UUID_1, IN_PROGRESS, TASK_UUID_2, PENDING);

    // do not peek second task as long as the first one is in progress
    peek = underTest.peek(db.getSession(), WORKER_UUID_1);
    assertThat(peek.isPresent()).isFalse();

    // first one is finished
    underTest.deleteByUuid(db.getSession(), TASK_UUID_1);
    peek = underTest.peek(db.getSession(), WORKER_UUID_2);
    assertThat(peek.get().getUuid()).isEqualTo(TASK_UUID_2);
    assertThat(peek.get().getWorkerUuid()).isEqualTo(WORKER_UUID_2);
  }

  @Test
  public void select_by_query() {
    // task status not in query
    insertPending(newCeQueueDto(TASK_UUID_1)
      .setMainComponentUuid(MAIN_COMPONENT_UUID_1)
      .setStatus(IN_PROGRESS)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(100_000L));

    // too early
    insertPending(newCeQueueDto(TASK_UUID_3)
      .setMainComponentUuid(MAIN_COMPONENT_UUID_1)
      .setStatus(PENDING)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(90_000L));

    // task type not in query
    insertPending(newCeQueueDto("TASK_4")
      .setMainComponentUuid("PROJECT_2")
      .setStatus(PENDING)
      .setTaskType("ANOTHER_TYPE")
      .setCreatedAt(100_000L));

    // correct
    insertPending(newCeQueueDto(TASK_UUID_2)
      .setMainComponentUuid(MAIN_COMPONENT_UUID_1)
      .setStatus(PENDING)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(100_000L));

    // correct submitted later
    insertPending(newCeQueueDto("TASK_5")
      .setMainComponentUuid(MAIN_COMPONENT_UUID_1)
      .setStatus(PENDING)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(120_000L));

    // select by component uuid, status, task type and minimum submitted at
    CeTaskQuery query = new CeTaskQuery()
      .setMainComponentUuids(newArrayList(MAIN_COMPONENT_UUID_1, "PROJECT_2"))
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
    insertPending(newCeQueueDto(TASK_UUID_1)
      .setComponentUuid(MAIN_COMPONENT_UUID_1)
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
    insertPending(newCeQueueDto(TASK_UUID_1)
      .setComponentUuid(MAIN_COMPONENT_UUID_1)
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
  public void select_by_query_returns_empty_list_when_empty_list_of_main_component_uuid() {
    insertPending(newCeQueueDto(TASK_UUID_1)
      .setComponentUuid(MAIN_COMPONENT_UUID_1)
      .setStatus(IN_PROGRESS)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(100_000L));

    CeTaskQuery query = new CeTaskQuery().setMainComponentUuids(Collections.emptyList());

    List<CeQueueDto> result = underTest.selectByQueryInDescOrder(db.getSession(), query, 1_000);
    int total = underTest.countByQuery(db.getSession(), query);

    assertThat(result).isEmpty();
    assertThat(total).isEqualTo(0);
  }

  @Test
  public void count_by_status_and_main_component_uuid() {
    // task retrieved in the queue
    insertPending(newCeQueueDto(TASK_UUID_1)
      .setMainComponentUuid(MAIN_COMPONENT_UUID_1)
      .setStatus(IN_PROGRESS)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(100_000L));
    // on component uuid 2, not returned
    insertPending(newCeQueueDto(TASK_UUID_2)
      .setMainComponentUuid(MAIN_COMPONENT_UUID_2)
      .setStatus(IN_PROGRESS)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(100_000L));
    // pending status, not returned
    insertPending(newCeQueueDto(TASK_UUID_3)
      .setMainComponentUuid(MAIN_COMPONENT_UUID_1)
      .setStatus(PENDING)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(100_000L));

    assertThat(underTest.countByStatusAndMainComponentUuid(db.getSession(), IN_PROGRESS, MAIN_COMPONENT_UUID_1)).isEqualTo(1);
    assertThat(underTest.countByStatus(db.getSession(), IN_PROGRESS)).isEqualTo(2);
  }

  @Test
  public void count_by_status_and_main_component_uuids() {
    // task retrieved in the queue
    insertPending(newCeQueueDto(TASK_UUID_1)
      .setMainComponentUuid(MAIN_COMPONENT_UUID_1)
      .setStatus(IN_PROGRESS)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(100_000L));
    // on component uuid 2, not returned
    insertPending(newCeQueueDto(TASK_UUID_2)
      .setMainComponentUuid(MAIN_COMPONENT_UUID_2)
      .setStatus(IN_PROGRESS)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(100_000L));
    // pending status, not returned
    insertPending(newCeQueueDto(TASK_UUID_3)
      .setMainComponentUuid(MAIN_COMPONENT_UUID_1)
      .setStatus(PENDING)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(100_000L));

    assertThat(underTest.countByStatusAndMainComponentUuids(db.getSession(), IN_PROGRESS, ImmutableSet.of())).isEmpty();
    assertThat(underTest.countByStatusAndMainComponentUuids(db.getSession(), IN_PROGRESS, ImmutableSet.of("non existing component uuid"))).isEmpty();
    assertThat(underTest.countByStatusAndMainComponentUuids(db.getSession(), IN_PROGRESS, ImmutableSet.of(MAIN_COMPONENT_UUID_1, MAIN_COMPONENT_UUID_2)))
      .containsOnly(entry(MAIN_COMPONENT_UUID_1, 1), entry(MAIN_COMPONENT_UUID_2, 1));
    assertThat(underTest.countByStatusAndMainComponentUuids(db.getSession(), PENDING, ImmutableSet.of(MAIN_COMPONENT_UUID_1, MAIN_COMPONENT_UUID_2)))
      .containsOnly(entry(MAIN_COMPONENT_UUID_1, 1));
    assertThat(underTest.countByStatus(db.getSession(), IN_PROGRESS)).isEqualTo(2);
  }

  @Test
  public void selectInProgressStartedBefore() {
    // pending task is ignored
    insertPending(newCeQueueDto(TASK_UUID_1)
      .setStatus(PENDING)
      .setStartedAt(1_000L));
    // in-progress tasks
    insertPending(newCeQueueDto(TASK_UUID_2)
      .setStatus(IN_PROGRESS)
      .setStartedAt(1_000L));
    insertPending(newCeQueueDto(TASK_UUID_3)
      .setStatus(IN_PROGRESS)
      .setStartedAt(2_000L));

    assertThat(underTest.selectInProgressStartedBefore(db.getSession(), 500L)).isEmpty();
    assertThat(underTest.selectInProgressStartedBefore(db.getSession(), 1_000L)).isEmpty();
    assertThat(underTest.selectInProgressStartedBefore(db.getSession(), 1_500L)).extracting(CeQueueDto::getUuid).containsExactly(TASK_UUID_2);
    assertThat(underTest.selectInProgressStartedBefore(db.getSession(), 3_000L)).extracting(CeQueueDto::getUuid).containsExactlyInAnyOrder(TASK_UUID_2, TASK_UUID_3);
  }

  private void insertPending(CeQueueDto dto) {
    underTest.insert(db.getSession(), dto);
    db.commit();
  }

  private CeQueueDto insertPending(String uuid) {
    return insertPending(uuid, (Consumer<CeQueueDto>) null);
  }

  private CeQueueDto insertPending(String uuid, @Nullable Consumer<CeQueueDto> dtoConsumer) {
    CeQueueDto dto = new CeQueueDto();
    dto.setUuid(uuid);
    dto.setTaskType(CeTaskTypes.REPORT);
    dto.setStatus(PENDING);
    dto.setSubmitterUuid("henri");
    if (dtoConsumer != null) {
      dtoConsumer.accept(dto);
    }
    underTestAlwaysIncreasingSystem2.insert(db.getSession(), dto);
    db.getSession().commit();
    return dto;
  }

  private int pendingComponentUuidGenerator = new Random().nextInt(200);

  private CeQueueDto insertPending(String uuid, String mainComponentUuid) {
    CeQueueDto dto = new CeQueueDto();
    dto.setUuid(uuid);
    dto.setTaskType(CeTaskTypes.REPORT);
    dto.setMainComponentUuid(mainComponentUuid);
    dto.setComponentUuid("uuid_" + pendingComponentUuidGenerator++);
    dto.setStatus(PENDING);
    dto.setSubmitterUuid("henri");
    underTest.insert(db.getSession(), dto);
    db.getSession().commit();
    return dto;
  }

  private CeQueueDto insertInProgress(String uuid, String componentUuid) {
    CeQueueDto ceQueueDto = insertPending(uuid);
    CeQueueTesting.makeInProgress(db.getSession(), "workerUuid", System2.INSTANCE.now(), ceQueueDto);
    return underTest.selectByUuid(db.getSession(), uuid).get();
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
