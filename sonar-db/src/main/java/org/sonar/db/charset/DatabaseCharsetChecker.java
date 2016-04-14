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
package org.sonar.db.charset;

import com.google.common.annotations.VisibleForTesting;
import java.sql.Connection;
import java.sql.SQLException;
import javax.annotation.CheckForNull;
import org.sonar.db.Database;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

/**
 * On fresh installations, checks that all db columns are UTF8. On all installations on MySQL or MSSQL,
 * whatever fresh or upgrade, fixes case-insensitive columns by converting them to
 * case-sensitive.
 *
 * See SONAR-6171 and SONAR-7549
 */
public class DatabaseCharsetChecker {

  private final Database db;
  private final SqlExecutor selectExecutor;

  public DatabaseCharsetChecker(Database db) {
    this(db, new SqlExecutor());
  }

  @VisibleForTesting
  DatabaseCharsetChecker(Database db, SqlExecutor selectExecutor) {
    this.db = db;
    this.selectExecutor = selectExecutor;
  }

  public void check(boolean enforceUtf8) {
    try {
      try (Connection connection = db.getDataSource().getConnection()) {
        CharsetHandler handler = getHandler(db.getDialect());
        if (handler != null) {
          handler.handle(connection, enforceUtf8);
        }
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
        return new OracleCharsetHandler(selectExecutor);
      case PostgreSql.ID:
        return new PostgresCharsetHandler(selectExecutor);
      case MySql.ID:
        return new MysqlCharsetHandler(selectExecutor);
      case MsSql.ID:
        return new MssqlCharsetHandler(selectExecutor);
      default:
        throw new IllegalArgumentException("Database not supported: " + dialect.getId());
    }
  }

}
