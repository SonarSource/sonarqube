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
import org.sonar.server.platform.db.migration.sql.DbPrimaryKeyConstraintFinder;
import org.sonar.server.platform.db.migration.sql.DropPrimaryKeySqlGenerator;

import static org.sonar.server.platform.db.migration.version.v107.DropPrimaryKeyOnDevopsPermsMappingTable.CONSTRAINT_NAME;
import static org.sonar.server.platform.db.migration.version.v107.DropPrimaryKeyOnDevopsPermsMappingTable.UUID_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v107.RenameGithubPermsMappingTable.DEVOPS_PERMS_MAPPING_TABLE_NAME;

class DropPrimaryKeyOnDevopsPermsMappingTableIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(DropPrimaryKeyOnDevopsPermsMappingTable.class);

  private final DbPrimaryKeyConstraintFinder dbPrimaryKeyConstraintFinder = new DbPrimaryKeyConstraintFinder(db.database());
  private final DropPrimaryKeySqlGenerator dropPrimaryKeySqlGenerator = new DropPrimaryKeySqlGenerator(db.database(), dbPrimaryKeyConstraintFinder);
  private final DropPrimaryKeyOnDevopsPermsMappingTable underTest = new DropPrimaryKeyOnDevopsPermsMappingTable(db.database(), dropPrimaryKeySqlGenerator, dbPrimaryKeyConstraintFinder);

  @Test
  void execute_shouldRemoveExistingPrimaryKey() throws Exception {
    db.assertPrimaryKey(DEVOPS_PERMS_MAPPING_TABLE_NAME, CONSTRAINT_NAME, UUID_COLUMN_NAME);
    underTest.execute();
    db.assertNoPrimaryKey(DEVOPS_PERMS_MAPPING_TABLE_NAME);
  }

  @Test
  void execute_when_reentrant_shouldRemoveExistingPrimaryKey() throws Exception {
    db.assertPrimaryKey(DEVOPS_PERMS_MAPPING_TABLE_NAME, CONSTRAINT_NAME, UUID_COLUMN_NAME);
    underTest.execute();
    underTest.execute();
    db.assertNoPrimaryKey(DEVOPS_PERMS_MAPPING_TABLE_NAME);
  }

}