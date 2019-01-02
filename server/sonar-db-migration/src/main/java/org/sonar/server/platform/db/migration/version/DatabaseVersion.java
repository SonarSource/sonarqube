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
package org.sonar.server.platform.db.migration.version;

import java.util.Optional;
import org.sonar.server.platform.db.migration.history.MigrationHistory;
import org.sonar.server.platform.db.migration.step.MigrationSteps;

public class DatabaseVersion {

  /**
   * The minimum supported version which can be upgraded. Lower
   * versions must be previously upgraded to LTS version.
   * Note that the value can't be less than current LTS version.
   */
  public static final long MIN_UPGRADE_VERSION = 1_830;

  private final MigrationSteps migrationSteps;
  private final MigrationHistory migrationHistory;

  public DatabaseVersion(MigrationSteps migrationSteps, MigrationHistory migrationHistory) {
    this.migrationSteps = migrationSteps;
    this.migrationHistory = migrationHistory;
  }

  public Status getStatus() {
    return getStatus(migrationHistory.getLastMigrationNumber(), migrationSteps.getMaxMigrationNumber());
  }

  /**
   * Convenience method to retrieve the value of {@link MigrationHistory#getLastMigrationNumber()}.
   */
  public Optional<Long> getVersion() {
    return migrationHistory.getLastMigrationNumber();
  }

  private static Status getStatus(Optional<Long> currentVersion, long lastVersion) {
    if (!currentVersion.isPresent()) {
      return Status.FRESH_INSTALL;
    }
    Long aLong = currentVersion.get();
    if (aLong == lastVersion) {
      return Status.UP_TO_DATE;
    }
    if (aLong > lastVersion) {
      return Status.REQUIRES_DOWNGRADE;
    }
    return Status.REQUIRES_UPGRADE;
  }

  public enum Status {
    UP_TO_DATE, REQUIRES_UPGRADE, REQUIRES_DOWNGRADE, FRESH_INSTALL
  }
}
