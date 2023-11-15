/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v102;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v102.CreateGithubOrganizationsGroupsTable.GROUP_UUID_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v102.CreateGithubOrganizationsGroupsTable.ORGANIZATION_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v102.CreateGithubOrganizationsGroupsTable.TABLE_NAME;

public class CreateGithubOrganizationsGroupsTableTest {
  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateGithubOrganizationsGroupsTable.class);
  private final DdlChange createGithubOrganizationsGroupsTable = new CreateGithubOrganizationsGroupsTable(db.database());

  @Test
  public void migration_should_create_a_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    createGithubOrganizationsGroupsTable.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertColumnDefinition(TABLE_NAME, GROUP_UUID_COLUMN_NAME, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, ORGANIZATION_COLUMN_NAME, Types.VARCHAR, 100, false);
    db.assertPrimaryKey(TABLE_NAME, "pk_github_orgs_groups", GROUP_UUID_COLUMN_NAME);
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    createGithubOrganizationsGroupsTable.execute();
    // re-entrant
    createGithubOrganizationsGroupsTable.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
