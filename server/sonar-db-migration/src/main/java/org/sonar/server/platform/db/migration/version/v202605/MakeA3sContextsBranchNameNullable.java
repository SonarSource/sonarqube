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

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.ColumnMetadata;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.server.platform.db.migration.sql.AlterColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.DropMsSQLDefaultConstraintsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;
import static org.sonar.server.platform.db.migration.version.v202605.CreateA3SContextsTable.BRANCH_NAME_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateA3SContextsTable.COLUMN_BRANCH_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateA3SContextsTable.TABLE_NAME;

public class MakeA3sContextsBranchNameNullable extends DdlChange {

  public MakeA3sContextsBranchNameNullable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      ColumnMetadata columnMetadata = DatabaseUtils.getColumnMetadata(connection, TABLE_NAME, COLUMN_BRANCH_NAME);
      if (columnMetadata == null) {
        return;
      }

      // Check if column is not nullable before dropping the NOT NULL constraint.
      // AlterColumnsBuilder is not re-entrant on Oracle and will throw ORA-01451 if column is already nullable.
      if (!columnMetadata.nullable()) {
        context.execute(new AlterColumnsBuilder(getDialect(), TABLE_NAME)
          .updateColumn(newVarcharColumnDefBuilder()
            .setColumnName(COLUMN_BRANCH_NAME)
            .setIsNullable(true)
            .setLimit(BRANCH_NAME_SIZE)
            .build())
          .build());
      }

      // The column was created with DEFAULT ''. AlterColumnsBuilder cannot express a default change, so on PostgreSQL,
      // Oracle and SQL Server the ALTER above leaves that default in place (only H2 drops it as a side effect of re-typing
      // the column). Drop it explicitly so branch_name becomes plain nullable on every database, matching the reference
      // schema. The statements below are idempotent, keeping this migration re-entrant.
      dropBranchNameDefault(context);
    }
  }

  private void dropBranchNameDefault(Context context) throws SQLException {
    if (MsSql.ID.equals(getDialect().getId())) {
      context.execute(new DropMsSQLDefaultConstraintsBuilder(getDatabase())
        .setTable(TABLE_NAME)
        .setColumns(COLUMN_BRANCH_NAME)
        .build());
    } else if (Oracle.ID.equals(getDialect().getId())) {
      context.execute("ALTER TABLE " + TABLE_NAME + " MODIFY (" + COLUMN_BRANCH_NAME + " DEFAULT NULL)");
    } else {
      // PostgreSQL and H2
      context.execute("ALTER TABLE " + TABLE_NAME + " ALTER COLUMN " + COLUMN_BRANCH_NAME + " DROP DEFAULT");
    }
  }
}