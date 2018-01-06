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

import java.util.List;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.platform.db.migration.step.MigrationNumber.validate;

class MigrationStepsImpl implements MigrationSteps {
  private final List<RegisteredMigrationStep> steps;

  MigrationStepsImpl(List<RegisteredMigrationStep> steps) {
    requireNonNull(steps, "steps can't be null");
    checkArgument(!steps.isEmpty(), "steps can't be empty");
    this.steps = copyOf(steps);
  }

  @Override
  public long getMaxMigrationNumber() {
    return steps.get(steps.size() -1).getMigrationNumber();
  }

  @Override
  public Stream<RegisteredMigrationStep> readAll() {
    return steps.stream();
  }

  @Override
  public Stream<RegisteredMigrationStep> readFrom(long migrationNumber) {
    validate(migrationNumber);
    int startingIndex = lookupIndexOfClosestTo(migrationNumber);
    if (startingIndex < 0) {
      return Stream.empty();
    }
    return steps.subList(startingIndex, steps.size()).stream();
  }

  private int lookupIndexOfClosestTo(long startingPoint) {
    int index = 0;
    for (RegisteredMigrationStep step : steps) {
      if (step.getMigrationNumber() >= startingPoint) {
        return index;
      }
      index++;
    }
    return -1;
  }
}
