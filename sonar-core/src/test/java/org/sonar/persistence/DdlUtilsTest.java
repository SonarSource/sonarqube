/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.persistence;

import org.apache.derby.jdbc.EmbeddedDriver;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

public class DdlUtilsTest {

  @Test
  public void shouldSupportOnlyDerby() {
    assertThat(DdlUtils.supportsDialect("derby"), Is.is(true));
    assertThat(DdlUtils.supportsDialect("mysql"), Is.is(false));
    assertThat(DdlUtils.supportsDialect("oracle"), Is.is(false));
    assertThat(DdlUtils.supportsDialect("mssql"), Is.is(false));
  }

  @Test
  public void shouldExecuteDerbyDdl() throws SQLException {
    int tables = 0;
    DriverManager.registerDriver(new EmbeddedDriver());
    Connection connection = DriverManager.getConnection("jdbc:derby:memory:sonar;create=true");
    DdlUtils.execute(connection, "derby");

    ResultSet resultSet = connection.getMetaData().getTables("", null, null, new String[]{"TABLE"});
    while (resultSet.next()) {
      tables++;
    }
    resultSet.close();
    connection.close();
    assertThat(tables, greaterThan(30));

    try {
      DriverManager.getConnection("jdbc:derby:memory:sonar;drop=true");
    } catch (Exception e) {
    }
  }
}
