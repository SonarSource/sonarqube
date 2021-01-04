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
package org.sonar.server.platform.db.migration.version.v84.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import org.sonar.db.Database;

import static java.lang.String.format;

public class SqlHelper {

  private final Database db;

  public SqlHelper(Database db) {
    this.db = db;
  }

  String getH2Constraint(String tableName) throws SQLException {
    try (Connection connection = db.getDataSource().getConnection();
      PreparedStatement pstmt = connection
        .prepareStatement(format("SELECT constraint_name "
          + "FROM information_schema.constraints "
          + "WHERE table_name = '%s' and constraint_type = 'PRIMARY KEY'", tableName.toUpperCase(Locale.ENGLISH)));
      ResultSet rs = pstmt.executeQuery()) {
      if (rs.next()) {
        return rs.getString(1);
      }
      throw contraintNotFoundException(tableName);
    }
  }

  String getPostgresSqlConstraint(String tableName) throws SQLException {
    try (Connection connection = db.getDataSource().getConnection();
      PreparedStatement pstmt = connection
        .prepareStatement(format("SELECT conname " +
          "FROM pg_constraint " +
          "WHERE conrelid = " +
          "    (SELECT oid " +
          "    FROM pg_class " +
          "    WHERE relname LIKE '%s')", tableName));
      ResultSet rs = pstmt.executeQuery()) {
      if (rs.next()) {
        return rs.getString(1);
      }
      throw contraintNotFoundException(tableName);
    }
  }

  String getPostgresSqlSequence(String tableName, String columnName) throws SQLException {
    try (Connection connection = db.getDataSource().getConnection();
         PreparedStatement pstmt = connection
           .prepareStatement(format("SELECT pg_get_serial_sequence('%s', '%s')", tableName, columnName));
         ResultSet rs = pstmt.executeQuery()) {
      if (rs.next()) {
        return rs.getString(1);
      }
      throw new IllegalStateException(format("Cannot find sequence for table '%s' on column '%s'", tableName, columnName));
    }
  }

  String getOracleConstraint(String tableName) throws SQLException {
    try (Connection connection = db.getDataSource().getConnection();
      PreparedStatement pstmt = connection
        .prepareStatement(format("SELECT constraint_name " +
          "FROM user_constraints " +
          "WHERE table_name = UPPER('%s') " +
          "AND constraint_type='P'", tableName));
      ResultSet rs = pstmt.executeQuery()) {
      if (rs.next()) {
        return rs.getString(1);
      }
      throw contraintNotFoundException(tableName);
    }
  }

  String getMssqlConstraint(String tableName) throws SQLException {
    try (Connection connection = db.getDataSource().getConnection();
      PreparedStatement pstmt = connection
        .prepareStatement(format("SELECT name " +
          "FROM sys.key_constraints " +
          "WHERE type = 'PK' " +
          "AND OBJECT_NAME(parent_object_id) = '%s'", tableName));
      ResultSet rs = pstmt.executeQuery()) {
      if (rs.next()) {
        return rs.getString(1);
      }
      throw contraintNotFoundException(tableName);
    }
  }

  private static IllegalStateException contraintNotFoundException(String tableName) {
    return new IllegalStateException(format("Cannot find constraint for table '%s'", tableName));
  }

}
