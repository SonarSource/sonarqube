/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.sql.SQLException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.AssumptionViolatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.config.internal.Settings;

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
    return create(schemaPath, true);
  }

  static CoreTestDb create(String schemaPath, boolean databaseToUpper) {
    requireNonNull(schemaPath, "schemaPath can't be null");

    return new CoreTestDb().init(schemaPath, databaseToUpper);
  }

  static CoreTestDb createEmpty() {
    return new CoreTestDb().init(null, true);
  }

  private CoreTestDb init(@Nullable String schemaPath, boolean databaseToUpper) {
    Consumer<Settings> noExtraSettingsLoaded = settings -> {
    };
    Function<Settings, Database> databaseCreator = settings -> {
      String dialect = settings.getString("sonar.jdbc.dialect");

      // test relying on CoreTestDb can only run on H2
      if (dialect != null && !"h2".equals(dialect)) {
        throw new AssumptionViolatedException("This test is intended to be run on H2 only");
      }

      String name = "h2Tests-" + (schemaPath == null ? "empty" : DigestUtils.md5Hex(schemaPath));
      if (!databaseToUpper) {
        name = name + ";DATABASE_TO_UPPER=FALSE";
      }
      name = name + ";NON_KEYWORDS=VALUE";
      return new CoreH2Database(name);
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
      LoggerFactory.getLogger(getClass()).debug("Test Database: " + db);

      String login = settings.getString(JDBC_USERNAME.getKey());

      extendedStart.accept(db, true);
    } else {
      extendedStart.accept(db, false);
    }
  }

  public void truncateTables() {
    try {
      DatabaseTestUtils.truncateAllTables(getDatabase().getDataSource());
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to truncate db tables", e);
    }
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
    Logger logger = LoggerFactory.getLogger(getClass());
    for (String key : settings.getKeysStartingWith("sonar.jdbc")) {
      logger.info(key + ": " + settings.getString(key));
    }
  }

}
