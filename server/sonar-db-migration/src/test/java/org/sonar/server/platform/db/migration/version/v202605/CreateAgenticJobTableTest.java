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
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.AGENT_BRANCH_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.AGENT_TYPE_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.ANALYSIS_TYPE_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.BRANCH_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_AGENT_BRANCH;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_AGENT_TYPE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_ANALYSIS_TYPE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_BRANCH;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_FINDINGS_COUNT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_FINISHED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_FLOW;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_HEAD_SHA;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_ISSUE_KEYS;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_ISSUE_OUTCOMES;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_JOB_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_PROJECT_KEY;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_PULL_REQUEST_KEY;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_PULL_REQUEST_URL;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_REPOSITORY_URL;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_REVISION;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_STARTED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_STATUS;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_SUB_STATUS;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.FLOW_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.HEAD_SHA_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.INDEX_PROJECT_STATUS;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.INDEX_STATUS;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.JOB_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.PROJECT_KEY_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.PULL_REQUEST_KEY_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.PULL_REQUEST_URL_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.REPOSITORY_URL_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.REVISION_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.STATUS_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.SUB_STATUS_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgenticJobTable.TABLE_NAME;

class CreateAgenticJobTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateAgenticJobTable.class);

  private final CreateAgenticJobTable underTest = new CreateAgenticJobTable(db.database());

  @Test
  void migration_should_create_table_and_base_columns() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_agentic_job", COLUMN_JOB_ID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_JOB_ID, Types.VARCHAR, JOB_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROJECT_KEY, Types.VARCHAR, PROJECT_KEY_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_REPOSITORY_URL, Types.VARCHAR, REPOSITORY_URL_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_BRANCH, Types.VARCHAR, BRANCH_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_REVISION, Types.VARCHAR, REVISION_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ANALYSIS_TYPE, Types.VARCHAR, ANALYSIS_TYPE_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_STATUS, Types.VARCHAR, STATUS_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_SUB_STATUS, Types.VARCHAR, SUB_STATUS_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_FINDINGS_COUNT, Types.INTEGER, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
  }

  @Test
  void migration_should_create_remediation_columns() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_AGENT_TYPE, Types.VARCHAR, AGENT_TYPE_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ISSUE_KEYS, Types.CLOB, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_FLOW, Types.VARCHAR, FLOW_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_STARTED_AT, Types.BIGINT, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PULL_REQUEST_URL, Types.VARCHAR, PULL_REQUEST_URL_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PULL_REQUEST_KEY, Types.VARCHAR, PULL_REQUEST_KEY_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_AGENT_BRANCH, Types.VARCHAR, AGENT_BRANCH_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_HEAD_SHA, Types.VARCHAR, HEAD_SHA_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ISSUE_OUTCOMES, Types.CLOB, null, true);
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
      db.assertIndex(TABLE_NAME, INDEX_PROJECT_STATUS, COLUMN_PROJECT_KEY, COLUMN_STATUS, COLUMN_CREATED_AT);
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
