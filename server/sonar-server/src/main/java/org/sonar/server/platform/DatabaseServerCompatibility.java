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
package org.sonar.server.platform;

import java.util.Optional;
import org.picocontainer.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.ProcessProperties;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;

import static org.sonar.server.log.ServerProcessLogging.STARTUP_LOGGER_NAME;

public class DatabaseServerCompatibility implements Startable {

  private static final String HIGHLIGHTER = "################################################################################";

  private final DatabaseVersion version;
  private final Configuration configuration;

  public DatabaseServerCompatibility(DatabaseVersion version, Configuration configuration) {
    this.version = version;
    this.configuration = configuration;
  }

  @Override
  public void start() {
    DatabaseVersion.Status status = version.getStatus();
    if (status == DatabaseVersion.Status.REQUIRES_DOWNGRADE) {
      throw MessageException.of("Database was upgraded to a more recent version of SonarQube. "
        + "A backup must probably be restored or the DB settings are incorrect.");
    }
    if (status == DatabaseVersion.Status.REQUIRES_UPGRADE) {
      Optional<Long> currentVersion = this.version.getVersion();
      if (currentVersion.isPresent() && currentVersion.get() < DatabaseVersion.MIN_UPGRADE_VERSION) {
        throw MessageException.of("Current version is too old. Please upgrade to Long Term Support version firstly.");
      }
      boolean blueGreen = configuration.getBoolean(ProcessProperties.Property.BLUE_GREEN_ENABLED.getKey()).orElse(false);
      if (!blueGreen) {
        String msg = "The database must be manually upgraded. Please backup the database and browse /setup. "
          + "For more information: https://docs.sonarqube.org/latest/setup/upgrading";
        Loggers.get(DatabaseServerCompatibility.class).warn(msg);
        Loggers.get(STARTUP_LOGGER_NAME).warn('\n'
          + HIGHLIGHTER + '\n'
          + "      " + msg
          + '\n' + HIGHLIGHTER);
      }
    }
  }

  @Override
  public void stop() {
    // do nothing
  }
}
