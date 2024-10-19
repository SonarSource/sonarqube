/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectexport.util;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class ResultSetUtils {
  private ResultSetUtils() {
    // prevents instantiation
  }

  public static int defaultIfNull(ResultSet rs, int columnIndex, int defaultValue) throws SQLException {
    int value = rs.getInt(columnIndex);
    if (rs.wasNull()) {
      return defaultValue;
    }
    return value;
  }

  public static double defaultIfNull(ResultSet rs, int columnIndex, double defaultValue) throws SQLException {
    double value = rs.getDouble(columnIndex);
    if (rs.wasNull()) {
      return defaultValue;
    }
    return value;
  }

  public static long defaultIfNull(ResultSet rs, int columnIndex, long defaultValue) throws SQLException {
    long value = rs.getLong(columnIndex);
    if (rs.wasNull()) {
      return defaultValue;
    }
    return value;
  }

  public static String emptyIfNull(ResultSet rs, int columnIndex) throws SQLException {
    String value = rs.getString(columnIndex);
    if (value == null) {
      return "";
    }
    return value;
  }
}
