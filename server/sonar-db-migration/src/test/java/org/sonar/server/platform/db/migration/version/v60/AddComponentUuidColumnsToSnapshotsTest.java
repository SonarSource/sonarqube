/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

public class AddComponentUuidColumnsToSnapshotsTest {

  private static final String SNAPSHOTS_TABLE = "snapshots";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddComponentUuidColumnsToSnapshotsTest.class, "old_snapshots.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AddComponentUuidColumnsToSnapshots underTest = new AddComponentUuidColumnsToSnapshots(db.database());

  @Test
  public void migration_adds_columns_to_empty_table() throws SQLException {
    underTest.execute();

    verifyAddedColumns();
  }

  @Test
  public void migration_adds_columns_to_populated_table() throws SQLException {
    for (int i = 0; i < 9; i++) {
      db.executeInsert(
        SNAPSHOTS_TABLE,
        "PROJECT_ID", valueOf(i),
        "ISLAST", "TRUE");
    }

    underTest.execute();

    verifyAddedColumns();
  }

  private void verifyAddedColumns() {
    db.assertColumnDefinition(SNAPSHOTS_TABLE, "component_uuid", Types.VARCHAR, 50, true);
    db.assertColumnDefinition(SNAPSHOTS_TABLE, "root_component_uuid", Types.VARCHAR, 50, true);
  }

  @Test
  public void migration_is_not_reentrant() throws SQLException {
    underTest.execute();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to execute ");
    underTest.execute();
  }
}
