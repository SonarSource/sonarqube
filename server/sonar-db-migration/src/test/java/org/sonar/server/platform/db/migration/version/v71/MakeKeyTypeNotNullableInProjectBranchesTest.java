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
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleScope;
import org.sonar.db.CoreDbTester;

import static org.sonar.server.platform.db.migration.version.v71.MakeKeyTypeNotNullableInProjectBranches.TABLE_NAME;

public class MakeKeyTypeNotNullableInProjectBranchesTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MakeKeyTypeNotNullableInProjectBranchesTest.class, "project_branches.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MakeKeyTypeNotNullableInProjectBranches underTest = new MakeKeyTypeNotNullableInProjectBranches(db.database());

  @Test
  public void execute_makes_column_not_null() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, "key_type", Types.VARCHAR, null, true);
    insertRow();

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, "key_type", Types.VARCHAR, null, false);
  }

  private void insertRow() {
    db.executeInsert(
      "PROJECT_BRANCHES",
      "UUID", "dummy_uuid",
      "PROJECT_UUID", "dummy_project_uuid",
      "KEE", "dummy_key",
      "KEY_TYPE", "BRANCH",
      "CREATED_AT", 456789,
      "UPDATED_AT", 456789,
      "BRANCH_TYPE", "BRANCH");
  }
}
