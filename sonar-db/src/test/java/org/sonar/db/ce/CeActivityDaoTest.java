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
import com.google.common.collect.FluentIterable;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.ce.CeActivityDto.Status.FAILED;
import static org.sonar.db.ce.CeActivityDto.Status.SUCCESS;
import static org.sonar.db.ce.CeTaskTypes.REPORT;

public class CeActivityDaoTest {

  TestSystem2 system2 = new TestSystem2().setNow(1_450_000_000_000L);

  @Rule
  public DbTester db = DbTester.create(system2);
  DbSession dbSession = db.getSession();

  CeActivityDao underTest = new CeActivityDao(system2);

  @Test
  public void test_insert() {
    insert("TASK_1", REPORT, "PROJECT_1", CeActivityDto.Status.SUCCESS);

    Optional<CeActivityDto> saved = underTest.selectByUuid(db.getSession(), "TASK_1");
    assertThat(saved.isPresent()).isTrue();
    assertThat(saved.get().getUuid()).isEqualTo("TASK_1");
    assertThat(saved.get().getComponentUuid()).isEqualTo("PROJECT_1");
    assertThat(saved.get().getStatus()).isEqualTo(CeActivityDto.Status.SUCCESS);
    assertThat(saved.get().getSubmitterLogin()).isEqualTo("henri");
    assertThat(saved.get().getIsLast()).isTrue();
    assertThat(saved.get().getIsLastKey()).isEqualTo("REPORTPROJECT_1");
    assertThat(saved.get().getSubmittedAt()).isEqualTo(1_300_000_000_000L);
    assertThat(saved.get().getCreatedAt()).isEqualTo(1_450_000_000_000L);
    assertThat(saved.get().getStartedAt()).isEqualTo(1_500_000_000_000L);
    assertThat(saved.get().getExecutedAt()).isEqualTo(1_500_000_000_500L);
    assertThat(saved.get().getExecutionTimeMs()).isEqualTo(500L);
    assertThat(saved.get().getSnapshotId()).isEqualTo(123_456);
    assertThat(saved.get().toString()).isNotEmpty();
  }

  @Test
  public void insert_must_set_relevant_is_last_field() {
    // only a single task on PROJECT_1 -> is_last=true
    insert("TASK_1", REPORT, "PROJECT_1", CeActivityDto.Status.SUCCESS);
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_1").get().getIsLast()).isTrue();

    // only a single task on PROJECT_2 -> is_last=true
    insert("TASK_2", REPORT, "PROJECT_2", CeActivityDto.Status.SUCCESS);
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_2").get().getIsLast()).isTrue();

    // two tasks on PROJECT_1, the most recent one is TASK_3
    insert("TASK_3", REPORT, "PROJECT_1", FAILED);
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_1").get().getIsLast()).isFalse();
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_2").get().getIsLast()).isTrue();
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_3").get().getIsLast()).isTrue();

    // inserting a cancelled task does not change the last task
    insert("TASK_4", REPORT, "PROJECT_1", CeActivityDto.Status.CANCELED);
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_1").get().getIsLast()).isFalse();
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_2").get().getIsLast()).isTrue();
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_3").get().getIsLast()).isTrue();
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_4").get().getIsLast()).isFalse();
  }

  @Test
  public void test_selectByQuery() {
    insert("TASK_1", REPORT, "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insert("TASK_2", REPORT, "PROJECT_1", FAILED);
    insert("TASK_3", REPORT, "PROJECT_2", CeActivityDto.Status.SUCCESS);
    insert("TASK_4", "views", null, CeActivityDto.Status.SUCCESS);

    // no filters
    CeTaskQuery query = new CeTaskQuery().setStatuses(Collections.<String>emptyList());
    List<CeActivityDto> dtos = underTest.selectByQuery(db.getSession(), query, 0, 10);
    assertThat(dtos).extracting("uuid").containsExactly("TASK_4", "TASK_3", "TASK_2", "TASK_1");

    // select by component uuid
    query = new CeTaskQuery().setComponentUuid("PROJECT_1");
    dtos = underTest.selectByQuery(db.getSession(), query, 0, 100);
    assertThat(dtos).extracting("uuid").containsExactly("TASK_2", "TASK_1");

    // select by status
    query = new CeTaskQuery().setStatuses(singletonList(CeActivityDto.Status.SUCCESS.name()));
    dtos = underTest.selectByQuery(db.getSession(), query, 0, 100);
    assertThat(dtos).extracting("uuid").containsExactly("TASK_4", "TASK_3", "TASK_1");

    // select by type
    query = new CeTaskQuery().setType(REPORT);
    dtos = underTest.selectByQuery(db.getSession(), query, 0, 100);
    assertThat(dtos).extracting("uuid").containsExactly("TASK_3", "TASK_2", "TASK_1");
    query = new CeTaskQuery().setType("views");
    dtos = underTest.selectByQuery(db.getSession(), query, 0, 100);
    assertThat(dtos).extracting("uuid").containsExactly("TASK_4");

    // select by multiple conditions
    query = new CeTaskQuery().setType(REPORT).setOnlyCurrents(true).setComponentUuid("PROJECT_1");
    dtos = underTest.selectByQuery(db.getSession(), query, 0, 100);
    assertThat(dtos).extracting("uuid").containsExactly("TASK_2");
  }

  @Test
  public void selectByQuery_is_paginated_and_return_results_sorted_from_last_to_first() {
    insert("TASK_1", REPORT, "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insert("TASK_2", REPORT, "PROJECT_1", CeActivityDto.Status.FAILED);
    insert("TASK_3", REPORT, "PROJECT_2", CeActivityDto.Status.SUCCESS);
    insert("TASK_4", "views", null, CeActivityDto.Status.SUCCESS);

    assertThat(selectPageOfUuids(0, 1)).containsExactly("TASK_4");
    assertThat(selectPageOfUuids(1, 1)).containsExactly("TASK_3");
    assertThat(selectPageOfUuids(0, 3)).containsExactly("TASK_4", "TASK_3", "TASK_2");
    assertThat(selectPageOfUuids(0, 4)).containsExactly("TASK_4", "TASK_3", "TASK_2", "TASK_1");
    assertThat(selectPageOfUuids(0, 100)).containsExactly("TASK_4", "TASK_3", "TASK_2", "TASK_1");
    assertThat(selectPageOfUuids(0, 0)).isEmpty();
    assertThat(selectPageOfUuids(10, 2)).isEmpty();
  }

  @Test
  public void selectByQuery_no_results_if_shortcircuited_by_component_uuids() {
    insert("TASK_1", REPORT, "PROJECT_1", CeActivityDto.Status.SUCCESS);

    CeTaskQuery query = new CeTaskQuery();
    query.setComponentUuids(Collections.<String>emptyList());
    assertThat(underTest.selectByQuery(db.getSession(), query, 0, 0)).isEmpty();
  }

  @Test
  public void select_and_count_by_date() {
    insertWithDates("UUID1", 1_450_000_000_000L, 1_470_000_000_000L);
    insertWithDates("UUID2", 1_460_000_000_000L, 1_480_000_000_000L);

    // search by min submitted date
    CeTaskQuery query = new CeTaskQuery().setMinSubmittedAt(1_455_000_000_000L);
    assertThat(underTest.selectByQuery(db.getSession(), query, 0, 5)).extracting("uuid").containsOnly("UUID2");

    // search by max executed date
    query = new CeTaskQuery().setMaxExecutedAt(1_475_000_000_000L);
    assertThat(underTest.selectByQuery(db.getSession(), query, 0, 5)).extracting("uuid").containsOnly("UUID1");

    // search by both dates
    query = new CeTaskQuery()
      .setMinSubmittedAt(1_400_000_000_000L)
      .setMaxExecutedAt(1_475_000_000_000L);
    assertThat(underTest.selectByQuery(db.getSession(), query, 0, 5)).extracting("uuid").containsOnly("UUID1");

  }

  private void insertWithDates(String uuid, long submittedAt, long executedAt) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setUuid(uuid);
    queueDto.setTaskType("fake");
    CeActivityDto dto = new CeActivityDto(queueDto);
    dto.setStatus(CeActivityDto.Status.SUCCESS);
    dto.setSubmittedAt(submittedAt);
    dto.setExecutedAt(executedAt);
    underTest.insert(db.getSession(), dto);
  }

  @Test
  public void selectOlderThan() {
    insertWithCreationDate("TASK_1", 1_450_000_000_000L);
    insertWithCreationDate("TASK_2", 1_460_000_000_000L);
    insertWithCreationDate("TASK_3", 1_470_000_000_000L);

    List<CeActivityDto> dtos = underTest.selectOlderThan(db.getSession(), 1_465_000_000_000L);
    assertThat(dtos).extracting("uuid").containsOnly("TASK_1", "TASK_2");
  }

  @Test
  public void deleteByUuid() {
    insert("TASK_1", "REPORT", "COMPONENT1", CeActivityDto.Status.SUCCESS);
    insert("TASK_2", "REPORT", "COMPONENT1", CeActivityDto.Status.SUCCESS);

    underTest.deleteByUuid(db.getSession(), "TASK_1");
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_1").isPresent()).isFalse();
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_2").isPresent()).isTrue();
  }

  @Test
  public void deleteByUuid_does_nothing_if_uuid_does_not_exist() {
    insert("TASK_1", "REPORT", "COMPONENT1", CeActivityDto.Status.SUCCESS);

    // must not fail
    underTest.deleteByUuid(db.getSession(), "TASK_2");

    assertThat(underTest.selectByUuid(db.getSession(), "TASK_1").isPresent()).isTrue();
  }

  @Test
  public void count_last_by_status_and_component_uuid() {
    insert("TASK_1", CeTaskTypes.REPORT, "COMPONENT1", CeActivityDto.Status.SUCCESS);
    // component 2
    insert("TASK_2", CeTaskTypes.REPORT, "COMPONENT2", CeActivityDto.Status.SUCCESS);
    // status failed
    insert("TASK_3", CeTaskTypes.REPORT, "COMPONENT1", CeActivityDto.Status.FAILED);
    // status canceled
    insert("TASK_4", CeTaskTypes.REPORT, "COMPONENT1", CeActivityDto.Status.CANCELED);
    insert("TASK_5", CeTaskTypes.REPORT, "COMPONENT1", CeActivityDto.Status.SUCCESS);
    db.commit();

    assertThat(underTest.countLastByStatusAndComponentUuid(dbSession, SUCCESS, "COMPONENT1")).isEqualTo(1);
    assertThat(underTest.countLastByStatusAndComponentUuid(dbSession, SUCCESS, null)).isEqualTo(2);
  }

  private void insert(String uuid, String type, String componentUuid, CeActivityDto.Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setUuid(uuid);
    queueDto.setTaskType(type);
    queueDto.setComponentUuid(componentUuid);
    queueDto.setSubmitterLogin("henri");
    queueDto.setCreatedAt(1_300_000_000_000L);

    CeActivityDto dto = new CeActivityDto(queueDto);
    dto.setStatus(status);
    dto.setStartedAt(1_500_000_000_000L);
    dto.setExecutedAt(1_500_000_000_500L);
    dto.setExecutionTimeMs(500L);
    dto.setSnapshotId(123_456L);
    underTest.insert(db.getSession(), dto);
  }

  private void insertWithCreationDate(String uuid, long date) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setUuid(uuid);
    queueDto.setTaskType("fake");

    CeActivityDto dto = new CeActivityDto(queueDto);
    dto.setStatus(CeActivityDto.Status.SUCCESS);
    system2.setNow(date);
    underTest.insert(db.getSession(), dto);
  }

  private List<String> selectPageOfUuids(int offset, int pageSize) {
    return FluentIterable.from(underTest.selectByQuery(db.getSession(), new CeTaskQuery(), offset, pageSize)).transform(CeActivityToUuid.INSTANCE).toList();
  }

  private enum CeActivityToUuid implements Function<CeActivityDto, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull CeActivityDto input) {
      return input.getUuid();
    }
  }
}
