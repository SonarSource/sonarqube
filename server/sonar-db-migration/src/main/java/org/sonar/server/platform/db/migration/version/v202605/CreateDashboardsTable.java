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
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.TimestampColumnDef.newTimestampColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateDashboardsTable extends CreateTableChange {

  static final String TABLE_NAME = "dashboards";
  static final String ID = "id";
  static final String NAME = "name";
  static final String LAYOUT_STATE = "layout_state";
  static final String DESCRIPTION = "description";
  static final String CREATED_AT = "created_at";
  static final String UPDATED_AT = "updated_at";
  static final String CREATOR_ID = "creator_id";
  static final String RESOURCE_TYPE = "resource_type";
  static final String RESOURCE_ID = "resource_id";
  static final String UPDATE_BY_ID = "update_by_id";
  static final String CREATED_AT_DEFAULT_CONSTRAINT = "dashboards_created_at_default";
  static final String UPDATED_AT_DEFAULT_CONSTRAINT = "dashboards_updated_at_default";
  static final String DASHBOARDS_NAME_RESOURCE_IDX = "idx_dashboards_name_resource";

  private static final String ALTER_COLUMN_DEFAULT_TEMPLATE = "ALTER TABLE %s ALTER COLUMN %s SET DEFAULT CURRENT_TIMESTAMP";
  private static final String ADD_DEFAULT_CONSTRAINT_TEMPLATE = "ALTER TABLE %s ADD CONSTRAINT %s DEFAULT CURRENT_TIMESTAMP FOR %s";
  private static final String MODIFY_COLUMNS_DEFAULT_TEMPLATE = "ALTER TABLE %s MODIFY (%s DEFAULT CURRENT_TIMESTAMP, %s DEFAULT CURRENT_TIMESTAMP)";

  protected CreateDashboardsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(ID).setIsNullable(false).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(NAME).setIsNullable(false).setLimit(300).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(LAYOUT_STATE).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(DESCRIPTION).setIsNullable(true).setLimit(500).build())
      .addColumn(newTimestampColumnDefBuilder().setColumnName(CREATED_AT).setIsNullable(false).build())
      .addColumn(newTimestampColumnDefBuilder().setColumnName(UPDATED_AT).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(CREATOR_ID).setIsNullable(false).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(RESOURCE_TYPE).setIsNullable(false).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(RESOURCE_ID).setIsNullable(false).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(UPDATE_BY_ID).setIsNullable(false).setLimit(40).build())
      .build());

    addTimestampDefaults(context, tableName);

    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(tableName)
      .setName(DASHBOARDS_NAME_RESOURCE_IDX)
      .addColumn(NAME)
      .addColumn(RESOURCE_TYPE)
      .addColumn(RESOURCE_ID)
      .build());
  }

  private void addTimestampDefaults(Context context, String tableName) {
    switch (getDialect().getId()) {
      case H2.ID, PostgreSql.ID -> context.execute(
        ALTER_COLUMN_DEFAULT_TEMPLATE.formatted(tableName, CREATED_AT),
        ALTER_COLUMN_DEFAULT_TEMPLATE.formatted(tableName, UPDATED_AT));
      case MsSql.ID -> context.execute(
        ADD_DEFAULT_CONSTRAINT_TEMPLATE.formatted(tableName, CREATED_AT_DEFAULT_CONSTRAINT, CREATED_AT),
        ADD_DEFAULT_CONSTRAINT_TEMPLATE.formatted(tableName, UPDATED_AT_DEFAULT_CONSTRAINT, UPDATED_AT));
      case Oracle.ID -> context.execute(
        MODIFY_COLUMNS_DEFAULT_TEMPLATE.formatted(tableName, CREATED_AT, UPDATED_AT));
      default -> throw new IllegalStateException("Unsupported database dialect: " + getDialect().getId());
    }
  }
}
