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
package org.sonar.server.platform.db.migration.version.v108;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateProjectDependenciesTable extends CreateTableChange {

  private static final String TABLE_NAME = "project_dependencies";
  private static final String COLUMN_UUID_NAME = "uuid";
  private static final String COLUMN_VERSION_NAME = "version";
  private static final String COLUMN_INCLUDE_PATHS_NAME = "include_paths";
  private static final String COLUMN_PACKAGE_MANAGER_NAME = "package_manager";
  private static final int COLUMN_PACKAGE_MANAGER_SIZE = 50;
  private static final String COLUMN_CREATED_AT_NAME = "created_at";
  private static final String COLUMN_UPDATED_AT_NAME = "updated_at";

  protected CreateProjectDependenciesTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID_NAME).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_VERSION_NAME).setIsNullable(true).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_INCLUDE_PATHS_NAME).setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PACKAGE_MANAGER_NAME).setIsNullable(true).setLimit(COLUMN_PACKAGE_MANAGER_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT_NAME).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT_NAME).setIsNullable(false).build())
      .build());
  }
}
