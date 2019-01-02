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
package org.sonar.server.platform.db.migration.engine;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.sonar.api.config.internal.ConfigurationBridge;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.process.ProcessProperties;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.history.MigrationHistory;
import org.sonar.server.platform.db.migration.step.MigrationStep;
import org.sonar.server.platform.db.migration.step.MigrationSteps;
import org.sonar.server.platform.db.migration.step.MigrationStepsExecutor;
import org.sonar.server.platform.db.migration.step.RegisteredMigrationStep;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class MigrationEngineImplTest {
  private MigrationHistory migrationHistory = mock(MigrationHistory.class);
  private ComponentContainer serverContainer = new ComponentContainer();
  private MigrationStepsExecutor stepsExecutor = mock(MigrationStepsExecutor.class);
  private MigrationContainerPopulator populator = new MigrationContainerPopulator() {
    @Override
    public void populateContainer(MigrationContainer container) {
      container.add(stepsExecutor);
    }
  };
  private MigrationSteps migrationSteps = mock(MigrationSteps.class);

  private MapSettings settings = new MapSettings();
  private MigrationEngineImpl underTest = new MigrationEngineImpl(migrationHistory, serverContainer, populator, migrationSteps, new ConfigurationBridge(settings));

  @Test
  public void execute_execute_all_steps_of_there_is_no_last_migration_number() {
    when(migrationHistory.getLastMigrationNumber()).thenReturn(Optional.empty());
    List<RegisteredMigrationStep> steps = singletonList(new RegisteredMigrationStep(1, "doo", MigrationStep.class));
    when(migrationSteps.readAll()).thenReturn(steps);

    underTest.execute();

    verify(migrationSteps).readAll();
    verify(stepsExecutor).execute(steps);
  }

  @Test
  public void execute_execute_steps_from_last_migration_number_plus_1() {
    when(migrationHistory.getLastMigrationNumber()).thenReturn(Optional.of(50L));
    List<RegisteredMigrationStep> steps = singletonList(new RegisteredMigrationStep(1, "doo", MigrationStep.class));
    when(migrationSteps.readFrom(51)).thenReturn(steps);

    underTest.execute();

    verify(migrationSteps).readFrom(51);
    verify(stepsExecutor).execute(steps);
  }

  @Test
  public void execute_steps_in_blue_green_mode() {
    settings.setProperty(ProcessProperties.Property.BLUE_GREEN_ENABLED.getKey(), true);
    when(migrationHistory.getLastMigrationNumber()).thenReturn(Optional.of(50L));
    List<RegisteredMigrationStep> steps = singletonList(new RegisteredMigrationStep(1, "doo", TestBlueGreenMigrationStep.class));
    when(migrationSteps.readFrom(51)).thenReturn(steps);

    underTest.execute();

    verify(migrationSteps).readFrom(51);
    verify(stepsExecutor).execute(steps);
  }

  @Test
  public void fail_blue_green_execution_if_some_migrations_are_not_compatible() {
    settings.setProperty(ProcessProperties.Property.BLUE_GREEN_ENABLED.getKey(), true);
    when(migrationHistory.getLastMigrationNumber()).thenReturn(Optional.of(50L));
    List<RegisteredMigrationStep> steps = asList(
      new RegisteredMigrationStep(1, "foo", TestBlueGreenMigrationStep.class),
      new RegisteredMigrationStep(2, "bar", MigrationStep.class));
    when(migrationSteps.readFrom(51)).thenReturn(steps);

    try {
      underTest.execute();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("All migrations canceled. #2 does not support blue/green deployment: bar");
      verifyZeroInteractions(stepsExecutor);
    }
  }

  @SupportsBlueGreen
  private static class TestBlueGreenMigrationStep implements MigrationStep {

    @Override
    public void execute() throws SQLException {

    }
  }
}
