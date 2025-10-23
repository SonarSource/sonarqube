/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.createdb;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.db.DatabaseClient;
import com.sonar.orchestrator.db.DatabaseFactory;
import com.sonar.orchestrator.db.DefaultDatabase;
import com.sonar.orchestrator.locator.Locators;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.SQDatabase;

public class CreateDb {

  public static void main(String[] args) {
    createDb(configuration -> {
      Settings settings = new MapSettings();
      configuration.asMap().forEach(settings::setProperty);
      logJdbcSettings(settings);
      new SQDatabase.Builder().createSchema(true).withSettings(settings).build().start();
    });
  }

  private static void createDb(Consumer<Configuration> execute) {
    Configuration configuration = Configuration.builder()
      .addSystemProperties()
      .addEnvVariables()
      .setProperty("orchestrator.keepDatabase", "false")
      .build();

    DatabaseClient databaseClient = DatabaseFactory.create(configuration, new Locators(configuration));
    DefaultDatabase defaultDatabase = new DefaultDatabase(databaseClient);
    defaultDatabase.killOtherConnections();
    try {
      defaultDatabase.start();

      execute.accept(configuration);
    } finally {
      defaultDatabase.stop();
    }
  }

  private static void logJdbcSettings(Settings settings) {
    Logger logger = LoggerFactory.getLogger(CreateDb.class);
    for (String key : settings.getKeysStartingWith("sonar.jdbc")) {
      logger.info(key + ": " + settings.getString(key));
    }
  }
}
