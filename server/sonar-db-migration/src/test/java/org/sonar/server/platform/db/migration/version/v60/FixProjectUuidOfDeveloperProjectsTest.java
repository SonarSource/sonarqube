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
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class FixProjectUuidOfDeveloperProjectsTest {

  private static final String TABLE_PROJECTS = "projects";
  private static final String PROJECT_UUID = "U1";
  private static final String FILE_UUID = "U2";
  private static final String DEVELOPER_UUID = "U3";
  private static final String DEV1_IN_PROJECT_UUID = "U4";
  private static final String DEV2_IN_PROJECT_UUID = "U5";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(FixProjectUuidOfDeveloperProjectsTest.class,
    "projects_5.6.sql");

  private FixProjectUuidOfDeveloperProjects underTest = new FixProjectUuidOfDeveloperProjects(db.database());

  @Test
  public void migration_has_no_effect_on_empty_tables() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_PROJECTS)).isEqualTo(0);
  }

  @Test
  public void migration_fixes_project_uuid_of_rows_with_qualifier_DEV_PRJ() throws SQLException {
    insertComponents();

    underTest.execute();

    verifyComponents();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertComponents();

    underTest.execute();
    verifyComponents();

    underTest.execute();
    verifyComponents();
  }

  private void verifyComponents() {
    verifyProjectUuid(PROJECT_UUID, PROJECT_UUID);
    verifyProjectUuid(FILE_UUID, PROJECT_UUID);
    verifyProjectUuid(DEVELOPER_UUID, DEVELOPER_UUID);
    verifyProjectUuid(DEV1_IN_PROJECT_UUID, DEVELOPER_UUID);
    verifyProjectUuid(DEV2_IN_PROJECT_UUID, DEVELOPER_UUID);
  }

  private void insertComponents() {
    // regular project
    insert(PROJECT_UUID, "TRK", null, PROJECT_UUID);
    insert(FILE_UUID, "FIL", null, PROJECT_UUID);
    // developer
    Long personId = insert(DEVELOPER_UUID, "DEV", null, DEVELOPER_UUID);
    insert(DEV1_IN_PROJECT_UUID, "DEV_PRJ", personId, /* not correct */PROJECT_UUID);
    insert(DEV2_IN_PROJECT_UUID, "DEV_PRJ", personId, /* not correct */PROJECT_UUID);
  }

  private void verifyProjectUuid(String uuid, @Nullable String expectedProjectUuid) {
    Map<String, Object> rows = db.selectFirst("select project_uuid as \"projectUuid\" from projects where uuid='" + uuid + "'");
    assertThat(rows.get("projectUuid")).isEqualTo(expectedProjectUuid);
  }

  private Long insert(String uuid, String qualifier, @Nullable Long personId, String projectUuid) {
    db.executeInsert(
      TABLE_PROJECTS,
      "UUID", uuid,
      "PERSON_ID", personId == null ? null : valueOf(personId),
      "PROJECT_UUID", projectUuid,
      "QUALIFIER", qualifier);

    return db.select("select ID from projects where UUID='" + uuid + "'").stream()
      .findFirst()
      .map(f -> (Long) f.get("ID"))
      .orElseThrow(() -> new IllegalStateException("NO ID??"));
  }
}
