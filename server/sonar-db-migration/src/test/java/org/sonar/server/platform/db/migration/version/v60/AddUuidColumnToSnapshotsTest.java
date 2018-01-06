/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v60;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;

public class AddUuidColumnToSnapshotsTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddUuidColumnToSnapshotsTest.class, "old_snapshots.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AddUuidColumnToSnapshots underTest = new AddUuidColumnToSnapshots(db.database());

  @Test
  public void migration_adds_columns_to_empty_table() throws SQLException {
    underTest.execute();

    verifyAddedColumns();
  }

  @Test
  public void migration_adds_column_to_populated_table() throws SQLException {
    for (int i = 0; i < 9; i++) {
      db.executeInsert(
        "snapshots",
        "component_uuid", valueOf(i),
        "root_component_uuid", valueOf(i + 10),
        "QUALIFIER", (i % 2 == 0 ? "FIL" : "TRK"));
    }

    underTest.execute();

    verifyAddedColumns();
  }

  @Test
  public void migration_is_not_reentrant() throws SQLException {
    underTest.execute();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to execute ");
    underTest.execute();
  }

  private void verifyAddedColumns() {
    db.assertColumnDefinition("snapshots", "uuid", Types.VARCHAR, 50, true);
  }

}
