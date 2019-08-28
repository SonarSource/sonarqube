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
package org.sonar.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.AssumptionViolatedException;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.version.SqTables;

import static java.util.Objects.requireNonNull;
import static org.sonar.process.ProcessProperties.Property.JDBC_USERNAME;

/**
 * This class should be call using @ClassRule in order to create the schema once (if @Rule is used
 * the schema will be recreated before each test).
 * <p>
 * <strong>Tests which rely on this class can only be run on H2</strong> because:
 * <ul>
 *   <li>resetting the schema for each test on non-H2 database is assumed to expensive and slow</li>
 *   <li>when a specific schema is provided, this schema can't provide a syntax supported by all SGBDs and therefor only
 *       H2 is targeted</li>
 * </ul>
 */
class CoreTestDb implements TestDb {

  private Database db;

  protected CoreTestDb() {
    // use static factory method
  }

  protected CoreTestDb(Database db) {
    this.db = db;
  }

  static CoreTestDb create(String schemaPath) {
    requireNonNull(schemaPath, "schemaPath can't be null");

    return new CoreTestDb().init(schemaPath);
  }

  static CoreTestDb createEmpty() {
    return new CoreTestDb().init(null);
  }

  private CoreTestDb init(@Nullable String schemaPath) {
    Consumer<Settings> noExtraSettingsLoaded = settings -> {
    };
    Function<Settings, Database> databaseCreator = settings -> {
      String dialect = settings.getString("sonar.jdbc.dialect");

      // test relying on CoreTestDb can only run on H2
      if (dialect != null && !"h2".equals(dialect)) {
        throw new AssumptionViolatedException("This test is intended to be run on H2 only");
      }

      return new CoreH2Database("h2Tests-" + (schemaPath == null ? "empty" : DigestUtils.md5Hex(schemaPath)));
    };
    Consumer<Database> databaseInitializer = database -> {
      if (schemaPath == null) {
        return;
      }

      ((CoreH2Database) database).executeScript(schemaPath);
    };
    BiConsumer<Database, Boolean> noPostStartAction = (db, created) -> {
    };

    init(noExtraSettingsLoaded, databaseCreator, databaseInitializer, noPostStartAction);
    return this;
  }

  protected void init(Consumer<Settings> settingsLoader,
    Function<Settings, Database> databaseCreator,
    Consumer<Database> databaseInitializer,
    BiConsumer<Database, Boolean> extendedStart) {
    if (db == null) {
      Settings settings = new MapSettings().addProperties(System.getProperties());
      settingsLoader.accept(settings);
      logJdbcSettings(settings);
      db = databaseCreator.apply(settings);
      db.start();

      databaseInitializer.accept(db);
      Loggers.get(getClass()).debug("Test Database: " + db);

      String login = settings.getString(JDBC_USERNAME.getKey());

      extendedStart.accept(db, true);
    } else {
      extendedStart.accept(db, false);
    }
  }

  public void truncateTables() {
    try {
      truncateDatabase(getDatabase().getDataSource());
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to truncate db tables", e);
    }
  }

  private void truncateDatabase(DataSource dataSource) throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try (Statement statement = connection.createStatement()) {
        for (String table : SqTables.TABLES) {
          try {
            if (shouldTruncate(connection, table)) {
              statement.executeUpdate(truncateSql(table));
              connection.commit();
            }
          } catch (Exception e) {
            connection.rollback();
            throw new IllegalStateException("Fail to truncate table " + table, e);
          }
        }
      }
    }
  }

  private static boolean shouldTruncate(Connection connection, String table) {
    try (Statement stmt = connection.createStatement();
      ResultSet rs = stmt.executeQuery("select count(1) from " + table)) {
      if (rs.next()) {
        return rs.getInt(1) > 0;
      }

    } catch (SQLException ignored) {
      // probably because table does not exist. That's the case with H2 tests.
    }
    return false;
  }

  private static String truncateSql(String table) {
    return "TRUNCATE TABLE " + table;
  }

  @Override
  public Database getDatabase() {
    return db;
  }

  @Override
  public void start() {
    // everything is done in init
  }

  @Override
  public void stop() {
    db.stop();
  }

  private void logJdbcSettings(Settings settings) {
    Logger logger = Loggers.get(getClass());
    for (String key : settings.getKeysStartingWith("sonar.jdbc")) {
      logger.info(key + ": " + settings.getString(key));
    }
  }

}
