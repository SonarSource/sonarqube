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
package org.sonar.server.platform.db.migration.step;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.api.config.Configuration;
import org.sonar.db.Database;
import org.sonar.db.dialect.Dialect;

public abstract class DataChange implements MigrationStep {

  private final Database db;

  public DataChange(Database db) {
    this.db = db;
  }

  protected final Dialect getDialect() {
    return db.getDialect();
  }

  @Override
  public final void execute() throws SQLException {
    try (Connection readConnection = createReadUncommittedConnection();
      Connection writeConnection = createDdlConnection()) {
      Context context = new Context(db, readConnection, writeConnection);
      execute(context);
    }
  }

  protected abstract void execute(Context context) throws SQLException;

  private Connection createReadUncommittedConnection() throws SQLException {
    Connection connection = db.getDataSource().getConnection();
    connection.setAutoCommit(false);
    if (connection.getMetaData().supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED)) {
      connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
    }
    return connection;
  }

  private Connection createDdlConnection() throws SQLException {
    Connection res = db.getDataSource().getConnection();
    res.setAutoCommit(false);
    return res;
  }

  protected static boolean isSonarCloud(Configuration configuration) {
    return configuration.getBoolean("sonar.sonarcloud.enabled").orElse(false);
  }

  public static class Context {
    private final Database db;
    private final Connection readConnection;
    private final Connection writeConnection;

    public Context(Database db, Connection readConnection, Connection writeConnection) {
      this.db = db;
      this.readConnection = readConnection;
      this.writeConnection = writeConnection;
    }

    public Select prepareSelect(String sql) throws SQLException {
      return SelectImpl.create(db, readConnection, sql);
    }

    public Upsert prepareUpsert(String sql) throws SQLException {
      return UpsertImpl.create(writeConnection, sql);
    }

    public MassUpdate prepareMassUpdate() {
      return new MassUpdate(db, readConnection, writeConnection);
    }
  }

}
