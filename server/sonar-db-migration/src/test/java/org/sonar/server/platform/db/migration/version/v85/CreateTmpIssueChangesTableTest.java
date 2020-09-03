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

import static java.sql.Types.BIGINT;
import static java.sql.Types.CLOB;
import static java.sql.Types.VARCHAR;

public class CreateTmpIssueChangesTableTest {

  private static final String TABLE_NAME = "tmp_issue_changes";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(CreateTmpIssueChangesTableTest.class, "schema.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CreateTmpIssueChangesTable underTest = new CreateTmpIssueChangesTable(dbTester.database());

  @Test
  public void table_has_been_created() throws SQLException {
    underTest.execute();

    dbTester.assertTableExists(TABLE_NAME);
    dbTester.assertColumnDefinition(TABLE_NAME, "uuid", VARCHAR, 40, true);
    dbTester.assertColumnDefinition(TABLE_NAME, "kee", VARCHAR, 50, true);
    dbTester.assertColumnDefinition(TABLE_NAME, "issue_key", VARCHAR, 50, true);
    dbTester.assertColumnDefinition(TABLE_NAME, "user_login", VARCHAR, 255, true);
    dbTester.assertColumnDefinition(TABLE_NAME, "change_type", VARCHAR, 20, true);
    dbTester.assertColumnDefinition(TABLE_NAME, "change_data", CLOB, null, true);
    dbTester.assertColumnDefinition(TABLE_NAME, "created_at", BIGINT, null, true);
    dbTester.assertColumnDefinition(TABLE_NAME, "updated_at", BIGINT, null, true);
    dbTester.assertColumnDefinition(TABLE_NAME, "issue_change_creation_date", BIGINT, null, true);
    dbTester.assertColumnDefinition(TABLE_NAME, "project_uuid", VARCHAR, 50, true);
  }

}
