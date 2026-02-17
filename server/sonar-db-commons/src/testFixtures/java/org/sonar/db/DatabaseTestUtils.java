/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils class for test-specific database opertations
 */
public class DatabaseTestUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseTestUtils.class);

  /**
   * The schema migrations table used by SonarQube migration framework.
   */
  private static final String SQS_MIGRATIONS_TABLE = "schema_migrations";

  private DatabaseTestUtils() {

  }

  /**
   * Loads all table names from the database using JDBC metadata.
   * Filters out migration-related tables (flyway_* and schema_migrations).
   *
   * <p><strong>Note:</strong> This method works on PostgreSQL and H2, but is not believed
   * to work on all database dialects at this time.
   *
   * @param dataSource the data source to query for table names
   * @return a set of table names in lowercase
   * @throws IllegalStateException if unable to load table names from the database
   */
  public static Set<String> loadTableNames(DataSource dataSource) {
    try (Connection connection = dataSource.getConnection()) {
      Set<String> result = new HashSet<>();
      ResultSet tables = connection.getMetaData().getTables(null, null, "%", new String[] {"TABLE"});
      while (tables.next()) {
        String tableName = tables.getString("TABLE_NAME");
        String lowerCaseTableName = tableName.toLowerCase(Locale.ROOT);

        // Filter out Flyway migration metadata table
        if (lowerCaseTableName.startsWith("flyway_")) {
          continue;
        }

        // Filter out SonarQube migration metadata table
        if (SQS_MIGRATIONS_TABLE.equals(lowerCaseTableName)) {
          continue;
        }

        result.add(lowerCaseTableName);
      }
      return result;
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to load table names", e);
    }
  }

  public static void truncateTables(DataSource dataSource, Collection<String> tableNames) throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try (Statement statement = connection.createStatement()) {
        for (String table : tableNames) {
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
      // Connection needs to be rollback to leave it in a clean state
      try {
        connection.rollback();
      } catch (SQLException e) {
        LOGGER.warn("Fail to rollback transaction when truncating table %s".formatted(table), e);
      }
    }
    return false;
  }

  private static String truncateSql(String table) {
    return "TRUNCATE TABLE " + table;
  }

}
