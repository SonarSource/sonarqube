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
package org.sonar.server.db.migrations;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.dbutils.DbUtils;
import org.sonar.core.persistence.Database;

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

  public static class Context {
    private final Connection writeConnection;

    public Context(Connection writeConnection) {
      this.writeConnection = writeConnection;
    }

    public void execute(String sql) throws SQLException {
      try {
        UpsertImpl.create(writeConnection, sql).execute().commit();
      } catch (Exception e) {
        throw new IllegalStateException(String.format("Fail to execute %s", sql), e);
      }
    }
  }

  public abstract void execute(Context context) throws SQLException;

}
