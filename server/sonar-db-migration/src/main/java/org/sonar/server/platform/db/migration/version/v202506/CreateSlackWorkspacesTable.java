/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateSlackWorkspacesTable extends CreateTableChange {

  static final String SLACK_WORKSPACES_TABLE_NAME = "slack_workspaces";
  static final String COLUMN_UUID = "uuid";
  static final String COLUMN_INTEGRATION_CONFIG_UUID = "integration_config_uuid";
  static final String COLUMN_WORKSPACE_ID = "workspace_id";
  static final String COLUMN_WORKSPACE_NAME = "workspace_name";
  static final String COLUMN_ACCESS_TOKEN = "access_token";
  static final String COLUMN_REFRESH_TOKEN = "refresh_token";
  static final String COLUMN_BOT_USER_ID = "bot_user_id";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_UPDATED_AT = "updated_at";

  protected CreateSlackWorkspacesTable(Database db) {
    super(db, SLACK_WORKSPACES_TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_INTEGRATION_CONFIG_UUID).setIsNullable(false).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_WORKSPACE_ID).setIsNullable(false).setLimit(255).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_WORKSPACE_NAME).setIsNullable(true).setLimit(255).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ACCESS_TOKEN).setIsNullable(true).setLimit(2000).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_REFRESH_TOKEN).setIsNullable(true).setLimit(2000).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_BOT_USER_ID).setIsNullable(true).setLimit(255).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName("sw_workspace_id")
      .addColumn(COLUMN_WORKSPACE_ID, false)
      .setUnique(true)
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName("sw_integration_config_uuid")
      .addColumn(COLUMN_INTEGRATION_CONFIG_UUID)
      .build());
  }

}
