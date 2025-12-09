/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

class DropIssueStatsByRuleKeyTableIT {
  public static final String TABLE_NAME = "issue_stats_by_rule_key";

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(DropIssueStatsByRuleKeyTable.class);
  private final CreateIssueStatsByRuleKeyTable createTable = new CreateIssueStatsByRuleKeyTable(db.database());
  private final DropIssueStatsByRuleKeyTable underTest = new DropIssueStatsByRuleKeyTable(db.database());

  @Test
  void execute_shouldDropTable() throws SQLException {
    createTable.execute();
    db.assertTableExists(TABLE_NAME);
    underTest.execute();
    db.assertTableDoesNotExist(TABLE_NAME);
  }

  @Test
  void execute_shouldSupportReentrantMigrationExecution() throws SQLException {
    createTable.execute();
    db.assertTableExists(TABLE_NAME);
    underTest.execute();
    underTest.execute();
    db.assertTableDoesNotExist(TABLE_NAME);
  }
}
