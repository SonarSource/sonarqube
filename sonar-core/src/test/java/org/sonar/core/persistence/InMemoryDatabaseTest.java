/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.core.persistence;

import org.apache.commons.dbcp.BasicDataSource;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class InMemoryDatabaseTest {

  static {
    DerbyUtils.fixDerbyLogs();
  }

  @Test
  public void shouldExecuteDdlAtStartup() throws SQLException {
    int tables = 0;
    InMemoryDatabase db = new InMemoryDatabase();
    try {
      db.start();
      assertNotNull(db.getDataSource());
      Connection connection = db.getDataSource().getConnection();
      assertNotNull(connection);

      ResultSet resultSet = connection.getMetaData().getTables("", null, null, new String[]{"TABLE"});
      while (resultSet.next()) {
        tables++;
      }


    } finally {
      db.stop();
    }
    assertThat(tables, greaterThan(30));
  }

  @Test
  public void shouldLimitThePoolSize() throws SQLException {
    InMemoryDatabase db = new InMemoryDatabase();
    try {
      db.startDatabase();
      assertThat(((BasicDataSource)db.getDataSource()).getMaxActive(), Is.is(2));
      assertThat(((BasicDataSource)db.getDataSource()).getMaxIdle(), Is.is(2));

    } finally {
      db.stop();
    }
  }
}
