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
package org.sonar.server.platform.db.migration.version.v108;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;


class DropIndexOnProjectBranchesMeasuresMigratedIT {

  private static final String TABLE_NAME = "project_branches";
  private static final String COLUMN_NAME = "measures_migrated";
  private static final String INDEX_NAME = "pb_measures_migrated";

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(DropIndexOnProjectBranchesMeasuresMigrated.class);
  private final DdlChange underTest = new DropIndexOnProjectBranchesMeasuresMigrated(db.database());

  @Test
  void execute_givenIndexExists_dropsIndex() throws Exception {
    db.assertIndex(TABLE_NAME, INDEX_NAME, COLUMN_NAME);
    underTest.execute();
    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);
  }

  @Test
  void execute_is_reentrant() throws Exception {
    db.assertIndex(TABLE_NAME, INDEX_NAME, COLUMN_NAME);
    underTest.execute();
    underTest.execute();
    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);
  }
}
