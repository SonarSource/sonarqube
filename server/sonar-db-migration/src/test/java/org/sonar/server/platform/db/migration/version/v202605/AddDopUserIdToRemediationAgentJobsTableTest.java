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

import static org.sonar.server.platform.db.migration.version.v202605.AddDopUserIdToRemediationAgentJobsTable.COLUMN_DOP_USER_ID;
import static org.sonar.server.platform.db.migration.version.v202605.AddDopUserIdToRemediationAgentJobsTable.DOP_USER_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.AddDopUserIdToRemediationAgentJobsTable.TABLE_NAME;

class AddDopUserIdToRemediationAgentJobsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AddDopUserIdToRemediationAgentJobsTable.class);

  private final AddDopUserIdToRemediationAgentJobsTable underTest = new AddDopUserIdToRemediationAgentJobsTable(db.database());

  @Test
  void execute_shouldAddNullableColumn() throws SQLException {
    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_DOP_USER_ID);

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_DOP_USER_ID, Types.VARCHAR, DOP_USER_ID_SIZE, true);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_DOP_USER_ID, Types.VARCHAR, DOP_USER_ID_SIZE, true);
  }
}
