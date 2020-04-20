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

package org.sonar.server.platform.db.migration.version.v83.util;

import java.sql.SQLException;
import java.util.List;
import org.sonar.db.Database;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class DropPrimaryKeySqlGenerator {

  private final Database db;
  private SqlHelper sqlHelper;

  public DropPrimaryKeySqlGenerator(Database db, SqlHelper sqlHelper) {
    this.db = db;
    this.sqlHelper = sqlHelper;
  }

  public List<String> generate(String tableName, String columnName) throws SQLException {
    Dialect dialect = db.getDialect();
    switch (dialect.getId()) {
      case PostgreSql.ID:
        return generateForPostgresSql(tableName, columnName, sqlHelper.getPostgresSqlConstraint(tableName));
      case MsSql.ID:
        return generateForMsSql(tableName, sqlHelper.getMssqlConstraint(tableName));
      case Oracle.ID:
        return generateForOracle(tableName, sqlHelper.getOracleConstraint(tableName));
      case H2.ID:
        return generateForH2(tableName, columnName, sqlHelper.getH2Constraint(tableName));
      default:
        throw new IllegalStateException(format("Unsupported database '%s'", dialect.getId()));
    }
  }

  private List<String> generateForPostgresSql(String tableName, String column, String constraintName) throws SQLException {
    String sequence = sqlHelper.getPostgresSqlSequence(tableName, column);
    return asList(
      format("ALTER TABLE %s ALTER COLUMN %s DROP DEFAULT", tableName, column),
      format("DROP SEQUENCE %s", sequence),
      format("ALTER TABLE %s DROP CONSTRAINT %s", tableName, constraintName));
  }

  private static List<String> generateForOracle(String tableName, String constraintName) {
    return asList(
      format("DROP TRIGGER %s_IDT", tableName),
      format("DROP SEQUENCE %s_SEQ", tableName),
      format("ALTER TABLE %s DROP CONSTRAINT %s", tableName, constraintName));
  }

  private static List<String> generateForMsSql(String tableName, String constraintName) {
    return singletonList(format("ALTER TABLE %s DROP CONSTRAINT %s", tableName, constraintName));
  }

  private static List<String> generateForH2(String tableName, String column, String constraintName) {
    return asList(
      format("ALTER TABLE %s DROP CONSTRAINT %s", tableName, constraintName),
      format("ALTER TABLE %s ALTER COLUMN %s INTEGER NOT NULL", tableName, column));
  }

}
