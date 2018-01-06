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
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class PopulateComponentUuidOfMeasuresTest {

  private static final int SNAPSHOT_ID_1 = 40;
  private static final int SNAPSHOT_ID_2 = 50;
  private static final int SNAPSHOT_ID_3 = 60;
  private static final int SNAPSHOT_ID_4 = 70;
  private static final int SNAPSHOT_ID_5 = 80;

  private static final int COMPONENT_ID_1 = 400;
  private static final int COMPONENT_ID_2 = 500;
  private static final int COMPONENT_ID_3 = 600;

  private static final String COMPONENT_UUID_1 = "U400";
  private static final String COMPONENT_UUID_2 = "U500";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateComponentUuidOfMeasuresTest.class,
    "in_progress_measures_with_projects.sql");

  private PopulateComponentUuidOfMeasures underTest = new PopulateComponentUuidOfMeasures(db.database());

  @Test
  public void migration_has_no_effect_on_empty_tables() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("project_measures")).isEqualTo(0);
    assertThat(db.countRowsOfTable("snapshots")).isEqualTo(0);
  }

  @Test
  public void migration_updates_component_uuid_with_values_from_table_snapshots_when_they_exist() throws SQLException {
    insertSnapshot(SNAPSHOT_ID_1, COMPONENT_UUID_1);
    insertSnapshot(SNAPSHOT_ID_2, COMPONENT_UUID_1);
    insertSnapshot(SNAPSHOT_ID_3, COMPONENT_UUID_2);
    insertSnapshot(SNAPSHOT_ID_4, COMPONENT_UUID_2);

    insertMeasure(1, SNAPSHOT_ID_1, COMPONENT_ID_1);
    insertMeasure(2, SNAPSHOT_ID_2, COMPONENT_ID_1);
    insertMeasure(3, SNAPSHOT_ID_3, COMPONENT_ID_2);
    insertMeasure(4, SNAPSHOT_ID_5, COMPONENT_ID_3); // snapshot does not exist

    underTest.execute();

    verifyMeasure(1, SNAPSHOT_ID_1, COMPONENT_UUID_1);
    verifyMeasure(2, SNAPSHOT_ID_2, COMPONENT_UUID_1);
    verifyMeasure(3, SNAPSHOT_ID_3, COMPONENT_UUID_2);
    verifyMeasure(4, SNAPSHOT_ID_5, null);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertSnapshot(SNAPSHOT_ID_1, COMPONENT_UUID_1);
    insertMeasure(1, SNAPSHOT_ID_1, COMPONENT_ID_1);

    underTest.execute();
    verifyMeasure(1, SNAPSHOT_ID_1, COMPONENT_UUID_1);

    underTest.execute();
    verifyMeasure(1, SNAPSHOT_ID_1, COMPONENT_UUID_1);

  }

  private void verifyMeasure(long id, long snapshotId, @Nullable String componentUuid) {
    List<Map<String, Object>> rows = db.select("select SNAPSHOT_ID, COMPONENT_UUID from project_measures where ID=" + id);
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.get(0);
    assertThat(row.get("SNAPSHOT_ID")).isEqualTo(snapshotId);
    assertThat(row.get("COMPONENT_UUID")).isEqualTo(componentUuid);
  }

  private void insertSnapshot(long id, String componentUuid) {
    db.executeInsert(
      "snapshots",
      "id", valueOf(id),
      "component_uuid", componentUuid,
      "root_component_uuid", "ROOT_" + componentUuid);
  }

  private void insertMeasure(long id, long snapshotId, long componentId) {
    db.executeInsert(
      "project_measures",
      "ID", valueOf(id),
      "METRIC_ID", valueOf(id + 10),
      "SNAPSHOT_ID", valueOf(snapshotId),
      "VALUE", valueOf(id + 1000),
      "PROJECT_ID", valueOf(componentId));

  }
}
