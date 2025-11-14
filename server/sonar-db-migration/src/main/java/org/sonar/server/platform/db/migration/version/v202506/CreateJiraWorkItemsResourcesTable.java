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

import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import java.sql.SQLException;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateJiraWorkItemsResourcesTable extends CreateTableChange {

  static final String TABLE_NAME = "jira_work_items_resources";

  static final String COLUMN_ID = "id";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_UPDATED_AT = "updated_at";
  static final String COLUMN_WORK_ITEM_ID = "jira_work_item_id";
  static final String COLUMN_RESOURCE_ID = "resource_id";
  static final String COLUMN_RESOURCE_TYPE = "resource_type";

  protected CreateJiraWorkItemsResourcesTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ID).setIsNullable(false).setLimit(40).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_WORK_ITEM_ID).setIsNullable(false).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_RESOURCE_ID).setIsNullable(false).setLimit(400).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_RESOURCE_TYPE).setIsNullable(false).setLimit(100).build())
      .build());
  }

}
