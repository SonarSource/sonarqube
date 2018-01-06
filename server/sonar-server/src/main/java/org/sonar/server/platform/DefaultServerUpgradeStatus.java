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
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.picocontainer.Startable;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.server.platform.db.migration.step.MigrationSteps;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;

/**
 * @since 2.5
 */
public final class DefaultServerUpgradeStatus implements ServerUpgradeStatus, Startable {

  private final DatabaseVersion dbVersion;
  private final MigrationSteps migrationSteps;

  // available when connected to db
  private long initialDbVersion;

  public DefaultServerUpgradeStatus(DatabaseVersion dbVersion, MigrationSteps migrationSteps) {
    this.dbVersion = dbVersion;
    this.migrationSteps = migrationSteps;
  }

  @Override
  public void start() {
    Optional<Long> v = dbVersion.getVersion();
    this.initialDbVersion = v.orElse(-1L);
  }

  @Override
  public void stop() {
    // do nothing
  }

  @Override
  public boolean isUpgraded() {
    return !isFreshInstall() && (initialDbVersion < migrationSteps.getMaxMigrationNumber());
  }

  @Override
  public boolean isFreshInstall() {
    return initialDbVersion < 0;
  }

  @Override
  public int getInitialDbVersion() {
    return (int) initialDbVersion;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }
}
