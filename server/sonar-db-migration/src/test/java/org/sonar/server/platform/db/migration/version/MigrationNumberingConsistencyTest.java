/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.core.platform.ListContainer;
import org.sonar.server.platform.db.migration.MigrationConfigurationModule;
import org.sonar.server.platform.db.migration.step.MigrationStepRegistryImpl;
import org.sonar.server.platform.db.migration.step.RegisteredMigrationStep;
import org.sonar.server.platform.db.migration.version.v00.DbVersion00;
import org.sonar.server.platform.db.migration.version.v202601.DbVersion202601;
import org.sonar.server.platform.db.migration.version.v202602.DbVersion202602;
import org.sonar.server.platform.db.migration.version.v202603.DbVersion202603;
import org.sonar.server.platform.db.migration.version.v202604.DbVersion202604;
import org.sonar.server.platform.db.migration.version.v202605.DbVersion202605;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against a migration step being registered under a stale, already-superseded
 * {@code DbVersion*} package (see SCA-3039): such a step never runs on an environment that has
 * already applied a numerically higher migration from a newer package, silently forever.
 * <p>
 * Two invariants are checked, because either alone leaves a gap:
 * <ul>
 *   <li>{@link #VALUE_matches_the_highest_migration_number_actually_registered()} — {@link CurrentMigrationNumber#VALUE}
 *   must match the highest registered migration number overall. Two PRs adding migrations around the same
 *   time now edit this same line, producing a real git merge conflict instead of silently landing in
 *   different packages.</li>
 *   <li>{@link #every_package_other_than_the_current_one_has_a_frozen_step_count()} — every package except
 *   the one holding that highest number must have a frozen step count. This alone catches a step added to
 *   an older package: the global max would be unaffected by such a step, so the first check would stay
 *   green on its own.</li>
 * </ul>
 * When a new package supersedes the current one, bump {@code CurrentMigrationNumber.VALUE} and add a
 * frozen entry for the package that just closed, both in the same commit.
 */
class MigrationNumberingConsistencyTest {

  private static final Map<Class<? extends DbVersion>, Integer> FROZEN_STEP_COUNTS = Map.ofEntries(
    entry(DbVersion00.class, 2),
    entry(DbVersion202601.class, 5),
    entry(DbVersion202602.class, 2),
    entry(DbVersion202603.class, 8),
    entry(DbVersion202604.class, 22),
    entry(DbVersion202605.class, 91));

  @Test
  void VALUE_matches_the_highest_migration_number_actually_registered() throws ReflectiveOperationException {
    PackageSteps packageSteps = readAllPackageSteps();

    assertThat(packageSteps.highestMigrationNumber)
      .describedAs("CurrentMigrationNumber.VALUE must be bumped to match the migration number of the most recently "
        + "added migration step, across every DbVersion* package. If you just added a migration, update it. "
        + "If this fails after a rebase/merge without you having added a migration, someone else's migration landed "
        + "with a higher number than yours — move your migration above it.")
      .isEqualTo(CurrentMigrationNumber.VALUE);
  }

  @Test
  void every_package_other_than_the_current_one_has_a_frozen_step_count() throws ReflectiveOperationException {
    PackageSteps packageSteps = readAllPackageSteps();

    Map<Class<? extends DbVersion>, Integer> expectedNonCurrentCounts = new HashMap<>(packageSteps.countsByClass);
    expectedNonCurrentCounts.remove(packageSteps.currentClass);

    assertThat(FROZEN_STEP_COUNTS)
      .describedAs("FROZEN_STEP_COUNTS must have exactly one entry per package other than the current one (%s). "
        + "If you just created a new package, add a frozen entry for the package it superseded. "
        + "If this fails because you added a step to an already-frozen package, move it to the current package instead.",
        packageSteps.currentClass)
      .containsExactlyInAnyOrderEntriesOf(expectedNonCurrentCounts);
  }

  private static PackageSteps readAllPackageSteps() throws ReflectiveOperationException {
    ListContainer container = new ListContainer();
    new MigrationConfigurationModule().configure(container);

    Map<Class<? extends DbVersion>, Integer> countsByClass = new HashMap<>();
    long highestMigrationNumber = Long.MIN_VALUE;
    Class<? extends DbVersion> currentClass = null;

    for (Object addedObject : container.getAddedObjects()) {
      if (!(addedObject instanceof Class<?> clazz) || !DbVersion.class.isAssignableFrom(clazz)) {
        continue;
      }
      @SuppressWarnings("unchecked")
      Class<? extends DbVersion> dbVersionClass = (Class<? extends DbVersion>) clazz;
      DbVersion dbVersion = dbVersionClass.getDeclaredConstructor().newInstance();
      MigrationStepRegistryImpl registry = new MigrationStepRegistryImpl();
      dbVersion.addSteps(registry);
      var steps = registry.build().readAll();
      countsByClass.put(dbVersionClass, steps.size());

      long classMax = steps.stream().mapToLong(RegisteredMigrationStep::getMigrationNumber).max().orElseThrow();
      if (classMax > highestMigrationNumber) {
        highestMigrationNumber = classMax;
        currentClass = dbVersionClass;
      }
    }

    return new PackageSteps(countsByClass, highestMigrationNumber, currentClass);
  }

  private record PackageSteps(Map<Class<? extends DbVersion>, Integer> countsByClass, long highestMigrationNumber,
    Class<? extends DbVersion> currentClass) {
  }
}
