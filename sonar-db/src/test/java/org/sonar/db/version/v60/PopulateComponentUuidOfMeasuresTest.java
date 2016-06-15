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
package org.sonar.db.version.v60;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class PopulateComponentUuidOfMeasuresTest {

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, PopulateComponentUuidOfMeasuresTest.class,
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
    String uuid1 = insertSnapshot(40);
    String uuid2 = insertSnapshot(50);
    String uuid3 = insertSnapshot(60);
    String uuid4 = insertSnapshot(70);

    insertMeasure(1, 40);
    insertMeasure(2, 60);
    insertMeasure(3, 90); // 90 does not exist
    insertMeasure(4, 100); // 100 does not exist
    db.commit();

    underTest.execute();

    verifyMeasure(1, 40, uuid1);
    verifyMeasure(2, 60, uuid3);
    verifyMeasure(3, 90, null);
    verifyMeasure(4, 100, null);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    String uuid1 = insertSnapshot(40);
    String uuid2 = insertSnapshot(50);
    insertMeasure(1, 40);

    underTest.execute();
    verifyMeasure(1, 40, uuid1);

    underTest.execute();
    verifyMeasure(1, 40, uuid1);

  }

  private void verifyMeasure(long id, long snapshotId, @Nullable String componentUuid) {
    List<Map<String, Object>> rows = db.select("select SNAPSHOT_ID, COMPONENT_UUID from project_measures where ID=" + id);
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.get(0);
    assertThat(row.get("SNAPSHOT_ID")).isEqualTo(snapshotId);
    assertThat(row.get("COMPONENT_UUID")).isEqualTo(componentUuid);
  }

  private String insertSnapshot(long id) {
    String uuid = "uuid_" + id;
    db.executeInsert(
      "snapshots",
      "id", valueOf(id),
      "component_uuid", uuid,
      "root_component_uuid", valueOf(id + 100));
    return uuid;
  }

  private void insertMeasure(long id, long snapshotId) {
    db.executeInsert(
      "project_measures",
      "ID", valueOf(id),
      "METRIC_ID", valueOf(id + 10),
      "SNAPSHOT_ID", valueOf(snapshotId),
      "VALUE", valueOf(id + 1000),
      "PROJECT_ID", valueOf(snapshotId + 1000));

  }
}
