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

import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.BRANCH_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.COLUMN_AGENT_CONTEXT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.COLUMN_BRANCH_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.COLUMN_CONTEXT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.COLUMN_EFFORT_MINUTES;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.COLUMN_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.COLUMN_ISSUE_STATUS;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.COLUMN_JOB_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.COLUMN_ORG_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.COLUMN_PLAYBOOK_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.COLUMN_PLAYBOOK_KEY;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.COLUMN_PROJECT_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.COLUMN_RULE_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.INDEX_ORG_PROJECT_BRANCH;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.ISSUE_STATUS_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.JOB_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.ORG_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.PLAYBOOK_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.PLAYBOOK_KEY_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.PROJECT_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.RULE_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingsTable.TABLE_NAME;

class CreateFindingsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateFindingsTable.class);

  private final CreateFindingsTable underTest = new CreateFindingsTable(db.database());

  @Test
  void migration_should_create_table_and_columns() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_findings", COLUMN_ID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ID, Types.VARCHAR, ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_JOB_ID, Types.VARCHAR, JOB_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ORG_ID, Types.VARCHAR, ORG_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROJECT_ID, Types.VARCHAR, PROJECT_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_BRANCH_ID, Types.VARCHAR, BRANCH_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PLAYBOOK_ID, Types.VARCHAR, PLAYBOOK_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PLAYBOOK_KEY, Types.VARCHAR, PLAYBOOK_KEY_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_RULE_ID, Types.VARCHAR, RULE_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_EFFORT_MINUTES, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ISSUE_STATUS, Types.VARCHAR, ISSUE_STATUS_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CONTEXT, Types.CLOB, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_AGENT_CONTEXT, Types.CLOB, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
  }

  @Test
  void migration_should_create_index() throws SQLException {
    underTest.execute();

    db.assertIndex(TABLE_NAME, INDEX_ORG_PROJECT_BRANCH, COLUMN_ORG_ID, COLUMN_PROJECT_ID, COLUMN_BRANCH_ID);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
