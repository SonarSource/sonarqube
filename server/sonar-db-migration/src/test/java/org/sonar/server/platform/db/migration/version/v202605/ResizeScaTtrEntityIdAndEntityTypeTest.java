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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.Oracle;

import static org.sonar.db.OracleIndexTestUtils.assertIndexExistsForOracle;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueTtrHistoryTable.COLUMN_ENTITY_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueTtrHistoryTable.COLUMN_ENTITY_TYPE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueTtrHistoryTable.ENTITY_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueTtrHistoryTable.ENTITY_TYPE_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.ResizeScaTtrEntityIdAndEntityType.INDEX_ENTITY_TYPE_RECORDED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.ResizeScaTtrEntityIdAndEntityType.INDEX_UNIQUE;
import static org.sonar.server.platform.db.migration.version.v202605.ResizeScaTtrEntityIdAndEntityType.SCA_TTR_HISTORY_TABLE;

class ResizeScaTtrEntityIdAndEntityTypeTest {

  private static final int OLD_ENTITY_ID_SIZE = 255;
  private static final int OLD_ENTITY_TYPE_SIZE = 32;

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(ResizeScaTtrEntityIdAndEntityType.class);

  private final ResizeScaTtrEntityIdAndEntityType underTest = new ResizeScaTtrEntityIdAndEntityType(db.database());

  @Test
  void migration_should_resize_columns_and_add_indices() throws SQLException {
    db.assertColumnDefinition(SCA_TTR_HISTORY_TABLE, COLUMN_ENTITY_ID, Types.VARCHAR, OLD_ENTITY_ID_SIZE, false);
    db.assertColumnDefinition(SCA_TTR_HISTORY_TABLE, COLUMN_ENTITY_TYPE, Types.VARCHAR, OLD_ENTITY_TYPE_SIZE, false);
    db.assertIndexDoesNotExist(SCA_TTR_HISTORY_TABLE, INDEX_UNIQUE);
    db.assertIndexDoesNotExist(SCA_TTR_HISTORY_TABLE, INDEX_ENTITY_TYPE_RECORDED_AT);

    underTest.execute();

    db.assertColumnDefinition(SCA_TTR_HISTORY_TABLE, COLUMN_ENTITY_ID, Types.VARCHAR, ENTITY_ID_SIZE, false);
    db.assertColumnDefinition(SCA_TTR_HISTORY_TABLE, COLUMN_ENTITY_TYPE, Types.VARCHAR, ENTITY_TYPE_SIZE, false);
    assertIndexExists(INDEX_UNIQUE, true, "entity_id", "entity_type", "sca_dimension_id", "recorded_at_epoch");
    assertIndexExists(INDEX_ENTITY_TYPE_RECORDED_AT, false, "entity_id", "entity_type", "recorded_at_epoch");
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertColumnDefinition(SCA_TTR_HISTORY_TABLE, COLUMN_ENTITY_ID, Types.VARCHAR, OLD_ENTITY_ID_SIZE, false);
    db.assertColumnDefinition(SCA_TTR_HISTORY_TABLE, COLUMN_ENTITY_TYPE, Types.VARCHAR, OLD_ENTITY_TYPE_SIZE, false);
    db.assertIndexDoesNotExist(SCA_TTR_HISTORY_TABLE, INDEX_UNIQUE);
    db.assertIndexDoesNotExist(SCA_TTR_HISTORY_TABLE, INDEX_ENTITY_TYPE_RECORDED_AT);

    underTest.execute();
    underTest.execute();

    db.assertColumnDefinition(SCA_TTR_HISTORY_TABLE, COLUMN_ENTITY_ID, Types.VARCHAR, ENTITY_ID_SIZE, false);
    db.assertColumnDefinition(SCA_TTR_HISTORY_TABLE, COLUMN_ENTITY_TYPE, Types.VARCHAR, ENTITY_TYPE_SIZE, false);
    assertIndexExists(INDEX_UNIQUE, true, "entity_id", "entity_type", "sca_dimension_id", "recorded_at_epoch");
    assertIndexExists(INDEX_ENTITY_TYPE_RECORDED_AT, false, "entity_id", "entity_type", "recorded_at_epoch");
  }

  private void assertIndexExists(String indexName, boolean unique, String column, String... additionalColumns) {
    if (Oracle.ID.equals(db.database().getDialect().getId())) {
      assertIndexExistsForOracle(db, unique, indexName, SCA_TTR_HISTORY_TABLE);
    } else {
      if (unique) {
        db.assertUniqueIndex(SCA_TTR_HISTORY_TABLE, indexName, column, additionalColumns);
      } else {
        db.assertIndex(SCA_TTR_HISTORY_TABLE, indexName, column, additionalColumns);
      }
    }
  }
}
