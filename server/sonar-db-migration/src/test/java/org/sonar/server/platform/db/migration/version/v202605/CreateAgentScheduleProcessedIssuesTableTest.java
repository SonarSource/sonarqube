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

import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentScheduleProcessedIssuesTable.COLUMN_ISSUE_KEY;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentScheduleProcessedIssuesTable.COLUMN_PROCESSED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentScheduleProcessedIssuesTable.COLUMN_PROJECT_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentScheduleProcessedIssuesTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentScheduleProcessedIssuesTable.INDEX_PROJECT_ISSUE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentScheduleProcessedIssuesTable.ISSUE_KEY_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentScheduleProcessedIssuesTable.PROJECT_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentScheduleProcessedIssuesTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentScheduleProcessedIssuesTable.UUID_SIZE;

class CreateAgentScheduleProcessedIssuesTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateAgentScheduleProcessedIssuesTable.class);

  private final CreateAgentScheduleProcessedIssuesTable underTest = new CreateAgentScheduleProcessedIssuesTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_agent_sched_proc_issues", COLUMN_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROJECT_ID, Types.VARCHAR, PROJECT_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ISSUE_KEY, Types.VARCHAR, ISSUE_KEY_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROCESSED_AT, Types.BIGINT, null, false);
    db.assertUniqueIndex(TABLE_NAME, INDEX_PROJECT_ISSUE, COLUMN_PROJECT_ID, COLUMN_ISSUE_KEY);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
