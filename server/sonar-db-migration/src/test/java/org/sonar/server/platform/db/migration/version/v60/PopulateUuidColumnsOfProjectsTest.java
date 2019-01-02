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
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class PopulateUuidColumnsOfProjectsTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateUuidColumnsOfProjectsTest.class,
    "in_progress_projects.sql");

  private PopulateUuidColumnsOfProjects underTest = new PopulateUuidColumnsOfProjects(db.database());

  @Test
  public void migration_has_no_effect_on_empty_tables() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("projects")).isEqualTo(0);
  }

  @Test
  public void migration_updates_uuid_columns_with_values_from_table_projects_when_they_exist() throws SQLException {
    String uuid1 = insertComponent(1, null, null, null); // project
    insertComponent(2, 1L, null, null); // a module or something not a project
    String uuid3 = insertComponent(3, null, null, null); // developer
    insertComponent(4, 3L, 1L, 3L); // a project copy of project 1 for developer 3
    String uuid5 = insertComponent(5, null, null, null); // another project
    String uuid6 = insertComponent(6, null, null, null); // a view
    insertComponent(7, 6L, 5L, null); // a project view of project 5
    insertComponent(8, 200L, 1L, 200L); // a project copy of project 1 for developer 200 (does't exist)
    insertComponent(9, 6L, 300L, null); // a project view of project 300 (doesn't exist)
    insertComponent(10, 400L, null, null); // a module of a non existing project

    underTest.execute();

    verifyProject(1, null, uuid1, null, null, null, null);
    verifyProject(2, 1L, uuid1, null, null, null, null);
    verifyProject(3, null, uuid3, null, null, null, null);
    verifyProject(4, 3L, uuid3, 1L, uuid1, 3L, uuid3);
    verifyProject(5, null, uuid5, null, null, null, null);
    verifyProject(6, null, uuid6, null, null, null, null);
    verifyProject(7, 6L, uuid6, 5L, uuid5, null, null);
    verifyProject(9, 6L, uuid6, 300L, null, null, null);
    verifyProject(10, 400L, null, null, null, null, null);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    String uuid1 = insertComponent(1, null, null, null); // project

    underTest.execute();
    verifyProject(1, null, uuid1, null, null, null, null);

    underTest.execute();
    verifyProject(1, null, uuid1, null, null, null, null);
  }

  private String insertComponent(long id, @Nullable Long rootId, @Nullable Long copyResourceId, @Nullable Long personId) {
    String uuid = "uuid_" + id;
    db.executeInsert(
      "projects",
      "ID", valueOf(id),
      "UUID", uuid,
      "ROOT_ID", toString(rootId),
      "COPY_RESOURCE_ID", toString(copyResourceId),
      "PERSON_ID", toString(personId));
    return uuid;
  }

  private static String toString(@Nullable Long aLong) {
    return aLong == null ? null : valueOf(aLong);
  }

  private void verifyProject(long id, @Nullable Long rootId, @Nullable String rootUuid, @Nullable Long copyResourceId, @Nullable String copyComponentUuuid, @Nullable Long personId,
    @Nullable String developerUuid) {
    List<Map<String, Object>> rows = db.select("select ROOT_ID, ROOT_UUID, COPY_RESOURCE_ID, COPY_COMPONENT_UUID, PERSON_ID, DEVELOPER_UUID from projects where ID=" + id);
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.get(0);
    assertThat(row.get("ROOT_ID")).isEqualTo(rootId);
    assertThat(row.get("ROOT_UUID")).isEqualTo(rootUuid);
    assertThat(row.get("COPY_RESOURCE_ID")).isEqualTo(copyResourceId);
    assertThat(row.get("COPY_COMPONENT_UUID")).isEqualTo(copyComponentUuuid);
    assertThat(row.get("PERSON_ID")).isEqualTo(personId);
    assertThat(row.get("DEVELOPER_UUID")).isEqualTo(developerUuid);
  }

}
