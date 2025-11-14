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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SQDatabaseForH2IT {
  SQDatabase db = new SQDatabase.Builder().asH2Database("sonar2").createSchema(true).build();

  @BeforeEach
  void startDb() {
    db.start();
  }

  @AfterEach
  void stopDb() {
    db.stop();
  }

  @Test
  void shouldExecuteDdlAtStartup() throws SQLException {
    Connection connection = db.getDataSource().getConnection();
    int tableCount = countTables(connection);
    connection.close();

    assertThat(tableCount).isGreaterThan(30);
  }

  private static int countTables(Connection connection) throws SQLException {
    int count = 0;
    ResultSet resultSet = connection.getMetaData().getTables("", null, null, new String[]{"TABLE"});
    while (resultSet.next()) {
      count++;
    }
    resultSet.close();
    return count;
  }
}
