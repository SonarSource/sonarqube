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
package org.sonar.server.platform.db.migration.version.v74;

import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.db.CoreDbTester;

import static java.util.Arrays.stream;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class PopulateTmpColumnsToCeQueueTest {
  private static final Map<String, String> NO_CHARACTERISTICS = Collections.emptyMap();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateTmpColumnsToCeQueueTest.class, "ce_queue.sql");

  private PopulateTmpColumnsToCeQueue underTest = new PopulateTmpColumnsToCeQueue(db.database());

  @Test
  public void no_action_on_empty_table() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("ce_queue")).isZero();
  }

  @Test
  @UseDataProvider("characteriticsOfMainBranchesAndPr")
  public void execute_populates_tmp_columns_with_component_uuid_if_task_has_no_row_in_PROJECTS(Map<String, String> characteristics) throws SQLException {
    Row[] notUpdatedRows = Stream.of(
      // not updated because no component_uuid
      new Row(newUuid(), null, null, null),
      new Row(newUuid(), null, randomAlphabetic(2), null),
      new Row(newUuid(), null, randomAlphabetic(3), randomAlphabetic(4)),
      new Row(newUuid(), null, null, randomAlphabetic(5)),
      // not updated because both target fields are already set (re-entrance)
      new Row(newUuid(), randomAlphabetic(14), randomAlphabetic(6), randomAlphabetic(7)))
      .toArray(Row[]::new);
    Row[] updatedRows = {
      new Row(newUuid(), randomAlphabetic(12), null, null),
      new Row(newUuid(), randomAlphabetic(13), randomAlphabetic(5), null),
      new Row(newUuid(), randomAlphabetic(14), null, randomAlphabetic(6)),
    };
    stream(notUpdatedRows).forEach(row -> insertCeQueue(row, characteristics));
    stream(updatedRows).forEach(row -> insertCeQueue(row, characteristics));

    underTest.execute();

    assertThat(rowsInCeQueue())
      .hasSize(notUpdatedRows.length + updatedRows.length)
      .contains(notUpdatedRows)
      .contains(stream(updatedRows)
        .map(row -> new Row(row.taskUuid, row.componentUuid, row.componentUuid, row.componentUuid))
        .toArray(Row[]::new));
  }

  @DataProvider
  public static Object[][] characteriticsOfMainBranchesAndPr() {
    return new Object[][] {
      {NO_CHARACTERISTICS},
      {branchCharacteristics("LONG", randomAlphabetic(15))},
      {branchCharacteristics("SHORT", randomAlphabetic(16))},
      {branchCharacteristics(randomAlphabetic(17), randomAlphabetic(18))},
      {prCharacteristics(randomAlphabetic(19))},
    };
  }

  @Test
  public void execute_populates_tmp_columns_with_component_uuid_for_existing_main_branch() throws SQLException {
    String mainComponentUuid = randomAlphabetic(2);
    insertProjects(mainComponentUuid, randomAlphabetic(3));
    String taskUuid = insertCeQueue(new Row(newUuid(), mainComponentUuid, null, null), NO_CHARACTERISTICS);

    underTest.execute();

    assertThat(rowsInCeQueue())
      .containsOnly(new Row(taskUuid, mainComponentUuid, mainComponentUuid, mainComponentUuid));
  }

  @Test
  public void execute_deletes_tasks_of_branches_without_row_in_PROJECTS_and_populates_others_matching_row_in_PROJECTS_by_KEE() throws SQLException {
    String mainComponentUuid = randomAlphabetic(2);
    String mainComponentKey = randomAlphabetic(3);
    String branchUuid = randomAlphabetic(4);
    String branchType1 = randomAlphabetic(5);
    String branchName1 = randomAlphabetic(6);
    String branchType2 = randomAlphabetic(7);
    String branchName2 = randomAlphabetic(8);
    insertProjects(mainComponentUuid, mainComponentKey);
    insertProjects(branchUuid, mainComponentKey + ":BRANCH:" + branchName2);
    String deletedTaskUuid = insertCeQueue(new Row(newUuid(), mainComponentUuid, null, null), branchCharacteristics(branchType1, branchName1));
    String updatedTaskUuid = insertCeQueue(new Row(newUuid(), mainComponentUuid, null, null), branchCharacteristics(branchType2, branchName2));

    underTest.execute();

    assertThat(rowsInCeQueue())
      .containsOnly(new Row(updatedTaskUuid, mainComponentUuid, branchUuid, mainComponentUuid));
  }

  @Test
  public void execute_deletes_tasks_of_prs_without_row_in_PROJECTS_and_populates_others_matching_row_in_PROJECTS_by_KEE() throws SQLException {
    String mainComponentUuid = randomAlphabetic(2);
    String mainComponentKey = randomAlphabetic(3);
    String prUuid = randomAlphabetic(4);
    String prName1 = randomAlphabetic(6);
    String prName2 = randomAlphabetic(8);
    insertProjects(mainComponentUuid, mainComponentKey);
    insertProjects(prUuid, mainComponentKey + ":PULL_REQUEST:" + prName2);
    String deletedTaskUuid = insertCeQueue(new Row(newUuid(), mainComponentUuid, null, null), prCharacteristics(prName1));
    String updatedTaskUuid = insertCeQueue(new Row(newUuid(), mainComponentUuid, null, null), prCharacteristics(prName2));

    underTest.execute();

    assertThat(rowsInCeQueue())
      .containsOnly(new Row(updatedTaskUuid, mainComponentUuid, prUuid, mainComponentUuid));
  }

  private Stream<Row> rowsInCeQueue() {
    return db.select("select" +
      " uuid as \"UUID\", component_uuid as \"COMPONENT_UUID\", tmp_component_uuid as \"TMP_COMPONENT_UUID\", tmp_main_component_UUID as \"TMP_MAIN_COMPONENT_UUID\"" +
      " from ce_queue")
      .stream()
      .map(row -> new Row(
        (String) row.get("UUID"),
        (String) row.get("COMPONENT_UUID"),
        (String) row.get("TMP_COMPONENT_UUID"),
        (String) row.get("TMP_MAIN_COMPONENT_UUID")));
  }

  private String insertCeQueue(Row row, Map<String, String> characteristics) {
    String uuid = insertCeQueue(row.taskUuid, row.componentUuid, row.tmpComponentUuid, row.tmpMainComponentUuid);
    characteristics.forEach((key, value) -> insertCeCharacteristic(uuid, key, value));
    return uuid;
  }

  private String insertCeQueue(String uuid, @Nullable String componentUuid, @Nullable String tmpComponentUuid, @Nullable String tmpMainComponentUuid) {
    Random random = new Random();
    db.executeInsert("ce_queue",
      "UUID", uuid,
      "TASK_TYPE", randomAlphabetic(6),
      "COMPONENT_UUID", componentUuid,
      "TMP_COMPONENT_UUID", tmpComponentUuid,
      "TMP_MAIN_COMPONENT_UUID", tmpMainComponentUuid,
      "STATUS", randomAlphabetic(7),
      "EXECUTION_COUNT", random.nextInt(500),
      "CREATED_AT", (long) random.nextInt(500),
      "UPDATED_AT", (long) random.nextInt(500));
    return uuid;
  }

  private void insertCeCharacteristic(String taskUuid, String key, String value) {
    db.executeInsert(
      "ce_task_characteristics",
      "UUID", newUuid(),
      "TASK_UUID", taskUuid,
      "KEE", key,
      "TEXT_VALUE", value);
  }

  private void insertProjects(String uuid, String key) {
    db.executeInsert(
      "PROJECTS",
      "UUID", uuid,
      "KEE", key,
      "ORGANIZATION_UUID", "org_" + uuid,
      "ROOT_UUID", uuid + "_root",
      "UUID_PATH", uuid + "_path",
      "PROJECT_UUID", uuid + "_project",
      "PRIVATE", new Random().nextBoolean());
  }

  private int uuidGenerator = new Random().nextInt(9000);

  private String newUuid() {
    return "uuid_" + uuidGenerator++;
  }

  private static Map<String, String> branchCharacteristics(String branchType, String branchName) {
    return ImmutableMap.of("branchType", branchType, "branch", branchName);
  }

  private static Map<String, String> prCharacteristics(String prName) {
    return ImmutableMap.of("pullRequest", prName);
  }

}
