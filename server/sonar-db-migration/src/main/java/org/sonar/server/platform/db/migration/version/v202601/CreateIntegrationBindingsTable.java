/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202601;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateIntegrationBindingsTable extends CreateTableChange {

  static final String TABLE_NAME = "integration_bindings";
  static final String COLUMN_ID = "id";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_UPDATED_AT = "updated_at";
  static final String COLUMN_INTEGRATION_TYPE = "integration_type";
  static final String COLUMN_ENTITY_TYPE = "entity_type";
  static final String COLUMN_ENTITY_UUID = "entity_uuid";
  static final String COLUMN_TOKEN_TYPE = "token_type";
  static final String COLUMN_UPDATED_BY = "updated_by";
  static final String COLUMN_ACCESS_TOKEN = "access_token";
  static final String COLUMN_REFRESH_TOKEN = "refresh_token";
  static final String COLUMN_SCOPE = "scope";
  static final String COLUMN_EXPIRES_IN_SECONDS = "expires_in_seconds";
  static final String COLUMN_EXTRA_DETAILS = "extra_details";

  static final int UUID_SIZE = 40;
  static final int INTEGRATION_TYPE_SIZE = 100;
  static final int ENTITY_TYPE_SIZE = 100;
  static final int TOKEN_TYPE_SIZE = 20;

  protected CreateIntegrationBindingsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_INTEGRATION_TYPE).setIsNullable(false).setLimit(INTEGRATION_TYPE_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ENTITY_TYPE).setIsNullable(false).setLimit(ENTITY_TYPE_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ENTITY_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_TOKEN_TYPE).setIsNullable(false).setLimit(TOKEN_TYPE_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UPDATED_BY).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_ACCESS_TOKEN).setIsNullable(false).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_REFRESH_TOKEN).setIsNullable(true).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_SCOPE).setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_EXPIRES_IN_SECONDS).setIsNullable(true).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_EXTRA_DETAILS).setIsNullable(true).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName("integration_bindings_idx")
      .setUnique(true)
      .addColumn(COLUMN_INTEGRATION_TYPE, false)
      .addColumn(COLUMN_ENTITY_TYPE, false)
      .addColumn(COLUMN_ENTITY_UUID, false)
      .build());
  }

}
