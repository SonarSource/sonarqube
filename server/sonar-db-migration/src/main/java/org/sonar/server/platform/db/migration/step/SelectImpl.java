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
package org.sonar.server.platform.db.migration.step;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.dbutils.DbUtils;
import org.sonar.db.Database;

public class SelectImpl extends BaseSqlStatement<Select> implements Select {

  private SelectImpl(PreparedStatement pstmt) {
    super(pstmt);
  }

  @Override
  public <T> List<T> list(Select.RowReader<T> reader) throws SQLException {
    ResultSet rs = pstmt.executeQuery();
    Select.Row row = new Select.Row(rs);
    try {
      List<T> rows = new ArrayList<>();
      while (rs.next()) {
        rows.add(reader.read(row));
      }
      return rows;
    } catch (Exception e) {
      throw newExceptionWithRowDetails(row, e);
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
    } catch (Exception e) {
      throw newExceptionWithRowDetails(row, e);
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
    } catch (Exception e) {
      throw newExceptionWithRowDetails(row, e);
    } finally {
      DbUtils.closeQuietly(rs);
      close();
    }
  }

  private static IllegalStateException newExceptionWithRowDetails(Select.Row row, Exception e) {
    return new IllegalStateException("Error during processing of row: [" + row + "]", e);
  }

  public static SelectImpl create(Database db, Connection connection, String sql) throws SQLException {
    // TODO use DbClient#newScrollingSelectStatement()
    PreparedStatement pstmt = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    pstmt.setFetchSize(db.getDialect().getScrollDefaultFetchSize());
    return new SelectImpl(pstmt);
  }
}
