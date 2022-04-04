/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v89;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class AddIndicesToNewCodePeriodTable extends DdlChange {
  private static final String TABLE_NAME = "new_code_periods";
  private static final String TYPE_INDEX_NAME = "idx_ncp_type";
  private static final String VALUE_INDEX_NAME = "idx_ncp_value";

  public AddIndicesToNewCodePeriodTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (!indexExists(TYPE_INDEX_NAME)) {
      context.execute(new CreateIndexBuilder()
        .setUnique(false)
        .setTable(TABLE_NAME)
        .setName(TYPE_INDEX_NAME)
        .addColumn(newVarcharColumnDefBuilder()
          .setColumnName("type")
          .setIsNullable(false)
          .setLimit(30)
          .build())
        .build());
    }

    if (!indexExists(VALUE_INDEX_NAME)) {
      context.execute(new CreateIndexBuilder()
        .setUnique(false)
        .setTable(TABLE_NAME)
        .setName(VALUE_INDEX_NAME)
        .addColumn(newVarcharColumnDefBuilder()
          .setColumnName("value")
          .setIsNullable(true)
          .setLimit(VarcharColumnDef.UUID_SIZE)
          .build())
        .build());
    }
  }

  private boolean indexExists(String index) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      return DatabaseUtils.indexExistsIgnoreCase(TABLE_NAME, index, connection);
    }
  }
}
