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
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateA3SAnalysisUsagesTable extends CreateTableChange {

  static final String TABLE_NAME = "a3s_analysis_usages";
  static final String COLUMN_UUID = "uuid";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_PROJECT_UUID = "project_uuid";
  static final String COLUMN_FILES_COUNT = "files_count";
  static final String COLUMN_USER_UUID = "user_uuid";
  static final String COLUMN_ANCLOC = "ancloc";

  static final int UUID_SIZE = 40;

  protected CreateA3SAnalysisUsagesTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PROJECT_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_FILES_COUNT).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_USER_UUID).setIsNullable(true).setLimit(UUID_SIZE).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_ANCLOC).setIsNullable(true).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName("idx_a3s_analysis_usages_proj")
      .setUnique(false)
      .addColumn(COLUMN_CREATED_AT, false)
      .addColumn(COLUMN_PROJECT_UUID, false)
      .build());
  }
}
