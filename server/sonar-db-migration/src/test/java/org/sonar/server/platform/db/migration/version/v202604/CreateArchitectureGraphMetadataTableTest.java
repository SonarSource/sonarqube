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
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureGraphMetadataTable.COLUMN_ANALYSIS_ID;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureGraphMetadataTable.COLUMN_BRANCH_ID;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureGraphMetadataTable.COLUMN_CONTENT_TYPE;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureGraphMetadataTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureGraphMetadataTable.COLUMN_ECOSYSTEM;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureGraphMetadataTable.COLUMN_ORGANIZATION_ID;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureGraphMetadataTable.COLUMN_PERSPECTIVE_KEY;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureGraphMetadataTable.COLUMN_PROJECT_ID;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureGraphMetadataTable.COLUMN_TYPE;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureGraphMetadataTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureGraphMetadataTable.COLUMN_VERSION;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureGraphMetadataTable.INDEX_ANALYSIS;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureGraphMetadataTable.INDEX_BRANCH;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureGraphMetadataTable.INDEX_PROJECT;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureGraphMetadataTable.INDEX_UUID;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureGraphMetadataTable.SHORT_TEXT_SIZE;
import static org.sonar.server.platform.db.migration.version.v202604.CreateArchitectureGraphMetadataTable.TABLE_NAME;

class CreateArchitectureGraphMetadataTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateArchitectureGraphMetadataTable.class);

  private final CreateArchitectureGraphMetadataTable underTest = new CreateArchitectureGraphMetadataTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    // Composite primary key = DynamoDB (partition key, sort key).
    db.assertPrimaryKey(TABLE_NAME, "pk_arch_graph_metadata", COLUMN_ORGANIZATION_ID, COLUMN_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ORGANIZATION_ID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ANALYSIS_ID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROJECT_ID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_BRANCH_ID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ECOSYSTEM, Types.VARCHAR, SHORT_TEXT_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_TYPE, Types.VARCHAR, SHORT_TEXT_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PERSPECTIVE_KEY, Types.VARCHAR, SHORT_TEXT_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CONTENT_TYPE, Types.VARCHAR, SHORT_TEXT_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_VERSION, Types.VARCHAR, SHORT_TEXT_SIZE, false);
    // GSI on uuid, and the analysis/project/branch LSIs (share the organization_id partition key).
    db.assertIndex(TABLE_NAME, INDEX_UUID, COLUMN_UUID);
    db.assertIndex(TABLE_NAME, INDEX_ANALYSIS, COLUMN_ORGANIZATION_ID, COLUMN_ANALYSIS_ID);
    db.assertIndex(TABLE_NAME, INDEX_PROJECT, COLUMN_ORGANIZATION_ID, COLUMN_PROJECT_ID);
    db.assertIndex(TABLE_NAME, INDEX_BRANCH, COLUMN_ORGANIZATION_ID, COLUMN_BRANCH_ID);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
