/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.bootstrap;

import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.core.persistence.BadDatabaseVersion;
import org.sonar.core.persistence.DatabaseVersion;

/**
 * Detects if database is not up-to-date with the version required by the batch.
 */
public class DatabaseCompatibility implements BatchComponent {

  private DatabaseVersion version;
  private Settings settings;
  private ServerMetadata server;

  public DatabaseCompatibility(DatabaseVersion version, ServerMetadata server, Settings settings) {
    this.version = version;
    this.server = server;
    this.settings = settings;
  }

  public void start() {
    if (!settings.getBoolean(CoreProperties.DRY_RUN)) {
      checkCorrectServerId();
      checkDatabaseStatus();
    }
  }

  private void checkCorrectServerId() {
    if (!settings.getString(CoreProperties.SERVER_ID).equals(server.getServerId())) {
      StringBuilder message = new StringBuilder("The current batch process and the configured remote server do not share the same DB configuration.\n");
      message.append("\t- Batch side: ");
      message.append(settings.getString(DatabaseProperties.PROP_URL));
      message.append(" (");
      String userName = settings.getString(DatabaseProperties.PROP_USER);
      message.append(userName == null ? "sonar" : userName);
      message.append(" / *****)\n\t- Server side: check the configuration at ");
      message.append(server.getURL());
      message.append("/system\n");
      throw new BadDatabaseVersion(message.toString());
    }
  }

  private void checkDatabaseStatus() {
    DatabaseVersion.Status status = version.getStatus();
    if (status == DatabaseVersion.Status.REQUIRES_DOWNGRADE) {
      throw new BadDatabaseVersion("Database relates to a more recent version of Sonar. Please check your settings (JDBC settings, version of Maven plugin)");
    }
    if (status == DatabaseVersion.Status.REQUIRES_UPGRADE) {
      throw new BadDatabaseVersion("Database must be upgraded. Please browse " + server.getURL() + "/setup");
    }
    if (status != DatabaseVersion.Status.UP_TO_DATE) {
      // Support other future values
      throw new BadDatabaseVersion("Unknown database status: " + status);
    }
  }

}
