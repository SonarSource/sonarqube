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
package org.sonar.db.createdb;

import com.sonar.orchestrator.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.VendorTestDatabaseCreator;

public class CreateDb {

  public static void main(String[] args) {
    Configuration configuration = Configuration.builder()
      .addSystemProperties()
      .addEnvVariables()
      .build();
    Settings settings = new MapSettings();
    configuration.asMap().forEach(settings::setProperty);
    logJdbcSettings(settings);
    VendorTestDatabaseCreator.recreateWithSchema(settings);
  }

  private static void logJdbcSettings(Settings settings) {
    Logger logger = LoggerFactory.getLogger(CreateDb.class);
    for (String key : settings.getKeysStartingWith("sonar.jdbc")) {
      logger.info(key + ": " + settings.getString(key));
    }
  }
}
