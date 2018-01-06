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

public class MakeUuidColumnNotNullOnSnapshotsTest {

  private static final String TABLE_SNAPSHOTS = "snapshots";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MakeUuidColumnNotNullOnSnapshotsTest.class,
    "in_progress_snapshots.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MakeUuidColumnNotNullOnSnapshots underTest = new MakeUuidColumnNotNullOnSnapshots(db.database());

  @Test
  public void migration_sets_uuid_column_not_nullable_on_empty_table() throws SQLException {
    underTest.execute();

    verifyColumnDefinitions();
  }

  @Test
  public void migration_sets_uuid_column_not_nullable_on_populated_table() throws SQLException {
    insertSnapshot(1, true);
    insertSnapshot(2, true);

    underTest.execute();

    verifyColumnDefinitions();
  }

  @Test
  public void migration_fails_if_some_row_has_a_null_uuid() throws SQLException {
    insertSnapshot(1, false);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to execute");

    underTest.execute();
  }

  private void verifyColumnDefinitions() {
    db.assertColumnDefinition(TABLE_SNAPSHOTS, "uuid", Types.VARCHAR, 50, false);
  }

  private String insertSnapshot(long id, boolean hasUuid) {
    String uuid = "uuid_" + id;
    db.executeInsert(
      TABLE_SNAPSHOTS,
      "ID", valueOf(id),
      "COMPONENT_UUID", valueOf(id + 10),
      "ROOT_COMPONENT_UUID", valueOf(id + 100),
      "UUID", hasUuid ? "uuid_" + id : null);
    return uuid;
  }

}
