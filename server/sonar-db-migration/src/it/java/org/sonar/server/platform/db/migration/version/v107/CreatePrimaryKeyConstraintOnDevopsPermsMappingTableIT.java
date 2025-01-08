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
package org.sonar.server.platform.db.migration.version.v107;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v107.CreatePrimaryKeyConstraintOnDevopsPermsMappingTable.UUID_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v107.RenameGithubPermsMappingTable.DEVOPS_PERMS_MAPPING_TABLE_NAME;

class CreatePrimaryKeyConstraintOnDevopsPermsMappingTableIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreatePrimaryKeyConstraintOnDevopsPermsMappingTable.class);
  private final CreatePrimaryKeyConstraintOnDevopsPermsMappingTable underTest = new CreatePrimaryKeyConstraintOnDevopsPermsMappingTable(db.database());
  private static final String PK_DEVOPS_PERMS_MAPPING = "pk_devops_perms_mapping";

  @Test
  void execute_whenPrimaryKeyDoesNotExist_shouldCreateIt() throws Exception {
    db.assertNoPrimaryKey(DEVOPS_PERMS_MAPPING_TABLE_NAME);
    underTest.execute();
    db.assertPrimaryKey(DEVOPS_PERMS_MAPPING_TABLE_NAME, PK_DEVOPS_PERMS_MAPPING, UUID_COLUMN_NAME);
  }

  @Test
  void execute_whenPrimaryKeyAlreadyExists_shouldDoNothing() throws Exception {
    underTest.execute();
    underTest.execute();
    db.assertPrimaryKey(DEVOPS_PERMS_MAPPING_TABLE_NAME, PK_DEVOPS_PERMS_MAPPING, UUID_COLUMN_NAME);
  }

}