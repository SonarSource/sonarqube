/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;

import static org.sonar.server.app.ServerProcessLogging.STARTUP_LOGGER_NAME;

public class DatabaseServerCompatibility implements Startable {

  private static final String HIGHLIGHTER = "################################################################################";
  private DatabaseVersion version;

  public DatabaseServerCompatibility(DatabaseVersion version) {
    this.version = version;
  }

  @Override
  public void start() {
    DatabaseVersion.Status status = version.getStatus();
    if (status == DatabaseVersion.Status.REQUIRES_DOWNGRADE) {
      throw MessageException.of("Database was upgraded to a more recent of SonarQube. Backup must probably be restored or db settings are incorrect.");
    }
    if (status == DatabaseVersion.Status.REQUIRES_UPGRADE) {
      Optional<Long> currentVersion = this.version.getVersion();
      if (currentVersion.isPresent() && currentVersion.get() < DatabaseVersion.MIN_UPGRADE_VERSION) {
        throw MessageException.of("Current version is too old. Please upgrade to Long Term Support version firstly.");
      }
      String msg = "Database must be upgraded. Please backup database and browse /setup";
      Loggers.get(DatabaseServerCompatibility.class).warn(msg);
      Loggers.get(STARTUP_LOGGER_NAME).warn('\n'
        + HIGHLIGHTER + '\n'
        + "      " + msg
        + '\n' + HIGHLIGHTER);
    }
  }

  @Override
  public void stop() {
    // do nothing
  }
}
