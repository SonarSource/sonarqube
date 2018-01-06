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
package org.sonar.server.platform.db.migration.step;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.core.util.stream.MoreCollectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.platform.db.migration.step.MigrationNumber.validate;

public class MigrationStepRegistryImpl implements InternalMigrationStepRegistry {
  private final Map<Long, RegisteredMigrationStep> migrations = new HashMap<>();

  @Override
  public <T extends MigrationStep> MigrationStepRegistry add(long migrationNumber, String description, Class<T> stepClass) {
    validate(migrationNumber);
    requireNonNull(description, "description can't be null");
    checkArgument(!description.isEmpty(), "description can't be empty");
    requireNonNull(stepClass, "MigrationStep class can't be null");
    checkState(!migrations.containsKey(migrationNumber), "A migration is already registered for migration number '%s'", migrationNumber);
    this.migrations.put(migrationNumber, new RegisteredMigrationStep(migrationNumber, description, stepClass));
    return this;
  }

  @Override
  public MigrationSteps build() {
    checkState(!migrations.isEmpty(), "Registry is empty");
    return new MigrationStepsImpl(toOrderedList(this.migrations));
  }

  private static List<RegisteredMigrationStep> toOrderedList(Map<Long, RegisteredMigrationStep> migrations) {
    return migrations.entrySet().stream()
      .sorted(Comparator.comparingLong(Map.Entry::getKey))
      .map(Map.Entry::getValue)
      .collect(MoreCollectors.toList(migrations.size()));
  }

}
