/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v202501;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateMigrationLogsTable extends CreateTableChange {

  static final String MIGRATION_LOGS_TABLE_NAME = "migration_logs";

  static final String COLUMN_UUID = "uuid";
  static final String COLUMN_STEP = "step";
  static final String COLUMN_DURATION_IN_MS = "duration_in_ms";
  static final String COLUMN_SUCCESS = "success";
  static final String COLUMN_STARTED_AT = "started_at";
  static final String COLUMN_TARGET_VERSION = "target_version";


  protected CreateMigrationLogsTable(Database db) {
    super(db, MIGRATION_LOGS_TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_STEP).setIsNullable(false).setLimit(40).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_DURATION_IN_MS).setIsNullable(false).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName(COLUMN_SUCCESS).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_STARTED_AT).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_TARGET_VERSION).setIsNullable(false).setLimit(40).build())
      .build());
  }
}
