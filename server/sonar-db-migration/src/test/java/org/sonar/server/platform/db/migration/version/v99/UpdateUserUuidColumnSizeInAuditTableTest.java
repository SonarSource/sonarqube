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
package org.sonar.server.platform.db.migration.version.v99;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.sonar.server.platform.db.migration.version.v99.UpdateUserUuidColumnSizeInAuditTable.COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v99.UpdateUserUuidColumnSizeInAuditTable.TABLE_NAME;


public class UpdateUserUuidColumnSizeInAuditTableTest {
  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(UpdateUserUuidColumnSizeInAuditTable.class, "schema.sql");

  private final UpdateUserUuidColumnSizeInAuditTable underTest = new UpdateUserUuidColumnSizeInAuditTable(db.database());

  @Test
  public void migration_should_add_column() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.VARCHAR, 40, false);
    underTest.execute();
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.VARCHAR, 255, false);
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.VARCHAR, 40, false);
    // re-entrant
    underTest.execute();
    underTest.execute();
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.VARCHAR, 255, false);
  }
}
