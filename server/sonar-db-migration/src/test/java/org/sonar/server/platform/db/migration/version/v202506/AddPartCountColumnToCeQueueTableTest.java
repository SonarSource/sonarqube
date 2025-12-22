/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202506;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static java.sql.Types.INTEGER;
import static org.sonar.server.platform.db.migration.version.v202506.AddPartCountColumnToCeQueueTable.COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v202506.AddPartCountColumnToCeQueueTable.TABLE_NAME;

class AddPartCountColumnToCeQueueTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AddPartCountColumnToCeQueueTable.class);

  private final AddPartCountColumnToCeQueueTable underTest = new AddPartCountColumnToCeQueueTable(db.database());

  @Test
  void execute_whenColumnDoesNotExist_shouldCreateColumn() throws SQLException {
    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_NAME);

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, INTEGER, null, false);
  }

  @Test
  void execute_whenColumnAlreadyExists_shouldBeReentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, INTEGER, null, false);
  }
}

