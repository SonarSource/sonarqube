/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

public class PopulateUuidColumnsOfResourceIndexTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateUuidColumnsOfResourceIndexTest.class,
    "in_progress_resourceindex_with_projects.sql");

  private PopulateUuidColumnsOfResourceIndex underTest = new PopulateUuidColumnsOfResourceIndex(db.database());

  @Test
  public void migration_has_no_effect_on_empty_tables() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("resource_index")).isEqualTo(0);
    assertThat(db.countRowsOfTable("projects")).isEqualTo(0);
  }

  @Test
  public void migration_updates_uuid_columns_with_values_from_table_projects_when_they_exist() throws SQLException {
    String uuid1 = insertComponent(40);
    String uuid2 = insertComponent(50);
    String uuid3 = insertComponent(60);
    String uuid4 = insertComponent(70);

    insertResourceIndex(1, 40, 50);
    insertResourceIndex(2, 60, 70);
    insertResourceIndex(3, 90, 70); // 90 does not exist
    insertResourceIndex(4, 40, 100); // 100 does not exist
    insertResourceIndex(5, 110, 100); // 110 and 100 do not exist

    underTest.execute();

    verifyResourceIndex(1, 40, uuid1, 50, uuid2);
    verifyResourceIndex(2, 60, uuid3, 70, uuid4);
    verifyResourceIndex(3, 90, null, 70, uuid4);
    verifyResourceIndex(4, 40, uuid1, 100, null);
    verifyResourceIndex(5, 110, null, 100, null);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    String uuid1 = insertComponent(40);
    String uuid2 = insertComponent(50);
    insertResourceIndex(1, 40, 50);

    underTest.execute();
    verifyResourceIndex(1, 40, uuid1, 50, uuid2);

    underTest.execute();
    verifyResourceIndex(1, 40, uuid1, 50, uuid2);

  }

  private void verifyResourceIndex(long id, long resourceId, @Nullable String componentUuid, long rootProjectId, @Nullable String rootComponentUuid) {
    List<Map<String, Object>> rows = db.select("select RESOURCE_ID, COMPONENT_UUID, ROOT_PROJECT_ID, ROOT_COMPONENT_UUID from resource_index where ID=" + id);
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.get(0);
    assertThat(row.get("RESOURCE_ID")).isEqualTo(resourceId);
    assertThat(row.get("COMPONENT_UUID")).isEqualTo(componentUuid);
    assertThat(row.get("ROOT_PROJECT_ID")).isEqualTo(rootProjectId);
    assertThat(row.get("ROOT_COMPONENT_UUID")).isEqualTo(rootComponentUuid);
  }

  private String insertComponent(long id) {
    String uuid = "uuid_" + id;
    db.executeInsert(
      "projects",
      "ID", valueOf(id),
      "UUID", uuid);
    return uuid;
  }

  private void insertResourceIndex(long id, long resourceId, long rootProjectId) {
    db.executeInsert(
      "resource_index",
      "ID", valueOf(id),
      "KEE", "key_" + id,
      "POSITION", valueOf(id + 100),
      "NAME_SIZE", valueOf(id + 1000),
      "RESOURCE_ID", valueOf(resourceId),
      "ROOT_PROJECT_ID", valueOf(rootProjectId),
      "QUALIFIER", "PROJECT");
  }
}
