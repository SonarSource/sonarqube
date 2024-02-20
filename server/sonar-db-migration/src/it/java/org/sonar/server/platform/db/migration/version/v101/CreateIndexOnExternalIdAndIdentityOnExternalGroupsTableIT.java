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
package org.sonar.server.platform.db.migration.version.v101;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v101.CreateExternalGroupsTable.EXTERNAL_GROUP_ID_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v101.CreateExternalGroupsTable.EXTERNAL_IDENTITY_PROVIDER_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v101.CreateExternalGroupsTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v101.CreateIndexOnExternalIdAndIdentityOnExternalGroupsTable.INDEX_NAME;

class CreateIndexOnExternalIdAndIdentityOnExternalGroupsTableIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateIndexOnExternalIdAndIdentityOnExternalGroupsTable.class);
  private final CreateIndexOnExternalIdAndIdentityOnExternalGroupsTable createIndexOnExternalIdAndIdentityOnExternalGroupsTable = new CreateIndexOnExternalIdAndIdentityOnExternalGroupsTable(
    db.database());

  @Test
  void migration_should_create_index() throws SQLException {
    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);

    createIndexOnExternalIdAndIdentityOnExternalGroupsTable.execute();

    db.assertUniqueIndex(TABLE_NAME, INDEX_NAME, EXTERNAL_IDENTITY_PROVIDER_COLUMN_NAME, EXTERNAL_GROUP_ID_COLUMN_NAME);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    createIndexOnExternalIdAndIdentityOnExternalGroupsTable.execute();
    createIndexOnExternalIdAndIdentityOnExternalGroupsTable.execute();

    db.assertUniqueIndex(TABLE_NAME, INDEX_NAME, EXTERNAL_IDENTITY_PROVIDER_COLUMN_NAME, EXTERNAL_GROUP_ID_COLUMN_NAME);
  }

}
