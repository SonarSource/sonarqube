/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.platform.db.migration.version.v60;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static java.lang.String.valueOf;

public class AddUuidColumnsToResourceIndexTest {

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, AddUuidColumnsToResourceIndexTest.class, "old_resourceindex.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AddUuidColumnsToResourceIndex underTest = new AddUuidColumnsToResourceIndex(db.database());

  @Test
  public void migration_adds_columns_to_empty_table() throws SQLException {
    underTest.execute();

    verifyAddedColumns();
  }

  @Test
  public void migration_adds_columns_to_populated_table() throws SQLException {
    for (int i = 0; i < 9; i++) {
      db.executeInsert(
        "resource_index",
        "KEE", "key_" + i,
        "POSITION", valueOf(i),
        "NAME_SIZE", valueOf(i + 1),
        "RESOURCE_ID", valueOf(i + 10),
        "ROOT_PROJECT_ID", valueOf(i + 20),
        "QUALIFIER", (i % 2 == 0 ? "FILE" : "PROJECT"));
    }
    db.commit();

    underTest.execute();

    verifyAddedColumns();
  }

  private void verifyAddedColumns() {
    db.assertColumnDefinition("resource_index", "component_uuid", Types.VARCHAR, 50, true);
    db.assertColumnDefinition("resource_index", "root_component_uuid", Types.VARCHAR, 50, true);
  }

  @Test
  public void migration_is_not_reentrant() throws SQLException {
    underTest.execute();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to execute ");
    underTest.execute();
  }
}
