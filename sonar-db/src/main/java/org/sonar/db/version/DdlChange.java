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
package org.sonar.db.version;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.apache.commons.dbutils.DbUtils;
import org.sonar.db.Database;

import static java.lang.String.*;
import static java.util.Arrays.asList;

public abstract class DdlChange implements MigrationStep {

  private final Database db;

  public DdlChange(Database db) {
    this.db = db;
  }

  @Override
  public final void execute() throws SQLException {
    Connection writeConnection = null;
    try {
      writeConnection = db.getDataSource().getConnection();
      writeConnection.setAutoCommit(false);
      Context context = new Context(writeConnection);
      execute(context);

    } finally {
      DbUtils.closeQuietly(writeConnection);
    }
  }

  public abstract void execute(Context context) throws SQLException;

  protected Database getDatabase() {
    return db;
  }

  public static class Context {
    private final Connection writeConnection;

    public Context(Connection writeConnection) {
      this.writeConnection = writeConnection;
    }

    public void execute(String sql) throws SQLException {
      try {
        UpsertImpl.create(writeConnection, sql).execute().commit();
      } catch (Exception e) {
        throw new IllegalStateException(format("Fail to execute %s", sql), e);
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
