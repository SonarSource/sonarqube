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
package org.sonar.server.platform.db.migration.version.v202502;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateScaDependenciesTable extends CreateTableChange {

  private static final String TABLE_NAME = "sca_dependencies";
  private static final String COLUMN_UUID_NAME = "uuid";
  private static final String COLUMN_COMPONENT_UUID_NAME = "component_uuid";
  private static final String COLUMN_PACKAGE_URL_NAME = "package_url";
  private static final int COLUMN_PACKAGE_URL_SIZE = 400;
  private static final String COLUMN_PACKAGE_MANAGER_NAME = "package_manager";
  private static final int COLUMN_PACKAGE_MANAGER_SIZE = 20;
  private static final String COLUMN_PACKAGE_NAME_NAME = "package_name";
  private static final int COLUMN_PACKAGE_NAME_SIZE = 400;
  private static final String COLUMN_VERSION_NAME = "version";
  private static final int COLUMN_VERSION_SIZE = 400;
  private static final String COLUMN_DIRECT_NAME = "direct";
  private static final String COLUMN_SCOPE_NAME = "scope";
  private static final int COLUMN_SCOPE_SIZE = 100;
  private static final String COLUMN_DEPENDENCY_FILE_PATH_NAME = "dependency_file_path";
  private static final int COLUMN_DEPENDENCY_FILE_PATH_SIZE = 1000;
  private static final String COLUMN_LICENSE_EXPRESSION_NAME = "license_expression";
  private static final int COLUMN_LICENSE_EXPRESSION_SIZE = 400;
  private static final String COLUMN_KNOWN_NAME = "known";
  private static final String COLUMN_CREATED_AT_NAME = "created_at";
  private static final String COLUMN_UPDATED_AT_NAME = "updated_at";

  protected CreateScaDependenciesTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID_NAME).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_COMPONENT_UUID_NAME).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PACKAGE_URL_NAME).setIsNullable(false).setLimit(COLUMN_PACKAGE_URL_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PACKAGE_MANAGER_NAME).setIsNullable(false).setLimit(COLUMN_PACKAGE_MANAGER_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PACKAGE_NAME_NAME).setIsNullable(false).setLimit(COLUMN_PACKAGE_NAME_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_VERSION_NAME).setIsNullable(false).setLimit(COLUMN_VERSION_SIZE).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName(COLUMN_DIRECT_NAME).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SCOPE_NAME).setIsNullable(false).setLimit(COLUMN_SCOPE_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_DEPENDENCY_FILE_PATH_NAME).setIsNullable(false).setLimit(COLUMN_DEPENDENCY_FILE_PATH_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_LICENSE_EXPRESSION_NAME).setIsNullable(false).setLimit(COLUMN_LICENSE_EXPRESSION_SIZE).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName(COLUMN_KNOWN_NAME).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT_NAME).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT_NAME).setIsNullable(false).build())
      .build());
  }
}
