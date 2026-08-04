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
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.AGENT_TYPE_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.ANALYSIS_TYPE_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.BRANCH_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.COLUMN_AGENT_TYPE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.COLUMN_ANALYSIS_TYPE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.COLUMN_BRANCH;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.COLUMN_ERROR_KEY;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.COLUMN_FINDINGS_COUNT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.COLUMN_FINISHED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.COLUMN_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.COLUMN_PROJECT_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.COLUMN_REPOSITORY_URL;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.COLUMN_REVISION;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.COLUMN_STARTED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.COLUMN_STATUS;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.COLUMN_SUBSTATUS;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.COLUMN_WORKFLOW_TYPE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.ERROR_KEY_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.INDEX_PROJECT_STATUS;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.INDEX_STATUS;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.PROJECT_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.REPOSITORY_URL_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.REVISION_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.STATUS_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.SUBSTATUS_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentJobsTable.WORKFLOW_TYPE_SIZE;

class CreateAgentJobsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateAgentJobsTable.class);

  private final CreateAgentJobsTable underTest = new CreateAgentJobsTable(db.database());

  @Test
  void migration_should_create_table_and_columns() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_agent_jobs", COLUMN_ID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ID, Types.VARCHAR, ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROJECT_ID, Types.VARCHAR, PROJECT_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_BRANCH, Types.VARCHAR, BRANCH_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_REPOSITORY_URL, Types.VARCHAR, REPOSITORY_URL_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_REVISION, Types.VARCHAR, REVISION_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_AGENT_TYPE, Types.VARCHAR, AGENT_TYPE_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_WORKFLOW_TYPE, Types.VARCHAR, WORKFLOW_TYPE_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ANALYSIS_TYPE, Types.VARCHAR, ANALYSIS_TYPE_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_STATUS, Types.VARCHAR, STATUS_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_SUBSTATUS, Types.VARCHAR, SUBSTATUS_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ERROR_KEY, Types.VARCHAR, ERROR_KEY_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_FINDINGS_COUNT, Types.INTEGER, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_STARTED_AT, Types.BIGINT, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_FINISHED_AT, Types.BIGINT, null, true);
  }

  @Test
  void migration_should_create_indexes() throws SQLException {
    underTest.execute();

    db.assertIndex(TABLE_NAME, INDEX_STATUS, COLUMN_STATUS);
    checkProjectStatusIndex();
  }

  private void checkProjectStatusIndex() throws SQLException {
    if (!Oracle.ID.equals(db.database().getDialect().getId())) {
      db.assertIndex(TABLE_NAME, INDEX_PROJECT_STATUS, COLUMN_PROJECT_ID, COLUMN_STATUS, COLUMN_CREATED_AT);
      return;
    }

    // Oracle exposes descending index columns using generated virtual-column names, so only the
    // presence and non-uniqueness of the index is asserted here, not the trailing column's name.
    boolean indexFound = false;
    boolean indexNonUnique = false;
    try (Connection connection = db.openConnection();
         ResultSet resultSet = connection.getMetaData().getIndexInfo(null, null, TABLE_NAME.toUpperCase(), false, false)) {
      while (resultSet.next()) {
        if (INDEX_PROJECT_STATUS.equalsIgnoreCase(resultSet.getString("INDEX_NAME"))) {
          indexFound = true;
          indexNonUnique = resultSet.getBoolean("NON_UNIQUE");
        }
      }
    }

    assertThat(indexFound).as("Index %s should exist", INDEX_PROJECT_STATUS).isTrue();
    assertThat(indexNonUnique).as("Index %s should not be unique", INDEX_PROJECT_STATUS).isTrue();
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
