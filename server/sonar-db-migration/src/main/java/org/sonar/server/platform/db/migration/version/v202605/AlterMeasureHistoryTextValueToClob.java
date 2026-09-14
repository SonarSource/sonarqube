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
import java.sql.Types;
import org.sonar.db.ColumnMetadata;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.DropColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.version.v202605.CreateMeasureHistoryTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateMeasureHistoryTable.TEXT_VALUE;

public class AlterMeasureHistoryTextValueToClob extends DdlChange {

  static final int LEGACY_TEXT_VALUE_SIZE = 4_000;
  static final String TEMP_COLUMN_NAME = "text_value_tmp";

  public AlterMeasureHistoryTextValueToClob(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      boolean hasTextValue = DatabaseUtils.tableColumnExists(connection, TABLE_NAME, TEXT_VALUE);
      boolean hasTemporaryTextValue = DatabaseUtils.tableColumnExists(connection, TABLE_NAME, TEMP_COLUMN_NAME);
      if (!hasTemporaryTextValue) {
        if (hasTextValue && isLegacyTextValue(connection)) {
          startConversion(context);
        }
      } else if (!hasTextValue) {
        finishConversion(context);
      } else if (isLegacyTextValue(connection)) {
        copyToTemporaryColumn(context);
        dropTextValueColumn(context);
        finishConversion(context);
      } else {
        copyFromTemporaryColumn(context);
        dropTemporaryColumn(context);
      }
    }
  }

  private static boolean isLegacyTextValue(Connection connection) throws SQLException {
    ColumnMetadata columnMetadata = DatabaseUtils.getColumnMetadata(connection, TABLE_NAME, TEXT_VALUE);
    return columnMetadata != null && columnMetadata.sqlType() != Types.CLOB && columnMetadata.limit() == LEGACY_TEXT_VALUE_SIZE;
  }

  private void startConversion(Context context) {
    addClobColumn(context, TEMP_COLUMN_NAME);
    copyToTemporaryColumn(context);
    dropTextValueColumn(context);
    finishConversion(context);
  }

  private void finishConversion(Context context) {
    addClobColumn(context, TEXT_VALUE);
    copyFromTemporaryColumn(context);
    dropTemporaryColumn(context);
  }

  private void addClobColumn(Context context, String columnName) {
    context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
      .addColumn(newClobColumnDefBuilder().setColumnName(columnName).setIsNullable(true).build())
      .build());
  }

  private static void copyToTemporaryColumn(Context context) {
    context.execute("UPDATE " + TABLE_NAME + " SET " + TEMP_COLUMN_NAME + " = " + TEXT_VALUE);
  }

  private static void copyFromTemporaryColumn(Context context) {
    context.execute("UPDATE " + TABLE_NAME + " SET " + TEXT_VALUE + " = " + TEMP_COLUMN_NAME);
  }

  private void dropTextValueColumn(Context context) {
    context.execute(new DropColumnsBuilder(getDialect(), TABLE_NAME, TEXT_VALUE).build());
  }

  private void dropTemporaryColumn(Context context) {
    context.execute(new DropColumnsBuilder(getDialect(), TABLE_NAME, TEMP_COLUMN_NAME).build());
  }
}
