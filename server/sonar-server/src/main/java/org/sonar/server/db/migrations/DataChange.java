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

import com.google.common.collect.Lists;
import org.apache.commons.dbutils.DbUtils;
import org.sonar.core.persistence.Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface DataChange {

  class Context {
    private final Database db;
    private final List<Connection> connections = Lists.newArrayList();

    Context(Database db) {
      this.db = db;
    }

    public Select prepareSelect(String sql) throws SQLException {
      return SelectImpl.create(db, openReadConnection(), sql);
    }

    public Upsert prepareUpsert(String sql) throws SQLException {
      return UpsertImpl.create(openWriteConnection(), sql);
    }

    public MassUpdate prepareMassUpdate() throws SQLException {
      return new MassUpdate(db, openReadConnection(), openWriteConnection());
    }

    private Connection openReadConnection() throws SQLException {
      Connection readConnection = db.getDataSource().getConnection();
      readConnection.setAutoCommit(false);
      if (readConnection.getMetaData().supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED)) {
        readConnection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
      }
      connections.add(readConnection);
      return readConnection;
    }

    private Connection openWriteConnection() throws SQLException {
      Connection writeConnection = db.getDataSource().getConnection();
      writeConnection.setAutoCommit(false);
      connections.add(writeConnection);
      return writeConnection;
    }

    void close() {
      for (Connection connection : connections) {
        DbUtils.closeQuietly(connection);
      }
      connections.clear();
    }
  }

  void execute(Context context) throws SQLException;
}
