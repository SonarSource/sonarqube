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

package org.sonar.db.ce;

import com.google.common.base.Optional;
import java.util.List;
import org.apache.ibatis.session.RowBounds;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.ce.CeTaskTypes.REPORT;

@Category(DbTests.class)
public class CeActivityDaoTest {

  TestSystem2 system2 = new TestSystem2().setNow(1_450_000_000_000L);

  @Rule
  public DbTester db = DbTester.create(system2);

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
    assertThat(saved.get().getFinishedAt()).isEqualTo(1_500_000_000_500L);
    assertThat(saved.get().getExecutionTimeMs()).isEqualTo(500L);
  }

  @Test
  public void insert_must_set_relevant_is_last_field() {
    // only a single task on PROJECT_1 -> is_last=true
    insert("TASK_1", REPORT, "PROJECT_1", CeActivityDto.Status.SUCCESS);
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_1").get().getIsLast()).isTrue();

    // only a single task on PROJECT_2 -> is_last=true
    insert("TASK_2", REPORT, "PROJECT_2", CeActivityDto.Status.SUCCESS);
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_2").get().getIsLast()).isTrue();

    // two tasks on PROJECT_1, the more recent one is TASK_3
    insert("TASK_3", REPORT, "PROJECT_1", CeActivityDto.Status.FAILED);
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_1").get().getIsLast()).isFalse();
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_2").get().getIsLast()).isTrue();
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_3").get().getIsLast()).isTrue();
  }

  @Test
  public void test_selectByQuery() throws Exception {
    insert("TASK_1", REPORT, "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insert("TASK_2", REPORT, "PROJECT_1", CeActivityDto.Status.FAILED);
    insert("TASK_3", REPORT, "PROJECT_2", CeActivityDto.Status.SUCCESS);
    insert("TASK_4", "views", null, CeActivityDto.Status.SUCCESS);

    // no filters
    CeActivityQuery query = new CeActivityQuery();
    List<CeActivityDto> dtos = underTest.selectByQuery(db.getSession(), query, new RowBounds(0, 10));
    assertThat(dtos).extracting("uuid").containsExactly("TASK_4", "TASK_3", "TASK_2", "TASK_1");

    // select by component uuid
    query = new CeActivityQuery().setComponentUuid("PROJECT_1");
    dtos = underTest.selectByQuery(db.getSession(), query, new RowBounds(0, 10));
    assertThat(dtos).extracting("uuid").containsExactly("TASK_2", "TASK_1");

    // select by status
    query = new CeActivityQuery().setStatus(CeActivityDto.Status.SUCCESS);
    dtos = underTest.selectByQuery(db.getSession(), query, new RowBounds(0, 10));
    assertThat(dtos).extracting("uuid").containsExactly("TASK_4", "TASK_3", "TASK_1");

    // select by type
    query = new CeActivityQuery().setType(REPORT);
    dtos = underTest.selectByQuery(db.getSession(), query, new RowBounds(0, 10));
    assertThat(dtos).extracting("uuid").containsExactly("TASK_3", "TASK_2", "TASK_1");
    query = new CeActivityQuery().setType("views");
    dtos = underTest.selectByQuery(db.getSession(), query, new RowBounds(0, 10));
    assertThat(dtos).extracting("uuid").containsExactly("TASK_4");

    // select by multiple conditions
    query = new CeActivityQuery().setType(REPORT).setOnlyCurrents(true).setComponentUuid("PROJECT_1");
    dtos = underTest.selectByQuery(db.getSession(), query, new RowBounds(0, 10));
    assertThat(dtos).extracting("uuid").containsExactly("TASK_2");
  }

  @Test
  public void test_countByQuery() throws Exception {
    insert("TASK_1", REPORT, "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insert("TASK_2", REPORT, "PROJECT_1", CeActivityDto.Status.FAILED);
    insert("TASK_3", REPORT, "PROJECT_2", CeActivityDto.Status.SUCCESS);
    insert("TASK_4", "views", null, CeActivityDto.Status.SUCCESS);

    // no filters
    CeActivityQuery query = new CeActivityQuery();
    assertThat(underTest.countByQuery(db.getSession(), query)).isEqualTo(4);

    // select by component uuid
    query = new CeActivityQuery().setComponentUuid("PROJECT_1");
    assertThat(underTest.countByQuery(db.getSession(), query)).isEqualTo(2);

    // select by status
    query = new CeActivityQuery().setStatus(CeActivityDto.Status.SUCCESS);
    assertThat(underTest.countByQuery(db.getSession(), query)).isEqualTo(3);

    // select by type
    query = new CeActivityQuery().setType(REPORT);
    assertThat(underTest.countByQuery(db.getSession(), query)).isEqualTo(3);
    query = new CeActivityQuery().setType("views");
    assertThat(underTest.countByQuery(db.getSession(), query)).isEqualTo(1);

    // select by multiple conditions
    query = new CeActivityQuery().setType(REPORT).setOnlyCurrents(true).setComponentUuid("PROJECT_1");
    assertThat(underTest.countByQuery(db.getSession(), query)).isEqualTo(1);
  }

  @Test
  public void select_and_count_by_date() throws Exception {
    insertWithDates("UUID1", 1_450_000_000_000L, 1_470_000_000_000L);
    insertWithDates("UUID2", 1_460_000_000_000L, 1_480_000_000_000L);

    // search by min submitted date
    CeActivityQuery query = new CeActivityQuery().setMinSubmittedAt(1_455_000_000_000L);
    assertThat(underTest.selectByQuery(db.getSession(), query, new RowBounds(0, 10))).extracting("uuid").containsOnly("UUID2");
    assertThat(underTest.countByQuery(db.getSession(), query)).isEqualTo(1);

    // search by max finished date
    query = new CeActivityQuery().setMaxFinishedAt(1_475_000_000_000L);
    assertThat(underTest.selectByQuery(db.getSession(), query, new RowBounds(0, 10))).extracting("uuid").containsOnly("UUID1");
    assertThat(underTest.countByQuery(db.getSession(), query)).isEqualTo(1);

    // search by both dates
    query = new CeActivityQuery()
      .setMinSubmittedAt(1_400_000_000_000L)
      .setMaxFinishedAt(1_475_000_000_000L);
    assertThat(underTest.selectByQuery(db.getSession(), query, new RowBounds(0, 10))).extracting("uuid").containsOnly("UUID1");
    assertThat(underTest.countByQuery(db.getSession(), query)).isEqualTo(1);

  }

  private void insertWithDates(String uuid, long submittedAt, long finishedAt) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setUuid(uuid);
    queueDto.setTaskType("fake");
    CeActivityDto dto = new CeActivityDto(queueDto);
    dto.setStatus(CeActivityDto.Status.SUCCESS);
    dto.setSubmittedAt(submittedAt);
    dto.setFinishedAt(finishedAt);
    underTest.insert(db.getSession(), dto);
  }

  @Test
  public void deleteOlderThan() throws Exception {
    insertWithCreationDate("TASK_1", 1_450_000_000_000L);
    insertWithCreationDate("TASK_2", 1_460_000_000_000L);
    insertWithCreationDate("TASK_3", 1_470_000_000_000L);

    underTest.deleteOlderThan(db.getSession(), 1_465_000_000_000L);
    db.getSession().commit();

    assertThat(underTest.selectByUuid(db.getSession(), "TASK_1").isPresent()).isFalse();
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_2").isPresent()).isFalse();
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_3").isPresent()).isTrue();
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
    dto.setFinishedAt(1_500_000_000_500L);
    dto.setExecutionTimeMs(500L);
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
}
