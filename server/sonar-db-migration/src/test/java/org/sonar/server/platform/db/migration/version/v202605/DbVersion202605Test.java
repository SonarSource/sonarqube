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
package org.sonar.server.platform.db.migration.version.v202605;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.server.platform.db.migration.step.MigrationStepRegistryImpl;
import org.sonar.server.platform.db.migration.step.RegisteredMigrationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.server.platform.db.migration.version.DbVersionTestUtils.verifyMigrationNotEmpty;
import static org.sonar.server.platform.db.migration.version.DbVersionTestUtils.verifyMinimumMigrationNumber;

class DbVersion202605Test {

  private final DbVersion202605 underTest = new DbVersion202605();

  @Test
  void migrationNumber_starts_at_2026_05_000() {
    verifyMinimumMigrationNumber(underTest, 202605000);
  }

  @Test
  void verify_migration_is_not_empty() {
    verifyMigrationNotEmpty(underTest);
  }

  @Test
  void historyBackfillSchemaAndDataSteps_areOrdered() {
    MigrationStepRegistryImpl registry = new MigrationStepRegistryImpl();
    underTest.addSteps(registry);

    List<RegisteredMigrationStep> historyBackfillSteps = registry.build().readAll().stream()
      .filter(step -> step.getStepClass() == AlterMeasureHistoryTextValueToClob.class
        || step.getStepClass() == IncreaseIssueCountDimensionsRuleKeyColumnSize.class
        || step.getStepClass() == PersistHistoryBackfillUtcDayEpoch.class
         || step.getStepClass() == BackfillProjectBranchHistory.class)
      .toList();

    assertThat(historyBackfillSteps)
      .extracting(RegisteredMigrationStep::getMigrationNumber, RegisteredMigrationStep::getStepClass)
      .containsExactly(
         tuple(2026_05_084L, AlterMeasureHistoryTextValueToClob.class),
         tuple(2026_05_085L, IncreaseIssueCountDimensionsRuleKeyColumnSize.class),
         tuple(2026_05_086L, PersistHistoryBackfillUtcDayEpoch.class),
         tuple(2026_05_087L, BackfillProjectBranchHistory.class));
    assertThat(historyBackfillSteps.getFirst().getMigrationNumber()).isGreaterThan(2026_05_081L);
  }
}
