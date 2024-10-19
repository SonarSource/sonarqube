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
import org.sonar.db.CoreDbTester;
import org.sonar.db.MigrationDbTester;



class DropIndexComponentUuidInUserRolesIT {

  private static final String TABLE_NAME = "user_roles";
  private static final String COLUMN_NAME = "component_uuid";
  private static final String INDEX_NAME = "user_roles_component_uuid";

  /**
   * {@link MigrationDbTester} is not used because we are expecting index with component_uuid to exist. However, renaming the column component_uuid to entity_uuid
   * also updated the index
   */
  @RegisterExtension
  public final CoreDbTester db = CoreDbTester.createForSchema(DropIndexComponentUuidInUserRolesIT.class, "schema.sql");
  private final DropIndexComponentUuidInUserRoles underTest = new DropIndexComponentUuidInUserRoles(db.database());

  @Test
  void index_is_dropped() throws SQLException {
    db.assertIndex(TABLE_NAME, INDEX_NAME, COLUMN_NAME);

    underTest.execute();

    db.assertIndexDoesNotExist(TABLE_NAME, COLUMN_NAME);
  }

  @Test
  void migration_is_reentrant() throws SQLException {
    db.assertIndex(TABLE_NAME, INDEX_NAME, COLUMN_NAME);

    underTest.execute();
    underTest.execute();

    db.assertIndexDoesNotExist(TABLE_NAME, COLUMN_NAME);
  }
}
