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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import javax.annotation.Nullable;
import org.apache.commons.dbutils.DbUtils;

class BaseSqlStatement<CHILD extends SqlStatement> implements SqlStatement<CHILD> {
  protected PreparedStatement pstmt;

  protected BaseSqlStatement(PreparedStatement pstmt) {
    this.pstmt = pstmt;
  }

  @Override
  public void close() {
    DbUtils.closeQuietly(pstmt);
    pstmt = null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public CHILD setString(int columnIndex, @Nullable String value) throws SQLException {
    pstmt.setString(columnIndex, value);
    return (CHILD) this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public CHILD setInt(int columnIndex, @Nullable Integer value) throws SQLException {
    if (value == null) {
      pstmt.setNull(columnIndex, Types.INTEGER);
    } else {
      pstmt.setInt(columnIndex, value);
    }
    return (CHILD) this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public CHILD setLong(int columnIndex, @Nullable Long value) throws SQLException {
    if (value == null) {
      pstmt.setNull(columnIndex, Types.BIGINT);
    } else {
      pstmt.setLong(columnIndex, value);
    }
    return (CHILD) this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public CHILD setBoolean(int columnIndex, @Nullable Boolean value) throws SQLException {
    if (value == null) {
      pstmt.setNull(columnIndex, Types.BOOLEAN);
    } else {
      pstmt.setBoolean(columnIndex, value);
    }
    return (CHILD) this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public CHILD setBytes(int columnIndex, @Nullable byte[] value) throws SQLException {
    if (value == null) {
      pstmt.setNull(columnIndex, Types.BINARY);
    } else {
      pstmt.setBytes(columnIndex, value);
    }
    return (CHILD) this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public CHILD setDouble(int columnIndex, @Nullable Double value) throws SQLException {
    if (value == null) {
      pstmt.setNull(columnIndex, Types.DECIMAL);
    } else {
      pstmt.setDouble(columnIndex, value);
    }
    return (CHILD) this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public CHILD setDate(int columnIndex, @Nullable Date value) throws SQLException {
    if (value == null) {
      pstmt.setNull(columnIndex, Types.TIMESTAMP);
    } else {
      pstmt.setTimestamp(columnIndex, new Timestamp(value.getTime()));
    }
    return (CHILD) this;
  }
}
