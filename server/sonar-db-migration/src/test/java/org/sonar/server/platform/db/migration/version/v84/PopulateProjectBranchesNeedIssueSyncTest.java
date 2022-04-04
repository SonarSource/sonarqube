/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateProjectBranchesNeedIssueSyncTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateProjectBranchesNeedIssueSyncTest.class, "schema.sql");

  private DataChange underTest = new PopulateProjectBranchesNeedIssueSync(db.database());

  @Test
  public void populate_need_issue_sync() throws SQLException {
    insertProjectBranches("uuid-1");
    insertProjectBranches("uuid-2");
    insertProjectBranches("uuid-3");

    underTest.execute();

    verifyNeedIssueSyncIsNotNull();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertProjectBranches("uuid-1");
    insertProjectBranches("uuid-2");
    insertProjectBranches("uuid-3");

    underTest.execute();
    // re-entrant
    underTest.execute();

    verifyNeedIssueSyncIsNotNull();
  }

  private void verifyNeedIssueSyncIsNotNull() {
    assertThat(db.select("select need_issue_sync from project_branches where need_issue_sync is null")).isEmpty();
  }

  private void insertProjectBranches(String uuid) {
    db.executeInsert("project_branches",
      "uuid", uuid,
      "project_uuid", "name",
      "kee", uuid,
      "key_type", "KEY_TYPE",
      "created_at", System.currentTimeMillis(),
      "updated_at", System.currentTimeMillis(),
      "exclude_from_purge", false);
  }
}
