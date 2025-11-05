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

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateUserBindingsSlackTable extends CreateTableChange {

  static final String USER_BINDINGS_SLACK_TABLE_NAME = "user_bindings_slack";
  static final String COLUMN_UUID = "uuid";
  static final String COLUMN_USER_BINDING_UUID = "user_binding_uuid";
  static final String COLUMN_SLACK_WORKSPACE_UUID = "slack_workspace_uuid";
  static final String COLUMN_SLACK_USER_ID = "slack_user_id";
  static final String COLUMN_USER_TYPE = "user_type";

  protected CreateUserBindingsSlackTable(Database db) {
    super(db, USER_BINDINGS_SLACK_TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_USER_BINDING_UUID).setIsNullable(false).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SLACK_WORKSPACE_UUID).setIsNullable(false).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SLACK_USER_ID).setIsNullable(false).setLimit(255).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_USER_TYPE).setIsNullable(false).setLimit(20).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName("ubs_binding_workspace_user")
      .addColumn(COLUMN_USER_BINDING_UUID, false)
      .addColumn(COLUMN_SLACK_WORKSPACE_UUID, false)
      .addColumn(COLUMN_SLACK_USER_ID, false)
      .setUnique(true)
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName("ubs_slack_workspace_uuid")
      .addColumn(COLUMN_SLACK_WORKSPACE_UUID)
      .build());
  }

}
