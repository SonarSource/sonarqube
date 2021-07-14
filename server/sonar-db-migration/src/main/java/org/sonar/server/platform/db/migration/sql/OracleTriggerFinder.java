/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.server.platform.db.migration.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.sonar.db.Database;

import static java.lang.String.format;

public class OracleTriggerFinder {

  private final Database db;

  public OracleTriggerFinder(Database db) {
    this.db = db;
  }

  public Optional<String> findTriggerName(String tableName) throws SQLException {
    String constraintQuery = getTriggerNameQueryBy(tableName);
    return executeQuery(constraintQuery);
  }

  private Optional<String> executeQuery(String query) throws SQLException {
    try (Connection connection = db.getDataSource().getConnection();
      PreparedStatement pstmt = connection
        .prepareStatement(query);
      ResultSet rs = pstmt.executeQuery()) {
      if (rs.next()) {
        return Optional.ofNullable(rs.getString(1));
      }
      return Optional.empty();
    }
  }

  private static String getTriggerNameQueryBy(String tableName) {
    return format("SELECT trigger_name FROM user_triggers WHERE upper(table_name) = upper('%s')", tableName);
  }
}
