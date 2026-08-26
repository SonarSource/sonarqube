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

import static org.sonar.server.platform.db.migration.version.v202605.CreateDopPrSnapshotsTable.AI_AGENT_JOB_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateDopPrSnapshotsTable.COLUMN_AI_AGENT_JOB_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateDopPrSnapshotsTable.COLUMN_CHECK_RUNS_JSON;
import static org.sonar.server.platform.db.migration.version.v202605.CreateDopPrSnapshotsTable.COLUMN_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateDopPrSnapshotsTable.COLUMN_PROJECT_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateDopPrSnapshotsTable.COLUMN_PR_STATE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateDopPrSnapshotsTable.COLUMN_PULL_REQUEST_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateDopPrSnapshotsTable.COLUMN_REQUESTED_REVIEWERS_JSON;
import static org.sonar.server.platform.db.migration.version.v202605.CreateDopPrSnapshotsTable.COLUMN_TERMINAL;
import static org.sonar.server.platform.db.migration.version.v202605.CreateDopPrSnapshotsTable.COLUMN_WORKFLOW_ORIGIN;
import static org.sonar.server.platform.db.migration.version.v202605.CreateDopPrSnapshotsTable.ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateDopPrSnapshotsTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateDopPrSnapshotsTable.INDEX_NON_TERMINAL;
import static org.sonar.server.platform.db.migration.version.v202605.CreateDopPrSnapshotsTable.INDEX_PR;
import static org.sonar.server.platform.db.migration.version.v202605.CreateDopPrSnapshotsTable.PROJECT_UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateDopPrSnapshotsTable.PR_STATE_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateDopPrSnapshotsTable.PULL_REQUEST_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateDopPrSnapshotsTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateDopPrSnapshotsTable.WORKFLOW_ORIGIN_SIZE;

class CreateDopPrSnapshotsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateDopPrSnapshotsTable.class);

  private final CreateDopPrSnapshotsTable underTest = new CreateDopPrSnapshotsTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_dop_pr_snapshots", COLUMN_ID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ID, Types.VARCHAR, ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROJECT_UUID, Types.VARCHAR, PROJECT_UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PULL_REQUEST_ID, Types.VARCHAR, PULL_REQUEST_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_AI_AGENT_JOB_ID, Types.VARCHAR, AI_AGENT_JOB_ID_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_WORKFLOW_ORIGIN, Types.VARCHAR, WORKFLOW_ORIGIN_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PR_STATE, Types.VARCHAR, PR_STATE_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CHECK_RUNS_JSON, Types.CLOB, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_REQUESTED_REVIEWERS_JSON, Types.CLOB, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_TERMINAL, Types.BOOLEAN, null, false);
    db.assertUniqueIndex(TABLE_NAME, INDEX_PR, COLUMN_PROJECT_UUID, COLUMN_PULL_REQUEST_ID);
    db.assertIndex(TABLE_NAME, INDEX_NON_TERMINAL, COLUMN_TERMINAL, COLUMN_UPDATED_AT);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
