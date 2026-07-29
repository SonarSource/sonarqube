/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v202605.RenameArchModelsUuidIndex.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.RenameArchModelsUuidIndex.NEW_INDEX_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.RenameArchModelsUuidIndex.OLD_INDEX_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.RenameArchModelsUuidIndex.TABLE_NAME;

class RenameArchModelsUuidIndexTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(RenameArchModelsUuidIndex.class);

  private final RenameArchModelsUuidIndex underTest = new RenameArchModelsUuidIndex(db.database());

  @Test
  void execute_shouldDropOldIndexAndCreateNewIndex() throws SQLException {
    db.assertIndex(TABLE_NAME, OLD_INDEX_NAME, COLUMN_UUID);

    underTest.execute();

    db.assertIndexDoesNotExist(TABLE_NAME, OLD_INDEX_NAME);
    db.assertIndex(TABLE_NAME, NEW_INDEX_NAME, COLUMN_UUID);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertIndexDoesNotExist(TABLE_NAME, OLD_INDEX_NAME);
    db.assertIndex(TABLE_NAME, NEW_INDEX_NAME, COLUMN_UUID);
  }
}
