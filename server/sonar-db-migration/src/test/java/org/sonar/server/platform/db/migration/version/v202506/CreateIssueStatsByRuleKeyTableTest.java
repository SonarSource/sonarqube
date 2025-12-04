/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v202506;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v202506.CreateIssueStatsByRuleKeyTable.COLUMN_AGGREGATION_ID;
import static org.sonar.server.platform.db.migration.version.v202506.CreateIssueStatsByRuleKeyTable.COLUMN_AGGREGATION_TYPE;
import static org.sonar.server.platform.db.migration.version.v202506.CreateIssueStatsByRuleKeyTable.COLUMN_HOTSPOTS_REVIEWED;
import static org.sonar.server.platform.db.migration.version.v202506.CreateIssueStatsByRuleKeyTable.COLUMN_HOTSPOT_COUNT;
import static org.sonar.server.platform.db.migration.version.v202506.CreateIssueStatsByRuleKeyTable.COLUMN_ISSUE_COUNT;
import static org.sonar.server.platform.db.migration.version.v202506.CreateIssueStatsByRuleKeyTable.COLUMN_MQR_RATING;
import static org.sonar.server.platform.db.migration.version.v202506.CreateIssueStatsByRuleKeyTable.COLUMN_RATING;
import static org.sonar.server.platform.db.migration.version.v202506.CreateIssueStatsByRuleKeyTable.COLUMN_RULE_KEY;
import static org.sonar.server.platform.db.migration.version.v202506.CreateIssueStatsByRuleKeyTable.TABLE_NAME;

class CreateIssueStatsByRuleKeyTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateJiraWorkItemsTable.class);

  private final CreateIssueStatsByRuleKeyTable underTest = new CreateIssueStatsByRuleKeyTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_issue_stats_by_rule_key", COLUMN_AGGREGATION_TYPE, COLUMN_AGGREGATION_ID, COLUMN_RULE_KEY);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_AGGREGATION_TYPE, Types.VARCHAR, 20, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_AGGREGATION_ID, Types.VARCHAR, 40, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_RULE_KEY, Types.VARCHAR, 200, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ISSUE_COUNT, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_RATING, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_MQR_RATING, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_HOTSPOT_COUNT, Types.INTEGER, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_HOTSPOTS_REVIEWED, Types.INTEGER, null, true);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
