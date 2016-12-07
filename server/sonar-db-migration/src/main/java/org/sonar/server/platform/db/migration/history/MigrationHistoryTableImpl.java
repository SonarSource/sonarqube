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
package org.sonar.server.platform.db.migration.history;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.version.CreateTableBuilder;
import org.sonar.db.version.SchemaMigrationMapper;
import org.sonar.db.version.VarcharColumnDef;

public class MigrationHistoryTableImpl implements MigrationHistoryTable {
  private static final String MIGRATION_HISTORY_TABLE_NAME = "schema_migrations";
  private static final String VERSION_COLUMN_NAME = "version";

  private final DbClient dbClient;

  public MigrationHistoryTableImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void start() {
    if (!tableExists()) {
      createTable();
    }
  }

  private boolean tableExists() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      // TODO replace with SQL not retrieving unlimited number of lines (eg. try and retrieve migration with number 0)
      dbSession.getMapper(SchemaMigrationMapper.class).selectVersions();
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  /**
   * Must use a specific connection for DDL to not be affected by impacts on transaction status of a SQL failure
   * during the check in {@link #tableExists()}.db
   */
  private void createTable() {
    List<String> sqls = new CreateTableBuilder(dbClient.getDatabase().getDialect(), MIGRATION_HISTORY_TABLE_NAME)
      .addColumn(VarcharColumnDef.newVarcharColumnDefBuilder().setColumnName(VERSION_COLUMN_NAME).setIsNullable(false).setLimit(255).build())
      .build();

    Loggers.get(MigrationHistoryTableImpl.class).info("Creating table " + MIGRATION_HISTORY_TABLE_NAME);
    try (Connection connection = createDdlConnection(dbClient)) {
      for (String sql : sqls) {
        execute(connection, sql);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to create table " + MIGRATION_HISTORY_TABLE_NAME, e);
    }
  }

  private static Connection createDdlConnection(DbClient dbClient) throws SQLException {
    Connection res = dbClient.getDatabase().getDataSource().getConnection();
    res.setAutoCommit(false);
    return res;
  }

  private void execute(Connection connection, String sql) throws SQLException {
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
