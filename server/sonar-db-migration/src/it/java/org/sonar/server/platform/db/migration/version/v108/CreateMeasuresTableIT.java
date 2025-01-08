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
package org.sonar.server.platform.db.migration.version.v108;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v108.CreateMeasuresTable.COLUMN_BRANCH_UUID;
import static org.sonar.server.platform.db.migration.version.v108.CreateMeasuresTable.COLUMN_COMPONENT_UUID;
import static org.sonar.server.platform.db.migration.version.v108.CreateMeasuresTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v108.CreateMeasuresTable.COLUMN_JSON_VALUE;
import static org.sonar.server.platform.db.migration.version.v108.CreateMeasuresTable.COLUMN_JSON_VALUE_HASH;
import static org.sonar.server.platform.db.migration.version.v108.CreateMeasuresTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v108.CreateMeasuresTable.MEASURES_TABLE_NAME;

class CreateMeasuresTableIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateMeasuresTable.class);

  private final DdlChange underTest = new CreateMeasuresTable(db.database());

  @Test
  void execute_shouldCreateTable() throws SQLException {
    db.assertTableDoesNotExist(MEASURES_TABLE_NAME);

    underTest.execute();

    db.assertTableExists(MEASURES_TABLE_NAME);
    db.assertNoPrimaryKey(MEASURES_TABLE_NAME);
    db.assertColumnDefinition(MEASURES_TABLE_NAME, COLUMN_COMPONENT_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(MEASURES_TABLE_NAME, COLUMN_BRANCH_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(MEASURES_TABLE_NAME, COLUMN_JSON_VALUE, Types.CLOB, null, false);
    db.assertColumnDefinition(MEASURES_TABLE_NAME, COLUMN_JSON_VALUE_HASH, Types.BIGINT, null, false);
    db.assertColumnDefinition(MEASURES_TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(MEASURES_TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    db.assertTableDoesNotExist(MEASURES_TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(MEASURES_TABLE_NAME);
  }
}
