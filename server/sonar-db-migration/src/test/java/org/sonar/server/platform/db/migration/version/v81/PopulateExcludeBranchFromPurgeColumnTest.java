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
package org.sonar.server.platform.db.migration.version.v81;

import java.sql.SQLException;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateExcludeBranchFromPurgeColumnTest {

  private static final String TABLE = "PROJECT_BRANCHES";

  private static final String PROJECT_1_UUID = "1a724f54-c2a4-4d36-b59c-61026f178613";
  private static final String PROJECT_2_UUID = "cc69ccb8-434a-489b-abd6-6a80371f64ff";

  private static final String MAIN_BRANCH_1 = "8a9789c8-7aee-4aa6-9cb7-407137935ac3";
  private static final String LONG_BRANCH_1 = "69fdfc19-491c-4ed9-bcac-ef04e0f6cdc6";
  private static final String SHORT_BRANCH_1 = "f7a6d247-1790-40f8-8591-a0cc39d05ecb";
  private static final String PR_1 = "d65466bf-271c-4f9f-ac7a-72add44848c0";

  private static final String MAIN_BRANCH_2 = "cdadf128-7bdb-4589-982d-62445d46ae1b";
  private static final String LONG_BRANCH_2 = "60bf6fa8-3ecc-4ba6-ad8d-991ea7c7f9cb";
  private static final String SHORT_BRANCH_2 = "ce5632ed-462e-4384-98b6-1773a7bbfe53";
  private static final String PR_2 = "5897f7d0-d34f-4558-b9c5-a7eb7a5c4b69";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(PopulateExcludeBranchFromPurgeColumnTest.class, "schema.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PopulateExcludeBranchFromPurgeColumn underTest = new PopulateExcludeBranchFromPurgeColumn(dbTester.database(), System2.INSTANCE);

  @Before
  public void setup() {
    insertBranch(MAIN_BRANCH_1, PROJECT_1_UUID, "master", "BRANCH", "LONG");
    insertBranch(LONG_BRANCH_1, PROJECT_1_UUID, "release-1", "BRANCH", "LONG");
    insertBranch(SHORT_BRANCH_1, PROJECT_1_UUID, "feature/foo", "BRANCH", "SHORT");
    insertBranch(PR_1, PROJECT_1_UUID, "feature/feature-1", "PULL_REQUEST", "PULL_REQUEST");

    insertBranch(MAIN_BRANCH_2, PROJECT_2_UUID, "trunk", "BRANCH", "LONG");
    insertBranch(LONG_BRANCH_2, PROJECT_2_UUID, "branch-1", "BRANCH", "LONG");
    insertBranch(SHORT_BRANCH_2, PROJECT_2_UUID, "feature/bar", "BRANCH", "SHORT");
    insertBranch(PR_2, PROJECT_2_UUID, "feature/feature-2", "PULL_REQUEST", "PULL_REQUEST");
  }

  @Test
  public void execute_migration() throws SQLException {
    underTest.execute();

    verifyMigrationResult();
  }

  @Test
  public void migration_is_re_entrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    verifyMigrationResult();
  }

  private void verifyMigrationResult() {
    assertThat(dbTester.select("select UUID from " + TABLE + " where EXCLUDE_FROM_PURGE = true")
      .stream()
      .map(e -> e.get("UUID"))
      .collect(Collectors.toList())).containsExactlyInAnyOrder(MAIN_BRANCH_1, LONG_BRANCH_1, MAIN_BRANCH_2, LONG_BRANCH_2);
    assertThat(dbTester.select("select UUID from " + TABLE + " where EXCLUDE_FROM_PURGE = false")
      .stream()
      .map(e -> e.get("UUID"))
      .collect(Collectors.toList())).containsExactlyInAnyOrder(SHORT_BRANCH_1, SHORT_BRANCH_2, PR_1, PR_2);
  }

  private void insertBranch(String uuid, String projectUuid, String key, String keyType, String branchType) {
    dbTester.executeInsert(
      TABLE,
      "UUID", uuid,
      "PROJECT_UUID", projectUuid,
      "KEE", key,
      "BRANCH_TYPE", branchType,
      "KEY_TYPE", keyType,
      "MERGE_BRANCH_UUID", null,
      "CREATED_AT", System2.INSTANCE.now(),
      "UPDATED_AT", System2.INSTANCE.now());
  }
}
