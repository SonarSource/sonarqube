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
package org.sonar.server.platform.db.migration.version.v84.rules.issues;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.MigrationStep;
import org.sonar.server.platform.db.migration.version.v84.issuechanges.CopyIssueChangesTable;

public class CopyIssuesTableTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(CopyIssuesTableTest.class, "schema.sql");

  private MigrationStep underTest = new CopyIssuesTable(db.database());

  @Test
  public void execute() throws SQLException {
    db.assertTableExists("issues");
    db.assertTableDoesNotExist("issues_copy");

    underTest.execute();
    db.assertTableExists("issues");
    db.assertTableExists("issues_copy");
    db.assertColumnDefinition("issues_copy", "kee", Types.VARCHAR, 50, false);
    db.assertColumnDefinition("issues_copy", "message", Types.VARCHAR, 4000, true);
    db.assertColumnDefinition("issues_copy", "issue_type", Types.TINYINT, null, true);
  }
}
