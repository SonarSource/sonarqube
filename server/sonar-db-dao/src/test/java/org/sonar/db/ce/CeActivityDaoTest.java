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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.Pagination;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.Pagination.forPage;
import static org.sonar.db.ce.CeActivityDto.Status.CANCELED;
import static org.sonar.db.ce.CeActivityDto.Status.FAILED;
import static org.sonar.db.ce.CeActivityDto.Status.SUCCESS;
import static org.sonar.db.ce.CeQueueDto.Status.PENDING;
import static org.sonar.db.ce.CeQueueTesting.makeInProgress;
import static org.sonar.db.ce.CeTaskTypes.REPORT;

@RunWith(DataProviderRunner.class)
public class CeActivityDaoTest {

  private static final String MAINCOMPONENT_1 = randomAlphabetic(12);
  private static final String MAINCOMPONENT_2 = randomAlphabetic(13);
  private static final String COMPONENT_1 = randomAlphabetic(14);

  private TestSystem2 system2 = new TestSystem2().setNow(1_450_000_000_000L);

  @Rule
  public DbTester db = DbTester.create(system2);

  private DbSession dbSession = db.getSession();
  private CeActivityDao underTest = new CeActivityDao(system2);

  @Test
  public void test_insert() {
    CeActivityDto inserted = insert("TASK_1", REPORT, COMPONENT_1, MAINCOMPONENT_1, SUCCESS);

    Optional<CeActivityDto> saved = underTest.selectByUuid(db.getSession(), "TASK_1");
    assertThat(saved).isPresent();
    CeActivityDto dto = saved.get();
    assertThat(dto.getUuid()).isEqualTo("TASK_1");
    assertThat(dto.getMainComponentUuid()).isEqualTo(MAINCOMPONENT_1);
    assertThat(dto.getComponentUuid()).isEqualTo(COMPONENT_1);
    assertThat(dto.getStatus()).isEqualTo(SUCCESS);
    assertThat(dto.getSubmitterUuid()).isEqualTo("submitter uuid");
    assertThat(dto.getSubmittedAt()).isEqualTo(1_450_000_000_000L);
    assertThat(dto.getWorkerUuid()).isEqualTo("worker uuid");
    assertThat(dto.getIsLast()).isTrue();
    assertThat(dto.getMainIsLast()).isTrue();
    assertThat(dto.getIsLastKey()).isEqualTo("REPORT" + COMPONENT_1);
    assertThat(dto.getMainIsLastKey()).isEqualTo("REPORT" + MAINCOMPONENT_1);
    assertThat(dto.getCreatedAt()).isEqualTo(1_450_000_000_000L);
    assertThat(dto.getStartedAt()).isEqualTo(1_500_000_000_000L);
    assertThat(dto.getExecutedAt()).isEqualTo(1_500_000_000_500L);
    assertThat(dto.getExecutionTimeMs()).isEqualTo(500L);
    assertThat(dto.getAnalysisUuid()).isEqualTo(inserted.getAnalysisUuid());
    assertThat(dto.toString()).isNotEmpty();
    assertThat(dto.getErrorMessage()).isNull();
    assertThat(dto.getErrorStacktrace()).isNull();
    assertThat(dto.getErrorType()).isNull();
    assertThat(dto.isHasScannerContext()).isFalse();
    assertThat(dto.getWarningCount()).isZero();
  }

  @Test
  public void selectByUuid_populates_warning_count() {
    CeActivityDto[] tasks = {
      insert("TASK_1", REPORT, "PROJECT_1", SUCCESS),
      insert("TASK_2", REPORT, "PROJECT_1", SUCCESS),
      insert("TASK_3", REPORT, "PROJECT_1", SUCCESS)
    };
    int moreThan1 = 2 + new Random().nextInt(5);
    insertWarnings(tasks[0], moreThan1);
    insertWarnings(tasks[1], 0);
    insertWarnings(tasks[2], 1);

    assertThat(underTest.selectByUuid(dbSession, tasks[0].getUuid()).get().getWarningCount()).isEqualTo(moreThan1);
    assertThat(underTest.selectByUuid(dbSession, tasks[1].getUuid()).get().getWarningCount()).isEqualTo(0);
    assertThat(underTest.selectByUuid(dbSession, tasks[2].getUuid()).get().getWarningCount()).isEqualTo(1);
  }

  private void insertWarnings(CeActivityDto task, int warningCount) {
    IntStream.range(0, warningCount).forEach(i -> db.getDbClient().ceTaskMessageDao().insert(dbSession,
      new CeTaskMessageDto()
        .setUuid(UuidFactoryFast.getInstance().create())
        .setTaskUuid(task.getUuid())
        .setMessage("message_" + task.getUuid() + "_" + i)
        .setCreatedAt(task.getUuid().hashCode() + i)));
    db.commit();
  }

  @Test
  @UseDataProvider("notCanceledStatus")
  public void insert_resets_is_last_and_main_is_last_fields_based_on_component_and_main_component(CeActivityDto.Status status) {
    String project1 = randomAlphabetic(5);
    String branch11 = randomAlphabetic(6);
    String project2 = randomAlphabetic(8);
    String branch21 = randomAlphabetic(9);
    String type = randomAlphabetic(10);

    String task1Project1 = insertAndCommit(newUuid(), type, project1, project1, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(task1Project1).containsOnly(tuple(true, true));

    String task2Project1 = insertAndCommit(newUuid(), type, project1, project1, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(task1Project1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task2Project1).containsOnly(tuple(true, true));

    String task1Branch11 = insertAndCommit(newUuid(), type, branch11, project1, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(task1Project1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task2Project1).containsOnly(tuple(true, false));
    assertIsLastAndMainIsLastFieldsOf(task1Branch11).containsOnly(tuple(true, true));

    String task2Branch11 = insertAndCommit(newUuid(), type, branch11, project1, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(task1Project1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task2Project1).containsOnly(tuple(true, false));
    assertIsLastAndMainIsLastFieldsOf(task1Branch11).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task2Branch11).containsOnly(tuple(true, true));

    String task1Project2 = insertAndCommit(newUuid(), type, project2, project2, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(task1Project1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task2Project1).containsOnly(tuple(true, false));
    assertIsLastAndMainIsLastFieldsOf(task1Branch11).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task2Branch11).containsOnly(tuple(true, true));
    assertIsLastAndMainIsLastFieldsOf(task1Project2).containsOnly(tuple(true, true));

    String task2Project2 = insertAndCommit(newUuid(), type, project2, project2, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(task1Project1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task2Project1).containsOnly(tuple(true, false));
    assertIsLastAndMainIsLastFieldsOf(task1Branch11).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task2Branch11).containsOnly(tuple(true, true));
    assertIsLastAndMainIsLastFieldsOf(task1Project2).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task2Project2).containsOnly(tuple(true, true));

    String task1Branch21 = insertAndCommit(newUuid(), type, branch21, project2, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(task1Project1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task2Project1).containsOnly(tuple(true, false));
    assertIsLastAndMainIsLastFieldsOf(task1Branch11).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task2Branch11).containsOnly(tuple(true, true));
    assertIsLastAndMainIsLastFieldsOf(task1Project2).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task2Project2).containsOnly(tuple(true, false));
    assertIsLastAndMainIsLastFieldsOf(task1Branch21).containsOnly(tuple(true, true));

    String task3project1 = insertAndCommit(newUuid(), type, project1, project1, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(task1Project1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task2Project1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task1Branch11).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task2Branch11).containsOnly(tuple(true, false));
    assertIsLastAndMainIsLastFieldsOf(task1Project2).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task2Project2).containsOnly(tuple(true, false));
    assertIsLastAndMainIsLastFieldsOf(task1Branch21).containsOnly(tuple(true, true));
    assertIsLastAndMainIsLastFieldsOf(task3project1).containsOnly(tuple(true, true));
  }

  @Test
  @UseDataProvider("notCanceledStatus")
  public void insert_resets_is_last_and_main_is_last_fields_based_on_type(CeActivityDto.Status status) {
    String type1 = randomAlphabetic(10);
    String type2 = randomAlphabetic(11);
    String project = randomAlphabetic(5);
    String branch = randomAlphabetic(6);

    String type1Project1 = insertAndCommit(newUuid(), type1, project, project, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(type1Project1).containsOnly(tuple(true, true));

    String type2Project1 = insertAndCommit(newUuid(), type2, project, project, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(type1Project1).containsOnly(tuple(true, true));
    assertIsLastAndMainIsLastFieldsOf(type2Project1).containsOnly(tuple(true, true));

    String type2Project2 = insertAndCommit(newUuid(), type2, project, project, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(type1Project1).containsOnly(tuple(true, true));
    assertIsLastAndMainIsLastFieldsOf(type2Project1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(type2Project2).containsOnly(tuple(true, true));

    String type1Branch1 = insertAndCommit(newUuid(), type1, branch, project, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(type1Project1).containsOnly(tuple(true, false));
    assertIsLastAndMainIsLastFieldsOf(type2Project1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(type2Project2).containsOnly(tuple(true, true));
    assertIsLastAndMainIsLastFieldsOf(type1Branch1).containsOnly(tuple(true, true));

    String type2Branch1 = insertAndCommit(newUuid(), type2, branch, project, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(type1Project1).containsOnly(tuple(true, false));
    assertIsLastAndMainIsLastFieldsOf(type2Project1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(type2Project2).containsOnly(tuple(true, false));
    assertIsLastAndMainIsLastFieldsOf(type1Branch1).containsOnly(tuple(true, true));
    assertIsLastAndMainIsLastFieldsOf(type2Branch1).containsOnly(tuple(true, true));

    String type2Branch2 = insertAndCommit(newUuid(), type2, branch, project, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(type1Project1).containsOnly(tuple(true, false));
    assertIsLastAndMainIsLastFieldsOf(type2Project1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(type2Project2).containsOnly(tuple(true, false));
    assertIsLastAndMainIsLastFieldsOf(type1Branch1).containsOnly(tuple(true, true));
    assertIsLastAndMainIsLastFieldsOf(type2Branch1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(type2Branch2).containsOnly(tuple(true, true));
  }

  @Test
  @UseDataProvider("notCanceledStatus")
  public void insert_resets_is_last_and_main_is_last_fields_based_on_component_or_not(CeActivityDto.Status status) {
    String project = randomAlphabetic(5);
    String type1 = randomAlphabetic(11);
    String type2 = randomAlphabetic(11);

    String type1Project1 = insertAndCommit(newUuid(), type1, project, project, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(type1Project1).containsOnly(tuple(true, true));

    String type1NoProject1 = insertAndCommit(newUuid(), type1, null, null, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(type1Project1).containsOnly(tuple(true, true));
    assertIsLastAndMainIsLastFieldsOf(type1NoProject1).containsOnly(tuple(true, true));

    String type1NoProject2 = insertAndCommit(newUuid(), type1, null, null, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(type1Project1).containsOnly(tuple(true, true));
    assertIsLastAndMainIsLastFieldsOf(type1NoProject1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(type1NoProject2).containsOnly(tuple(true, true));

    String type2NoProject1 = insertAndCommit(newUuid(), type2, null, null, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(type1Project1).containsOnly(tuple(true, true));
    assertIsLastAndMainIsLastFieldsOf(type1NoProject1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(type1NoProject2).containsOnly(tuple(true, true));
    assertIsLastAndMainIsLastFieldsOf(type2NoProject1).containsOnly(tuple(true, true));

    String type2NoProject2 = insertAndCommit(newUuid(), type2, null, null, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(type1Project1).containsOnly(tuple(true, true));
    assertIsLastAndMainIsLastFieldsOf(type1NoProject1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(type1NoProject2).containsOnly(tuple(true, true));
    assertIsLastAndMainIsLastFieldsOf(type2NoProject1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(type2NoProject2).containsOnly(tuple(true, true));

    String type1Project2 = insertAndCommit(newUuid(), type1, project, project, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(type1Project1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(type1NoProject1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(type1NoProject2).containsOnly(tuple(true, true));
    assertIsLastAndMainIsLastFieldsOf(type2NoProject1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(type2NoProject2).containsOnly(tuple(true, true));
    assertIsLastAndMainIsLastFieldsOf(type1Project2).containsOnly(tuple(true, true));
  }

  @Test
  @UseDataProvider("notCanceledStatus")
  public void insert_does_not_resets_is_last_and_main_is_last_fields_if_status_is_CANCELED(CeActivityDto.Status status) {
    String project = randomAlphabetic(5);
    String branch = randomAlphabetic(6);
    String type = randomAlphabetic(10);

    String task1Project1 = insertAndCommit(newUuid(), type, project, project, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(task1Project1).containsOnly(tuple(true, true));

    String task1Project2 = insertAndCommit(newUuid(), type, project, project, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(task1Project1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task1Project2).containsOnly(tuple(true, true));

    String task1Project3 = insertAndCommit(newUuid(), type, project, project, CANCELED).getUuid();
    assertIsLastAndMainIsLastFieldsOf(task1Project1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task1Project2).containsOnly(tuple(true, true));
    assertIsLastAndMainIsLastFieldsOf(task1Project3).containsOnly(tuple(false, false));

    String task1Branch1 = insertAndCommit(newUuid(), type, branch, project, status).getUuid();
    assertIsLastAndMainIsLastFieldsOf(task1Project1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task1Project2).containsOnly(tuple(true, false));
    assertIsLastAndMainIsLastFieldsOf(task1Project3).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task1Branch1).containsOnly(tuple(true, true));

    String task1Branch2 = insertAndCommit(newUuid(), type, branch, project, CANCELED).getUuid();
    assertIsLastAndMainIsLastFieldsOf(task1Project1).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task1Project2).containsOnly(tuple(true, false));
    assertIsLastAndMainIsLastFieldsOf(task1Project3).containsOnly(tuple(false, false));
    assertIsLastAndMainIsLastFieldsOf(task1Branch1).containsOnly(tuple(true, true));
    assertIsLastAndMainIsLastFieldsOf(task1Branch2).containsOnly(tuple(false, false));
  }

  @DataProvider
  public static Object[][] notCanceledStatus() {
    return Arrays.stream(CeActivityDto.Status.values())
      .filter(t -> t != CANCELED)
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  private AbstractListAssert<?, List<? extends Tuple>, Tuple, ObjectAssert<Tuple>> assertIsLastAndMainIsLastFieldsOf(String taskUuid) {
    return assertThat(db.select("select is_last as \"IS_LAST\", main_is_last as \"MAIN_IS_LAST\" from ce_activity where uuid='" + taskUuid + "'"))
      .extracting(t -> toBoolean(t.get("IS_LAST")), t -> toBoolean(t.get("MAIN_IS_LAST")));
  }

  private static boolean toBoolean(Object o) {
    if (o instanceof Boolean) {
      return (Boolean) o;
    }
    if (o instanceof Long) {
      Long longBoolean = (Long) o;
      return longBoolean.equals(1L);
    }
    throw new IllegalArgumentException("Unsupported object type returned for boolean: " + o.getClass());
  }

  @Test
  public void test_insert_of_errorMessage_of_1_000_chars() {
    CeActivityDto dto = createActivityDto("TASK_1", REPORT, COMPONENT_1, MAINCOMPONENT_1, FAILED)
      .setErrorMessage(Strings.repeat("x", 1_000));
    underTest.insert(db.getSession(), dto);

    Optional<CeActivityDto> saved = underTest.selectByUuid(db.getSession(), "TASK_1");
    assertThat(saved.get().getErrorMessage()).isEqualTo(dto.getErrorMessage());
  }

  @Test
  public void test_insert_of_errorMessage_of_1_001_chars_is_truncated_to_1000() {
    String expected = Strings.repeat("x", 1_000);
    CeActivityDto dto = createActivityDto("TASK_1", REPORT, COMPONENT_1, MAINCOMPONENT_1, FAILED)
      .setErrorMessage(expected + "y");
    underTest.insert(db.getSession(), dto);

    Optional<CeActivityDto> saved = underTest.selectByUuid(db.getSession(), "TASK_1");
    assertThat(saved.get().getErrorMessage()).isEqualTo(expected);
  }

  @Test
  public void test_insert_error_message_and_stacktrace() {
    CeActivityDto dto = createActivityDto("TASK_1", REPORT, COMPONENT_1, MAINCOMPONENT_1, FAILED)
      .setErrorStacktrace("error stack");
    underTest.insert(db.getSession(), dto);

    Optional<CeActivityDto> saved = underTest.selectByUuid(db.getSession(), "TASK_1");
    CeActivityDto read = saved.get();
    assertThat(read.getErrorMessage()).isEqualTo(dto.getErrorMessage());
    assertThat(read.getErrorStacktrace()).isEqualTo(dto.getErrorStacktrace());
    assertThat(read.getErrorType()).isNotNull().isEqualTo(dto.getErrorType());
  }

  @Test
  public void test_insert_error_message_only() {
    CeActivityDto dto = createActivityDto("TASK_1", REPORT, COMPONENT_1, MAINCOMPONENT_1, FAILED);
    underTest.insert(db.getSession(), dto);

    Optional<CeActivityDto> saved = underTest.selectByUuid(db.getSession(), "TASK_1");
    CeActivityDto read = saved.get();
    assertThat(read.getErrorMessage()).isEqualTo(read.getErrorMessage());
    assertThat(read.getErrorStacktrace()).isNull();
  }

  @Test
  public void insert_must_set_relevant_is_last_field() {
    // only a single task on MAINCOMPONENT_1 -> is_last=true
    insert("TASK_1", REPORT, MAINCOMPONENT_1, SUCCESS);
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_1").get().getIsLast()).isTrue();

    // only a single task on MAINCOMPONENT_2 -> is_last=true
    insert("TASK_2", REPORT, MAINCOMPONENT_2, SUCCESS);
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_2").get().getIsLast()).isTrue();

    // two tasks on MAINCOMPONENT_1, the most recent one is TASK_3
    insert("TASK_3", REPORT, MAINCOMPONENT_1, FAILED);
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_1").get().getIsLast()).isFalse();
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_2").get().getIsLast()).isTrue();
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_3").get().getIsLast()).isTrue();

    // inserting a cancelled task does not change the last task
    insert("TASK_4", REPORT, MAINCOMPONENT_1, CANCELED);
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_1").get().getIsLast()).isFalse();
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_2").get().getIsLast()).isTrue();
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_3").get().getIsLast()).isTrue();
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_4").get().getIsLast()).isFalse();
  }

  @Test
  public void test_selectByQuery() {
    insert("TASK_1", REPORT, MAINCOMPONENT_1, SUCCESS);
    insert("TASK_2", REPORT, MAINCOMPONENT_1, FAILED);
    insert("TASK_3", REPORT, MAINCOMPONENT_2, SUCCESS);
    insert("TASK_4", "views", null, SUCCESS);

    // no filters
    CeTaskQuery query = new CeTaskQuery().setStatuses(Collections.emptyList());
    List<CeActivityDto> dtos = underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(10));
    assertThat(dtos).extracting("uuid").containsExactly("TASK_4", "TASK_3", "TASK_2", "TASK_1");

    // select by component uuid
    query = new CeTaskQuery().setMainComponentUuid(MAINCOMPONENT_1);
    dtos = underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(100));
    assertThat(dtos).extracting("uuid").containsExactly("TASK_2", "TASK_1");

    // select by status
    query = new CeTaskQuery().setStatuses(singletonList(SUCCESS.name()));
    dtos = underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(100));
    assertThat(dtos).extracting("uuid").containsExactly("TASK_4", "TASK_3", "TASK_1");

    // select by type
    query = new CeTaskQuery().setType(REPORT);
    dtos = underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(100));
    assertThat(dtos).extracting("uuid").containsExactly("TASK_3", "TASK_2", "TASK_1");
    query = new CeTaskQuery().setType("views");
    dtos = underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(100));
    assertThat(dtos).extracting("uuid").containsExactly("TASK_4");

    // select by multiple conditions
    query = new CeTaskQuery().setType(REPORT).setOnlyCurrents(true).setMainComponentUuid(MAINCOMPONENT_1);
    dtos = underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(100));
    assertThat(dtos).extracting("uuid").containsExactly("TASK_2");
  }

  @Test
  public void selectByQuery_does_not_populate_errorStacktrace_field() {
    insert("TASK_1", REPORT, MAINCOMPONENT_1, FAILED);
    underTest.insert(db.getSession(), createActivityDto("TASK_2", REPORT, COMPONENT_1, MAINCOMPONENT_1, FAILED).setErrorStacktrace("some stack"));

    List<CeActivityDto> dtos = underTest.selectByQuery(db.getSession(), new CeTaskQuery().setMainComponentUuid(MAINCOMPONENT_1), forPage(1).andSize(100));

    assertThat(dtos)
      .hasSize(2)
      .extracting("errorStacktrace").containsOnly((String) null);
  }

  @Test
  public void selectByQuery_populates_hasScannerContext_flag() {
    insert("TASK_1", REPORT, MAINCOMPONENT_1, SUCCESS);
    CeActivityDto dto2 = insert("TASK_2", REPORT, MAINCOMPONENT_2, SUCCESS);
    insertScannerContext(dto2.getUuid());

    CeActivityDto dto = underTest.selectByQuery(db.getSession(), new CeTaskQuery().setMainComponentUuid(MAINCOMPONENT_1), forPage(1).andSize(100)).iterator().next();
    assertThat(dto.isHasScannerContext()).isFalse();
    dto = underTest.selectByQuery(db.getSession(), new CeTaskQuery().setMainComponentUuid(MAINCOMPONENT_2), forPage(1).andSize(100)).iterator().next();
    assertThat(dto.isHasScannerContext()).isTrue();
  }

  @Test
  public void selectByQuery_populates_warningCount() {
    CeActivityDto[] tasks = {
      insert("TASK_1", REPORT, "PROJECT_1", SUCCESS),
      insert("TASK_2", REPORT, "PROJECT_1", FAILED),
      insert("TASK_3", REPORT, "PROJECT_2", SUCCESS),
      insert("TASK_4", "views", null, SUCCESS)
    };
    int moreThan1 = 2 + new Random().nextInt(5);
    insertWarnings(tasks[0], moreThan1);
    insertWarnings(tasks[1], 3);
    insertWarnings(tasks[2], 1);
    insertWarnings(tasks[3], 6);

    // no filters
    CeTaskQuery query = new CeTaskQuery().setStatuses(Collections.emptyList());
    assertThat(underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(10)))
      .extracting(CeActivityDto::getUuid, CeActivityDto::getWarningCount)
      .containsExactly(tuple("TASK_4", 6), tuple("TASK_3", 1), tuple("TASK_2", 3), tuple("TASK_1", moreThan1));

    // select by component uuid
    query = new CeTaskQuery().setMainComponentUuid("PROJECT_1");
    assertThat(underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(100)))
      .extracting(CeActivityDto::getUuid, CeActivityDto::getWarningCount)
      .containsExactly(tuple("TASK_2", 3), tuple("TASK_1", moreThan1));

    // select by status
    query = new CeTaskQuery().setStatuses(singletonList(SUCCESS.name()));
    assertThat(underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(100)))
      .extracting(CeActivityDto::getUuid, CeActivityDto::getWarningCount)
      .containsExactly(tuple("TASK_4", 6), tuple("TASK_3", 1), tuple("TASK_1", moreThan1));

    // select by type
    query = new CeTaskQuery().setType(REPORT);
    assertThat(underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(100)))
      .extracting(CeActivityDto::getUuid, CeActivityDto::getWarningCount)
      .containsExactly(tuple("TASK_3", 1), tuple("TASK_2", 3), tuple("TASK_1", moreThan1));
    query = new CeTaskQuery().setType("views");
    assertThat(underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(100)))
      .extracting(CeActivityDto::getUuid, CeActivityDto::getWarningCount)
      .containsExactly(tuple("TASK_4", 6));

    // select by multiple conditions
    query = new CeTaskQuery().setType(REPORT).setOnlyCurrents(true).setMainComponentUuid("PROJECT_1");
    assertThat(underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(100)))
      .extracting(CeActivityDto::getUuid, CeActivityDto::getWarningCount)
      .containsExactly(tuple("TASK_2", 3));

  }

  @Test
  public void selectByQuery_is_paginated_and_return_results_sorted_from_last_to_first() {
    insert("TASK_1", REPORT, MAINCOMPONENT_1, SUCCESS);
    insert("TASK_2", REPORT, MAINCOMPONENT_1, FAILED);
    insert("TASK_3", REPORT, MAINCOMPONENT_2, SUCCESS);
    insert("TASK_4", "views", null, SUCCESS);

    assertThat(selectPageOfUuids(forPage(1).andSize(1))).containsExactly("TASK_4");
    assertThat(selectPageOfUuids(forPage(2).andSize(1))).containsExactly("TASK_3");
    assertThat(selectPageOfUuids(forPage(1).andSize(3))).containsExactly("TASK_4", "TASK_3", "TASK_2");
    assertThat(selectPageOfUuids(forPage(1).andSize(4))).containsExactly("TASK_4", "TASK_3", "TASK_2", "TASK_1");
    assertThat(selectPageOfUuids(forPage(2).andSize(3))).containsExactly("TASK_1");
    assertThat(selectPageOfUuids(forPage(1).andSize(100))).containsExactly("TASK_4", "TASK_3", "TASK_2", "TASK_1");
    assertThat(selectPageOfUuids(forPage(5).andSize(2))).isEmpty();
  }

  @Test
  public void selectByQuery_no_results_if_shortcircuited_by_component_uuids() {
    insert("TASK_1", REPORT, MAINCOMPONENT_1, SUCCESS);

    CeTaskQuery query = new CeTaskQuery();
    query.setMainComponentUuids(Collections.emptyList());
    assertThat(underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(1))).isEmpty();
  }

  @Test
  public void select_and_count_by_date() {
    insertWithDates("UUID1", 1_450_000_000_000L, 1_470_000_000_000L);
    insertWithDates("UUID2", 1_460_000_000_000L, 1_480_000_000_000L);

    // search by min submitted date
    CeTaskQuery query = new CeTaskQuery().setMinSubmittedAt(1_455_000_000_000L);
    assertThat(underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(5))).extracting("uuid").containsOnly("UUID2");

    // search by max executed date
    query = new CeTaskQuery().setMaxExecutedAt(1_475_000_000_000L);
    assertThat(underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(5))).extracting("uuid").containsOnly("UUID1");

    // search by both dates
    query = new CeTaskQuery()
      .setMinSubmittedAt(1_400_000_000_000L)
      .setMaxExecutedAt(1_475_000_000_000L);
    assertThat(underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(5))).extracting("uuid").containsOnly("UUID1");
  }

  @Test
  public void select_by_minExecutedAt() {
    insertWithDates("UUID1", 1_450_000_000_000L, 1_470_000_000_000L);
    insertWithDates("UUID2", 1_460_000_000_000L, 1_480_000_000_000L);

    CeTaskQuery query = new CeTaskQuery().setMinExecutedAt(1_460_000_000_000L);
    assertThat(underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(5))).extracting("uuid").containsExactlyInAnyOrder("UUID1", "UUID2");

    query = new CeTaskQuery().setMinExecutedAt(1_475_000_000_000L);
    assertThat(underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(5))).extracting("uuid").containsExactlyInAnyOrder("UUID2");

    query = new CeTaskQuery().setMinExecutedAt(1_485_000_000_000L);
    assertThat(underTest.selectByQuery(db.getSession(), query, forPage(1).andSize(5))).isEmpty();
  }

  private void insertWithDates(String uuid, long submittedAt, long executedAt) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setUuid(uuid);
    queueDto.setTaskType("fake");
    CeActivityDto dto = new CeActivityDto(queueDto);
    dto.setStatus(SUCCESS);
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
  public void selectOlder_populates_hasScannerContext_flag() {
    insertWithCreationDate("TASK_1", 1_450_000_000_000L);
    CeActivityDto dto2 = insertWithCreationDate("TASK_2", 1_450_000_000_000L);
    insertScannerContext(dto2.getUuid());

    List<CeActivityDto> dtos = underTest.selectOlderThan(db.getSession(), 1_465_000_000_000L);
    assertThat(dtos).hasSize(2);
    dtos.forEach((dto) -> assertThat(dto.isHasScannerContext()).isEqualTo(dto.getUuid().equals("TASK_2")));
  }

  @Test
  public void selectOlderThan_does_not_populate_errorStacktrace() {
    insert("TASK_1", REPORT, MAINCOMPONENT_1, FAILED);
    underTest.insert(db.getSession(), createActivityDto("TASK_2", REPORT, COMPONENT_1, MAINCOMPONENT_1, FAILED).setErrorStacktrace("some stack"));

    List<CeActivityDto> dtos = underTest.selectOlderThan(db.getSession(), system2.now() + 1_000_000L);

    assertThat(dtos)
      .hasSize(2)
      .extracting("errorStacktrace").containsOnly((String) null);
  }

  @Test
  public void selectOlderThan_does_not_populate_warningCount() {
    CeActivityDto activity1 = insert("TASK_1", REPORT, "PROJECT_1", FAILED);
    insertWarnings(activity1, 10);
    CeActivityDto activity2 = insert("TASK_2", REPORT, "PROJECT_1", SUCCESS);
    insertWarnings(activity2, 1);

    List<CeActivityDto> dtos = underTest.selectOlderThan(db.getSession(), system2.now() + 1_000_000L);

    assertThat(dtos)
      .hasSize(2)
      .extracting(CeActivityDto::getWarningCount)
      .containsOnly(0);
  }

  @Test
  public void deleteByUuids() {
    insert("TASK_1", "REPORT", MAINCOMPONENT_1, SUCCESS);
    insert("TASK_2", "REPORT", MAINCOMPONENT_1, SUCCESS);
    insert("TASK_3", "REPORT", MAINCOMPONENT_1, SUCCESS);

    underTest.deleteByUuids(db.getSession(), ImmutableSet.of("TASK_1", "TASK_3"));
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_1").isPresent()).isFalse();
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_2")).isPresent();
    assertThat(underTest.selectByUuid(db.getSession(), "TASK_3").isPresent()).isFalse();
  }

  @Test
  public void deleteByUuids_does_nothing_if_uuid_does_not_exist() {
    insert("TASK_1", "REPORT", MAINCOMPONENT_1, SUCCESS);

    // must not fail
    underTest.deleteByUuids(db.getSession(), singleton("TASK_2"));

    assertThat(underTest.selectByUuid(db.getSession(), "TASK_1")).isPresent();
  }

  @Test
  public void count_last_by_status_and_main_component_uuid() {
    insert("TASK_1", CeTaskTypes.REPORT, MAINCOMPONENT_1, SUCCESS);
    // component 2
    insert("TASK_2", CeTaskTypes.REPORT, MAINCOMPONENT_2, SUCCESS);
    // status failed
    insert("TASK_3", CeTaskTypes.REPORT, MAINCOMPONENT_1, FAILED);
    // status canceled
    insert("TASK_4", CeTaskTypes.REPORT, MAINCOMPONENT_1, CANCELED);
    insert("TASK_5", CeTaskTypes.REPORT, MAINCOMPONENT_1, SUCCESS);
    db.commit();

    assertThat(underTest.countLastByStatusAndMainComponentUuid(dbSession, SUCCESS, MAINCOMPONENT_1)).isEqualTo(1);
    assertThat(underTest.countLastByStatusAndMainComponentUuid(dbSession, SUCCESS, null)).isEqualTo(2);
  }

  @Test
  public void selectLastByComponentUuidAndTaskType_returns_task_of_given_type() {
    insert("TASK_1", "VIEW_REFRESH", MAINCOMPONENT_1, MAINCOMPONENT_1, SUCCESS);
    insert("TASK_2", CeTaskTypes.REPORT, MAINCOMPONENT_1, MAINCOMPONENT_1, SUCCESS);
    insert("TASK_3", "PROJECT_EXPORT", MAINCOMPONENT_1, MAINCOMPONENT_1, SUCCESS);
    insert("TASK_4", "PROJECT_IMPORT", MAINCOMPONENT_1, MAINCOMPONENT_1, SUCCESS);
    db.commit();

    Optional<CeActivityDto> result = underTest.selectLastByComponentUuidAndTaskType(db.getSession(), MAINCOMPONENT_1, "PROJECT_EXPORT");

    assertThat(result).hasValueSatisfying(value -> assertThat(value.getUuid()).isEqualTo("TASK_3"));
  }

  @Test
  public void selectLastByComponentUuidAndTaskType_returns_empty_if_task_of_given_type_does_not_exist() {
    insert("TASK_1", CeTaskTypes.REPORT, MAINCOMPONENT_1, SUCCESS);
    db.commit();

    Optional<CeActivityDto> result = underTest.selectLastByComponentUuidAndTaskType(db.getSession(), MAINCOMPONENT_1, "PROJECT_EXPORT");

    assertThat(result).isEmpty();
  }

  private CeActivityDto insert(String uuid, String type, @Nullable String mainComponentUuid, CeActivityDto.Status status) {
    return insert(uuid, type, mainComponentUuid, mainComponentUuid, status);
  }

  private CeActivityDto insertAndCommit(String uuid, String type, @Nullable String componentUuid, @Nullable String mainComponentUuid, CeActivityDto.Status status) {
    CeActivityDto res = insert(uuid, type, componentUuid, mainComponentUuid, status);
    db.commit();
    return res;
  }

  private CeActivityDto insert(String uuid, String type, @Nullable String componentUuid, @Nullable String mainComponentUuid, CeActivityDto.Status status) {
    CeActivityDto dto = createActivityDto(uuid, type, componentUuid, mainComponentUuid, status);
    underTest.insert(db.getSession(), dto);
    return dto;
  }

  private CeActivityDto createActivityDto(String uuid, String type, @Nullable String componentUuid, @Nullable String mainComponentUuid, CeActivityDto.Status status) {
    CeQueueDto creating = new CeQueueDto();
    creating.setUuid(uuid);
    creating.setStatus(PENDING);
    creating.setTaskType(type);
    creating.setComponentUuid(componentUuid);
    creating.setMainComponentUuid(mainComponentUuid);
    creating.setSubmitterUuid("submitter uuid");
    creating.setCreatedAt(1_300_000_000_000L);

    db.getDbClient().ceQueueDao().insert(dbSession, creating);
    makeInProgress(dbSession, "worker uuid", 1_400_000_000_000L, creating);

    CeQueueDto ceQueueDto = db.getDbClient().ceQueueDao().selectByUuid(dbSession, uuid).get();

    CeActivityDto dto = new CeActivityDto(ceQueueDto);
    dto.setStatus(status);
    dto.setStartedAt(1_500_000_000_000L);
    dto.setExecutedAt(1_500_000_000_500L);
    dto.setExecutionTimeMs(500L);
    dto.setAnalysisUuid(uuid + "_2");
    if (status == FAILED) {
      dto.setErrorMessage("error msg for " + uuid);
      dto.setErrorType("anErrorType");
    }
    return dto;
  }

  private CeActivityDto insertWithCreationDate(String uuid, long date) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setUuid(uuid);
    queueDto.setTaskType("fake");

    CeActivityDto dto = new CeActivityDto(queueDto);
    dto.setStatus(SUCCESS);
    dto.setAnalysisUuid(uuid + "_AA");
    system2.setNow(date);
    underTest.insert(db.getSession(), dto);
    return dto;
  }

  private void insertScannerContext(String taskUuid) {
    db.getDbClient().ceScannerContextDao().insert(dbSession, taskUuid, CloseableIterator.from(singletonList("scanner context of " + taskUuid).iterator()));
    dbSession.commit();
  }

  private List<String> selectPageOfUuids(Pagination pagination) {
    return underTest.selectByQuery(db.getSession(), new CeTaskQuery(), pagination).stream()
      .map(CeActivityToUuid.INSTANCE::apply)
      .collect(MoreCollectors.toList());
  }

  private enum CeActivityToUuid implements Function<CeActivityDto, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull CeActivityDto input) {
      return input.getUuid();
    }
  }

  private static String newUuid() {
    return UuidFactoryFast.getInstance().create();
  }
}
