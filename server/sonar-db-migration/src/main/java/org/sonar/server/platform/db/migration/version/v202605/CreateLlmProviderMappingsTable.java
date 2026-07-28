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
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Creates the {@code llm_provider_mappings} table: one row per AI capability
 * ({@code AI_CODEFIX}, {@code HUNTER_AGENT}, {@code REMEDIATION_AGENT}) pointing at the
 * {@code llm_providers} row it uses.
 *
 * <p>{@code ai_capability} is unique &mdash; there is exactly one selection per capability. The
 * {@code llm_provider_id} column is a logical reference to {@code llm_providers.id}; SonarQube does
 * not create physical foreign keys, so referential integrity (and blocking deletion of a provider
 * that is in use) is enforced in the application layer. "No selection yet" is represented by the
 * absence of a row rather than a nullable column.
 */
public class CreateLlmProviderMappingsTable extends CreateTableChange {

  static final String TABLE_NAME = "llm_provider_mappings";

  static final String COLUMN_ID = "id";
  static final String COLUMN_AI_CAPABILITY = "ai_capability";
  static final String COLUMN_LLM_PROVIDER_ID = "llm_provider_id";
  static final String COLUMN_MODEL_IDENTIFIER = "model_identifier";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_UPDATED_AT = "updated_at";

  static final int ID_SIZE = 40;
  static final int AI_CAPABILITY_SIZE = 40;
  static final int MODEL_IDENTIFIER_SIZE = 255;

  static final String INDEX_CAPABILITY = "uq_llm_prov_map_capability";
  static final String INDEX_PROVIDER = "idx_llm_prov_map_provider";

  protected CreateLlmProviderMappingsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ID).setIsNullable(false).setLimit(ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_AI_CAPABILITY).setIsNullable(false).setLimit(AI_CAPABILITY_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_LLM_PROVIDER_ID).setIsNullable(false).setLimit(ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_MODEL_IDENTIFIER).setIsNullable(true).setLimit(MODEL_IDENTIFIER_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .build());

    // One selection per capability.
    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_CAPABILITY)
      .setUnique(true)
      .addColumn(COLUMN_AI_CAPABILITY, false)
      .build());

    // Supports the "is this provider selected by any capability?" lookup used to block provider deletion.
    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_PROVIDER)
      .setUnique(false)
      .addColumn(COLUMN_LLM_PROVIDER_ID, false)
      .build());
  }
}
