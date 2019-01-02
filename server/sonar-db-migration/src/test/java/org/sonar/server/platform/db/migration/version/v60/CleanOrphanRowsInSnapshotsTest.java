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

public class CleanOrphanRowsInSnapshotsTest {

  private static final String SNAPSHOTS_TABLE = "snapshots";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(CleanOrphanRowsInSnapshotsTest.class,
    "in_progress_snapshots_and_children_tables.sql");

  private CleanOrphanRowsInSnapshots underTest = new CleanOrphanRowsInSnapshots(db.database());

  @Test
  public void migration_has_no_effect_on_empty_table() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable(SNAPSHOTS_TABLE)).isEqualTo(0);
  }

  @Test
  public void migration_deletes_any_row_with_a_null_uuid() throws SQLException {
    insertSnapshots(1, true, true);
    insertSnapshots(2, false, false);
    insertSnapshots(3, true, false);
    insertSnapshots(4, false, true);
    insertSnapshots(5, true, true);

    underTest.execute();

    assertThat(idsOfRowsInSnapshots()).containsOnly(1l, 5l);
  }

  @Test
  public void migration_deletes_rows_in_children_tables_referencing_snapshots_with_at_least_null_uuid() throws SQLException {
    insertSnapshots(1, true, true);
    insertSnapshots(2, false, true);
    insertSnapshots(3, true, false);
    insertDuplicationIndex(1, 1);
    insertDuplicationIndex(30, 1);
    insertDuplicationIndex(1, 40);
    insertDuplicationIndex(50, 2);
    insertDuplicationIndex(2, 2);
    insertDuplicationIndex(2, 60);
    insertDuplicationIndex(3, 3);
    insertDuplicationIndex(70, 3);
    insertDuplicationIndex(3, 90);
    insertProjectMeasure(1);
    insertProjectMeasure(2);
    insertProjectMeasure(3);
    insertCeActivity(1);
    insertCeActivity(2);
    insertCeActivity(3);
    insertEvents(1);
    insertEvents(2);
    insertEvents(3);

    underTest.execute();

    verifyLineCountsPerSnapshot(1, 1, 3, 1, 1, 1);
    verifyLineCountsPerSnapshot(2, 0, 0, 0, 0, 0);
    verifyLineCountsPerSnapshot(3, 0, 0, 0, 0, 0);
  }

  private void verifyLineCountsPerSnapshot(int snapshotId, int snapshotCount, int duplicationIndexCount, int projectMeasureCount, int ceActivityCount, int eventCount) {
    assertThat(count("SNAPSHOTS where id=" + snapshotId)).isEqualTo(snapshotCount);
    assertThat(count("DUPLICATIONS_INDEX where snapshot_id=" + snapshotId + " or project_snapshot_id=" + snapshotId)).isEqualTo(duplicationIndexCount);
    assertThat(count("PROJECT_MEASURES where snapshot_id=" + snapshotId)).isEqualTo(projectMeasureCount);
    assertThat(count("CE_ACTIVITY where snapshot_id=" + snapshotId)).isEqualTo(ceActivityCount);
    assertThat(count("EVENTS where snapshot_id=" + snapshotId)).isEqualTo(eventCount);
  }

  private long count(String tableAndWhereClause) {
    return (Long) db.selectFirst("select count(*) from " + tableAndWhereClause).entrySet().iterator().next().getValue();
  }

  private void insertDuplicationIndex(int snapshotId, int parentSnapshotId) {
    db.executeInsert(
      "DUPLICATIONS_INDEX",
      "SNAPSHOT_ID", valueOf(snapshotId),
      "PROJECT_SNAPSHOT_ID", valueOf(parentSnapshotId),
      "HASH", "hash_" + snapshotId + "-" + parentSnapshotId,
      "INDEX_IN_FILE", valueOf(snapshotId + parentSnapshotId),
      "START_LINE", "1",
      "END_LINE", "1");
  }

  private void insertProjectMeasure(int snapshotId) {
    db.executeInsert(
      "PROJECT_MEASURES",
      "SNAPSHOT_ID", valueOf(snapshotId),
      "METRIC_ID", "111");
  }

  private void insertCeActivity(int snapshotId) {
    db.executeInsert(
      "CE_ACTIVITY",
      "UUID", valueOf(snapshotId + 10),
      "TASK_TYPE", "REPORT",
      "STATUS", "OK",
      "COMPONENT_UUID", valueOf(snapshotId + 20),
      "SNAPSHOT_ID", valueOf(snapshotId),
      "IS_LAST", "true",
      "IS_LAST_KEY", "key",
      "SUBMITTED_AT", "984651",
      "CREATED_AT", "984651",
      "UPDATED_AT", "984651");
  }

  private void insertEvents(int snapshotId) {
    db.executeInsert(
      "EVENTS",
      "SNAPSHOT_ID", valueOf(snapshotId),
      "EVENT_DATE", "984651",
      "CREATED_AT", "984651");
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

  private List<Long> idsOfRowsInSnapshots() {
    return db.select("select ID from snapshots").stream().map(map -> (Long) map.get("ID")).collect(Collectors.toList());
  }

}
