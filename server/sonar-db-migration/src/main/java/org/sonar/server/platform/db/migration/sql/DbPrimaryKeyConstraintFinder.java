/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import java.util.Locale;
import java.util.function.Supplier;
import org.sonar.db.Database;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static java.lang.String.format;

public class DbPrimaryKeyConstraintFinder {

  private final Database db;

  public DbPrimaryKeyConstraintFinder(Database db) {
    this.db = db;
  }

  public String findConstraintName(String tableName) throws SQLException {
    String constraintQuery = getDbVendorSpecificQuery(tableName);
    return executeQuery(constraintQuery, () -> constraintNotFoundException(tableName));
  }

  String getDbVendorSpecificQuery(String tableName) {
    Dialect dialect = db.getDialect();
    String constraintQuery;
    switch (dialect.getId()) {
      case PostgreSql.ID:
        constraintQuery = getPostgresSqlConstraintQuery(tableName);
        break;
      case MsSql.ID:
        constraintQuery = getMssqlConstraintQuery(tableName);
        break;
      case Oracle.ID:
        constraintQuery = getOracleConstraintQuery(tableName);
        break;
      case H2.ID:
        constraintQuery = getH2ConstraintQuery(tableName);
        break;
      default:
        throw new IllegalStateException(format("Unsupported database '%s'", dialect.getId()));
    }
    return constraintQuery;
  }

  private String executeQuery(String query, Supplier<? extends RuntimeException> exceptionSupplier) throws SQLException {
    try (Connection connection = db.getDataSource().getConnection();
      PreparedStatement pstmt = connection
        .prepareStatement(query);
      ResultSet rs = pstmt.executeQuery()) {
      if (rs.next()) {
        return rs.getString(1);
      }
      throw exceptionSupplier.get();
    }
  }

  private static String getPostgresSqlConstraintQuery(String tableName) {
    return format("SELECT conname " +
      "FROM pg_constraint " +
      "WHERE conrelid = " +
      "    (SELECT oid " +
      "    FROM pg_class " +
      "    WHERE relname LIKE '%s')", tableName);
  }

  private static String getMssqlConstraintQuery(String tableName) {
    return format("SELECT name " +
      "FROM sys.key_constraints " +
      "WHERE type = 'PK' " +
      "AND OBJECT_NAME(parent_object_id) = '%s'", tableName);
  }

  private static String getOracleConstraintQuery(String tableName) {
    return format("SELECT constraint_name " +
      "FROM user_constraints " +
      "WHERE table_name = UPPER('%s') " +
      "AND constraint_type='P'", tableName);
  }

  private static String getH2ConstraintQuery(String tableName) {
    return format("SELECT constraint_name "
      + "FROM information_schema.constraints "
      + "WHERE table_name = '%s' and constraint_type = 'PRIMARY KEY'", tableName.toUpperCase(Locale.ENGLISH));
  }

  static IllegalStateException constraintNotFoundException(String tableName) {
    return new IllegalStateException(format("Cannot find constraint for table '%s'", tableName));
  }

  // FIXME:: this method should be moved somewhere else
  String getPostgresSqlSequence(String tableName, String columnName) throws SQLException {
    return executeQuery(format("SELECT pg_get_serial_sequence('%s', '%s')", tableName, columnName),
      () -> new IllegalStateException(format("Cannot find sequence for table '%s' on column '%s'", tableName, columnName)));
  }

}
