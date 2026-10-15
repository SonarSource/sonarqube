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

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.VULNERABILITY_ID_MAX_LENGTH;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbReachabilityDefinitionsTable.COLUMN_ANCESTOR_CALLERS;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbReachabilityDefinitionsTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbReachabilityDefinitionsTable.COLUMN_LANGUAGE_KEY;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbReachabilityDefinitionsTable.COLUMN_PUBLIC_CALLERS;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbReachabilityDefinitionsTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbReachabilityDefinitionsTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbReachabilityDefinitionsTable.COLUMN_VULNERABILITY_ID;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbReachabilityDefinitionsTable.COLUMN_VULNERABLE_FUNCTIONS;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbReachabilityDefinitionsTable.INDEX_LANGUAGE_VULNERABILITY_UNIQ;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbReachabilityDefinitionsTable.LANGUAGE_KEY_MAX_LENGTH;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbReachabilityDefinitionsTable.TABLE_NAME;

class CreateScaRefdbReachabilityDefinitionsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateScaRefdbReachabilityDefinitionsTable.class);

  private final CreateScaRefdbReachabilityDefinitionsTable underTest = new CreateScaRefdbReachabilityDefinitionsTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_sca_ref_reachability_defs", COLUMN_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_LANGUAGE_KEY, Types.VARCHAR, LANGUAGE_KEY_MAX_LENGTH, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_VULNERABILITY_ID, Types.VARCHAR, VULNERABILITY_ID_MAX_LENGTH, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_VULNERABLE_FUNCTIONS, Types.CLOB, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PUBLIC_CALLERS, Types.CLOB, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ANCESTOR_CALLERS, Types.CLOB, null, false);
    db.assertUniqueIndex(TABLE_NAME, INDEX_LANGUAGE_VULNERABILITY_UNIQ, COLUMN_LANGUAGE_KEY, COLUMN_VULNERABILITY_ID);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
