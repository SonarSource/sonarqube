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
package org.sonar.server.platform.db.migration.version.v71;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static org.sonar.server.platform.db.migration.version.v71.ReplaceIndexInProjectBranches.NEW_INDEX_NAME;
import static org.sonar.server.platform.db.migration.version.v71.ReplaceIndexInProjectBranches.KEE_COLUMN;
import static org.sonar.server.platform.db.migration.version.v71.ReplaceIndexInProjectBranches.KEY_TYPE_COLUMN;
import static org.sonar.server.platform.db.migration.version.v71.ReplaceIndexInProjectBranches.PROJECT_UUID_COLUMN;
import static org.sonar.server.platform.db.migration.version.v71.ReplaceIndexInProjectBranches.TABLE_NAME;

public class ReplaceIndexInProjectBranchesTest {
  @Rule
  public final CoreDbTester dbTester = CoreDbTester.createForSchema(ReplaceIndexInProjectBranchesTest.class, "project_branches.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ReplaceIndexInProjectBranches underTest = new ReplaceIndexInProjectBranches(dbTester.database());

  @Test
  public void column_is_part_of_index() throws SQLException {
    underTest.execute();

    dbTester.assertUniqueIndex(TABLE_NAME, NEW_INDEX_NAME, PROJECT_UUID_COLUMN.getName(), KEE_COLUMN.getName(), KEY_TYPE_COLUMN.getName());
  }

  @Test
  public void adding_pr_with_same_key_as_existing_branch_fails_before_migration() {
    expectedException.expect(IllegalStateException.class);

    String key = "feature/foo";
    insertBranch(1, key);
    insertPullRequest(2, key);
  }

  @Test
  public void adding_pr_with_same_key_as_existing_branch_works_after_migration() throws SQLException {
    underTest.execute();

    String key = "feature/foo";
    insertBranch(1, key);
    insertPullRequest(2, key);
  }

  private void insertBranch(int id, String name) {
    insertRow(id, "SHORT", name, "BRANCH");
  }

  private void insertPullRequest(int id, String pullRequestId) {
    insertRow(id, "PULL_REQUEST", pullRequestId, "PULL_REQUEST");
  }

  private void insertRow(int id, String branchType, String key, String keyType) {
    dbTester.executeInsert(
      "PROJECT_BRANCHES",
      "UUID", "dummy_uuid" + id,
      "PROJECT_UUID", "dummy_project_uuid",
      "KEE", key,
      "KEY_TYPE", keyType,
      "CREATED_AT", 456789 + id,
      "UPDATED_AT", 456789 + id,
      "BRANCH_TYPE", branchType);
  }
}
