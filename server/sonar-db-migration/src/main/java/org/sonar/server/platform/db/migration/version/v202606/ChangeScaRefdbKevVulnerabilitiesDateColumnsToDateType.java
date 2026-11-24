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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Objects;
import org.sonar.db.ColumnMetadata;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.dialect.Oracle;
import org.sonar.server.platform.db.migration.def.DateColumnDef;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.DropColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.COLUMN_DATE_ADDED;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.COLUMN_DUE_DATE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.TABLE_NAME;

public class ChangeScaRefdbKevVulnerabilitiesDateColumnsToDateType extends DdlChange {

  private static final String SELECT_ONE_FROM_TABLE = "SELECT 1 FROM " + TABLE_NAME;

  public ChangeScaRefdbKevVulnerabilitiesDateColumnsToDateType(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = Objects.requireNonNull(getDatabase().getDataSource()).getConnection()) {
      if (!DatabaseUtils.tableExists(TABLE_NAME, connection)) {
        throw new IllegalStateException("Table does not exist: " + TABLE_NAME);
      }

      ColumnStatus dateAddedStatus = inspectColumn(connection, COLUMN_DATE_ADDED);
      ColumnStatus dueDateStatus = inspectColumn(connection, COLUMN_DUE_DATE);
      if (dateAddedStatus == ColumnStatus.TARGET && dueDateStatus == ColumnStatus.TARGET) {
        return;
      }
      if (tableHasRows(connection)) {
        throw new IllegalStateException("Table " + TABLE_NAME + " must be empty to change calendar date columns to DATE");
      }
      changeColumn(context, COLUMN_DATE_ADDED, dateAddedStatus);
      changeColumn(context, COLUMN_DUE_DATE, dueDateStatus);
    }
  }

  private void changeColumn(Context context, String columnName, ColumnStatus status) {
    if (status == ColumnStatus.TARGET) {
      return;
    }
    if (status == ColumnStatus.LEGACY) {
      context.execute(new DropColumnsBuilder(getDialect(), TABLE_NAME, columnName).build());
    }
    context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
      .addColumn(DateColumnDef.newDateColumnDefBuilder()
        .setColumnName(columnName)
        .setIsNullable(false)
        .build())
      .build());
  }

  private ColumnStatus inspectColumn(Connection connection, String columnName) throws SQLException {
    ColumnMetadata metadata = DatabaseUtils.getColumnMetadata(connection, TABLE_NAME, columnName);
    if (metadata == null) {
      return ColumnStatus.MISSING;
    }
    if (isTargetDate(metadata)) {
      return ColumnStatus.TARGET;
    }
    if (isLegacyBigInteger(metadata)) {
      return ColumnStatus.LEGACY;
    }
    throw new IllegalStateException("Unexpected schema for " + TABLE_NAME + "." + columnName);
  }

  private boolean isTargetDate(ColumnMetadata metadata) {
    if (metadata.nullable()) {
      return false;
    }
    if (metadata.sqlType() == Types.DATE) {
      return true;
    }
    return Oracle.ID.equals(getDialect().getId()) && metadata.sqlType() == Types.TIMESTAMP;
  }

  private boolean isLegacyBigInteger(ColumnMetadata metadata) {
    if (metadata.nullable()) {
      return false;
    }
    if (metadata.sqlType() == Types.BIGINT) {
      return true;
    }
    return Oracle.ID.equals(getDialect().getId()) && (metadata.sqlType() == Types.NUMERIC || metadata.sqlType() == Types.DECIMAL);
  }

  private static boolean tableHasRows(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.setMaxRows(1);
      try (ResultSet resultSet = statement.executeQuery(SELECT_ONE_FROM_TABLE)) {
        return resultSet.next();
      }
    }
  }

  private enum ColumnStatus {
    MISSING,
    LEGACY,
    TARGET
  }
}
