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
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class PopulateUuidPathColumnOnProjectsTest {

  private static final String TABLE_PROJECTS = "projects";
  private static final String TABLE_SNAPSHOTS = "snapshots";
  private static final String A_PROJECT_UUID = "U_PRJ";
  private static final String A_MODULE_UUID = "U_MOD";
  private static final String A_DIR_UUID = "U_DIR";
  private static final String A_FILE_UUID = "U_FIL";
  private static final String QUALIFIER_PROJECT = "TRK";
  private static final String QUALIFIER_MODULE = "BRC";
  private static final String QUALIFIER_DIR = "DIR";
  private static final String QUALIFIER_FILE = "FIL";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateUuidPathColumnOnProjectsTest.class,
    "in_progress_projects_and_snapshots.sql");

  private PopulateUuidPathColumnOnProjects underTest = new PopulateUuidPathColumnOnProjects(db.database());

  @Test
  public void has_no_effect_on_empty_tables() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_PROJECTS)).isEqualTo(0);
  }

  @Test
  public void migrates_provisioned_projects() throws SQLException {
    insert(QUALIFIER_PROJECT, A_PROJECT_UUID, A_PROJECT_UUID);

    underTest.execute();

    verifyPath(A_PROJECT_UUID, ".");
  }

  @Test
  public void migrates_projects_without_modules() throws SQLException {
    insert(QUALIFIER_PROJECT, A_PROJECT_UUID, A_PROJECT_UUID, new Snapshot(1L, "", true));
    insert(QUALIFIER_DIR, A_DIR_UUID, A_PROJECT_UUID, new Snapshot(2L, "1.", true));
    insert(QUALIFIER_FILE, A_FILE_UUID, A_PROJECT_UUID, new Snapshot(3L, "1.2.", true));

    underTest.execute();

    verifyPath(A_PROJECT_UUID, ".");
    verifyPath(A_DIR_UUID, format(".%s.", A_PROJECT_UUID));
    verifyPath(A_FILE_UUID, format(".%s.%s.", A_PROJECT_UUID, A_DIR_UUID));
  }

  @Test
  public void migrates_projects_with_modules() throws SQLException {
    insert(QUALIFIER_PROJECT, A_PROJECT_UUID, A_PROJECT_UUID, new Snapshot(1L, "", true));
    insert(QUALIFIER_MODULE, A_MODULE_UUID, A_PROJECT_UUID, new Snapshot(2L, "1.", true));
    insert(QUALIFIER_DIR, A_DIR_UUID, A_PROJECT_UUID, new Snapshot(3L, "1.2.", true));
    insert(QUALIFIER_FILE, A_FILE_UUID, A_PROJECT_UUID, new Snapshot(4L, "1.2.3.", true));

    underTest.execute();

    verifyPath(A_PROJECT_UUID, ".");
    verifyPath(A_MODULE_UUID, format(".%s.", A_PROJECT_UUID));
    verifyPath(A_DIR_UUID, format(".%s.%s.", A_PROJECT_UUID, A_MODULE_UUID));
    verifyPath(A_FILE_UUID, format(".%s.%s.%s.", A_PROJECT_UUID, A_MODULE_UUID, A_DIR_UUID));
  }

  @Test
  public void migrates_components_without_snapshot_path() throws SQLException {
    // these components do not have snapshots
    insert(QUALIFIER_DIR, A_DIR_UUID, A_PROJECT_UUID);
    insert(QUALIFIER_FILE, A_FILE_UUID, A_PROJECT_UUID);

    underTest.execute();

    verifyPath(A_DIR_UUID, format(".%s.", A_PROJECT_UUID));
    verifyPath(A_FILE_UUID, format(".%s.", A_PROJECT_UUID));
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insert(QUALIFIER_PROJECT, A_PROJECT_UUID, A_PROJECT_UUID, new Snapshot(1L, "", true));
    insert(QUALIFIER_DIR, A_DIR_UUID, A_PROJECT_UUID, new Snapshot(2L, "1.", true));
    insert(QUALIFIER_FILE, A_FILE_UUID, A_PROJECT_UUID, new Snapshot(3L, "1.2.", true));

    underTest.execute();
    verifyNoNullPath();

    underTest.execute();
    verifyNoNullPath();
  }

  @Test
  public void ignore_snapshots_with_invalid_snapshots_in_path() throws SQLException {
    insert(QUALIFIER_PROJECT, A_PROJECT_UUID, A_PROJECT_UUID, new Snapshot(1L, "", true));
    // the ID 999999 is unknown in the path
    insert(QUALIFIER_DIR, A_DIR_UUID, A_PROJECT_UUID, new Snapshot(2L, "1.999999.", true));

    underTest.execute();

    verifyPath(A_PROJECT_UUID, ".");
    // path of orphans is the path to project only
    verifyPath(A_DIR_UUID, format(".%s.", A_PROJECT_UUID));
  }

  private void insert(String qualifier, String uuid, String rootUuid, Snapshot... snapshots) {
    db.executeInsert(
      TABLE_PROJECTS,
      "uuid", uuid,
      "project_uuid", rootUuid,
      "root_uuid", rootUuid,
      "qualifier", qualifier);

    for (Snapshot snapshot : snapshots) {
      db.executeInsert(
        TABLE_SNAPSHOTS,
        "id", String.valueOf(snapshot.id),
        "uuid", "u" + snapshot.id,
        "path", snapshot.idPath,
        "islast", String.valueOf(snapshot.isLast),
        "component_uuid", uuid,
        "root_component_uuid", rootUuid,
        "qualifier", qualifier);
    }
  }

  private void verifyPath(String componentUuid, String expectedUuidPath) {
    Map<String, Object> row = db.selectFirst("select uuid_path from projects where uuid='" + componentUuid + "'");
    assertThat(row.get("UUID_PATH")).isEqualTo(expectedUuidPath);
  }

  private void verifyNoNullPath() {
    assertThat(db.select("select * from projects where uuid_path is null or uuid_path = ''")).isEmpty();
  }

  private static final class Snapshot {
    private final long id;
    private final String idPath;
    private final boolean isLast;

    Snapshot(long id, String idPath, boolean isLast) {
      this.id = id;
      this.idPath = idPath;
      this.isLast = isLast;
    }
  }
}
