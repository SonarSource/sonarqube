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
package org.sonar.server.platform.db.migration.step;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.regex.Pattern;
import org.sonar.db.Database;
import org.sonar.db.dialect.Dialect;

import static java.lang.String.format;
import static java.util.Arrays.asList;

public abstract class DdlChange implements MigrationStep {

  private final Database db;

  public DdlChange(Database db) {
    this.db = db;
  }

  @Override
  public final void execute() throws SQLException {
    try (Connection writeConnection = createDdlConnection()) {
      Context context = new Context(writeConnection);
      execute(context);
    }
  }

  private Connection createDdlConnection() throws SQLException {
    Connection writeConnection = db.getDataSource().getConnection();
    writeConnection.setAutoCommit(false);
    return writeConnection;
  }

  public abstract void execute(Context context) throws SQLException;

  protected Database getDatabase() {
    return db;
  }

  protected Dialect getDialect() {
    return db.getDialect();
  }

  public static class Context {
    private static final int ERROR_HANDLING_THRESHOLD = 10;
    // the tricky regexp is required to match "NULL" but not "NOT NULL"
    private final Pattern nullPattern = Pattern.compile("\\h?(?<!NOT )NULL");
    private final Pattern notNullPattern = Pattern.compile("\\h?NOT NULL");
    private final Connection writeConnection;

    public Context(Connection writeConnection) {
      this.writeConnection = writeConnection;
    }

    public void execute(String sql) throws SQLException {
      execute(sql, sql, 0);
    }

    public void execute(String original, String sql, int errorCount) throws SQLException {
      try (Statement stmt = writeConnection.createStatement()) {
        stmt.execute(sql);
        writeConnection.commit();
      } catch (SQLException e) {
        if (errorCount < ERROR_HANDLING_THRESHOLD) {
          String message = e.getMessage();
          if (message.contains("ORA-01451")) {
            String newSql = nullPattern.matcher(sql).replaceFirst("");
            execute(original, newSql, errorCount + 1);
            return;
          } else if (message.contains("ORA-01442")) {
            String newSql = notNullPattern.matcher(sql).replaceFirst("");
            execute(original, newSql, errorCount + 1);
            return;
          }
        }
        throw new IllegalStateException(messageForIseOf(original, sql, errorCount), e);
      } catch (Exception e) {
        throw new IllegalStateException(messageForIseOf(original, sql, errorCount), e);
      }
    }

    private static String messageForIseOf(String original, String sql, int errorCount) {
      if (!original.equals(sql) || errorCount > 0) {
        return format("Fail to execute %s %n (caught %s error, original was %s)", sql, errorCount, original);
      } else {
        return format("Fail to execute %s", sql);
      }
    }

    public void execute(String... sqls) throws SQLException {
      execute(asList(sqls));
    }

    public void execute(List<String> sqls) throws SQLException {
      for (String sql : sqls) {
        execute(sql);
      }
    }
  }
}
