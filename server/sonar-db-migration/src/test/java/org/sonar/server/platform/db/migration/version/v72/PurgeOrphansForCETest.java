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
package org.sonar.server.platform.db.migration.version.v72;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.CoreDbTester.createForSchema;

public class PurgeOrphansForCETest {

  @Rule
  public CoreDbTester db = createForSchema(PurgeOrphansForCETest.class, "ce.sql");

  private PurgeOrphansForCE underTest = new PurgeOrphansForCE(db.database());
  private String uuid;

  @Test
  public void test_is_reentrant() throws SQLException {
    underTest.execute();
    underTest.execute();
  }

  @Test
  public void purge_should_not_delete_data_containing() throws SQLException {
    for (int i = 0; i < 10 ; i++) {
      insertCeActivity(randomAlphanumeric(15));
    }

    underTest.execute();

    assertThat(db.countRowsOfTable("CE_ACTIVITY")).isEqualTo(10);
    assertThat(db.countRowsOfTable("CE_TASK_CHARACTERISTICS")).isEqualTo(10 * 10);
    assertThat(db.countRowsOfTable("CE_TASK_INPUT")).isEqualTo(10);
    assertThat(db.countRowsOfTable("CE_SCANNER_CONTEXT")).isEqualTo(10);
  }

  @Test
  public void purge_should_delete_orphans() throws SQLException {
    for (int i = 0; i < 10 ; i++) {
      uuid = randomAlphanumeric(20);

      insertCeCharacteristics(uuid);
      insertCeScannerContext(uuid);
      insertCeTaskInput(uuid);
    }

    underTest.execute();

    assertThat(db.countRowsOfTable("CE_ACTIVITY")).isEqualTo(0);
    assertThat(db.countRowsOfTable("CE_TASK_CHARACTERISTICS")).isEqualTo(0);
    assertThat(db.countRowsOfTable("CE_TASK_INPUT")).isEqualTo(0);
    assertThat(db.countRowsOfTable("CE_SCANNER_CONTEXT")).isEqualTo(0);
  }

  @Test
  public void purge_should_keep_existant_ce_activity_and_delete_orphans() throws SQLException {
    for (int i = 0; i < 5 ; i++) {
      insertCeActivity(randomAlphanumeric(15));
      uuid = randomAlphanumeric(20);

      insertCeCharacteristics(uuid);
      insertCeScannerContext(uuid);
      insertCeTaskInput(uuid);
    }

    underTest.execute();

    assertThat(db.countRowsOfTable("CE_ACTIVITY")).isEqualTo(5);
    assertThat(db.countRowsOfTable("CE_TASK_CHARACTERISTICS")).isEqualTo(5 * 10);
    assertThat(db.countRowsOfTable("CE_TASK_INPUT")).isEqualTo(5);
    assertThat(db.countRowsOfTable("CE_SCANNER_CONTEXT")).isEqualTo(5);

    assertThat(
      db.selectFirst("select count(*) as count from ce_task_characteristics ctc where length(task_uuid) = 20")
        .get("COUNT")
    ).isEqualTo(0L);
    assertThat(
      db.selectFirst("select count(*) as count from ce_task_input ctc where length(task_uuid) = 20")
        .get("COUNT")
    ).isEqualTo(0L);
    assertThat(
      db.selectFirst("select count(*) as count from ce_scanner_context ctc where length(task_uuid) = 20")
        .get("COUNT")
    ).isEqualTo(0L);

    assertThat(
      db.selectFirst("select count(*) as count from ce_task_characteristics ctc where not exists (select 1 from ce_activity ca where ca.uuid = ctc.task_uuid)")
        .get("COUNT")
    ).isEqualTo(0L);
    assertThat(
      db.selectFirst("select count(*) as count from ce_task_input cti where not exists (select 1 from ce_activity ca where ca.uuid = cti.task_uuid)")
        .get("COUNT")
    ).isEqualTo(0L);
    assertThat(
      db.selectFirst("select count(*) as count from ce_scanner_context csc where not exists (select 1 from ce_activity ca where ca.uuid = csc.task_uuid)")
        .get("COUNT")
    ).isEqualTo(0L);
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
      "UPDATED_AT", now
    );

    insertCeTaskInput(uuid);
    insertCeScannerContext(uuid);

    for (int i = 0; i < 10; i++) {
      insertCeCharacteristics(uuid);
    }
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
