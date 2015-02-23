/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.platform;

import org.picocontainer.Startable;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.MessageException;
import org.sonar.core.persistence.DatabaseVersion;

public class DatabaseServerCompatibility implements Startable {

  private DatabaseVersion version;

  public DatabaseServerCompatibility(DatabaseVersion version) {
    this.version = version;
  }

  @Override
  public void start() {
    DatabaseVersion.Status status = version.getStatus();
    if (status == DatabaseVersion.Status.REQUIRES_DOWNGRADE) {
      throw MessageException.of("Database relates to a more recent version of SonarQube. Please check your settings.");
    }
    if (status == DatabaseVersion.Status.REQUIRES_UPGRADE) {
      Loggers.get(DatabaseServerCompatibility.class).warn("Database must be upgraded. Please browse /setup");
    }
  }

  @Override
  public void stop() {
    // do nothing
  }
}
