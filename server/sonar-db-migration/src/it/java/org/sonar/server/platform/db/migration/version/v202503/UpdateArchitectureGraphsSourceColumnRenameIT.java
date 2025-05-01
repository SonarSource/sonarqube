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
package org.sonar.server.platform.db.migration.version.v202503;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static java.sql.Types.VARCHAR;
import static org.sonar.db.MigrationDbTester.createForMigrationStep;

class UpdateArchitectureGraphsSourceColumnRenameIT {

  private static final String TABLE_NAME = "architecture_graphs";
  private static final String OLD_COLUMN = "source";
  private static final String NEW_COLUMN = "ecosystem";

  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(UpdateArchitectureGraphsSourceColumnRename.class);
  private final DdlChange underTest = new UpdateArchitectureGraphsSourceColumnRename(db.database());

  @Test
  void execute_shouldUpdateColumn() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, OLD_COLUMN, VARCHAR, null, null);
    db.assertColumnDoesNotExist(TABLE_NAME, NEW_COLUMN);

    underTest.execute();

    db.assertColumnDoesNotExist(TABLE_NAME, OLD_COLUMN);
    db.assertColumnDefinition(TABLE_NAME, NEW_COLUMN, VARCHAR, null, null);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, OLD_COLUMN, VARCHAR, null, null);
    db.assertColumnDoesNotExist(TABLE_NAME, NEW_COLUMN);

    underTest.execute();
    underTest.execute();

    db.assertColumnDoesNotExist(TABLE_NAME, OLD_COLUMN);
    db.assertColumnDefinition(TABLE_NAME, NEW_COLUMN, VARCHAR, null, null);
  }
}
