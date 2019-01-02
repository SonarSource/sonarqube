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
package org.sonar.server.platform.db.migration.version.v71;

import java.sql.SQLException;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class CleanCeChildTablesOrphansTest {

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(CleanCeChildTablesOrphansTest.class, "ce_tables.sql");

  private final Random random = new Random();
  private CleanCeChildTablesOrphans underTest = new CleanCeChildTablesOrphans(db.database());

  @Test
  public void execute_has_no_effect_if_tables_are_empty() throws SQLException {
    underTest.execute();
  }

  @Test
  public void execute_deletes_rows_of_ce_task_input_which_task_uuid_appears_in_neither_ce_queue_nor_ce_activity() throws SQLException {
    String taskInQueueUuid = insertCeQueue();
    String taskInActivityUuid = insertCeActivity();
    insertCeTaskInput(taskInQueueUuid);
    insertCeTaskInput(taskInActivityUuid);
    insertCeTaskInput("missing_task");

    underTest.execute();

    assertThat(db.select("select task_uuid as \"TASK_UUID\" from ce_task_input"))
      .extracting(r -> (String) r.get("TASK_UUID"))
      .containsOnly(taskInQueueUuid, taskInActivityUuid);
  }

  @Test
  public void execute_deletes_rows_of_ce_scanner_context_which_task_uuid_appears_in_neither_ce_queue_nor_ce_activity() throws SQLException {
    String taskInQueueUuid = insertCeQueue();
    String taskInActivityUuid = insertCeActivity();
    insertCeScannerContext(taskInQueueUuid);
    insertCeScannerContext(taskInActivityUuid);
    insertCeScannerContext("missing_task");

    underTest.execute();

    assertThat(db.select("select task_uuid as \"TASK_UUID\" from ce_scanner_context"))
      .extracting(r -> (String) r.get("TASK_UUID"))
      .containsOnly(taskInQueueUuid, taskInActivityUuid);
  }

  @Test
  public void execute_deletes_rows_of_ce_task_characteristics_which_task_uuid_appears_in_neither_ce_queue_nor_ce_activity() throws SQLException {
    String taskInQueueUuid = insertCeQueue();
    String taskInActivityUuid = insertCeActivity();
    insertCeTaskCharacteristics(taskInQueueUuid);
    insertCeTaskCharacteristics(taskInActivityUuid);
    insertCeTaskCharacteristics("missing_task");

    underTest.execute();

    assertThat(db.select("select task_uuid as \"TASK_UUID\" from ce_task_characteristics"))
      .extracting(r -> (String) r.get("TASK_UUID"))
      .containsOnly(taskInQueueUuid, taskInActivityUuid);
  }

  @Test
  public void execute_is_reentrant() throws SQLException {
    String taskInQueueUuid = insertCeQueue();
    String taskInActivityUuid = insertCeActivity();
    insertCeScannerContext(taskInQueueUuid);
    insertCeScannerContext(taskInActivityUuid);
    insertCeTaskInput(taskInQueueUuid);
    insertCeTaskInput(taskInActivityUuid);
    insertCeTaskCharacteristics(taskInQueueUuid);
    insertCeTaskCharacteristics(taskInActivityUuid);
    insertCeTaskInput("missing_task");
    insertCeScannerContext("missing_task");
    insertCeTaskCharacteristics("missing_task");

    underTest.execute();

    verifyOrphansDeleted(taskInQueueUuid, taskInActivityUuid);

    underTest.execute();

    verifyOrphansDeleted(taskInQueueUuid, taskInActivityUuid);
  }

  private void verifyOrphansDeleted(String taskInQueueUuid, String taskInActivityUuid) {
    assertThat(db.select("select task_uuid as \"TASK_UUID\" from ce_task_input"))
      .extracting(r -> (String) r.get("TASK_UUID"))
      .containsOnly(taskInQueueUuid, taskInActivityUuid);
    assertThat(db.select("select task_uuid as \"TASK_UUID\" from ce_scanner_context"))
      .extracting(r -> (String) r.get("TASK_UUID"))
      .containsOnly(taskInQueueUuid, taskInActivityUuid);
    assertThat(db.select("select task_uuid as \"TASK_UUID\" from ce_task_characteristics"))
      .extracting(r -> (String) r.get("TASK_UUID"))
      .containsOnly(taskInQueueUuid, taskInActivityUuid);
  }

  private String insertCeQueue() {
    String uuid = UuidFactoryFast.getInstance().create();
    db.executeInsert(
      "ce_queue",
      "UUID", uuid,
      "TASK_TYPE", randomAlphanumeric(10),
      "STATUS", randomAlphanumeric(10),
      "EXECUTION_COUNT", random.nextInt(99),
      "CREATED_AT", random.nextInt(95654354),
      "UPDATED_AT", random.nextInt(95654354));
    return uuid;
  }

  private String insertCeActivity() {
    String uuid = UuidFactoryFast.getInstance().create();
    db.executeInsert(
      "ce_activity",
      "UUID", uuid,
      "TASK_TYPE", randomAlphanumeric(10),
      "STATUS", randomAlphanumeric(10),
      "IS_LAST", random.nextBoolean(),
      "IS_LAST_KEY", randomAlphanumeric(15),
      "EXECUTION_COUNT", random.nextInt(99),
      "SUBMITTED_AT", random.nextInt(95654354),
      "CREATED_AT", random.nextInt(95654354),
      "UPDATED_AT", random.nextInt(95654354));
    return uuid;
  }

  private void insertCeTaskInput(String taskUuid) {
    db.executeInsert(
      "ce_task_input",
      "TASK_UUID", taskUuid,
      "INPUT_DATA", randomAlphanumeric(123).getBytes(),
      "CREATED_AT", random.nextInt(95654354),
      "UPDATED_AT", random.nextInt(95654354));
  }

  private void insertCeScannerContext(String taskUuid) {
    db.executeInsert(
      "ce_scanner_context",
      "TASK_UUID", taskUuid,
      "CONTEXT_DATA", randomAlphanumeric(123).getBytes(),
      "CREATED_AT", random.nextInt(95654354),
      "UPDATED_AT", random.nextInt(95654354));
  }

  private void insertCeTaskCharacteristics(String taskUuid) {
    for (int i = 0; i < 1 + random.nextInt(3); i++) {
      String uuid = UuidFactoryFast.getInstance().create();
      db.executeInsert(
        "ce_task_characteristics",
        "UUID", uuid,
        "TASK_UUID", taskUuid,
        "KEE", "kee_" + uuid + i,
        "TEXT_VALUE", randomAlphanumeric(18));
    }
  }
}
