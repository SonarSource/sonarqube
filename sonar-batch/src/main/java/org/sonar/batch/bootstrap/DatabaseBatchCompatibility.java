/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import org.sonar.api.BatchComponent;
import org.sonar.api.platform.Server;
import org.sonar.core.persistence.BadDatabaseVersion;
import org.sonar.core.persistence.DatabaseVersion;

/**
 * Detects if database is not up-to-date with the version required by the batch.
 */
public class DatabaseBatchCompatibility implements BatchComponent {

  private DatabaseVersion version;
  private Server server;

  public DatabaseBatchCompatibility(DatabaseVersion version, Server server) {
    this.version = version;
    this.server = server;
  }

  public void start() {
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
