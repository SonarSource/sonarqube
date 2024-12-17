/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.engine;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.core.platform.SpringComponentContainer;
import org.sonar.server.platform.db.migration.MutableDatabaseMigrationState;
import org.sonar.server.platform.db.migration.history.MigrationHistory;
import org.sonar.server.platform.db.migration.step.MigrationStep;
import org.sonar.server.platform.db.migration.step.MigrationSteps;
import org.sonar.server.platform.db.migration.step.NoOpMigrationStatusListener;
import org.sonar.server.platform.db.migration.step.RegisteredMigrationStep;
import org.sonar.server.telemetry.TelemetryDbMigrationStepDurationProvider;
import org.sonar.server.telemetry.TelemetryDbMigrationStepsProvider;
import org.sonar.server.telemetry.TelemetryDbMigrationSuccessProvider;
import org.sonar.server.telemetry.TelemetryDbMigrationTotalTimeProvider;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MigrationEngineImplTest {
  private final MigrationHistory migrationHistory = mock(MigrationHistory.class);
  private final MutableDatabaseMigrationState databaseMigrationState = mock();
  private final SpringComponentContainer serverContainer = new SpringComponentContainer();
  private final MigrationSteps migrationSteps = mock(MigrationSteps.class);
  private final StepRegistry stepRegistry = new StepRegistry();
  private final TelemetryDbMigrationTotalTimeProvider telemetryDbMigrationTotalTimeProvider = new TelemetryDbMigrationTotalTimeProvider();
  private final TelemetryDbMigrationStepsProvider telemetryUpgradeStepsProvider = new TelemetryDbMigrationStepsProvider();
  private final TelemetryDbMigrationSuccessProvider telemetryDbMigrationSuccessProvider = new TelemetryDbMigrationSuccessProvider();
  private final TelemetryDbMigrationStepDurationProvider telemetryDbMigrationStepDurationProvider = new TelemetryDbMigrationStepDurationProvider();
  private final MigrationEngineImpl underTest = new MigrationEngineImpl(migrationHistory, serverContainer, migrationSteps);

  @BeforeEach
  void before() {
    serverContainer.add(telemetryDbMigrationTotalTimeProvider);
    serverContainer.add(telemetryUpgradeStepsProvider);
    serverContainer.add(telemetryDbMigrationSuccessProvider);
    serverContainer.add(migrationSteps);
    serverContainer.add(migrationHistory);
    serverContainer.add(stepRegistry);
    serverContainer.add(databaseMigrationState);
    serverContainer.add(telemetryDbMigrationStepDurationProvider);
    serverContainer.startComponents();
  }

  @Test
  void execute_execute_all_steps_of_there_is_no_last_migration_number() {
    when(migrationHistory.getLastMigrationNumber()).thenReturn(Optional.empty());
    List<RegisteredMigrationStep> steps = singletonList(new RegisteredMigrationStep(1, "doo", TestMigrationStep.class));
    when(migrationSteps.readAll()).thenReturn(steps);

    underTest.execute(new NoOpMigrationStatusListener());

    verify(migrationSteps, times(2)).readAll();
    assertThat(stepRegistry.stepRan).isTrue();
  }

  @Test
  void execute_execute_steps_from_last_migration_number_plus_1() {
    when(migrationHistory.getLastMigrationNumber()).thenReturn(Optional.of(50L));
    List<RegisteredMigrationStep> steps = singletonList(new RegisteredMigrationStep(1, "doo", TestMigrationStep.class));
    when(migrationSteps.readFrom(51)).thenReturn(steps);
    when(migrationSteps.readAll()).thenReturn(steps);

    underTest.execute(new NoOpMigrationStatusListener());

    verify(migrationSteps).readFrom(51);
    assertThat(stepRegistry.stepRan).isTrue();
  }

  private static class StepRegistry {
    boolean stepRan = false;
  }

  private record TestMigrationStep(StepRegistry registry) implements MigrationStep {

    @Override
    public void execute() {
      registry.stepRan = true;
    }
  }
}
