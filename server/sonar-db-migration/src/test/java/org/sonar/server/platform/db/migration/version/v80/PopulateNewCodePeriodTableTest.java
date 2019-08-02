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
package org.sonar.server.platform.db.migration.version.v80;

import java.sql.SQLException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.CoreDbTester;

public class PopulateNewCodePeriodTableTest {
  private static final String NEW_CODE_PERIODS_TABLE_NAME = "new_code_periods";
  private static final String PROJECT_BRANCHES_TABLE_NAME = "project_branches";
  private static final int NUMBER_OF_PROJECT_BRANCHES_TO_INSERT = 10;

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(PopulateNewCodePeriodTableTest.class, "schema.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PopulateNewCodePeriodTable underTest = new PopulateNewCodePeriodTable(dbTester.database(), UuidFactoryImpl.INSTANCE, System2.INSTANCE);

  @Test
  public void copy_manual_baseline_analysis_to_new_code_period_table() throws SQLException {
    for (long i = 1; i <= NUMBER_OF_PROJECT_BRANCHES_TO_INSERT; i++) {
      insertProjectBranch(i);
    }

    underTest.execute();

    int propertiesCount = dbTester.countRowsOfTable(NEW_CODE_PERIODS_TABLE_NAME);
    Assert.assertEquals(10, propertiesCount);

    //should not fail if executed twice
    underTest.execute();
  }

  private void insertProjectBranch(long counter) {
    dbTester.executeInsert(
      PROJECT_BRANCHES_TABLE_NAME,
      "UUID", "pb-uuid-" + counter,
      "PROJECT_UUID", "pb-uuid-" + counter,
      "KEE", "pb-key-" + counter,
      "KEY_TYPE", "TSR",
      "BRANCH_TYPE", "LONG",
      "MERGE_BRANCH_UUID", "mb-uuid-" + counter,
      "MANUAL_BASELINE_ANALYSIS_UUID", "mba-uuid" + counter,
      "CREATED_AT", System2.INSTANCE.now(),
      "UPDATED_AT", System2.INSTANCE.now()
    );
  }
}
