/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.charset;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static java.lang.String.format;

public class MssqlMetadataReader {
  private final SqlExecutor sqlExecutor;

  public MssqlMetadataReader(SqlExecutor sqlExecutor) {
    this.sqlExecutor = sqlExecutor;
  }

  public String getDefaultCollation(Connection connection) throws SQLException {
    return sqlExecutor.selectSingleString(connection, "SELECT CONVERT(VARCHAR, DATABASEPROPERTYEX(DB_NAME(), 'Collation'))");
  }

  public List<ColumnDef> getColumnDefs(Connection connection) throws SQLException {
    return sqlExecutor.select(connection,
      ColumnDef.SELECT_COLUMNS +
        "FROM [INFORMATION_SCHEMA].[COLUMNS] " +
        "WHERE collation_name is not null " +
        "ORDER BY table_name,column_name",
      ColumnDef.ColumnDefRowConverter.INSTANCE);
  }

  public List<MssqlCharsetHandler.ColumnIndex> getColumnIndices(Connection connection, ColumnDef column) throws SQLException {
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
    return sqlExecutor.select(connection, selectIndicesSql, MssqlCharsetHandler.ColumnIndexConverter.INSTANCE);
  }
}
