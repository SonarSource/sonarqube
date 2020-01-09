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

import static org.apache.commons.lang.math.RandomUtils.nextInt;

public class RenameProjectsTableToComponentsTest {
  private static final String TABLE_NAME = "projects";
  private static final String NEW_TABLE_NAME = "components";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(RenameProjectsTableToComponentsTest.class, "schema.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CreateProjectsTable underTest = new CreateProjectsTable(dbTester.database());

  @Test
  public void table_has_been_renamed() throws SQLException {

    underTest.execute();

    dbTester.assertTableExists(TABLE_NAME);
    dbTester.assertPrimaryKey(TABLE_NAME, "pk_projects", "id");

    dbTester.assertIndex(TABLE_NAME, "PROJECTS_ORGANIZATION", "organization_uuid");
    dbTester.assertUniqueIndex(TABLE_NAME, "PROJECTS_KEE", "kee");
    dbTester.assertIndex(TABLE_NAME, "PROJECTS_ROOT_UUID", "root_uuid");
    dbTester.assertUniqueIndex(TABLE_NAME, "PROJECTS_UUID", "uuid");
    dbTester.assertIndex(TABLE_NAME, "PROJECTS_PROJECT_UUID", "project_uuid");
    dbTester.assertIndex(TABLE_NAME, "PROJECTS_MODULE_UUID", "module_uuid");
    dbTester.assertIndex(TABLE_NAME, "PROJECTS_QUALIFIER", "qualifier");

    underTest.execute();

    dbTester.assertTableExists(TABLE_NAME);
  }

}
