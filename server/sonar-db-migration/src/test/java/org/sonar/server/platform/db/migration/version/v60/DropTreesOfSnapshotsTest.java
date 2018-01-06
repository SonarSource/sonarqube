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
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class DropTreesOfSnapshotsTest {

  private static final String SNAPSHOTS_TABLE = "snapshots";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DropTreesOfSnapshotsTest.class,
    "in_progress_snapshots.sql");

  private DropTreesOfSnapshots underTest = new DropTreesOfSnapshots(db.database());

  @Test
  public void migration_has_no_effect_on_empty_tables() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable(SNAPSHOTS_TABLE)).isEqualTo(0);
  }

  @Test
  public void migration_deletes_snapshots_of_non_root_components() throws SQLException {
    insertSnapshot(1L, 0);
    insertSnapshot(2L, 2);
    insertSnapshot(3L, 1);
    insertSnapshot(4L, 0);
    insertSnapshot(5L, 3);

    underTest.execute();

    verifySnapshots(1L, 4L);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertSnapshot(1L, 0);
    insertSnapshot(2L, 2);

    underTest.execute();
    verifySnapshots(1L);

    underTest.execute();
    verifySnapshots(1L);
  }

  private void insertSnapshot(long id, int depth) {
    db.executeInsert(
      SNAPSHOTS_TABLE,
      "ID", valueOf(id),
      "DEPTH", valueOf(depth),
      "COMPONENT_UUID", valueOf(id + 10),
      "UUID", valueOf(id + 100));
  }

  private void verifySnapshots(long... expectedIds) {
    List<Map<String, Object>> rows = db.select("select id from snapshots");
    long[] ids = rows.stream()
      .mapToLong(row -> (long) row.get("ID"))
      .toArray();
    assertThat(ids).containsOnly(expectedIds);
  }

}
