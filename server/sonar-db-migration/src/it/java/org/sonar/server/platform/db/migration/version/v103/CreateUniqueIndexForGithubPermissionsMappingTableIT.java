/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v103.CreateGithubPermissionsMappingTable.GITHUB_PERMISSIONS_MAPPING_TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v103.CreateGithubPermissionsMappingTable.GITHUB_ROLE_COLUMN;
import static org.sonar.server.platform.db.migration.version.v103.CreateGithubPermissionsMappingTable.SONARQUBE_PERMISSION_COLUMN;
import static org.sonar.server.platform.db.migration.version.v103.CreateUniqueIndexForGithubPermissionsMappingTable.INDEX_NAME;

class CreateUniqueIndexForGithubPermissionsMappingTableIT {
  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateUniqueIndexForGithubPermissionsMappingTable.class);
  private final CreateUniqueIndexForGithubPermissionsMappingTable createIndex = new CreateUniqueIndexForGithubPermissionsMappingTable(db.database());

  @Test
  void migration_should_create_index() throws SQLException {
    db.assertIndexDoesNotExist(GITHUB_PERMISSIONS_MAPPING_TABLE_NAME, INDEX_NAME);

    createIndex.execute();

    db.assertUniqueIndex(GITHUB_PERMISSIONS_MAPPING_TABLE_NAME, INDEX_NAME, GITHUB_ROLE_COLUMN, SONARQUBE_PERMISSION_COLUMN);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    createIndex.execute();
    createIndex.execute();

    db.assertUniqueIndex(GITHUB_PERMISSIONS_MAPPING_TABLE_NAME, INDEX_NAME, GITHUB_ROLE_COLUMN, SONARQUBE_PERMISSION_COLUMN);
  }
}
