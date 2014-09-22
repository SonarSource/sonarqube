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

import org.apache.commons.dbutils.DbUtils;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.dialect.MySql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class SelectImpl extends BaseSqlStatement<Select> implements Select {

  private SelectImpl(PreparedStatement pstmt) {
    super(pstmt);
  }

  @Override
  public <T> List<T> list(Select.RowReader<T> reader) throws SQLException {
    ResultSet rs = pstmt.executeQuery();
    Select.Row row = new Select.Row(rs);
    try {
      List<T> rows = new ArrayList<T>();
      while (rs.next()) {
        rows.add(reader.read(row));
      }
      return rows;
    } finally {
      DbUtils.closeQuietly(rs);
      close();
    }
  }

  @Override
  public <T> T get(Select.RowReader<T> reader) throws SQLException {
    ResultSet rs = pstmt.executeQuery();
    Select.Row row = new Select.Row(rs);
    try {
      if (rs.next()) {
        return reader.read(row);
      }
      return null;
    } finally {
      DbUtils.closeQuietly(rs);
      close();
    }
  }

  @Override
  public void scroll(Select.RowHandler handler) throws SQLException {
    ResultSet rs = pstmt.executeQuery();
    Select.Row row = new Select.Row(rs);
    try {
      while (rs.next()) {
        handler.handle(row);
      }
    } finally {
      DbUtils.closeQuietly(rs);
      close();
    }
  }

  static SelectImpl create(Database db, Connection connection, String sql) throws SQLException {
    PreparedStatement pstmt = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    if (db.getDialect().getId().equals(MySql.ID)) {
      pstmt.setFetchSize(Integer.MIN_VALUE);
    } else {
      pstmt.setFetchSize(1000);
    }
    return new SelectImpl(pstmt);
  }
}
