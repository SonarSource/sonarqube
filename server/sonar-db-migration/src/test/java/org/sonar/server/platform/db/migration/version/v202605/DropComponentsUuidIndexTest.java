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

import static org.sonar.db.MigrationDbTester.createForMigrationStep;

class DropComponentsUuidIndexTest {

  private static final String TABLE_NAME = "components";
  private static final String INDEX_NAME = "components_uuid";

  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(DropComponentsUuidIndex.class);
  private final DropComponentsUuidIndex underTest = new DropComponentsUuidIndex(db.database());

  @Test
  void execute_shouldDropIndex() throws SQLException {
    db.assertUniqueIndex(TABLE_NAME, INDEX_NAME, "uuid");

    underTest.execute();

    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    db.assertUniqueIndex(TABLE_NAME, INDEX_NAME, "uuid");

    underTest.execute();
    underTest.execute();

    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);
  }
}
