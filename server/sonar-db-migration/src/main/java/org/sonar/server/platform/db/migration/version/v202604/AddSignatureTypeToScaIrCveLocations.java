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
package org.sonar.server.platform.db.migration.version.v202604;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.ColumnMetadata;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.AlterColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.db.DatabaseUtils.tableColumnExists;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class AddSignatureTypeToScaIrCveLocations extends DdlChange {

  static final String TABLE_NAME = "sca_ir_cve_locations";
  static final String COLUMN_NAME = "signature_type";
  static final int COLUMN_SIZE = 31;

  public AddSignatureTypeToScaIrCveLocations(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!tableColumnExists(connection, TABLE_NAME, COLUMN_NAME)) {
        context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
          .addColumn(newVarcharColumnDefBuilder()
            .setColumnName(COLUMN_NAME)
            .setIsNullable(true)
            .setLimit(COLUMN_SIZE)
            .build())
          .build());
      }

      try (var statement = connection.createStatement()) {
        statement.executeUpdate("UPDATE sca_ir_cve_locations SET signature_type = 'VULNERABLE_FUNCTION' WHERE signature_type IS NULL");
      }

      ColumnMetadata columnMetadata = DatabaseUtils.getColumnMetadata(connection, TABLE_NAME, COLUMN_NAME);
      if (columnMetadata != null && columnMetadata.nullable()) {
        context.execute(new AlterColumnsBuilder(getDialect(), TABLE_NAME)
          .updateColumn(newVarcharColumnDefBuilder()
            .setColumnName(COLUMN_NAME)
            .setIsNullable(false)
            .setLimit(COLUMN_SIZE)
            .build())
          .build());
      }
    }
  }
}
