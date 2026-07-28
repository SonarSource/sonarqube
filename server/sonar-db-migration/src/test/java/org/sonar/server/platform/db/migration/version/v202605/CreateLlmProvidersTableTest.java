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

import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProvidersTable.COLUMN_CONNECTION_CONFIG;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProvidersTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProvidersTable.COLUMN_ENCRYPTED_SECRET;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProvidersTable.COLUMN_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProvidersTable.COLUMN_LABEL;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProvidersTable.COLUMN_PROVIDER;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProvidersTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProvidersTable.ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProvidersTable.LABEL_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProvidersTable.PROVIDER_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProvidersTable.TABLE_NAME;

class CreateLlmProvidersTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateLlmProvidersTable.class);

  private final CreateLlmProvidersTable underTest = new CreateLlmProvidersTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_llm_providers", COLUMN_ID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ID, Types.VARCHAR, ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROVIDER, Types.VARCHAR, PROVIDER_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_LABEL, Types.VARCHAR, LABEL_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CONNECTION_CONFIG, Types.CLOB, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ENCRYPTED_SECRET, Types.CLOB, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
