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
package org.sonar.server.platform.db;

import org.picocontainer.Startable;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.db.charset.DatabaseCharsetChecker;

import static org.sonar.db.charset.DatabaseCharsetChecker.Flag.ENFORCE_UTF8;

/**
 * Checks charset of all existing database columns at startup, before executing db migrations. This requires
 * to be defined in platform level 2 ({@link org.sonar.server.platform.platformlevel.PlatformLevel2}).
 */
public class CheckDatabaseCharsetAtStartup implements Startable {

  private final ServerUpgradeStatus upgradeStatus;
  private final DatabaseCharsetChecker charsetChecker;

  public CheckDatabaseCharsetAtStartup(ServerUpgradeStatus upgradeStatus, DatabaseCharsetChecker charsetChecker) {
    this.upgradeStatus = upgradeStatus;
    this.charsetChecker = charsetChecker;
  }

  @Override
  public void start() {
    check();
  }

  @Override
  public void stop() {
    // do nothing
  }

  protected final void check() {
    if (upgradeStatus.isFreshInstall()) {
      charsetChecker.check(ENFORCE_UTF8);
    } else if (!upgradeStatus.isUpgraded()) {
      charsetChecker.check();
    }
  }
}
