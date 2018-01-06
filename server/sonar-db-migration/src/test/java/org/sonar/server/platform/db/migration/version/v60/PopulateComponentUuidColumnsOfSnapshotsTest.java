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

public class PopulateComponentUuidColumnsOfSnapshotsTest {

  private static final String SNAPSHOTS_TABLE = "snapshots";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateComponentUuidColumnsOfSnapshotsTest.class,
    "in_progress_snapshots_with_projects.sql");

  private PopulateComponentUuidColumnsOfSnapshots underTest = new PopulateComponentUuidColumnsOfSnapshots(db.database());

  @Test
  public void migration_has_no_effect_on_empty_tables() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable(SNAPSHOTS_TABLE)).isEqualTo(0);
    assertThat(db.countRowsOfTable("projects")).isEqualTo(0);
  }

  @Test
  public void migration_updates_uuid_columns_with_values_from_table_projects_when_they_exist() throws SQLException {
    String uuid1 = insertComponent(40);
    String uuid2 = insertComponent(50);
    String uuid3 = insertComponent(60);
    String uuid4 = insertComponent(70);
    String uuid5 = insertComponent(80);

    insertSnapshots(1, 40, 50L);
    insertSnapshots(2, 60, 70L);
    insertSnapshots(3, 90, 70L); // 90 does not exist
    insertSnapshots(4, 40, 100L); // 100 does not exist
    insertSnapshots(5, 110, 100L); // 110 and 100 do not exist
    insertSnapshots(6, 80, null); // no root
    insertSnapshots(7, 120, null); // no root and 120 does not exist

    underTest.execute();

    verifySnapshots(1, 40, uuid1, 50L, uuid2);
    verifySnapshots(2, 60, uuid3, 70L, uuid4);
    verifySnapshots(3, 90, null, 70L, uuid4);
    verifySnapshots(4, 40, uuid1, 100L, null);
    verifySnapshots(5, 110, null, 100L, null);
    verifySnapshots(6, 80, uuid5, null, null);
    verifySnapshots(7, 120, null, null, null);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    String uuid1 = insertComponent(40);
    String uuid2 = insertComponent(50);
    insertSnapshots(1, 40, 50L);

    underTest.execute();
    verifySnapshots(1, 40, uuid1, 50L, uuid2);

    underTest.execute();
    verifySnapshots(1, 40, uuid1, 50L, uuid2);
  }

  private void insertSnapshots(long id, long projectId, @Nullable Long rootId) {
    db.executeInsert(
      SNAPSHOTS_TABLE,
      "ID", valueOf(id),
      "ISLAST", "TRUE",
      "PROJECT_ID", valueOf(projectId),
      "ROOT_PROJECT_ID", rootId == null ? null : valueOf(rootId));
  }

  private String insertComponent(long id) {
    String uuid = "uuid_" + id;
    db.executeInsert(
      "projects",
      "ID", valueOf(id),
      "UUID", uuid);
    return uuid;
  }

  private void verifySnapshots(long id, long resourceId, @Nullable String componentUuid, @Nullable Long rootProjectId, @Nullable String rootComponentUuid) {
    List<Map<String, Object>> rows = db.select("select PROJECT_ID, COMPONENT_UUID, ROOT_PROJECT_ID, ROOT_COMPONENT_UUID from snapshots where ID=" + id);
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.get(0);
    assertThat(row.get("PROJECT_ID")).isEqualTo(resourceId);
    assertThat(row.get("COMPONENT_UUID")).isEqualTo(componentUuid);
    assertThat(row.get("ROOT_PROJECT_ID")).isEqualTo(rootProjectId);
    assertThat(row.get("ROOT_COMPONENT_UUID")).isEqualTo(rootComponentUuid);
  }

}
