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
package org.sonar.server.platform.db.migrations;

import com.google.common.annotations.VisibleForTesting;
import java.sql.Connection;
import org.apache.commons.dbutils.DbUtils;
import org.apache.ibatis.session.SqlSession;
import org.picocontainer.Startable;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DdlUtils;
import org.sonar.server.plugins.ServerPluginRepository;

/**
 * Restore schema by executing DDL scripts. Only H2 database is supported.
 * Other databases are created by Ruby on Rails migrations.
 *
 * @since 2.12
 */
@ServerSide
public class DatabaseMigrator implements Startable {

  private final DbClient dbClient;
  private final ServerUpgradeStatus serverUpgradeStatus;

  /**
   * ServerPluginRepository is used to ensure H2 schema creation is done only after copy of bundle plugins have been done
   */
  public DatabaseMigrator(DbClient dbClient, ServerUpgradeStatus serverUpgradeStatus, ServerPluginRepository unused) {
    this.dbClient = dbClient;
    this.serverUpgradeStatus = serverUpgradeStatus;
  }

  @Override
  public void start() {
    createDatabase();
  }

  @Override
  public void stop() {
    // Nothing to do
  }

  /**
   * @return true if the database has been created, false if this database is not supported or if database has already been created
   */
  @VisibleForTesting
  boolean createDatabase() {
    if (DdlUtils.supportsDialect(dbClient.getDatabase().getDialect().getId()) && serverUpgradeStatus.isFreshInstall()) {
      Loggers.get(getClass()).info("Create database");
      SqlSession session = dbClient.openSession(false);
      Connection connection = null;
      try {
        connection = session.getConnection();
        createSchema(connection, dbClient.getDatabase().getDialect().getId());
        return true;
      } finally {
        session.close();

        // The connection is probably already closed by session.close()
        // but it's not documented in mybatis javadoc.
        DbUtils.closeQuietly(connection);
      }
    }
    return false;
  }

  @VisibleForTesting
  protected void createSchema(Connection connection, String dialectId) {
    DdlUtils.createSchema(connection, dialectId, false);
  }
}
