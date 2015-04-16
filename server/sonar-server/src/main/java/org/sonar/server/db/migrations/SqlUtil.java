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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import javax.annotation.CheckForNull;

import org.sonar.api.utils.log.Logger;

public class SqlUtil {

  private SqlUtil() {
    // only static methods
  }

  /**
   * Logback does not log exceptions associated to {@link java.sql.SQLException#getNextException()}.
   * See http://jira.qos.ch/browse/LOGBACK-775
   */
  public static void log(Logger logger, SQLException e) {
    SQLException next = e.getNextException();
    while (next != null) {
      logger.error("SQL error: {}. Message: {}", next.getSQLState(), next.getMessage());
      next = next.getNextException();
    }
  }

  @CheckForNull
  public static Long getLong(ResultSet rs, String columnName) throws SQLException {
    long l = rs.getLong(columnName);
    return rs.wasNull() ? null : l;
  }

  @CheckForNull
  public static Double getDouble(ResultSet rs, String columnName) throws SQLException {
    double d = rs.getDouble(columnName);
    return rs.wasNull() ? null : d;
  }

  @CheckForNull
  public static Integer getInt(ResultSet rs, String columnName) throws SQLException {
    int i = rs.getInt(columnName);
    return rs.wasNull() ? null : i;
  }

  @CheckForNull
  public static String getString(ResultSet rs, String columnName) throws SQLException {
    String s = rs.getString(columnName);
    return rs.wasNull() ? null : s;
  }

  @CheckForNull
  public static Long getLong(ResultSet rs, int columnIndex) throws SQLException {
    long l = rs.getLong(columnIndex);
    return rs.wasNull() ? null : l;
  }

  @CheckForNull
  public static Double getDouble(ResultSet rs, int columnIndex) throws SQLException {
    double d = rs.getDouble(columnIndex);
    return rs.wasNull() ? null : d;
  }

  @CheckForNull
  public static Integer getInt(ResultSet rs, int columnIndex) throws SQLException {
    int i = rs.getInt(columnIndex);
    return rs.wasNull() ? null : i;
  }

  @CheckForNull
  public static String getString(ResultSet rs, int columnIndex) throws SQLException {
    String s = rs.getString(columnIndex);
    return rs.wasNull() ? null : s;
  }

  @CheckForNull
  public static Date getDate(ResultSet rs, int columnIndex) throws SQLException {
    Timestamp t = rs.getTimestamp(columnIndex);
    return rs.wasNull() ? null : new Date(t.getTime());
  }
}
