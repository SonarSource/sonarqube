/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

/**
 * Fail-fast checks of some database requirements
 */
public class DatabaseChecker implements Startable {

  private static final Map<String, Version> MINIMAL_SUPPORTED_DB_VERSIONS = ImmutableMap.of(
    // MsSQL 2014 is 12.x
    // https://support.microsoft.com/en-us/kb/321185
    MsSql.ID, Version.create(12, 0, 0),
    MySql.ID, Version.create(5, 6, 0),
    Oracle.ID, Version.create(11, 0, 0),
    PostgreSql.ID, Version.create(8, 0, 0));

  private final Database db;

  public DatabaseChecker(Database db) {
    this.db = db;
  }

  @Override
  public void start() {
    try {
      checkMinDatabaseVersion();

      // additional checks
      if (H2.ID.equals(db.getDialect().getId())) {
        Loggers.get(DatabaseChecker.class).warn("H2 database should be used for evaluation purpose only");
      } else if (Oracle.ID.equals(db.getDialect().getId())) {
        checkOracleDriverVersion();
      }
    } catch (SQLException e) {
      Throwables.propagate(e);
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }

  private void checkMinDatabaseVersion() throws SQLException {
    Version minDbVersion = MINIMAL_SUPPORTED_DB_VERSIONS.get(db.getDialect().getId());
    if (minDbVersion != null) {
      try (Connection connection = db.getDataSource().getConnection()) {
        int dbMajorVersion = connection.getMetaData().getDatabaseMajorVersion();
        int dbMinorVersion = connection.getMetaData().getDatabaseMinorVersion();
        Version dbVersion = Version.create(dbMajorVersion, dbMinorVersion, 0);
        if (dbVersion.compareTo(minDbVersion) < 0) {
          throw MessageException.of(String.format(
            "Unsupported %s version: %s. Minimal supported version is %s.", db.getDialect().getId(), dbVersion, minDbVersion));
        }
      }
    }
  }

  private void checkOracleDriverVersion() throws SQLException {
    try (Connection connection = db.getDataSource().getConnection()) {
      String driverVersion = connection.getMetaData().getDriverVersion();
      String[] parts = StringUtils.split(driverVersion, ".");
      int intVersion = Integer.parseInt(parts[0]) * 100 + Integer.parseInt(parts[1]);
      if (intVersion < 1102) {
        throw MessageException.of(String.format(
          "Unsupported Oracle driver version: %s. Minimal supported version is 11.2.", driverVersion));
      }
    }
  }
}
