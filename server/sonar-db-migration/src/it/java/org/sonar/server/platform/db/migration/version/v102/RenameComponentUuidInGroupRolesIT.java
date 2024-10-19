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
package org.sonar.server.platform.db.migration.version.v102;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static java.sql.Types.VARCHAR;

class RenameComponentUuidInGroupRolesIT {
  public static final String TABLE_NAME = "group_roles";
  public static final String NEW_COLUMN_NAME = "entity_uuid";

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(RenameComponentUuidInGroupRoles.class);
  private final RenameComponentUuidInGroupRoles underTest = new RenameComponentUuidInGroupRoles(db.database());

  @Test
  void columnIsRenamed() throws SQLException {
    db.assertColumnDoesNotExist(TABLE_NAME, NEW_COLUMN_NAME);
    underTest.execute();
    db.assertColumnDefinition(TABLE_NAME, NEW_COLUMN_NAME, VARCHAR, 40, true);
  }

  @Test
  void migration_is_reentrant() throws SQLException {
    db.assertColumnDoesNotExist(TABLE_NAME, NEW_COLUMN_NAME);
    underTest.execute();
    underTest.execute();
    db.assertColumnDefinition(TABLE_NAME, NEW_COLUMN_NAME, VARCHAR, 40, true);
  }
}
