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
package org.sonar.server.platform.db.migration;

import org.picocontainer.Startable;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.platform.DefaultServerUpgradeStatus;
import org.sonar.server.platform.db.migration.engine.MigrationEngine;

public class AutoDbMigration implements Startable {
  private final DefaultServerUpgradeStatus serverUpgradeStatus;
  private final MigrationEngine migrationEngine;

  public AutoDbMigration(DefaultServerUpgradeStatus serverUpgradeStatus, MigrationEngine migrationEngine) {
    this.serverUpgradeStatus = serverUpgradeStatus;
    this.migrationEngine = migrationEngine;
  }

  @Override
  public void start() {
    if (serverUpgradeStatus.isFreshInstall()) {
      Loggers.get(getClass()).info("Automatically perform DB migration on fresh install");
      migrationEngine.execute();
    } else if (serverUpgradeStatus.isUpgraded() && serverUpgradeStatus.isBlueGreen()) {
      Loggers.get(getClass()).info("Automatically perform DB migration on blue/green deployment");
      migrationEngine.execute();
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }

}
