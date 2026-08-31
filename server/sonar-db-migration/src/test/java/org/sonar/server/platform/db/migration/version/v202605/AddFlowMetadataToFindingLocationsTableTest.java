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
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v202605.AddFlowMetadataToFindingLocationsTable.COLUMN_FLOW_DESCRIPTION;
import static org.sonar.server.platform.db.migration.version.v202605.AddFlowMetadataToFindingLocationsTable.COLUMN_FLOW_INDEX;
import static org.sonar.server.platform.db.migration.version.v202605.AddFlowMetadataToFindingLocationsTable.COLUMN_FLOW_TYPE;
import static org.sonar.server.platform.db.migration.version.v202605.AddFlowMetadataToFindingLocationsTable.COLUMN_LOCATION_INDEX;
import static org.sonar.server.platform.db.migration.version.v202605.AddFlowMetadataToFindingLocationsTable.FLOW_DESCRIPTION_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.AddFlowMetadataToFindingLocationsTable.FLOW_TYPE_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.AddFlowMetadataToFindingLocationsTable.TABLE_NAME;

class AddFlowMetadataToFindingLocationsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AddFlowMetadataToFindingLocationsTable.class);

  private final AddFlowMetadataToFindingLocationsTable underTest = new AddFlowMetadataToFindingLocationsTable(db.database());

  /**
   * All four nullable: a PRIMARY location belongs to no flow, which is every finding's anchor, so this
   * is the common case rather than an edge case.
   */
  @Test
  void migration_should_add_the_flow_columns_as_nullable() throws SQLException {
    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_FLOW_INDEX);

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_FLOW_INDEX, Types.INTEGER, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_LOCATION_INDEX, Types.INTEGER, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_FLOW_DESCRIPTION, Types.VARCHAR, FLOW_DESCRIPTION_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_FLOW_TYPE, Types.VARCHAR, FLOW_TYPE_SIZE, true);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_FLOW_TYPE, Types.VARCHAR, FLOW_TYPE_SIZE, true);
  }
}
