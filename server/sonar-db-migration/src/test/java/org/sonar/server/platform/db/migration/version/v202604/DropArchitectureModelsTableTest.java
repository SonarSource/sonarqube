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
package org.sonar.server.platform.db.migration.version.v202604;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v202604.DropArchitectureModelsTable.TABLE_NAME;

class DropArchitectureModelsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(DropArchitectureModelsTable.class);

  private final DropArchitectureModelsTable underTest = new DropArchitectureModelsTable(db.database());

  @Test
  void migration_should_drop_table() throws SQLException {
    db.assertTableExists(TABLE_NAME);

    underTest.execute();

    db.assertTableDoesNotExist(TABLE_NAME);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertTableDoesNotExist(TABLE_NAME);
  }
}
