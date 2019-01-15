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
package org.sonar.server.platform.db.migration.version.v77;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static java.sql.Types.VARCHAR;
import static org.sonar.server.platform.db.migration.version.v77.AddManualBaselineToProjectBranches.TABLE_NAME;

public class AddManualBaselineToProjectBranchesTest {

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(AddManualBaselineToProjectBranchesTest.class, "initial.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AddManualBaselineToProjectBranches underTest = new AddManualBaselineToProjectBranches(db.database());

  @Test
  public void adds_nullable_columns_to_existing_table() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, "manual_baseline_analysis_uuid", VARCHAR, 40, true);
  }

  @Test
  public void migration_is_not_reentrant() throws SQLException {
    underTest.execute();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to execute ");
    underTest.execute();
  }
}
