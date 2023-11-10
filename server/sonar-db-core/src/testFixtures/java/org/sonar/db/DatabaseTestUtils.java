/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.sonar.db.version.SqTables;

/**
 * Utils class for test-specific database opertations
 */
public class DatabaseTestUtils {

  private DatabaseTestUtils() {

  }

  public static void truncateAllTables(DataSource dataSource) throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try (Statement statement = connection.createStatement()) {
        for (String table : SqTables.TABLES) {
          try {
            if (shouldTruncate(connection, table)) {
              statement.executeUpdate(truncateSql(table));
              connection.commit();
            }
          } catch (Exception e) {
            connection.rollback();
            throw new IllegalStateException("Fail to truncate table " + table, e);
          }
        }
      }
    }
  }

  private static boolean shouldTruncate(Connection connection, String table) {
    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery("select count(1) from " + table)) {
      if (rs.next()) {
        return rs.getInt(1) > 0;
      }

    } catch (SQLException ignored) {
      // probably because table does not exist. That's the case with H2 tests.
    }
    return false;
  }

  private static String truncateSql(String table) {
    return "TRUNCATE TABLE " + table;
  }

}
