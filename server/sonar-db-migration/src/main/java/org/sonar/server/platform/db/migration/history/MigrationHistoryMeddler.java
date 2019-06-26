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
package org.sonar.server.platform.db.migration.history;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.sonar.server.platform.db.migration.step.MigrationSteps;
import org.sonar.server.platform.db.migration.step.RegisteredMigrationStep;

/**
 * Under some conditions, the migration history must be manipulated. This is the role of this class
 * which is called by {@link MigrationHistory#start()}.
 */
public class MigrationHistoryMeddler {
  private final Map<Long, Long> meddledSteps = ImmutableMap.of(
    // SONAR-12127 several DB migration were added in 7.9 to migrate to from 6.7 to 7.0
    // If already on 7.0, we don't want any of these DB migrations ran
    // => we change last migration number of those 7.0 instance to the new max migration number for 7.0
    1_923L, 1_959L);
  private final MigrationSteps migrationSteps;

  public MigrationHistoryMeddler(MigrationSteps migrationSteps) {
    this.migrationSteps = migrationSteps;
  }

  public void meddle(MigrationHistory migrationHistory) {
    // change last migration number on specific cases
    migrationHistory.getLastMigrationNumber()
      .ifPresent(migrationNumber -> {
        Long newMigrationNumber = meddledSteps.get(migrationNumber);
        if (newMigrationNumber != null) {
          RegisteredMigrationStep registeredMigrationStep = migrationSteps.readFrom(newMigrationNumber).get(0);
          migrationHistory.done(registeredMigrationStep);
        }
      });
  }
}
