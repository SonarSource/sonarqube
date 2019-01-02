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
package org.sonar.server.platform.db.migration.version.v72;

import com.google.common.collect.ImmutableSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.CoreDbTester.createForSchema;

public class PurgeOrphansForCETest {

  private static final Set<String> CE_TABLES = ImmutableSet.of("CE_QUEUE", "CE_ACTIVITY");
  private static final Set<String> CE_CHILD_TABLES = ImmutableSet.of(
    "CE_TASK_CHARACTERISTICS", "CE_TASK_INPUT", "CE_SCANNER_CONTEXT");

  @Rule
  public CoreDbTester db = createForSchema(PurgeOrphansForCETest.class, "ce.sql");

  private Random random = new Random();
  private PurgeOrphansForCE underTest = new PurgeOrphansForCE(db.database());
  private String uuid;

  @Test
  public void no_effect_on_empty_db() throws SQLException {
    underTest.execute();
  }

  @Test
  public void delete_rows_from_CE_child_tables_when_ce_tables_are_empty() throws SQLException {
    int count = 1 + random.nextInt(5);
    IntStream.range(0, count)
      .forEach(i -> {
        String uuid = i + randomAlphanumeric(10);
        insertInChildTables(uuid);
      });
    CE_CHILD_TABLES.forEach(tableName -> assertThat(db.countRowsOfTable(tableName)).isEqualTo(count));

    underTest.execute();

    assertThat(db.countRowsOfTable("CE_ACTIVITY")).isZero();
    assertThat(db.countRowsOfTable("CE_QUEUE")).isZero();
    CE_CHILD_TABLES.forEach(tableName -> assertThat(db.countRowsOfTable(tableName)).isZero());
  }

  @Test
  public void do_not_delete_rows_from_CE_child_tables_if_parent_in_CE_ACTIVITY() throws SQLException {
    int count = 1 + random.nextInt(5);
    IntStream.range(0, count)
      .forEach(i -> {
        String uuid = i + randomAlphanumeric(10);
        insertCeActivity(uuid);
        insertInChildTables(uuid);
      });
    CE_CHILD_TABLES.forEach(tableName -> assertThat(db.countRowsOfTable(tableName)).isEqualTo(count));

    underTest.execute();

    assertThat(db.countRowsOfTable("CE_ACTIVITY")).isEqualTo(count);
    assertThat(db.countRowsOfTable("CE_QUEUE")).isZero();
    CE_CHILD_TABLES.forEach(tableName -> assertThat(db.countRowsOfTable(tableName)).isEqualTo(count));
  }

  @Test
  public void do_not_delete_rows_from_CE_child_tables_if_parent_in_CE_QUEUE() throws SQLException {
    int count = 1 + random.nextInt(5);
    IntStream.range(0, count)
      .forEach(i -> {
        String uuid = i + randomAlphanumeric(10);
        insertCeQueue(uuid);
        insertInChildTables(uuid);
      });
    CE_CHILD_TABLES.forEach(tableName -> assertThat(db.countRowsOfTable(tableName)).isEqualTo(count));

    underTest.execute();

    assertThat(db.countRowsOfTable("CE_ACTIVITY")).isZero();
    assertThat(db.countRowsOfTable("CE_QUEUE")).isEqualTo(count);
    CE_CHILD_TABLES.forEach(tableName -> assertThat(db.countRowsOfTable(tableName)).isEqualTo(count));
  }

  @Test
  public void delete_only_orphan_rows_from_ce_child_tables() throws SQLException {
    int withCeActivityParent = 1 + new Random().nextInt(10);
    int withCeQueueParent = 1 + new Random().nextInt(10);
    int orphans = 1 + new Random().nextInt(10);
    IntStream.range(0, withCeActivityParent)
      .forEach(i -> {
        String uuid = "ca_" + i;
        insertCeActivity(uuid);
        insertInChildTables(uuid);
      });
    IntStream.range(0, withCeQueueParent)
      .forEach(i -> {
        String uuid = "cq_" + i;
        insertCeQueue(uuid);
        insertInChildTables(uuid);
      });
    IntStream.range(0, orphans)
      .forEach(i -> {
        String uuid = "orph_" + i;
        insertInChildTables(uuid);
      });

    underTest.execute();

    assertThat(db.countRowsOfTable("CE_ACTIVITY")).isEqualTo(withCeActivityParent);
    assertThat(db.countRowsOfTable("CE_QUEUE")).isEqualTo(withCeQueueParent);
    CE_CHILD_TABLES.forEach(tableName -> assertThat(db.countRowsOfTable(tableName)).isEqualTo(withCeActivityParent + withCeQueueParent));
    CE_CHILD_TABLES.forEach(tableName -> assertThat(db.select("select task_uuid as \"TASK_UUID\" from " + tableName))
      .extracting(t -> (String) t.get("TASK_UUID"))
      .allMatch(t -> t.startsWith("ca_") || t.startsWith("cq_")));
  }

  @Test
  public void reentrant_on_empty_db() throws SQLException {
    underTest.execute();

    underTest.execute();
  }

  @Test
  public void reentrant_on_non_empty_db() throws SQLException {
    delete_only_orphan_rows_from_ce_child_tables();

    underTest.execute();
  }

  private void insertCeActivity(String uuid) {
    long now = System.currentTimeMillis();

    db.executeInsert("CE_ACTIVITY",
      "UUID", uuid,
      "TASK_TYPE", randomAlphanumeric(15),
      "COMPONENT_UUID", randomAlphanumeric(15),
      "ANALYSIS_UUID", randomAlphanumeric(15),
      "STATUS", randomAlphanumeric(15),
      "IS_LAST", false,
      "IS_LAST_KEY", randomAlphanumeric(15),
      "SUBMITTER_UUID", randomAlphanumeric(15),
      "EXECUTION_COUNT", 0,
      "SUBMITTED_AT", now,
      "CREATED_AT", now,
      "UPDATED_AT", now);
  }

  private void insertCeQueue(String uuid) {
    long now = System.currentTimeMillis();

    db.executeInsert("CE_QUEUE",
      "UUID", uuid,
      "TASK_TYPE", randomAlphanumeric(15),
      "STATUS", randomAlphanumeric(15),
      "EXECUTION_COUNT", 0,
      "CREATED_AT", now,
      "UPDATED_AT", now);
  }

  private void insertInChildTables(String uuid) {
    insertCeCharacteristics(uuid);
    insertCeTaskInput(uuid);
    insertCeScannerContext(uuid);
  }

  private void insertCeCharacteristics(String uuid) {
    db.executeInsert("CE_TASK_CHARACTERISTICS",
      "UUID", randomAlphanumeric(15),
      "TASK_UUID", uuid,
      "KEE", randomAlphanumeric(15));
  }

  private void insertCeTaskInput(String uuid) {
    long now = System.currentTimeMillis();

    db.executeInsert("CE_TASK_INPUT",
      "TASK_UUID", uuid,
      "INPUT_DATA", randomAlphanumeric(15).getBytes(),
      "CREATED_AT", now,
      "UPDATED_AT", now);
  }

  private void insertCeScannerContext(String uuid) {
    long now = System.currentTimeMillis();

    db.executeInsert("CE_SCANNER_CONTEXT",
      "TASK_UUID", uuid,
      "CONTEXT_DATA", randomAlphanumeric(15).getBytes(),
      "CREATED_AT", now,
      "UPDATED_AT", now);
  }
}
