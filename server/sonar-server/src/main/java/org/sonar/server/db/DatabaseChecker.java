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
package org.sonar.server.db;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.dialect.H2;
import org.sonar.core.persistence.dialect.Oracle;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseChecker implements ServerComponent {

  private final Database db;

  public DatabaseChecker(Database db) {
    this.db = db;
  }

  public void start() throws SQLException {
    if (H2.ID.equals(db.getDialect().getId())) {
      LoggerFactory.getLogger(DatabaseChecker.class).warn("H2 database should be used for evaluation purpose only");
    } else if (Oracle.ID.equals(db.getDialect().getId())) {
      checkOracleDriverVersion();
    }
  }

  private void checkOracleDriverVersion() throws SQLException {
    Connection connection = db.getDataSource().getConnection();
    try {
      String driverVersion = connection.getMetaData().getDriverVersion();
      String[] parts = StringUtils.split(driverVersion, ".");
      int intVersion = Integer.parseInt(parts[0]) * 100 + Integer.parseInt(parts[1]);
      if (intVersion < 1102) {
        throw new IllegalStateException(String.format(
          "Unsupported Oracle JDBC driver version: %s. Minimal required version is 11.2.", driverVersion));
      }
    } finally {
      DbUtils.closeQuietly(connection);
    }
  }

}
