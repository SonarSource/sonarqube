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
package org.sonar.server.platform.db.migration.version.v71;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class SetKeyTypeToBranchInProjectBranchesTest {
  private static final String TABLE_NAME = "project_branches";
  private static final String KEY_TYPE_BRANCH = "BRANCH";

  @Rule
  public final CoreDbTester dbTester = CoreDbTester.createForSchema(SetKeyTypeToBranchInProjectBranchesTest.class, "project_branches.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private System2 system = new System2();

  private SetKeyTypeToBranchInProjectBranches underTest = new SetKeyTypeToBranchInProjectBranches(dbTester.database(), system);

  @Test
  public void has_no_effect_if_table_project_branches_is_empty() throws SQLException {
    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_NAME)).isEqualTo(0);
  }

  @Test
  public void updates_rows_to_BRANCH() throws SQLException {
    insertRow(1, "SHORT");
    insertRow(2, "LONG");
    insertRow(3, "SHORT");
    insertRow(4, "LONG");

    assertThat(countRowsWithValue(null)).isEqualTo(4);
    assertThat(countRowsWithValue(KEY_TYPE_BRANCH)).isEqualTo(0);

    underTest.execute();

    assertThat(countRowsWithValue(null)).isEqualTo(0);
    assertThat(countRowsWithValue(KEY_TYPE_BRANCH)).isEqualTo(4);
  }

  @Test
  public void execute_is_reentreant() throws SQLException {
    insertRow(1, "SHORT");
    insertRow(2, "LONG");
    insertRow(3, "SHORT");
    insertRow(4, "LONG");

    underTest.execute();

    underTest.execute();

    assertThat(countRowsWithValue(null)).isEqualTo(0);
    assertThat(countRowsWithValue(KEY_TYPE_BRANCH)).isEqualTo(4);
  }

  private int countRowsWithValue(String value) {
    if (value == null) {
      return dbTester.countSql("select count(1) from " + TABLE_NAME + " where key_type is null");
    }
    return dbTester.countSql("select count(1) from " + TABLE_NAME + " where key_type = '" + value + "'");
  }

  private void insertRow(int id, String branchType) {
    dbTester.executeInsert(
      "PROJECT_BRANCHES",
      "UUID", "dummy_uuid" + id,
      "PROJECT_UUID", "dummy_project_uuid" + id,
      "KEE", "dummy_key" + id,
      "CREATED_AT", 456789 + id,
      "UPDATED_AT", 456789 + id,
      "BRANCH_TYPE", branchType);
  }
}
