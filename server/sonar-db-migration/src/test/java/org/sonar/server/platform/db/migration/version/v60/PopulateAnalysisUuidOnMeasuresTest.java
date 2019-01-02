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
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class PopulateAnalysisUuidOnMeasuresTest {

  private static final String TABLE_MEASURES = "project_measures";
  private static final String TABLE_SNAPSHOTS = "snapshots";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateAnalysisUuidOnMeasuresTest.class,
    "old_measures.sql");

  private PopulateAnalysisUuidOnMeasures underTest = new PopulateAnalysisUuidOnMeasures(db.database());

  @Test
  public void migration_has_no_effect_on_empty_tables() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_MEASURES)).isEqualTo(0);
  }

  @Test
  public void migration_populates_analysis_uuids() throws SQLException {
    insertSnapshot(1, "U1", Qualifiers.PROJECT, null);
    insertSnapshot(2, "U2", Qualifiers.DIRECTORY, 1L);
    insertSnapshot(3, "U3", Qualifiers.FILE, 1L);
    insertMeasure(21, 1);
    insertMeasure(22, 2);
    insertMeasure(23, 3);

    underTest.execute();

    verifyAnalysisUuid(21, "U1");
    verifyAnalysisUuid(22, "U1");
    verifyAnalysisUuid(23, "U1");
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertSnapshot(1, "U1", Qualifiers.PROJECT, 1L);
    insertMeasure(21, 1);

    underTest.execute();
    verifyAnalysisUuid(21, "U1");

    underTest.execute();
    verifyAnalysisUuid(21, "U1");
  }

  private void verifyAnalysisUuid(int measureId, @Nullable String expectedAnalysisUuid) {
    Map<String, Object> rows = db.selectFirst("select analysis_uuid as \"analysisUuid\" from project_measures where id=" + measureId);
    assertThat(rows.get("analysisUuid")).isEqualTo(expectedAnalysisUuid);
  }

  private void insertSnapshot(long id, String uuid, String qualifier, @Nullable Long rootSnapshotId) {
    int depth;
    switch (qualifier) {
      case "TRK":
        depth = 0;
        break;
      case "BRC":
        depth = 1;
        break;
      case "DIR":
        depth = 2;
        break;
      case "FIL":
        depth = 3;
        break;
      default:
        throw new IllegalArgumentException();
    }
    db.executeInsert(
      TABLE_SNAPSHOTS,
      "ID", valueOf(id),
      "UUID", uuid,
      "COMPONENT_UUID", valueOf(id + 10),
      "ROOT_COMPONENT_UUID", valueOf(id + 10),
      "ROOT_SNAPSHOT_ID", rootSnapshotId != null ? valueOf(rootSnapshotId) : null,
      "QUALIFIER", qualifier,
      "DEPTH", valueOf(depth));
  }

  private void insertMeasure(long id, long snapshotId) {
    db.executeInsert(
      TABLE_MEASURES,
      "ID", valueOf(id),
      "SNAPSHOT_ID", valueOf(snapshotId),
      "METRIC_ID", valueOf(id + 100),
      "VALUE", valueOf(id + 200),
      "COMPONENT_UUID", valueOf(id + 300));
  }
}
