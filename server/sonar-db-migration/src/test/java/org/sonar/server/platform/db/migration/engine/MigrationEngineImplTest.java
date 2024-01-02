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

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.core.platform.SpringComponentContainer;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.history.MigrationHistory;
import org.sonar.server.platform.db.migration.step.MigrationStep;
import org.sonar.server.platform.db.migration.step.MigrationSteps;
import org.sonar.server.platform.db.migration.step.MigrationStepsExecutor;
import org.sonar.server.platform.db.migration.step.RegisteredMigrationStep;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MigrationEngineImplTest {
  private final MigrationHistory migrationHistory = mock(MigrationHistory.class);
  private final SpringComponentContainer serverContainer = new SpringComponentContainer();
  private final MigrationSteps migrationSteps = mock(MigrationSteps.class);
  private final StepRegistry stepRegistry = new StepRegistry();
  private final MapSettings settings = new MapSettings();
  private final MigrationEngineImpl underTest = new MigrationEngineImpl(migrationHistory, serverContainer, migrationSteps);

  @Before
  public void before() {
    serverContainer.add(migrationSteps);
    serverContainer.add(migrationHistory);
    serverContainer.add(stepRegistry);
    serverContainer.startComponents();
  }

  @Test
  public void execute_execute_all_steps_of_there_is_no_last_migration_number() {
    when(migrationHistory.getLastMigrationNumber()).thenReturn(Optional.empty());
    List<RegisteredMigrationStep> steps = singletonList(new RegisteredMigrationStep(1, "doo", TestMigrationStep.class));
    when(migrationSteps.readAll()).thenReturn(steps);

    underTest.execute();

    verify(migrationSteps, times(2)).readAll();
    assertThat(stepRegistry.stepRan).isTrue();
  }

  @Test
  public void execute_execute_steps_from_last_migration_number_plus_1() {
    when(migrationHistory.getLastMigrationNumber()).thenReturn(Optional.of(50L));
    List<RegisteredMigrationStep> steps = singletonList(new RegisteredMigrationStep(1, "doo", TestMigrationStep.class));
    when(migrationSteps.readFrom(51)).thenReturn(steps);
    when(migrationSteps.readAll()).thenReturn(steps);

    underTest.execute();

    verify(migrationSteps).readFrom(51);
    assertThat(stepRegistry.stepRan).isTrue();
  }

  private static class NoOpExecutor implements MigrationStepsExecutor {
    @Override
    public void execute(List<RegisteredMigrationStep> steps) {
      // no op
    }
  }

  private static class StepRegistry {
    boolean stepRan = false;
  }

  private static class TestMigrationStep implements MigrationStep {
    private final StepRegistry registry;

    public TestMigrationStep(StepRegistry registry) {
      this.registry = registry;
    }
    @Override
    public void execute() throws SQLException {
      registry.stepRan = true;
    }
  }

  @SupportsBlueGreen
  private static class TestBlueGreenMigrationStep implements MigrationStep {
    private final StepRegistry registry;

    public TestBlueGreenMigrationStep(StepRegistry registry) {
      this.registry = registry;
    }
    @Override
    public void execute() throws SQLException {
      registry.stepRan = true;
    }
  }
}
