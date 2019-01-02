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
package org.sonar.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.assertj.core.api.Assertions;
import org.h2.Driver;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class DdlUtilsTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void shouldSupportOnlyH2() {
    Assertions.assertThat(DdlUtils.supportsDialect("h2")).isTrue();
    assertThat(DdlUtils.supportsDialect("mysql")).isFalse();
    assertThat(DdlUtils.supportsDialect("oracle")).isFalse();
    assertThat(DdlUtils.supportsDialect("mssql")).isFalse();
  }

  @Test
  public void shouldCreateSchema_with_schema_migrations() throws SQLException {
    DriverManager.registerDriver(new Driver());
    try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:sonar_test")) {
      DdlUtils.createSchema(connection, "h2", true);

      int tableCount = countTables(connection);
      assertThat(tableCount).isGreaterThan(30);

      verifySchemaMigrationsNotPopulated(connection);
    }
  }

  @Test
  public void shouldCreateSchema_without_schema_migrations() throws SQLException {
    DriverManager.registerDriver(new Driver());
    try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:sonar_test2")) {
      try (Statement statement = connection.createStatement()) {
        statement.execute("create table schema_migrations (version varchar(255) not null)");
      }
      DdlUtils.createSchema(connection, "h2", false);

      verifySchemaMigrationsNotPopulated(connection);
    }
  }

  static int countTables(Connection connection) throws SQLException {
    int count = 0;
    ResultSet resultSet = connection.getMetaData().getTables("", null, null, new String[] {"TABLE"});
    while (resultSet.next()) {
      count++;
    }
    resultSet.close();
    return count;
  }

  private void verifySchemaMigrationsNotPopulated(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement();
      ResultSet resultSet = statement.executeQuery("select count(*) from schema_migrations")) {
      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getLong(1)).isEqualTo(0);
      assertThat(resultSet.next()).isFalse();
    }
  }
}
