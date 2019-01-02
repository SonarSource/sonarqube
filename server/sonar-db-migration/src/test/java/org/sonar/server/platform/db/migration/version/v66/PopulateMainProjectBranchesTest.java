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
package org.sonar.server.platform.db.migration.version.v66;

import java.sql.SQLException;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.core.util.Uuids;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateMainProjectBranchesTest {

  private final static long PAST = 10_000_000_000L;
  private final static long NOW = 50_000_000_000L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateMainProjectBranchesTest.class, "initial.sql");

  private System2 system2 = new TestSystem2().setNow(NOW);
  private PopulateMainProjectBranches underTest = new PopulateMainProjectBranches(db.database(), system2);

  @Test
  public void migrate() throws SQLException {
    String project = insertProject();

    underTest.execute();

    assertProjectBranches(tuple("master", project, project, "LONG", NOW, NOW));
  }

  @Test
  public void does_nothing_on_non_projects() throws SQLException {
    insertProject(null, "BRC");
    insertProject(null, "VW");

    underTest.execute();

    assertThat(db.countRowsOfTable("project_branches")).isZero();
  }

  @Test
  public void does_nothing_on_empty_table() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("project_branches")).isZero();
  }

  @Test
  public void does_nothing_if_already_migrated() throws SQLException {
    String project = insertProject();
    insertMainBranch(project);

    underTest.execute();

    assertProjectBranches(tuple("master", project, project, "LONG", PAST, PAST));
  }

  private void assertProjectBranches(Tuple... expectedTuples) {
    assertThat(db.select("SELECT KEE, UUID, PROJECT_UUID, BRANCH_TYPE, CREATED_AT, UPDATED_AT FROM PROJECT_BRANCHES")
      .stream()
      .map(map -> new Tuple(map.get("KEE"), map.get("UUID"), map.get("PROJECT_UUID"), map.get("BRANCH_TYPE"), map.get("CREATED_AT"), map.get("UPDATED_AT")))
      .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(expectedTuples);
  }

  private String insertProject() {
    return insertProject(null, "PRJ");
  }

  private String insertProject(@Nullable String mainBranchUuid, String scope) {
    String uuid = Uuids.createFast();
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
    return uuid;
  }

  private void insertMainBranch(String uuid) {
    db.executeInsert("PROJECT_BRANCHES",
      "uuid", uuid,
      "project_uuid", uuid,
      "kee", "master",
      "branch_type", "LONG",
      "created_at", PAST,
      "updated_at", PAST);
  }
}
