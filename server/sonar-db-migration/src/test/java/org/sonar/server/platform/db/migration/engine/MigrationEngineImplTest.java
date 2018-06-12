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
package org.sonar.server.platform.db.migration.engine;

import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.platform.db.migration.history.MigrationHistory;
import org.sonar.server.platform.db.migration.step.MigrationStep;
import org.sonar.server.platform.db.migration.step.MigrationSteps;
import org.sonar.server.platform.db.migration.step.MigrationStepsExecutor;
import org.sonar.server.platform.db.migration.step.RegisteredMigrationStep;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

  private Configuration configuration;
  private MigrationEngineImpl underTest = new MigrationEngineImpl(migrationHistory, serverContainer, populator, migrationSteps, configuration);

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

}
