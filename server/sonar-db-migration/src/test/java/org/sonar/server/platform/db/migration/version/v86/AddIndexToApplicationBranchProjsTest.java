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
package org.sonar.server.platform.db.migration.version.v86;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.MigrationStep;

public class AddIndexToApplicationBranchProjsTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddIndexToApplicationBranchProjsTest.class, "schema.sql");

  private final MigrationStep underTest = new AddIndexToApplicationBranchProjs(db.database());

  @Test
  public void execute() throws SQLException {
    underTest.execute();

    db.assertUniqueIndex("app_branch_project_branch", "uniq_app_branch_proj", "application_branch_uuid", "project_branch_uuid");
    db.assertIndex("app_branch_project_branch", "idx_abpb_app_uuid", "application_uuid");
    db.assertIndex("app_branch_project_branch", "idx_abpb_app_branch_uuid", "application_branch_uuid");
    db.assertIndex("app_branch_project_branch", "idx_abpb_proj_uuid", "project_uuid");
    db.assertIndex("app_branch_project_branch", "idx_abpb_proj_branch_uuid", "project_branch_uuid");
  }
}
