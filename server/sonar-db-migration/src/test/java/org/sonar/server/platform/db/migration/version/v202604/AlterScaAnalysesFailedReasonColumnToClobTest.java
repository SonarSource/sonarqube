/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202604;

import java.sql.SQLException;
import java.sql.Types;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.DropColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.version.v202604.AlterScaAnalysesFailedReasonColumnToClob.COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v202604.AlterScaAnalysesFailedReasonColumnToClob.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202604.AlterScaAnalysesFailedReasonColumnToClob.TEMP_COLUMN_NAME;

class AlterScaAnalysesFailedReasonColumnToClobTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AlterScaAnalysesFailedReasonColumnToClob.class);

  private final DdlChange underTest = new AlterScaAnalysesFailedReasonColumnToClob(db.database());

  @Test
  void execute_shouldAlterColumnToClob() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.VARCHAR, 255, true);

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.CLOB, null, true);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.CLOB, null, true);
  }

  @Test
  void execute_shouldResume_whenTempColumnExistsAndOriginalDropped() throws SQLException {
    addClobColumn(TEMP_COLUMN_NAME);
    db.executeUpdateSql("UPDATE " + TABLE_NAME + " SET " + TEMP_COLUMN_NAME + " = " + COLUMN_NAME);
    dropColumn(COLUMN_NAME);

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.CLOB, null, true);
    db.assertColumnDoesNotExist(TABLE_NAME, TEMP_COLUMN_NAME);
  }

  @Test
  void execute_shouldResume_whenBothColumnsExist() throws SQLException {
    addClobColumn(TEMP_COLUMN_NAME);
    db.executeUpdateSql("UPDATE " + TABLE_NAME + " SET " + TEMP_COLUMN_NAME + " = " + COLUMN_NAME);

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.CLOB, null, true);
    db.assertColumnDoesNotExist(TABLE_NAME, TEMP_COLUMN_NAME);
  }

  @Test
  void execute_shouldResume_whenBothColumnsExistButDataNotYetCopied() throws SQLException {
    db.executeInsert(TABLE_NAME,
      "uuid", "test-uuid",
      "component_uuid", "comp-uuid",
      "status", "FAILED",
      "failed_reason", "some reason",
      "errors", "[]",
      "parsed_files", "[]",
      "created_at", 1_000_000L,
      "updated_at", 1_000_000L);
    addClobColumn(TEMP_COLUMN_NAME);
    // Note: temp column has not been populated yet (simulates crash between ADD COLUMN and UPDATE)

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.CLOB, null, true);
    db.assertColumnDoesNotExist(TABLE_NAME, TEMP_COLUMN_NAME);
    Assertions.assertThat(db.selectFirst("SELECT failed_reason FROM " + TABLE_NAME))
      .containsEntry("FAILED_REASON", "some reason");
  }

  @Test
  void execute_shouldResume_whenBothColumnsExistAndOriginalIsAlreadyClob() throws SQLException {
    db.executeInsert(TABLE_NAME,
      "uuid", "test-uuid",
      "component_uuid", "comp-uuid",
      "status", "FAILED",
      "failed_reason", "some reason",
      "errors", "[]",
      "parsed_files", "[]",
      "created_at", 1_000_000L,
      "updated_at", 1_000_000L);
    // Simulate finishConversion partially completing:
    // temp was added and populated, original VARCHAR was dropped,
    // finishConversion's ADD COLUMN change auto-committed, but the UPDATE has not run yet.
    addClobColumn(TEMP_COLUMN_NAME);
    db.executeUpdateSql("UPDATE " + TABLE_NAME + " SET " + TEMP_COLUMN_NAME + " = " + COLUMN_NAME);
    dropColumn(COLUMN_NAME);
    addClobColumn(COLUMN_NAME);
    // Note: finishConversion's UPDATE (SET failed_reason = failed_reason_tmp) has not run yet

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.CLOB, null, true);
    db.assertColumnDoesNotExist(TABLE_NAME, TEMP_COLUMN_NAME);
    Assertions.assertThat(db.selectFirst("SELECT failed_reason FROM " + TABLE_NAME))
      .containsEntry("FAILED_REASON", "some reason");
  }

  private void addClobColumn(String columnName) {
    String sql = new AddColumnsBuilder(db.database().getDialect(), TABLE_NAME)
      .addColumn(newClobColumnDefBuilder().setColumnName(columnName).setIsNullable(true).build())
      .build();
    db.executeDdl(sql);
  }

  private void dropColumn(String column) {
    new DropColumnsBuilder(db.database().getDialect(), TABLE_NAME, column).build()
      .forEach(db::executeDdl);
  }
}
