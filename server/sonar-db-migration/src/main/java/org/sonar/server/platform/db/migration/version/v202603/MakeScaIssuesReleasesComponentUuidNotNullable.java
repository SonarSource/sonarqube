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
package org.sonar.server.platform.db.migration.version.v202603;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.ColumnMetadata;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.AlterColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class MakeScaIssuesReleasesComponentUuidNotNullable extends DdlChange {

  static final String TABLE_NAME = "sca_issues_releases";
  static final String COLUMN_NAME = "component_uuid";
  static final String INDEX_NAME = "sca_ir_component_uuid";

  public MakeScaIssuesReleasesComponentUuidNotNullable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      // Check if column is nullable before applying NOT NULL constraint
      // AlterColumnsBuilder is not re-entrant on Oracle and will throw ORA-01442 if column is already NOT NULL
      ColumnMetadata columnMetadata = DatabaseUtils.getColumnMetadata(connection, TABLE_NAME, COLUMN_NAME);
      if (columnMetadata != null && columnMetadata.nullable()) {
        context.execute(new AlterColumnsBuilder(getDialect(), TABLE_NAME)
          .updateColumn(newVarcharColumnDefBuilder()
            .setColumnName(COLUMN_NAME)
            .setIsNullable(false)
            .setLimit(40)
            .build())
          .build());
      }

      if (!DatabaseUtils.indexExistsIgnoreCase(TABLE_NAME, INDEX_NAME, connection)) {
        context.execute(new CreateIndexBuilder(getDialect())
          .setTable(TABLE_NAME)
          .setName(INDEX_NAME)
          .addColumn(COLUMN_NAME, false)
          .build());
      }
    }
  }
}
