/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v85;

import java.sql.SQLException;
import java.sql.Types;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;

public class DropProjectBranchesKeyTypeTest {
  private static final String TABLE_NAME = "project_branches";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DropProjectBranchesKeyTypeTest.class, "schema.sql");

  private MigrationStep underTest = new DropProjectBranchesKeyType(db.database());

  @Test
  public void drops_table() throws SQLException {
    insertData(1, "PULL_REQUEST", "PULL_REQUEST");
    insertData(2, "BRANCH", "BRANCH");

    db.assertColumnDefinition(TABLE_NAME, "key_type", Types.VARCHAR, 12, false);

    underTest.execute();
    db.assertIndexDoesNotExist(TABLE_NAME, "project_branches_kee_key_type");
    db.assertUniqueIndex(TABLE_NAME, "uniq_project_branches", "branch_type", "project_uuid", "kee");
    db.assertColumnDoesNotExist(TABLE_NAME, "key_type");
    assertThat(db.countRowsOfTable(TABLE_NAME)).isEqualTo(2);
  }

  private void insertData(int id, String keyType, @Nullable String branchType) {
    db.executeInsert(TABLE_NAME,
      "uuid", "uuid" + id,
      "project_uuid", "project" + id,
      "kee", "key" + id,
      "key_type", keyType,
      "created_at", id,
      "updated_at", id + 1,
      "need_issue_sync", true,
      "branch_type", branchType
    );
  }
}
