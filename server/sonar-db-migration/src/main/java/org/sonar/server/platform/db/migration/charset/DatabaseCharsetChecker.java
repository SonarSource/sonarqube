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
package org.sonar.server.platform.db.migration.charset;

import com.google.common.annotations.VisibleForTesting;
import java.sql.Connection;
import java.sql.SQLException;
import javax.annotation.CheckForNull;
import org.sonar.db.Database;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

/**
 * On fresh installations, checks that all db columns are UTF8. On MSSQL,
 * whatever fresh or upgrade, fixes case-insensitive columns by converting them to
 * case-sensitive.
 * <p>
 * See SONAR-6171 and SONAR-7549
 */
public class DatabaseCharsetChecker {

  public enum State {
    FRESH_INSTALL, UPGRADE, STARTUP
  }

  private final Database db;
  private final SqlExecutor sqlExecutor;

  public DatabaseCharsetChecker(Database db) {
    this(db, new SqlExecutor());
  }

  @VisibleForTesting
  DatabaseCharsetChecker(Database db, SqlExecutor sqlExecutor) {
    this.db = db;
    this.sqlExecutor = sqlExecutor;
  }

  public void check(State state) {
    try (Connection connection = db.getDataSource().getConnection()) {
      CharsetHandler handler = getHandler(db.getDialect());
      if (handler != null) {
        handler.handle(connection, state);
      }
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  @VisibleForTesting
  @CheckForNull
  CharsetHandler getHandler(Dialect dialect) {
    switch (dialect.getId()) {
      case H2.ID:
        // nothing to check
        return null;
      case Oracle.ID:
        return new OracleCharsetHandler(sqlExecutor);
      case PostgreSql.ID:
        return new PostgresCharsetHandler(sqlExecutor, new PostgresMetadataReader(sqlExecutor));
      case MsSql.ID:
        return new MssqlCharsetHandler(sqlExecutor, new MssqlMetadataReader(sqlExecutor));
      default:
        throw new IllegalArgumentException("Database not supported: " + dialect.getId());
    }
  }
}
