/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.db;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.db.DatabaseClient;
import com.sonar.orchestrator.db.DatabaseFactory;
import com.sonar.orchestrator.db.DefaultDatabase;
import com.sonar.orchestrator.locator.Locators;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.sonar.api.config.internal.Settings;

/**
 * Drops and recreates the PostgreSQL, SQL Server or Oracle database described by the settings, then creates the
 * SonarQube schema in it.
 */
public final class VendorTestDatabaseCreator {

  private VendorTestDatabaseCreator() {
    // prevents instantiation
  }

  public static void recreateWithSchema(Settings settings) {
    Configuration configuration = Configuration.builder()
      .addEnvVariables()
      .addProperties(settings.getProperties())
      .setProperty("orchestrator.keepDatabase", "false")
      .build();

    DatabaseClient databaseClient = DatabaseFactory.create(configuration, new Locators(configuration));
    DefaultDatabase defaultDatabase = new DefaultDatabase(databaseClient);
    defaultDatabase.killOtherConnections();
    try {
      defaultDatabase.start();
      enableReadCommittedSnapshotOnMsSql(defaultDatabase);

      SQDatabase database = new SQDatabase.Builder().createSchema(true).withSettings(settings).build();
      database.start();
      database.stop();
    } finally {
      defaultDatabase.stop();
    }
  }

  /**
   * Required on SQL Server in production, where readers otherwise block on uncommitted writes.
   */
  private static void enableReadCommittedSnapshotOnMsSql(DefaultDatabase database) {
    DatabaseClient client = database.getClient();
    if (!"mssql".equals(client.getDialect())) {
      return;
    }
    String databaseName = database.executeSql("SELECT DB_NAME()").get(0).values().iterator().next();
    try (Connection connection = DriverManager.getConnection(client.getRootUrl(), client.getRootLogin(), client.getRootPassword());
      Statement statement = connection.createStatement()) {
      statement.execute("ALTER DATABASE [" + databaseName + "] SET READ_COMMITTED_SNAPSHOT ON WITH ROLLBACK IMMEDIATE");
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to enable READ_COMMITTED_SNAPSHOT on " + databaseName, e);
    }
  }
}
