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
package org.sonar.server.platform.db.migration.version.v82;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.version.v82.CreateProjectsTable;

import static java.sql.Types.BIGINT;
import static java.sql.Types.BOOLEAN;
import static java.sql.Types.VARCHAR;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

public class CreateProjectsTableTest {
  private static final String TABLE_NAME = "projects";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createEmpty();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CreateProjectsTable underTest = new CreateProjectsTable(dbTester.database());

  @Test
  public void table_has_been_created() throws SQLException {
    underTest.execute();

    dbTester.assertTableExists(TABLE_NAME);
    dbTester.assertPrimaryKey(TABLE_NAME, "pk_new_projects", "uuid");
    dbTester.assertUniqueIndex(TABLE_NAME, "uniq_projects_kee", "kee");

    dbTester.assertColumnDefinition(TABLE_NAME, "uuid", VARCHAR, UUID_SIZE, false);
    dbTester.assertColumnDefinition(TABLE_NAME, "kee", VARCHAR, 400, false);
    dbTester.assertColumnDefinition(TABLE_NAME, "organization_uuid", VARCHAR, UUID_SIZE, false);
    dbTester.assertColumnDefinition(TABLE_NAME, "name", VARCHAR, 2000, true);
    dbTester.assertColumnDefinition(TABLE_NAME, "description", VARCHAR, 2000, true);
    dbTester.assertColumnDefinition(TABLE_NAME, "private", BOOLEAN, null, false);
    dbTester.assertColumnDefinition(TABLE_NAME, "tags", VARCHAR, 500, true);
    dbTester.assertColumnDefinition(TABLE_NAME, "created_at", BIGINT, null, true);
    dbTester.assertColumnDefinition(TABLE_NAME, "updated_at", BIGINT, null, false);

    // script should not fail if executed twice
    underTest.execute();
  }
}
