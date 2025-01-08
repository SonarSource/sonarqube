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
package org.sonar.server.platform.db.migration.version.v101;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static java.sql.Types.VARCHAR;
import static org.sonar.server.platform.db.migration.version.v101.IncreaseTaskTypeColumnSizeInCeActivity.COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v101.IncreaseTaskTypeColumnSizeInCeActivity.NEW_COLUMN_SIZE;
import static org.sonar.server.platform.db.migration.version.v101.IncreaseTaskTypeColumnSizeInCeActivity.TABLE_NAME;

class IncreaseTaskTypeColumnSizeInCeActivityIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(IncreaseTaskTypeColumnSizeInCeActivity.class);
  private final IncreaseTaskTypeColumnSizeInCeActivity underTest = new IncreaseTaskTypeColumnSizeInCeActivity(db.database());

  @Test
  void execute_increaseColumnSize() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, VARCHAR, 15, false);
    underTest.execute();
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, VARCHAR, NEW_COLUMN_SIZE, false);
  }

  @Test
  void migration_is_reentrant() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, VARCHAR, 15, false);
    underTest.execute();
    underTest.execute();
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, VARCHAR, NEW_COLUMN_SIZE, false);
  }
}
