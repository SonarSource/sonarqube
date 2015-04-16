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

public abstract class BaseDataChange implements DataChange, MigrationStep {

  private final Database db;

  public BaseDataChange(Database db) {
    this.db = db;
  }

  @Override
  public final void execute() throws SQLException {
    Connection readConnection = null, writeConnection = null;
    try {
      readConnection = openConnection();

      writeConnection = db.getDataSource().getConnection();
      writeConnection.setAutoCommit(false);
      Context context = new Context(db, readConnection, writeConnection);
      execute(context);

    } finally {
      DbUtils.closeQuietly(readConnection);
      DbUtils.closeQuietly(writeConnection);
    }
  }

  /**
   * Do not forget to close it !
   */
  protected Connection openConnection() throws SQLException {
    Connection connection = db.getDataSource().getConnection();
    connection.setAutoCommit(false);
    if (connection.getMetaData().supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED)) {
      connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
    }
    return connection;
  }

}
