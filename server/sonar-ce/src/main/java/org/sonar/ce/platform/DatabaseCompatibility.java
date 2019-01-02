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
package org.sonar.ce.platform;

import org.picocontainer.Startable;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;

import static com.google.common.base.Preconditions.checkState;
import static org.sonar.server.platform.db.migration.version.DatabaseVersion.Status.FRESH_INSTALL;
import static org.sonar.server.platform.db.migration.version.DatabaseVersion.Status.UP_TO_DATE;

public class DatabaseCompatibility implements Startable {

  private final DatabaseVersion version;

  public DatabaseCompatibility(DatabaseVersion version) {
    this.version = version;
  }

  @Override
  public void start() {
    DatabaseVersion.Status status = version.getStatus();
    checkState(status == UP_TO_DATE || status == FRESH_INSTALL, "Compute Engine can't start unless Database is up to date");
  }

  @Override
  public void stop() {
    // do nothing
  }
}
