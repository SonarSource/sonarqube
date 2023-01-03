/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.issuechanges;

import java.sql.JDBCType;
import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.MigrationStep;

public class CopyIssueChangesTableTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(CopyIssueChangesTableTest.class, "schema.sql");

  private MigrationStep underTest = new CopyIssueChangesTable(db.database());

  @Test
  public void execute() throws SQLException {
    db.assertTableExists("issue_changes");
    db.assertTableDoesNotExist("issue_changes_copy");

    underTest.execute();
    db.assertTableExists("issue_changes");
    db.assertTableExists("issue_changes_copy");
    db.assertColumnDefinition("issue_changes_copy", "uuid", Types.VARCHAR, 40, false);
    db.assertColumnDefinition("issue_changes_copy", "issue_key", Types.VARCHAR, 50, false);
    db.assertColumnDefinition("issue_changes_copy", "updated_at", Types.BIGINT, null, true);
  }
}
