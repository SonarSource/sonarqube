/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.db.migrations;

import javax.annotation.CheckForNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

public interface Select extends SqlStatement<Select> {

  static class Row {
    private final ResultSet rs;

    Row(ResultSet rs) {
      this.rs = rs;
    }

    @CheckForNull
    public Long getLong(int columnIndex) throws SQLException {
      long l = rs.getLong(columnIndex);
      return rs.wasNull() ? null : l;
    }

    @CheckForNull
    public Double getDouble(int columnIndex) throws SQLException {
      double d = rs.getDouble(columnIndex);
      return rs.wasNull() ? null : d;
    }

    @CheckForNull
    public Integer getInt(int columnIndex) throws SQLException {
      int i = rs.getInt(columnIndex);
      return rs.wasNull() ? null : i;
    }

    @CheckForNull
    public Boolean getBoolean(int columnIndex) throws SQLException {
      boolean b = rs.getBoolean(columnIndex);
      return rs.wasNull() ? null : b;
    }

    @CheckForNull
    public String getString(int columnIndex) throws SQLException {
      String s = rs.getString(columnIndex);
      return rs.wasNull() ? null : s;
    }

    @CheckForNull
    public Date getDate(int columnIndex) throws SQLException {
      Timestamp t = rs.getTimestamp(columnIndex);
      return rs.wasNull() ? null : t;
    }
  }

  static interface RowReader<T> {
    T read(Row row) throws SQLException;
  }

  static class LongReader implements RowReader<Long> {
    private LongReader() {
    }

    @Override
    public Long read(Row row) throws SQLException {
      return row.getLong(1);
    }
  }

  static final RowReader<Long> LONG_READER = new LongReader();

  static interface RowHandler<T> {
    void handle(Row row) throws SQLException;
  }

  <T> List<T> list(RowReader<T> reader) throws SQLException;

  @CheckForNull
  <T> T get(RowReader<T> reader) throws SQLException;

  void scroll(RowHandler handler) throws SQLException;
}
