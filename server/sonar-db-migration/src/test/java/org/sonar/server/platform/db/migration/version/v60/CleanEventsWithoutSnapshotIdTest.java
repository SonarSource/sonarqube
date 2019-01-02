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
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class CleanEventsWithoutSnapshotIdTest {

  private static final String TABLE_EVENTS = "events";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(CleanEventsWithoutSnapshotIdTest.class,
    "in_progress_events.sql");

  private CleanEventsWithoutSnapshotId underTest = new CleanEventsWithoutSnapshotId(db.database());

  @Test
  public void migration_has_no_effect_on_empty_table() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_EVENTS)).isEqualTo(0);
  }

  @Test
  public void migration_deletes_any_row_with_a_null_uuid() throws SQLException {
    insertEvent(1, true);
    insertEvent(2, false);
    insertEvent(3, false);
    insertEvent(4, true);

    underTest.execute();

    assertThat(idsOfRowsInResourceIndex()).containsOnly(1L, 4L);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertEvent(1, true);
    insertEvent(2, false);

    underTest.execute();

    assertThat(idsOfRowsInResourceIndex()).containsOnly(1L);

    underTest.execute();

    assertThat(idsOfRowsInResourceIndex()).containsOnly(1L);
  }

  private List<Long> idsOfRowsInResourceIndex() {
    return db.select("select ID from events").stream().map(map -> (Long) map.get("ID")).collect(Collectors.toList());
  }

  private void insertEvent(long id, boolean hasSnapshotId) {
    db.executeInsert(
      TABLE_EVENTS,
      "ID", valueOf(id),
      "NAME", "name_" + id,
      "SNAPSHOT_ID", hasSnapshotId ? valueOf(id + 100) : null,
      "EVENT_DATE", valueOf(1 + 100),
      "CREATED_AT", valueOf(1 + 300));
  }

}
