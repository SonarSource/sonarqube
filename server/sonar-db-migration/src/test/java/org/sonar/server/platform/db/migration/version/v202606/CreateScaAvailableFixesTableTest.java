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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v202606.CreateScaAvailableFixesTable.CLASSIFICATION_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaAvailableFixesTable.COLUMN_BASELINE_CLASSIFICATION;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaAvailableFixesTable.COLUMN_BASELINE_LATEST_FIX_VERSION;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaAvailableFixesTable.COLUMN_BASELINE_NEAREST_FIX_VERSION;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaAvailableFixesTable.COLUMN_BASELINE_NEAREST_IS_COMPLETE_FIX;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaAvailableFixesTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaAvailableFixesTable.COLUMN_LIGHTWELL_CLASSIFICATION;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaAvailableFixesTable.COLUMN_LIGHTWELL_LATEST_FIX_VERSION;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaAvailableFixesTable.COLUMN_LIGHTWELL_NEAREST_FIX_VERSION;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaAvailableFixesTable.COLUMN_LIGHTWELL_NEAREST_IS_COMPLETE_FIX;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaAvailableFixesTable.COLUMN_SCA_ISSUE_UUID;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaAvailableFixesTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaAvailableFixesTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaAvailableFixesTable.COLUMN_VERSION_IN_USE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaAvailableFixesTable.INDEX_UNIQUE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaAvailableFixesTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaAvailableFixesTable.VERSION_SIZE;

class CreateScaAvailableFixesTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateScaAvailableFixesTable.class);

  private final CreateScaAvailableFixesTable underTest = new CreateScaAvailableFixesTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_sca_available_fixes", COLUMN_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UUID, Types.VARCHAR, 40, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_SCA_ISSUE_UUID, Types.VARCHAR, 40, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_VERSION_IN_USE, Types.VARCHAR, VERSION_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_BASELINE_CLASSIFICATION, Types.VARCHAR, CLASSIFICATION_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_BASELINE_NEAREST_FIX_VERSION, Types.VARCHAR, VERSION_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_BASELINE_NEAREST_IS_COMPLETE_FIX, Types.BOOLEAN, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_BASELINE_LATEST_FIX_VERSION, Types.VARCHAR, VERSION_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_LIGHTWELL_CLASSIFICATION, Types.VARCHAR, CLASSIFICATION_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_LIGHTWELL_NEAREST_FIX_VERSION, Types.VARCHAR, VERSION_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_LIGHTWELL_NEAREST_IS_COMPLETE_FIX, Types.BOOLEAN, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_LIGHTWELL_LATEST_FIX_VERSION, Types.VARCHAR, VERSION_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);

    db.assertUniqueIndex(TABLE_NAME, INDEX_UNIQUE, COLUMN_SCA_ISSUE_UUID, COLUMN_VERSION_IN_USE);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }

}
