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
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Creates the {@code llm_providers} table backing the "Bring Your Own Provider" (BYOP) feature:
 * one row per admin-configured LLM provider (Azure OpenAI, AWS Bedrock, Vertex AI, custom proxy, Sonar).
 *
 * <p>{@code connection_config} holds the non-secret provider configuration serialized as JSON;
 * {@code encrypted_secret} holds the single secret encrypted with the instance secret key and is
 * {@code null} for identity-based authentication such as AWS Bedrock (IAM).
 */
public class CreateLlmProvidersTable extends CreateTableChange {

  static final String TABLE_NAME = "llm_providers";

  static final String COLUMN_ID = "id";
  static final String COLUMN_PROVIDER = "provider";
  static final String COLUMN_LABEL = "label";
  static final String COLUMN_CONNECTION_CONFIG = "connection_config";
  static final String COLUMN_ENCRYPTED_SECRET = "encrypted_secret";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_UPDATED_AT = "updated_at";

  static final int ID_SIZE = 40;
  static final int PROVIDER_SIZE = 40;
  static final int LABEL_SIZE = 255;

  protected CreateLlmProvidersTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ID).setIsNullable(false).setLimit(ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PROVIDER).setIsNullable(false).setLimit(PROVIDER_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_LABEL).setIsNullable(false).setLimit(LABEL_SIZE).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_CONNECTION_CONFIG).setIsNullable(false).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_ENCRYPTED_SECRET).setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .build());
  }
}
