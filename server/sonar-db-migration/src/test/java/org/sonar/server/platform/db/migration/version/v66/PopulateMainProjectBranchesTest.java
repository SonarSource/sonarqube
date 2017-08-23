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
package org.sonar.server.platform.db.migration.version.v66;

import java.sql.SQLException;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateMainProjectBranchesTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateMainProjectBranchesTest.class, "initial.sql");

  private System2 system2 = new AlwaysIncreasingSystem2();
  private PopulateMainProjectBranches underTest = new PopulateMainProjectBranches(db.database(), system2);

  @Test
  public void populate_with_existing_project() throws SQLException {
    insertProject("project1");
    insertProject("project2", "project1", "PRJ");
    insertProject("project3", null, "XXX");

    underTest.execute();

    assertThat(db.countRowsOfTable("project_branches")).isEqualTo(1);

    Map<String, Object> row = db.selectFirst("select * from project_branches");
    assertThat(row.get("UUID")).isEqualTo("project1");
    assertThat(row.get("PROJECT_UUID")).isEqualTo("project1");
    assertThat(row.get("KEE")).isEqualTo(PopulateMainProjectBranches.NULL_KEY);
    assertThat(row.get("BRANCH_TYPE")).isEqualTo("LONG");

  }

  @Test
  public void do_nothing_if_no_project() throws SQLException {
    insertProject("project2", "project1", "PRJ");
    insertProject("project3", null, "XXX");

    underTest.execute();
    assertThat(db.countRowsOfTable("project_branches")).isEqualTo(0);
  }

  @Test
  public void do_not_populate_if_already_exists() throws SQLException {
    insertProject("project1");
    insertBranch("project1");

    assertThat(db.countRowsOfTable("project_branches")).isEqualTo(1);
    underTest.execute();
    assertThat(db.countRowsOfTable("project_branches")).isEqualTo(1);
  }

  private void insertProject(String uuid) {
    insertProject(uuid, null, "PRJ");
  }

  private void insertProject(String uuid, @Nullable String mainBranchUuid, String scope) {
    db.executeInsert("PROJECTS",
      "ORGANIZATION_UUID", "default-org",
      "KEE", uuid + "-key",
      "UUID", uuid,
      "PROJECT_UUID", uuid,
      "main_branch_project_uuid", mainBranchUuid,
      "UUID_PATH", ".",
      "ROOT_UUID", uuid,
      "PRIVATE", "true",
      "qualifier", "TRK",
      "scope", scope);
  }

  private void insertBranch(String uuid) {
    db.executeInsert("PROJECT_BRANCHES",
      "uuid", uuid,
      "project_uuid", uuid,
      "kee_type", "BRANCH",
      "kee", PopulateMainProjectBranches.NULL_KEY,
      "branch_type", "LONG",
      "created_at", 0,
      "updated_at", 0);
  }
}
