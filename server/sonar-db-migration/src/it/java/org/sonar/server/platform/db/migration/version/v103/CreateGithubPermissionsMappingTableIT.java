/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v103;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v103.CreateGithubPermissionsMappingTable.GITHUB_PERMISSIONS_MAPPING_TABLE_NAME;

public class CreateGithubPermissionsMappingTableIT {
  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateGithubPermissionsMappingTable.class);

  private final DdlChange createGithubPermissionsMappingTable = new CreateGithubPermissionsMappingTable(db.database());

  @Test
  public void migration_should_create_a_table() throws SQLException {
    db.assertTableDoesNotExist(GITHUB_PERMISSIONS_MAPPING_TABLE_NAME);

    createGithubPermissionsMappingTable.execute();

    db.assertTableExists(GITHUB_PERMISSIONS_MAPPING_TABLE_NAME);
    db.assertColumnDefinition(GITHUB_PERMISSIONS_MAPPING_TABLE_NAME, "uuid", Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(GITHUB_PERMISSIONS_MAPPING_TABLE_NAME, "github_role", Types.VARCHAR, 100, false);
    db.assertColumnDefinition(GITHUB_PERMISSIONS_MAPPING_TABLE_NAME, "sonarqube_permission", Types.VARCHAR, 64, false);
    db.assertPrimaryKey(GITHUB_PERMISSIONS_MAPPING_TABLE_NAME, "pk_github_perms_mapping", "uuid");
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(GITHUB_PERMISSIONS_MAPPING_TABLE_NAME);

    createGithubPermissionsMappingTable.execute();
    // re-entrant
    createGithubPermissionsMappingTable.execute();

    db.assertTableExists(GITHUB_PERMISSIONS_MAPPING_TABLE_NAME);
  }
}
