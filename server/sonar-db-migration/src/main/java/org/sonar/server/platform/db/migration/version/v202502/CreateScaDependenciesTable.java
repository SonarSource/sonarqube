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
package org.sonar.server.platform.db.migration.version.v202502;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateScaDependenciesTable extends CreateTableChange {

  private static final String TABLE_NAME = "sca_dependencies";
  private static final String COLUMN_UUID_NAME = "uuid";
  private static final String COLUMN_SCA_RELEASE_UUID_NAME = "sca_release_uuid";
  private static final String COLUMN_DIRECT_NAME = "direct";
  private static final String COLUMN_SCOPE_NAME = "scope";
  private static final int COLUMN_SCOPE_SIZE = 100;
  private static final String COLUMN_USER_DEPENDENCY_FILE_PATH_NAME = "user_dependency_file_path";
  private static final String COLUMN_LOCKFILE_DEPENDENCY_FILE_PATH_NAME = "lockfile_dependency_file_path";
  private static final int COLUMN_DEPENDENCY_FILE_PATH_SIZE = 1000;
  private static final String COLUMN_CHAINS_NAME = "chains";
  private static final String COLUMN_CREATED_AT_NAME = "created_at";
  private static final String COLUMN_UPDATED_AT_NAME = "updated_at";

  protected CreateScaDependenciesTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID_NAME).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SCA_RELEASE_UUID_NAME).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName(COLUMN_DIRECT_NAME).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SCOPE_NAME).setIsNullable(false).setLimit(COLUMN_SCOPE_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_USER_DEPENDENCY_FILE_PATH_NAME).setIsNullable(true).setLimit(COLUMN_DEPENDENCY_FILE_PATH_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_LOCKFILE_DEPENDENCY_FILE_PATH_NAME).setIsNullable(true).setLimit(COLUMN_DEPENDENCY_FILE_PATH_SIZE).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_CHAINS_NAME).setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT_NAME).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT_NAME).setIsNullable(false).build())
      .build());
  }
}
