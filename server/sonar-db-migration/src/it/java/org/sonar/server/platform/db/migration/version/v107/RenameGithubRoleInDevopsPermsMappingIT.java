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
package org.sonar.server.platform.db.migration.version.v107;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static java.sql.Types.VARCHAR;
import static org.sonar.server.platform.db.migration.version.v107.RenameGithubPermsMappingTable.DEVOPS_PERMS_MAPPING_TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v107.RenameGithubRoleInDevopsPermsMapping.OLD_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v107.RenameGithubRoleInDevopsPermsMapping.NEW_COLUMN_NAME;

class RenameGithubRoleInDevopsPermsMappingIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(RenameGithubRoleInDevopsPermsMapping.class);

  private final RenameGithubRoleInDevopsPermsMapping underTest = new RenameGithubRoleInDevopsPermsMapping(db.database());

  @Test
  void execute_renameColumn() throws SQLException {
    db.assertColumnDoesNotExist(DEVOPS_PERMS_MAPPING_TABLE_NAME, NEW_COLUMN_NAME);
    db.assertColumnDefinition(DEVOPS_PERMS_MAPPING_TABLE_NAME, OLD_COLUMN_NAME, VARCHAR, 100, false);
    underTest.execute();
    db.assertColumnDefinition(DEVOPS_PERMS_MAPPING_TABLE_NAME, NEW_COLUMN_NAME, VARCHAR, 100, false);
    db.assertColumnDoesNotExist(DEVOPS_PERMS_MAPPING_TABLE_NAME, OLD_COLUMN_NAME);
  }

  @Test
  void execute_whenRunTwice_isReentrant() throws SQLException {
    db.assertColumnDoesNotExist(DEVOPS_PERMS_MAPPING_TABLE_NAME, NEW_COLUMN_NAME);
    underTest.execute();
    underTest.execute();
    db.assertColumnDefinition(DEVOPS_PERMS_MAPPING_TABLE_NAME, NEW_COLUMN_NAME, VARCHAR, 100, false);
  }

}
