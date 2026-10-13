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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v202606.WidenRemediationAgentJobsResultCommitColumn.COLUMN_RESULT_COMMIT;
import static org.sonar.server.platform.db.migration.version.v202606.WidenRemediationAgentJobsResultCommitColumn.TABLE_NAME;

class WidenRemediationAgentJobsResultCommitColumnTest {

  private static final int OLD_RESULT_COMMIT_SIZE = 40;
  private static final int NEW_RESULT_COMMIT_SIZE = 64;

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(WidenRemediationAgentJobsResultCommitColumn.class);

  private final WidenRemediationAgentJobsResultCommitColumn underTest = new WidenRemediationAgentJobsResultCommitColumn(db.database());

  @Test
  void migration_should_widen_the_column() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, COLUMN_RESULT_COMMIT, Types.VARCHAR, OLD_RESULT_COMMIT_SIZE, true);

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_RESULT_COMMIT, Types.VARCHAR, NEW_RESULT_COMMIT_SIZE, true);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_RESULT_COMMIT, Types.VARCHAR, NEW_RESULT_COMMIT_SIZE, true);
  }
}
