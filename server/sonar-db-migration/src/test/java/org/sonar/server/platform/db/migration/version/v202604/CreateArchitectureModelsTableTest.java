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
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureModelsTable.COLUMN_MODEL;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureModelsTable.COLUMN_ORGANIZATION_ID;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureModelsTable.COLUMN_PROJECT_ID;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureModelsTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureModelsTable.INDEX_UUID;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureModelsTable.TABLE_NAME;

class CreateArchitectureModelsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateArchitectureModelsTable.class);

  private final CreateArchitectureModelsTable underTest = new CreateArchitectureModelsTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    // Composite primary key = DynamoDB (partition key, sort key); secondary index = the 'UuidIndex' GSI.
    db.assertPrimaryKey(TABLE_NAME, "pk_architecture_models", COLUMN_PROJECT_ID, COLUMN_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROJECT_ID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ORGANIZATION_ID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_MODEL, Types.CLOB, null, false);
    db.assertIndex(TABLE_NAME, INDEX_UUID, COLUMN_UUID);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
