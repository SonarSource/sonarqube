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

public class MakeComponentUuidColumnsNotNullOnSnapshotsTest {

  private static final String SNAPSHOTS_TABLE = "snapshots";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MakeComponentUuidColumnsNotNullOnSnapshotsTest.class,
    "in_progress_snapshots.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MakeComponentUuidColumnsNotNullOnSnapshots underTest = new MakeComponentUuidColumnsNotNullOnSnapshots(db.database());

  @Test
  public void migration_sets_uuid_columns_not_nullable_on_empty_table() throws SQLException {
    underTest.execute();

    verifyColumnDefinitions();
    verifyIndices();
  }

  @Test
  public void migration_sets_uuid_columns_not_nullable_on_populated_table() throws SQLException {
    insertSnapshots(1, true, true);
    insertSnapshots(2, true, true);

    underTest.execute();

    verifyColumnDefinitions();
    verifyIndices();
  }

  @Test
  public void migration_fails_if_some_uuid_columns_are_null() throws SQLException {
    insertSnapshots(1, false, true);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to execute");

    underTest.execute();
  }

  private void verifyColumnDefinitions() {
    db.assertColumnDefinition(SNAPSHOTS_TABLE, "component_uuid", Types.VARCHAR, 50, false);
    db.assertColumnDefinition(SNAPSHOTS_TABLE, "root_component_uuid", Types.VARCHAR, 50, false);
  }

  private void verifyIndices() {
    db.assertIndex(SNAPSHOTS_TABLE, "snapshot_component", "component_uuid");
    db.assertIndex(SNAPSHOTS_TABLE, "snapshot_root_component", "root_component_uuid");
  }

  private void insertSnapshots(long id, boolean hasComponentUiid, boolean hasRootComponentUuid) {
    db.executeInsert(
      SNAPSHOTS_TABLE,
      "ID", valueOf(id),
      "ISLAST", "TRUE",
      "PROJECT_ID", valueOf(id + 300),
      "COMPONENT_UUID", hasComponentUiid ? "uuid_" + id : null,
      "ROOT_COMPONENT_UUID", hasRootComponentUuid ? "root_uuid_" + id : null);
  }

}
