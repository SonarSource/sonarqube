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
package org.sonar.db.ce;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbTester;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.ce.CeQueueDto.Status.IN_PROGRESS;
import static org.sonar.db.ce.CeQueueDto.Status.PENDING;
import static org.sonar.db.ce.CeQueueTesting.newCeQueueDto;

public class CeQueueDaoTest {
  private static final long INIT_TIME = 1_450_000_000_000L;
  private static final String TASK_UUID_1 = "TASK_1";
  private static final String TASK_UUID_2 = "TASK_2";
  private static final String COMPONENT_UUID_1 = "PROJECT_1";
  private static final String COMPONENT_UUID_2 = "PROJECT_2";
  public static final String TASK_UUID_3 = "TASK_3";

  private TestSystem2 system2 = new TestSystem2().setNow(INIT_TIME);

  @Rule
  public DbTester db = DbTester.create(system2);

  private CeQueueDao underTest = new CeQueueDao(system2);
  private static final String SELECT_QUEUE_UUID_AND_STATUS_QUERY = "select uuid,status from ce_queue";

  @Test
  public void test_insert() {
    insert(TASK_UUID_1, COMPONENT_UUID_1, PENDING);

    Optional<CeQueueDto> saved = underTest.selectByUuid(db.getSession(), TASK_UUID_1);
    assertThat(saved.isPresent()).isTrue();
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
  public void test_delete() {
    insert(TASK_UUID_1, COMPONENT_UUID_1, PENDING);

    underTest.deleteByUuid(db.getSession(), "UNKNOWN");
    assertThat(underTest.selectByUuid(db.getSession(), TASK_UUID_1).isPresent()).isTrue();

    underTest.deleteByUuid(db.getSession(), TASK_UUID_1);
    assertThat(underTest.selectByUuid(db.getSession(), TASK_UUID_1).isPresent()).isFalse();
  }

  @Test
  public void test_resetAllToPendingStatus() throws Exception {
    insert(TASK_UUID_1, COMPONENT_UUID_1, PENDING);
    insert(TASK_UUID_2, COMPONENT_UUID_1, IN_PROGRESS);
    insert(TASK_UUID_3, COMPONENT_UUID_1, IN_PROGRESS);
    verifyCeQueueStatuses(TASK_UUID_1, PENDING, TASK_UUID_2, IN_PROGRESS, TASK_UUID_3, IN_PROGRESS);

    underTest.resetAllToPendingStatus(db.getSession());
    db.getSession().commit();

    verifyCeQueueStatuses(TASK_UUID_1, PENDING, TASK_UUID_2, PENDING, TASK_UUID_3, PENDING);
  }

  @Test
  public void peek_none_if_no_pendings() throws Exception {
    assertThat(underTest.peek(db.getSession()).isPresent()).isFalse();

    // not pending, but in progress
    insert(TASK_UUID_1, COMPONENT_UUID_1, IN_PROGRESS);
    assertThat(underTest.peek(db.getSession()).isPresent()).isFalse();
  }

  @Test
  public void peek_oldest_pending() throws Exception {
    insert(TASK_UUID_1, COMPONENT_UUID_1, PENDING);
    system2.setNow(INIT_TIME + 3_000_000);
    insert(TASK_UUID_2, COMPONENT_UUID_2, PENDING);

    assertThat(db.countRowsOfTable("ce_queue")).isEqualTo(2);
    verifyCeQueueStatuses(TASK_UUID_1, PENDING, TASK_UUID_2, PENDING);

    // peek first one
    Optional<CeQueueDto> peek = underTest.peek(db.getSession());
    assertThat(peek.isPresent()).isTrue();
    assertThat(peek.get().getUuid()).isEqualTo(TASK_UUID_1);
    assertThat(peek.get().getStatus()).isEqualTo(IN_PROGRESS);
    verifyCeQueueStatuses(TASK_UUID_1, IN_PROGRESS, TASK_UUID_2, PENDING);

    // peek second one
    peek = underTest.peek(db.getSession());
    assertThat(peek.isPresent()).isTrue();
    assertThat(peek.get().getUuid()).isEqualTo(TASK_UUID_2);
    assertThat(peek.get().getStatus()).isEqualTo(IN_PROGRESS);
    verifyCeQueueStatuses(TASK_UUID_1, IN_PROGRESS, TASK_UUID_2, IN_PROGRESS);

    // no more pendings
    assertThat(underTest.peek(db.getSession()).isPresent()).isFalse();
  }

  @Test
  public void do_not_peek_multiple_tasks_on_same_project_at_the_same_time() throws Exception {
    // two pending tasks on the same project
    insert(TASK_UUID_1, COMPONENT_UUID_1, PENDING);
    system2.setNow(INIT_TIME + 3_000_000);
    insert(TASK_UUID_2, COMPONENT_UUID_1, PENDING);

    Optional<CeQueueDto> peek = underTest.peek(db.getSession());
    assertThat(peek.isPresent()).isTrue();
    assertThat(peek.get().getUuid()).isEqualTo(TASK_UUID_1);
    verifyCeQueueStatuses(TASK_UUID_1, IN_PROGRESS, TASK_UUID_2, PENDING);

    // do not peek second task as long as the first one is in progress
    peek = underTest.peek(db.getSession());
    assertThat(peek.isPresent()).isFalse();

    // first one is finished
    underTest.deleteByUuid(db.getSession(), TASK_UUID_1);
    peek = underTest.peek(db.getSession());
    assertThat(peek.get().getUuid()).isEqualTo(TASK_UUID_2);
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

    CeTaskQuery query = new CeTaskQuery().setComponentUuids(Collections.<String>emptyList());

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

  private void insert(String uuid, String componentUuid, CeQueueDto.Status status) {
    CeQueueDto dto = new CeQueueDto();
    dto.setUuid(uuid);
    dto.setTaskType(CeTaskTypes.REPORT);
    dto.setComponentUuid(componentUuid);
    dto.setStatus(status);
    dto.setSubmitterLogin("henri");
    underTest.insert(db.getSession(), dto);
    db.getSession().commit();
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
    return ImmutableMap.<String, Object>of("UUID", uuid, "STATUS", status.name());
  }
}
