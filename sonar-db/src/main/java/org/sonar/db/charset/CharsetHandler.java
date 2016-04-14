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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.db.DatabaseUtils;

abstract class CharsetHandler {

  protected static final String UTF8 = "utf8";

  private final SelectExecutor selectExecutor;

  protected CharsetHandler(SelectExecutor selectExecutor) {
    this.selectExecutor = selectExecutor;
  }

  abstract void handle(Connection connection, boolean enforceUtf8) throws SQLException;

  @CheckForNull
  protected final String selectSingleCell(Connection connection, String sql) throws SQLException {
    String[] cols = selectSingleRow(connection, sql, 1);
    return cols == null ? null : cols[0];
  }

  @CheckForNull
  protected final String[] selectSingleRow(Connection connection, String sql, int columns) throws SQLException {
    List<String[]> rows = select(connection, sql, columns);
    if (rows.isEmpty()) {
      return null;
    }
    if (rows.size() == 1) {
      return rows.get(0);
    }
    throw new IllegalStateException("Expecting only one result for [" + sql + "]");
  }

  protected final List<String[]> select(Connection connection, String sql, int columns) throws SQLException {
    return selectExecutor.executeQuery(connection, sql, columns);
  }

  @VisibleForTesting
  static class SelectExecutor {
    List<String[]> executeQuery(Connection connection, String sql, int columns) throws SQLException {
      Statement stmt = null;
      ResultSet rs = null;
      try {
        stmt = connection.createStatement();
        rs = stmt.executeQuery(sql);
        List<String[]> result = new ArrayList<>();
        while (rs.next()) {
          String[] row = new String[columns];
          for (int i = 0; i < columns; i++) {
            row[i] = DatabaseUtils.getString(rs, i + 1);
          }
          result.add(row);
        }
        return result;

      } finally {
        DatabaseUtils.closeQuietly(rs);
        DatabaseUtils.closeQuietly(stmt);
      }
    }
  }
}
