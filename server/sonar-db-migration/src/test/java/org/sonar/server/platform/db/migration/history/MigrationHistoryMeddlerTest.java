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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.server.platform.db.migration.step.MigrationStep;
import org.sonar.server.platform.db.migration.step.MigrationSteps;
import org.sonar.server.platform.db.migration.step.RegisteredMigrationStep;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class MigrationHistoryMeddlerTest {
  private static final long OLD_VERSION_70_LAST_MIGRATION_NUMBER = 1_923L;
  private static final long NEW_VERSION_70_LAST_MIGRATION_NUMBER = 1_959L;

  private MigrationSteps migrationSteps = mock(MigrationSteps.class);
  private MigrationHistory migrationHistory = mock(MigrationHistory.class);
  private MigrationHistoryMeddler underTest = new MigrationHistoryMeddler(migrationSteps);

  @Test
  public void no_effect_if_no_last_migration_number() {
    when(migrationHistory.getLastMigrationNumber()).thenReturn(Optional.empty());

    underTest.meddle(migrationHistory);

    verify(migrationHistory).getLastMigrationNumber();
    verifyNoMoreInteractions(migrationHistory, migrationSteps);
  }

  @Test
  @UseDataProvider("non_old_70_last_migration_number")
  public void no_history_meddling_if_last_migration_number_is_not_old_70_last_migration_number(long lastMigrationNumber) {
    when(migrationHistory.getLastMigrationNumber()).thenReturn(Optional.of(lastMigrationNumber));

    underTest.meddle(migrationHistory);

    verify(migrationHistory).getLastMigrationNumber();
    verifyNoMoreInteractions(migrationHistory, migrationSteps);
  }

  @Test
  public void update_last_migration_number_if_last_migration_number_is_old_70_last_migration_number() {
    verifyUpdateLastMigrationNumber(OLD_VERSION_70_LAST_MIGRATION_NUMBER, NEW_VERSION_70_LAST_MIGRATION_NUMBER);
  }

  public void verifyUpdateLastMigrationNumber(long oldVersion, long expectedNewVersion) {
    when(migrationHistory.getLastMigrationNumber()).thenReturn(Optional.of(oldVersion));
    List<RegisteredMigrationStep> stepsFromNewLastMigrationNumber = IntStream.range(0, 1 + new Random().nextInt(30))
      .mapToObj(i -> new RegisteredMigrationStep(i, "desc_" + i, MigrationStep.class))
      .collect(Collectors.toList());
    when(migrationSteps.readFrom(expectedNewVersion)).thenReturn(stepsFromNewLastMigrationNumber);

    underTest.meddle(migrationHistory);

    verify(migrationHistory).getLastMigrationNumber();
    verify(migrationSteps).readFrom(expectedNewVersion);
    verify(migrationHistory).done(stepsFromNewLastMigrationNumber.get(0));
    verifyNoMoreInteractions(migrationHistory, migrationSteps);
  }

  @DataProvider
  public static Object[][] non_old_70_last_migration_number() {
    return new Object[][] {
      {1L},
      {OLD_VERSION_70_LAST_MIGRATION_NUMBER - 1 - new Random().nextInt(12)},
      {OLD_VERSION_70_LAST_MIGRATION_NUMBER + 1 + new Random().nextInt(12)}
    };
  }

}
