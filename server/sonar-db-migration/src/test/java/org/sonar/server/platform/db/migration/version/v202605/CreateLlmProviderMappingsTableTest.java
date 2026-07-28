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

import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProviderMappingsTable.AI_CAPABILITY_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProviderMappingsTable.COLUMN_AI_CAPABILITY;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProviderMappingsTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProviderMappingsTable.COLUMN_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProviderMappingsTable.COLUMN_LLM_PROVIDER_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProviderMappingsTable.COLUMN_MODEL_IDENTIFIER;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProviderMappingsTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProviderMappingsTable.ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProviderMappingsTable.INDEX_CAPABILITY;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProviderMappingsTable.INDEX_PROVIDER;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProviderMappingsTable.MODEL_IDENTIFIER_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateLlmProviderMappingsTable.TABLE_NAME;

class CreateLlmProviderMappingsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateLlmProviderMappingsTable.class);

  private final CreateLlmProviderMappingsTable underTest = new CreateLlmProviderMappingsTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_llm_provider_mappings", COLUMN_ID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ID, Types.VARCHAR, ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_AI_CAPABILITY, Types.VARCHAR, AI_CAPABILITY_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_LLM_PROVIDER_ID, Types.VARCHAR, ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_MODEL_IDENTIFIER, Types.VARCHAR, MODEL_IDENTIFIER_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
    db.assertUniqueIndex(TABLE_NAME, INDEX_CAPABILITY, COLUMN_AI_CAPABILITY);
    db.assertIndex(TABLE_NAME, INDEX_PROVIDER, COLUMN_LLM_PROVIDER_ID);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
