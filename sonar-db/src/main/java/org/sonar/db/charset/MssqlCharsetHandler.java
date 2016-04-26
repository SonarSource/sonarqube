/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.charset;

import com.google.common.annotations.VisibleForTesting;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.endsWithIgnoreCase;

class MssqlCharsetHandler extends CharsetHandler {

  private static final Logger LOGGER = Loggers.get(MssqlCharsetHandler.class);

  protected MssqlCharsetHandler(SqlExecutor selectExecutor) {
    super(selectExecutor);
  }

  @Override
  void handle(Connection connection, boolean enforceUtf8) throws SQLException {
    LOGGER.info("Verify that database collation is case-sensitive and accent-sensitive");
    checkCollation(connection);
  }

  private void checkCollation(Connection connection) throws SQLException {
    // All VARCHAR columns are returned. No need to check database general collation.
    // Example of row:
    // issues | kee | Latin1_General_CS_AS
    List<ColumnDef> columns = select(connection,
      ColumnDef.SELECT_COLUMNS +
        "FROM [INFORMATION_SCHEMA].[COLUMNS] " +
        "WHERE collation_name is not null " +
        "ORDER BY table_name,column_name", ColumnDef.ColumnDefRowConverter.INSTANCE);
    for (ColumnDef column : columns) {
      if (!endsWithIgnoreCase(column.getCollation(), "_CS_AS")) {
        repairColumnCollation(connection, column);
      }
    }
  }

  private void repairColumnCollation(Connection connection, ColumnDef column) throws SQLException {
    // 1. select the indices defined on this column
    String selectIndicesSql = format("SELECT I.name as index_name, I.is_unique as unik, IndexedColumns " +
      "     FROM sys.indexes I " +
      "     JOIN sys.tables T ON T.Object_id = I.Object_id " +
      "     JOIN (SELECT * FROM ( " +
      "     SELECT IC2.object_id, IC2.index_id, " +
      "     STUFF((SELECT ' ,' + C.name " +
      "     FROM sys.index_columns IC1 " +
      "     JOIN sys.columns C " +
      "     ON C.object_id = IC1.object_id " +
      "     AND C.column_id = IC1.column_id " +
      "     AND IC1.is_included_column = 0 " +
      "     WHERE IC1.object_id = IC2.object_id " +
      "     AND IC1.index_id = IC2.index_id " +
      "     GROUP BY IC1.object_id,C.name,index_id " +
      "     ORDER BY MAX(IC1.key_ordinal) " +
      "     FOR XML PATH('')), 1, 2, '') IndexedColumns " +
      "     FROM sys.index_columns IC2 " +
      "     GROUP BY IC2.object_id ,IC2.index_id) tmp1 )tmp2 " +
      "     ON I.object_id = tmp2.object_id AND I.Index_id = tmp2.index_id " +
      "     WHERE I.is_primary_key = 0 AND I.is_unique_constraint = 0 " +
      "     and T.name =('%s') " +
      "     and CHARINDEX ('%s',IndexedColumns)>0", column.getTable(), column.getColumn());
    List<ColumnIndex> indices = getSqlExecutor().executeSelect(connection, selectIndicesSql, ColumnIndexConverter.INSTANCE);

    // 2. drop indices
    for (ColumnIndex index : indices) {
      getSqlExecutor().executeUpdate(connection, format("DROP INDEX %s.%s", column.getTable(), index.name));
    }

    // 3. alter collation of column
    String csCollation = toCaseSensitive(column.getCollation());

    String nullability = column.isNullable() ? "NULL" : "NOT NULL";
    String size = column.getSize() >= 0 ? String.valueOf(column.getSize()) : "max";
    String alterSql = format("ALTER TABLE %s ALTER COLUMN %s %s(%s) COLLATE %s %s",
      column.getTable(), column.getColumn(), column.getDataType(), size, csCollation, nullability);
    LOGGER.info("Changing collation of column [{}.{}] from {} to {} | sql=", column.getTable(), column.getColumn(), column.getCollation(), csCollation, alterSql);
    getSqlExecutor().executeUpdate(connection, alterSql);

    // 4. re-create indices
    for (ColumnIndex index : indices) {
      String uniqueSql = index.unique ? "UNIQUE" : "";
      String createIndexSql = format("CREATE %s INDEX %s ON %s (%s)", uniqueSql, index.name, column.getTable(), index.csvColumns);
      getSqlExecutor().executeUpdate(connection, createIndexSql);
    }
  }

  @VisibleForTesting
  static String toCaseSensitive(String ciCollation) {
    // Example: Latin1_General_CI_AI --> Latin1_General_CS_AS
    return ciCollation.substring(0, ciCollation.length() - "_CI_AI".length()) + "_CS_AS";
  }

  @VisibleForTesting
  static class ColumnIndex {
    private final String name;
    private final boolean unique;
    private final String csvColumns;

    public ColumnIndex(String name, boolean unique, String csvColumns) {
      this.name = name;
      this.unique = unique;
      this.csvColumns = csvColumns;
    }
  }

  @VisibleForTesting
  enum ColumnIndexConverter implements SqlExecutor.RowConverter<ColumnIndex> {
    INSTANCE;
    @Override
    public ColumnIndex convert(ResultSet rs) throws SQLException {
      return new ColumnIndex(rs.getString(1), rs.getBoolean(2), rs.getString(3));
    }
  }
}
