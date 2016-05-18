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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;

abstract class CharsetHandler {

  protected static final String UTF8 = "utf8";

  private final SqlExecutor selectExecutor;

  protected CharsetHandler(SqlExecutor selectExecutor) {
    this.selectExecutor = selectExecutor;
  }

  abstract void handle(Connection connection, Set<DatabaseCharsetChecker.Flag> flags) throws SQLException;

  protected SqlExecutor getSqlExecutor() {
    return selectExecutor;
  }

  @CheckForNull
  protected final String selectSingleString(Connection connection, String sql) throws SQLException {
    String[] cols = selectSingleRow(connection, sql, new SqlExecutor.StringsConverter(1));
    return cols == null ? null : cols[0];
  }

  @CheckForNull
  protected final <T> T selectSingleRow(Connection connection, String sql, SqlExecutor.RowConverter<T> rowConverter) throws SQLException {
    List<T> rows = select(connection, sql, rowConverter);
    if (rows.isEmpty()) {
      return null;
    }
    if (rows.size() == 1) {
      return rows.get(0);
    }
    throw new IllegalStateException("Expecting only one result for [" + sql + "]");
  }

  protected final <T> List<T> select(Connection connection, String sql, SqlExecutor.RowConverter<T> rowConverter) throws SQLException {
    return selectExecutor.executeSelect(connection, sql, rowConverter);
  }
}
