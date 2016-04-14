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

import java.sql.ResultSet;
import java.sql.SQLException;
import javax.annotation.concurrent.Immutable;

/**
 * Result of standard SQL command "select * from INFORMATION_SCHEMA"
 */
@Immutable
public class ColumnDef {
  private final String table;
  private final String column;
  private final String charset;
  private final String collation;
  private final String dataType;
  private final int size;
  private final boolean nullable;

  public ColumnDef(String table, String column, String charset, String collation, String dataType, int size, boolean nullable) {
    this.table = table;
    this.column = column;
    this.charset = charset;
    this.collation = collation;
    this.dataType = dataType;
    this.size = size;
    this.nullable = nullable;
  }

  public String getTable() {
    return table;
  }

  public String getColumn() {
    return column;
  }

  public String getCharset() {
    return charset;
  }

  public String getCollation() {
    return collation;
  }

  public String getDataType() {
    return dataType;
  }

  public int getSize() {
    return size;
  }

  public boolean isNullable() {
    return nullable;
  }

  public static final String SELECT_COLUMNS = "select table_name, column_name, character_set_name, collation_name, data_type, character_maximum_length, is_nullable ";

  public enum ColumnDefRowConverter implements SqlExecutor.RowConverter<ColumnDef> {
    INSTANCE;
    @Override
    public ColumnDef convert(ResultSet rs) throws SQLException {
      return new ColumnDef(
        rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getInt(6), rs.getBoolean(7));
    }
  }
}
