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
package org.sonar.server.platform.db.migration.history;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;

public class MigrationHistoryTableImpl implements MigrationHistoryTable {
  private static final String VERSION_COLUMN_NAME = "version";

  private final Database database;

  public MigrationHistoryTableImpl(Database database) {
    this.database = database;
  }

  @Override
  public void start() {
    try (Connection connection = createDdlConnection(database)) {
      if (!DatabaseUtils.tableExists(NAME, connection)) {
        createTable(connection);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to create table " + NAME, e);
    }
  }

  private void createTable(Connection connection) throws SQLException {
    List<String> sqls = new CreateTableBuilder(database.getDialect(), NAME)
      .addColumn(VarcharColumnDef.newVarcharColumnDefBuilder().setColumnName(VERSION_COLUMN_NAME).setIsNullable(false).setLimit(255).build())
      .build();

    Loggers.get(MigrationHistoryTableImpl.class).info("Creating table " + NAME);
    for (String sql : sqls) {
      execute(connection, sql);
    }
  }

  private static Connection createDdlConnection(Database database) throws SQLException {
    Connection res = database.getDataSource().getConnection();
    res.setAutoCommit(false);
    return res;
  }

  private static void execute(Connection connection, String sql) throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      stmt.execute(sql);
      connection.commit();
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
