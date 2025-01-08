/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v101;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateIsMainColumnInProjectBranchesIT {

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(UpdateIsMainColumnInProjectBranches.class);

  private final DataChange underTest = new UpdateIsMainColumnInProjectBranches(db.database());

  private static int not_random_value_always_incremented = 0;

  @Test
  void migration_updates_is_main_if_row_has_the_same_uuids() throws SQLException {
    String branchUuid1 = insertProjectBranch(true);
    String branchUuid2 = insertProjectBranch(false);

    underTest.execute();

    assertBranchIsMain(branchUuid1);
    assertBranchIsNotMain(branchUuid2);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    String branchUuid1 = insertProjectBranch(true);
    String branchUuid2 = insertProjectBranch(false);

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertBranchIsMain(branchUuid1);
    assertBranchIsNotMain(branchUuid2);
  }

  private void assertBranchIsMain(String branchUuid) {
    assertBranchIs(branchUuid, true);
  }

  private void assertBranchIsNotMain(String branchUuid) {
    assertBranchIs(branchUuid, false);
  }

  private void assertBranchIs(String branchUuid, boolean isMain) {
    String selectSql = String.format("select is_main from project_branches where uuid='%s'", branchUuid);
    assertThat(db.select(selectSql).stream()
      .map(row -> row.get("IS_MAIN"))
      .toList())
      .containsExactlyInAnyOrder(isMain);
  }

  private String insertProjectBranch(boolean sameUuids) {
    Map<String, Object> map = new HashMap<>();
    String uuid = uuidFactory.create();
    map.put("UUID", uuid);
    if(sameUuids) {
      map.put("PROJECT_UUID", uuid);
    } else {
      map.put("PROJECT_UUID", "uuid" + not_random_value_always_incremented++);
    }
    map.put("KEE", "randomKey");
    map.put("BRANCH_TYPE", "BRANCH");
    map.put("MERGE_BRANCH_UUID", null);
    map.put("CREATED_AT", System.currentTimeMillis());
    map.put("UPDATED_AT", System.currentTimeMillis());
    map.put("PULL_REQUEST_BINARY", null);
    map.put("EXCLUDE_FROM_PURGE", true);
    map.put("NEED_ISSUE_SYNC", false);
    db.executeInsert("project_branches", map);
    return uuid;
  }
}
