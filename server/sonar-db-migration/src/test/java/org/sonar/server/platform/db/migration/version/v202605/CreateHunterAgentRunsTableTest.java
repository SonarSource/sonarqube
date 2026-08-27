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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.Oracle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterAgentRunsTable.ANALYZED_COMMIT_SHA_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterAgentRunsTable.BRANCH_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterAgentRunsTable.COLUMN_ANALYZED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterAgentRunsTable.COLUMN_ANALYZED_COMMIT_SHA;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterAgentRunsTable.COLUMN_BRANCH_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterAgentRunsTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterAgentRunsTable.COLUMN_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterAgentRunsTable.COLUMN_JOB_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterAgentRunsTable.COLUMN_ORG_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterAgentRunsTable.COLUMN_PROJECT_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterAgentRunsTable.ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterAgentRunsTable.INDEX_BRANCH_ANALYZED;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterAgentRunsTable.INDEX_JOB_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterAgentRunsTable.JOB_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterAgentRunsTable.ORG_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterAgentRunsTable.PROJECT_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterAgentRunsTable.TABLE_NAME;

class CreateHunterAgentRunsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateHunterAgentRunsTable.class);

  private final CreateHunterAgentRunsTable underTest = new CreateHunterAgentRunsTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_hunter_agent_runs", COLUMN_ID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ID, Types.VARCHAR, ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_JOB_ID, Types.VARCHAR, JOB_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ORG_ID, Types.VARCHAR, ORG_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROJECT_ID, Types.VARCHAR, PROJECT_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_BRANCH_ID, Types.VARCHAR, BRANCH_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ANALYZED_COMMIT_SHA, Types.VARCHAR, ANALYZED_COMMIT_SHA_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ANALYZED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
  }

  /**
   * The unique index is what the capability's upsert conflicts on to make an at-least-once
   * redelivery idempotent, so its absence is a runtime failure rather than a slow query.
   */
  @Test
  void migration_should_create_indexes() throws SQLException {
    underTest.execute();

    db.assertUniqueIndex(TABLE_NAME, INDEX_JOB_ID, COLUMN_JOB_ID);
    checkBranchAnalyzedIndex();
  }

  private void checkBranchAnalyzedIndex() throws SQLException {
    if (!Oracle.ID.equals(db.database().getDialect().getId())) {
      db.assertIndex(TABLE_NAME, INDEX_BRANCH_ANALYZED, COLUMN_ORG_ID, COLUMN_PROJECT_ID, COLUMN_BRANCH_ID, COLUMN_ANALYZED_AT);
      return;
    }

    // Oracle exposes descending index columns using generated virtual-column names, so only the
    // presence and non-uniqueness of the index is asserted here, not the trailing column's name.
    boolean indexFound = false;
    boolean indexNonUnique = false;
    try (Connection connection = db.openConnection();
      ResultSet resultSet = connection.getMetaData().getIndexInfo(null, null, TABLE_NAME.toUpperCase(), false, false)) {
      while (resultSet.next()) {
        if (INDEX_BRANCH_ANALYZED.equalsIgnoreCase(resultSet.getString("INDEX_NAME"))) {
          indexFound = true;
          indexNonUnique = resultSet.getBoolean("NON_UNIQUE");
        }
      }
    }

    assertThat(indexFound).as("Index %s should exist", INDEX_BRANCH_ANALYZED).isTrue();
    assertThat(indexNonUnique).as("Index %s should not be unique", INDEX_BRANCH_ANALYZED).isTrue();
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
