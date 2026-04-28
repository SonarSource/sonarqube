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
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.DropColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;

public class AlterScaAnalysesFailedReasonColumnToClob extends DdlChange {

  static final String TABLE_NAME = "sca_analyses";
  static final String COLUMN_NAME = "failed_reason";
  static final String TEMP_COLUMN_NAME = "failed_reason_tmp";

  public AlterScaAnalysesFailedReasonColumnToClob(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!DatabaseUtils.tableColumnExists(connection, TABLE_NAME, TEMP_COLUMN_NAME)) {
        // Cannot alter the column type directly on all databases, so we will use a staging column to move data.
        // Add a clob temp column, copy data, drop the old varchar column, then finish the migration.
        context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
          .addColumn(newClobColumnDefBuilder()
            .setColumnName(TEMP_COLUMN_NAME)
            .setIsNullable(true)
            .build())
          .build());
        context.execute("UPDATE " + TABLE_NAME + " SET " + TEMP_COLUMN_NAME + " = " + COLUMN_NAME);
        context.execute(new DropColumnsBuilder(getDialect(), TABLE_NAME, COLUMN_NAME).build());
        finishConversion(context);
      } else if (!DatabaseUtils.tableColumnExists(connection, TABLE_NAME, COLUMN_NAME)) {
        // Temp column exists, but the original was already dropped. Resume from the final steps.
        finishConversion(context);
      } else {
        // Both columns exist. Two distinct substates are possible.
        ColumnMetadata columnMetadata = DatabaseUtils.getColumnMetadata(connection, TABLE_NAME, COLUMN_NAME);
        if (columnMetadata != null && columnMetadata.limit() == 255) {
          // failed_reason is still VARCHAR: the temp column was added, but the data copy may or may not have
          // completed before the process died. Redo the copy unconditionally (safe since the original column
          // still exists), then drop and finish.
          context.execute("UPDATE " + TABLE_NAME + " SET " + TEMP_COLUMN_NAME + " = " + COLUMN_NAME);
          context.execute(new DropColumnsBuilder(getDialect(), TABLE_NAME, COLUMN_NAME).build());
          finishConversion(context);
        } else {
          // failed_reason is already CLOB: finishConversion ran its add column step and auto-committed,
          // but died before copying data back from the temp column. The temp column still holds all the data.
          // Do not overwrite the temp column — just complete the copy-back and drop the temp.
          context.execute("UPDATE " + TABLE_NAME + " SET " + COLUMN_NAME + " = " + TEMP_COLUMN_NAME);
          context.execute(new DropColumnsBuilder(getDialect(), TABLE_NAME, TEMP_COLUMN_NAME).build());
        }
      }
    }
  }

  private void finishConversion(Context context) {
    context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
      .addColumn(newClobColumnDefBuilder()
        .setColumnName(COLUMN_NAME)
        .setIsNullable(true)
        .build())
      .build());
    context.execute("UPDATE " + TABLE_NAME + " SET " + COLUMN_NAME + " = " + TEMP_COLUMN_NAME);
    context.execute(new DropColumnsBuilder(getDialect(), TABLE_NAME, TEMP_COLUMN_NAME).build());
  }
}
