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
import org.sonar.db.CoreDbTester;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.assertj.core.api.Assertions.assertThat;

public class IncreaseBranchTypeSizeForPullRequestTest {
  private static final String TABLE_NAME = "project_branches";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(IncreaseBranchTypeSizeForPullRequestTest.class, "project_branches.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private IncreaseBranchTypeSizeForPullRequest underTest = new IncreaseBranchTypeSizeForPullRequest(db.database());

  @Test
  public void cannot_insert_PULL_REQUEST_type_before_migration() {
    expectedException.expect(IllegalStateException.class);

    insertRow();
  }

  @Test
  public void can_insert_PULL_REQUEST_after_execute() throws SQLException {
    underTest.execute();
    assertThat(db.countRowsOfTable(TABLE_NAME)).isEqualTo(0);
    insertRow();
    assertThat(db.countRowsOfTable(TABLE_NAME)).isEqualTo(1);
  }

  private void insertRow() {
    db.executeInsert(
      "PROJECT_BRANCHES",
      "UUID", "dummy_uuid",
      "PROJECT_UUID", "dummy_project_uuid",
      "KEE", "dummy_key",
      "CREATED_AT", 456789,
      "UPDATED_AT", 456789,
      "BRANCH_TYPE", "PULL_REQUEST");
  }
}
