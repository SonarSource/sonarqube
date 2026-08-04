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

import static org.sonar.server.platform.db.migration.version.v202605.CreateRemediationAgentJobsTable.COLUMN_DEVOPS_PLATFORM;
import static org.sonar.server.platform.db.migration.version.v202605.CreateRemediationAgentJobsTable.COLUMN_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateRemediationAgentJobsTable.COLUMN_ISSUE_KEYS;
import static org.sonar.server.platform.db.migration.version.v202605.CreateRemediationAgentJobsTable.COLUMN_JOB_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateRemediationAgentJobsTable.COLUMN_RESULT_BRANCH;
import static org.sonar.server.platform.db.migration.version.v202605.CreateRemediationAgentJobsTable.COLUMN_RESULT_COMMIT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateRemediationAgentJobsTable.COLUMN_RESULT_PR_KEY;
import static org.sonar.server.platform.db.migration.version.v202605.CreateRemediationAgentJobsTable.COLUMN_SOURCE_PR_KEY;
import static org.sonar.server.platform.db.migration.version.v202605.CreateRemediationAgentJobsTable.DEVOPS_PLATFORM_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateRemediationAgentJobsTable.ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateRemediationAgentJobsTable.INDEX_JOB_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateRemediationAgentJobsTable.JOB_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateRemediationAgentJobsTable.RESULT_BRANCH_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateRemediationAgentJobsTable.RESULT_COMMIT_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateRemediationAgentJobsTable.RESULT_PR_KEY_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateRemediationAgentJobsTable.SOURCE_PR_KEY_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateRemediationAgentJobsTable.TABLE_NAME;

class CreateRemediationAgentJobsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateRemediationAgentJobsTable.class);

  private final CreateRemediationAgentJobsTable underTest = new CreateRemediationAgentJobsTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_remediation_agent_jobs", COLUMN_ID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ID, Types.VARCHAR, ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_JOB_ID, Types.VARCHAR, JOB_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_DEVOPS_PLATFORM, Types.VARCHAR, DEVOPS_PLATFORM_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_SOURCE_PR_KEY, Types.VARCHAR, SOURCE_PR_KEY_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_RESULT_PR_KEY, Types.VARCHAR, RESULT_PR_KEY_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_RESULT_BRANCH, Types.VARCHAR, RESULT_BRANCH_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_RESULT_COMMIT, Types.VARCHAR, RESULT_COMMIT_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ISSUE_KEYS, Types.CLOB, null, true);
    db.assertUniqueIndex(TABLE_NAME, INDEX_JOB_ID, COLUMN_JOB_ID);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
