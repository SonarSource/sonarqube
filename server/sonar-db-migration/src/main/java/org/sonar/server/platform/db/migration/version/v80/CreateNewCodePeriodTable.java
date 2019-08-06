/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v80;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;


public class CreateNewCodePeriodTable extends DdlChange {

  private static final String TABLE_NAME = "new_code_periods";

  private static final VarcharColumnDef UUID_COLUMN = newVarcharColumnDefBuilder()
    .setColumnName("uuid")
    .setIsNullable(false)
    .setLimit(UUID_SIZE)
    .build();

  private static final VarcharColumnDef PROJECT_UUID_COLUMN = newVarcharColumnDefBuilder()
    .setColumnName("project_uuid")
    .setIsNullable(true)
    .setLimit(UUID_SIZE)
    .build();
  private static final VarcharColumnDef BRANCH_UUID_COLUMN = newVarcharColumnDefBuilder()
    .setColumnName("branch_uuid")
    .setIsNullable(true)
    .setLimit(UUID_SIZE)
    .build();

  /**
   * Available values:
   * * PREVIOUS_VERSION
   * * NUMBER_OF_DAYS
   * * DATE
   * * SPECIFIC_ANALYSIS
   */
  private static final VarcharColumnDef TYPE = newVarcharColumnDefBuilder()
    .setColumnName("type")
    .setIsNullable(false)
    .setLimit(30)
    .build();

  private static final VarcharColumnDef VALUE = newVarcharColumnDefBuilder()
    .setColumnName("value")
    .setIsNullable(true)
    .setLimit(40)
    .build();

  private static final BigIntegerColumnDef UPDATED_AT = newBigIntegerColumnDefBuilder()
    .setColumnName("updated_at")
    .setIsNullable(false)
    .build();

  private static final BigIntegerColumnDef CREATED_AT = newBigIntegerColumnDefBuilder()
    .setColumnName("created_at")
    .setIsNullable(false)
    .build();


  public CreateNewCodePeriodTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (tableExists()) {
      return;
    }
    context.execute(new CreateTableBuilder(getDialect(), TABLE_NAME)
      .addPkColumn(UUID_COLUMN)
      .addColumn(PROJECT_UUID_COLUMN)
      .addColumn(BRANCH_UUID_COLUMN)
      .addColumn(TYPE)
      .addColumn(VALUE)
      .addColumn(UPDATED_AT)
      .addColumn(CREATED_AT)
      .build());

    context.execute(new CreateIndexBuilder()
      .setTable(TABLE_NAME)
      .setName("uniq_new_code_periods")
      .setUnique(true)
      .addColumn(PROJECT_UUID_COLUMN)
      .addColumn(BRANCH_UUID_COLUMN)
      .build());
  }

  private boolean tableExists() throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      return DatabaseUtils.tableExists(TABLE_NAME, connection);
    }
  }
}
