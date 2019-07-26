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
package org.sonar.db.createdb;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.db.DatabaseClient;
import com.sonar.orchestrator.db.DatabaseFactory;
import com.sonar.orchestrator.db.DefaultDatabase;
import java.util.function.Consumer;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.SQDatabase;

public class CreateDb {

  public static void main(String[] args) {
    createDb(configuration -> {
      Settings settings = new MapSettings();
      configuration.asMap().forEach(settings::setProperty);
      logJdbcSettings(settings);
      SQDatabase.newDatabase(settings, true).start();
    });
  }

  private static void createDb(Consumer<Configuration> execute) {
    Configuration configuration = Configuration.builder()
      .addSystemProperties()
      .setProperty("orchestrator.keepDatabase", "false")
      .build();

    DatabaseClient databaseClient = DatabaseFactory.create(configuration, configuration.locators());
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
    Logger logger = Loggers.get(CreateDb.class);
    for (String key : settings.getKeysStartingWith("sonar.jdbc")) {
      logger.info(key + ": " + settings.getString(key));
    }
  }
}
