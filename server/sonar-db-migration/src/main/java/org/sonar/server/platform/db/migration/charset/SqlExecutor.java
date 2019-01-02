/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.db.DatabaseUtils;

public class SqlExecutor {

  public <T> List<T> select(Connection connection, String sql, RowConverter<T> rowConverter) throws SQLException {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.prepareStatement(sql);
      rs = stmt.executeQuery();
      List<T> result = new ArrayList<>();
      while (rs.next()) {
        result.add(rowConverter.convert(rs));
      }
      return result;

    } finally {
      DatabaseUtils.closeQuietly(rs);
      DatabaseUtils.closeQuietly(stmt);
    }
  }

  public void executeDdl(Connection connection, String sql) throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      stmt.execute(sql);
    }
  }

  @CheckForNull
  public final String selectSingleString(Connection connection, String sql) throws SQLException {
    String[] cols = selectSingleRow(connection, sql, new SqlExecutor.StringsConverter(1));
    return cols == null ? null : cols[0];
  }

  @CheckForNull
  public final <T> T selectSingleRow(Connection connection, String sql, SqlExecutor.RowConverter<T> rowConverter) throws SQLException {
    List<T> rows = select(connection, sql, rowConverter);
    if (rows.isEmpty()) {
      return null;
    }
    if (rows.size() == 1) {
      return rows.get(0);
    }
    throw new IllegalStateException("Expecting only one result for [" + sql + "]");
  }

  @FunctionalInterface
  public interface RowConverter<T> {
    T convert(ResultSet rs) throws SQLException;
  }

  public static class StringsConverter implements RowConverter<String[]> {
    private final int nbColumns;

    public StringsConverter(int nbColumns) {
      this.nbColumns = nbColumns;
    }

    @Override
    public String[] convert(ResultSet rs) throws SQLException {
      String[] row = new String[nbColumns];
      for (int i = 0; i < nbColumns; i++) {
        row[i] = DatabaseUtils.getString(rs, i + 1);
      }
      return row;
    }
  }
}
