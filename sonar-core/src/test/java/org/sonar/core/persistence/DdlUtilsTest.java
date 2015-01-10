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
package org.sonar.core.persistence;

import org.h2.Driver;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class DdlUtilsTest {

  @Test
  public void shouldSupportOnlyH2() {
    assertThat(DdlUtils.supportsDialect("h2")).isTrue();
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
