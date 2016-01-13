/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.assertj.core.api.Assertions;
import org.h2.Driver;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DdlUtilsTest {

  @Test
  public void shouldSupportOnlyH2() {
    Assertions.assertThat(DdlUtils.supportsDialect("h2")).isTrue();
    assertThat(DdlUtils.supportsDialect("mysql")).isFalse();
    assertThat(DdlUtils.supportsDialect("oracle")).isFalse();
    assertThat(DdlUtils.supportsDialect("mssql")).isFalse();
  }

  @Test
  public void shouldCreateSchema() throws SQLException {
    DriverManager.registerDriver(new Driver());
    Connection connection = DriverManager.getConnection("jdbc:h2:mem:sonar_test");
    DdlUtils.createSchema(connection, "h2");

    int tableCount = countTables(connection);

    connection.close();
    assertThat(tableCount).isGreaterThan(30);
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
}
