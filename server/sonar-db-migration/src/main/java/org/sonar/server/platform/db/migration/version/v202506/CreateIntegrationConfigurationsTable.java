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
package org.sonar.server.platform.db.migration.version.v202506;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateIntegrationConfigurationsTable extends CreateTableChange {

  static final String INTEGRATION_CONFIGURATIONS_TABLE_NAME = "integration_configs";
  static final String COLUMN_UUID = "uuid";
  static final String COLUMN_INTEGRATION_TYPE = "integration_type";
  static final String COLUMN_CLIENT_ID = "client_id";
  static final String COLUMN_CLIENT_SECRET = "client_secret";
  static final String COLUMN_SIGNING_SECRET = "signing_secret";
  static final String COLUMN_APP_ID = "app_id";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_UPDATED_AT = "updated_at";

  protected CreateIntegrationConfigurationsTable(Database db) {
    super(db, INTEGRATION_CONFIGURATIONS_TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_INTEGRATION_TYPE).setIsNullable(false).setLimit(20).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_CLIENT_ID).setIsNullable(true).setLimit(255).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_CLIENT_SECRET).setIsNullable(true).setLimit(255).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SIGNING_SECRET).setIsNullable(true).setLimit(255).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_APP_ID).setIsNullable(true).setLimit(255).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .build());
  }

}
