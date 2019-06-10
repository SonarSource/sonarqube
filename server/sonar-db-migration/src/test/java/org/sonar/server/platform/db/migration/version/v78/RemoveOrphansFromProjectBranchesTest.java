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
package org.sonar.server.platform.db.migration.version.v78;

import java.sql.SQLException;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoveOrphansFromProjectBranchesTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(RemoveOrphansFromProjectBranchesTest.class, "project_and_project_branches.sql");

  private RemoveOrphansFromProjectBranches underTest = new RemoveOrphansFromProjectBranches(db.database());

  @Test
  public void migration_has_no_effect_on_empty_tables() throws SQLException {
    String[] keeps = IntStream.range(0, 1 + new Random().nextInt(30)).mapToObj(i -> {
      String uuid = "Keep_" + i;
      insertIntoProjectBranches(uuid);
      insertIntoProjects(uuid);
      return uuid;
    })
      .toArray(String[]::new);
    String[] deletes = IntStream.range(0, 1 + new Random().nextInt(30)).mapToObj(i -> {
      String uuid = "Dl_" + i;
      insertIntoProjectBranches(uuid);
      return uuid;
    })
      .toArray(String[]::new);

    underTest.execute();

    assertThat(db.select("select uuid as \"UUID\" from project_branches").stream().map(t -> t.get("UUID")))
      .containsOnly(keeps)
      .doesNotContain(deletes);
  }

  private void insertIntoProjects(String uuid) {
    db.executeInsert("PROJECTS",
      "ORGANIZATION_UUID", "org_" + uuid,
      "PROJECT_UUID", "PU_" + uuid,
      "UUID", uuid,
      "UUID_PATH", "UP_" + uuid,
      "ROOT_UUID", "R_" + uuid,
      "PRIVATE", new Random().nextBoolean());
  }

  @Test
  public void migration_deletes_all_rows_in_project_branches_table_if_projects_table_is_empty() throws SQLException {
    IntStream.range(0, 1 + new Random().nextInt(30)).forEach(this::insertIntoProjectBranches);

    underTest.execute();

    assertThat(db.countRowsOfTable("PROJECT_BRANCHES")).isZero();
  }

  @Test
  public void db_migration_is_reentrant() throws SQLException {
    migration_deletes_all_rows_in_project_branches_table_if_projects_table_is_empty();

    underTest.execute();

    assertThat(db.countRowsOfTable("PROJECT_BRANCHES")).isZero();
  }

  private void insertIntoProjectBranches(int i) {
    String uuid = "UUID_" + i;
    insertIntoProjectBranches(uuid);
  }

  private void insertIntoProjectBranches(String uuid) {
    int i1 = new Random().nextInt(9_000);
    db.executeInsert(
      "PROJECT_BRANCHES",
      "UUID", uuid,
      "PROJECT_UUID", "PU_" + uuid,
      "KEE", "KEE_" + uuid,
      "KEY_TYPE", uuid + "_KT",
      "CREATED_AT", 12 + i1,
      "UPDATED_AT", 500 + i1);
  }
}
