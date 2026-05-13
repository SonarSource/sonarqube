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

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.AssumptionViolatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.dialect.H2;
import org.sonar.db.testfixtures.DatabaseTestUtils;
import org.sonar.db.testfixtures.TestDb;
import org.sonar.db.version.SqTables;
import org.sonar.process.logging.LogbackHelper;

class TestDbImpl implements TestDb {
  private static final Logger LOG = LoggerFactory.getLogger(TestDbImpl.class);
  private static TestDbImpl defaultSchemaBaseTestDb;

  private final Database database;
  private final boolean isDefault;

  private TestDbImpl(@Nullable String schemaPath) {
    database = createDatabase(schemaPath);
    isDefault = (schemaPath == null);
  }

  private static Database createDatabase(@Nullable String schemaPath) {
    // Load settings from system properties and orchestrator
    Settings settings = new MapSettings().addProperties(System.getProperties());
    OrchestratorSettingsUtils.loadOrchestratorSettings(settings);

    // Create database based on dialect
    String dialect = settings.getString("sonar.jdbc.dialect");
    Database database;
    if (dialect != null && !"h2".equals(dialect)) {
      database = new DefaultDatabase(new LogbackHelper(), settings);
    } else {
      database = new SQDatabase.Builder()
        .asH2Database("h2Tests" + DigestUtils.md5Hex(StringUtils.defaultString(schemaPath)))
        .createSchema(schemaPath == null)
        .build();
    }

    // Start database
    database.start();

    // Execute schema script if provided (H2 only)
    if (schemaPath != null) {
      if (!database.getDialect().getId().equals("h2")) {
        database.stop();
        throw new AssumptionViolatedException("This test is intended to be run on H2 only");
      }
      ((SQDatabase) database).executeScript(schemaPath);
    }

    LOG.debug("Test Database: {}", database);
    return database;
  }

  static TestDbImpl create(@Nullable String schemaPath) {
    if (schemaPath == null) {
      if (defaultSchemaBaseTestDb == null) {
        defaultSchemaBaseTestDb = new TestDbImpl(null);
      }
      return defaultSchemaBaseTestDb;
    }
    return new TestDbImpl(schemaPath);
  }

  @Override
  public void start() {
    if (!isDefault && !H2.ID.equals(getDatabase().getDialect().getId())) {
      throw new AssumptionViolatedException("Test disabled because it supports only H2");
    }
  }

  @Override
  public void stop() {
    if (!isDefault) {
      getDatabase().stop();
    }
  }

  @Override
  public Database getDatabase() {
    return database;
  }

  @Override
  public void truncateTables() {
    try {
      // we are overriding truncateTables to use a fixed list of tables instead of loading them from the database
      // because here we may be using Oracle which our dynamic table name query does not handle correctly.
      DatabaseTestUtils.truncateTables(getDatabase().getDataSource(), SqTables.TABLES);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to truncate db tables", e);
    }
  }
}
