/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.db;

import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.db.charset.DatabaseCharsetChecker;

/**
 * Checks charset of all database columns when at least one db migration has been executed. This requires
 * to be defined in platform level 3 ({@link org.sonar.server.platform.platformlevel.PlatformLevel3}).
 */
public class CheckDatabaseCharsetAfterMigration extends CheckDatabaseCharsetAtStartup {

  public CheckDatabaseCharsetAfterMigration(ServerUpgradeStatus upgradeStatus, DatabaseCharsetChecker charsetChecker) {
    super(upgradeStatus, charsetChecker);
  }

  @Override
  public void start() {
    if (getUpgradeStatus().isFreshInstall() || getUpgradeStatus().isUpgraded()) {
      check();
    }
  }
}
