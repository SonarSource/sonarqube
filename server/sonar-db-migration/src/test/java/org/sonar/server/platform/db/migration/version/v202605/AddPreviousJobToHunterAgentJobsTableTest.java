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

import static org.sonar.server.platform.db.migration.version.v202605.AddPreviousJobToHunterAgentJobsTable.COLUMN_PREVIOUS_JOB_COMMIT_SHA;
import static org.sonar.server.platform.db.migration.version.v202605.AddPreviousJobToHunterAgentJobsTable.COLUMN_PREVIOUS_JOB_ID;
import static org.sonar.server.platform.db.migration.version.v202605.AddPreviousJobToHunterAgentJobsTable.COLUMN_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.AddPreviousJobToHunterAgentJobsTable.TABLE_NAME;

class AddPreviousJobToHunterAgentJobsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AddPreviousJobToHunterAgentJobsTable.class);

  private final AddPreviousJobToHunterAgentJobsTable underTest = new AddPreviousJobToHunterAgentJobsTable(db.database());

  @Test
  void execute_shouldAddBothNullableColumns() throws SQLException {
    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_PREVIOUS_JOB_ID);
    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_PREVIOUS_JOB_COMMIT_SHA);

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_PREVIOUS_JOB_ID, Types.VARCHAR, COLUMN_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PREVIOUS_JOB_COMMIT_SHA, Types.VARCHAR, COLUMN_SIZE, true);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_PREVIOUS_JOB_ID, Types.VARCHAR, COLUMN_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PREVIOUS_JOB_COMMIT_SHA, Types.VARCHAR, COLUMN_SIZE, true);
  }
}
