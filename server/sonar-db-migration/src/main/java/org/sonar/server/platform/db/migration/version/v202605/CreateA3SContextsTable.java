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

public class CreateA3SContextsTable extends CreateTableChange {

  static final String TABLE_NAME = "a3s_contexts";
  static final String COLUMN_UUID = "uuid";
  static final String COLUMN_ANALYSIS_UUID = "analysis_uuid";
  static final String COLUMN_BRANCH_UUID = "branch_uuid";
  static final String COLUMN_BRANCH_NAME = "branch_name";
  static final String COLUMN_PROJECT_UUID = "project_uuid";
  static final String COLUMN_KIND = "kind";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_EXPIRES_AT = "expires_at";

  static final int UUID_SIZE = 40;
  static final int BRANCH_NAME_SIZE = 255;
  static final int KIND_SIZE = 40;

  protected CreateA3SContextsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ANALYSIS_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_BRANCH_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_BRANCH_NAME).setIsNullable(false).setLimit(BRANCH_NAME_SIZE).setDefaultValue("").build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PROJECT_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_KIND).setIsNullable(false).setLimit(KIND_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_EXPIRES_AT).setIsNullable(true).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName("idx_a3s_contexts_proj_branch")
      .setUnique(false)
      .addColumn(COLUMN_PROJECT_UUID, false)
      .addColumn(COLUMN_BRANCH_NAME, false)
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName("idx_a3s_contexts_expires_at")
      .setUnique(false)
      .addColumn(COLUMN_EXPIRES_AT, true)
      .build());
  }
}
