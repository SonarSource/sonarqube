/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.config.internal.Settings;
import org.sonar.server.platform.db.migration.step.MigrationStep;

public class MigrationTestDb implements TestDb {

  private final DefaultDatabase database;

  public MigrationTestDb(@Nullable Class<? extends MigrationStep> migrationStepClass) {
    Settings settings = new MapSettings().addProperties(System.getProperties());
    OrchestratorSettingsUtils.loadOrchestratorSettings(settings);
    logJdbcSettings(settings);

    SQDatabase.Builder builder = new SQDatabase.Builder();

    String dialect = settings.getString("sonar.jdbc.dialect");
    if (dialect == null || "h2".equals(dialect)) {
      builder.asH2Database("sonar");
    } else {
      createDatabase(settings);
      builder.withSettings(settings);
    }

    if (migrationStepClass != null) {
      builder.createSchema(true).untilMigrationStep(migrationStepClass);
    }

    database = builder.build();
  }

  private static void createDatabase(Settings settings) {
    Configuration configuration = Configuration.builder()
      .addProperties(settings.getProperties())
      .build();

    DatabaseClient databaseClient = DatabaseFactory.create(configuration, configuration.locators());
    com.sonar.orchestrator.db.DefaultDatabase defaultDatabase = new com.sonar.orchestrator.db.DefaultDatabase(databaseClient);
    defaultDatabase.start();
  }

  private void logJdbcSettings(Settings settings) {
    Logger logger = LoggerFactory.getLogger(getClass());
    for (String key : settings.getKeysStartingWith("sonar.jdbc")) {
      logger.info(key + ": " + settings.getString(key));
    }
  }

  @Override
  public void start() {
    database.start();
  }

  @Override
  public void stop() {
    database.stop();
  }

  @Override
  public DefaultDatabase getDatabase() {
    return database;
  }
}
