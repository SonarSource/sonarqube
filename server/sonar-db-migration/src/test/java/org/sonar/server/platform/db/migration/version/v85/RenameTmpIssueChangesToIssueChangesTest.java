/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v85;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

public class RenameTmpIssueChangesToIssueChangesTest {

  private static final String OLD_TABLE_NAME = "tmp_issue_changes";
  private static final String NEW_TABLE_NAME = "issue_changes";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(RenameTmpIssueChangesToIssueChangesTest.class, "schema.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final RenameTmpIssueChangesToIssueChanges underTest = new RenameTmpIssueChangesToIssueChanges(dbTester.database());

  @Test
  public void table_has_been_renamed() throws SQLException {
    underTest.execute();

    dbTester.assertTableDoesNotExist(OLD_TABLE_NAME);
    dbTester.assertTableExists(NEW_TABLE_NAME);
  }

}
