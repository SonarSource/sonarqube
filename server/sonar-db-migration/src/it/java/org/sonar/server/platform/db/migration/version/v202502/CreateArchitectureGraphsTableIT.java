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
package org.sonar.server.platform.db.migration.version.v202502;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

class CreateArchitectureGraphsTableIT {
  public static final String TABLE_NAME = "architecture_graphs";

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateArchitectureGraphsTable.class);
  private final CreateArchitectureGraphsTable underTest = new CreateArchitectureGraphsTable(db.database());

  @Test
  void execute_shouldCreateTable() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);
    underTest.execute();
    db.assertTableExists(TABLE_NAME);
    db.assertColumnDefinition(TABLE_NAME, "uuid", Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, "branch_uuid", Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, "source", Types.VARCHAR, 255, false);
    db.assertColumnDefinition(TABLE_NAME, "type", Types.VARCHAR, 255, false);
    db.assertColumnDefinition(TABLE_NAME, "graph_data", Types.CLOB, null, false);
  }

  @Test
  void execute_shouldSupportReentrantMigrationExecution() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);
    underTest.execute();
    underTest.execute();
    db.assertTableExists(TABLE_NAME);
  }
}
