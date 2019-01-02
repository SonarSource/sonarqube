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

import java.util.List;
import java.util.Optional;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.process.ProcessProperties;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.history.MigrationHistory;
import org.sonar.server.platform.db.migration.step.MigrationSteps;
import org.sonar.server.platform.db.migration.step.MigrationStepsExecutor;
import org.sonar.server.platform.db.migration.step.RegisteredMigrationStep;

import static java.lang.String.format;

public class MigrationEngineImpl implements MigrationEngine {
  private final MigrationHistory migrationHistory;
  private final ComponentContainer serverContainer;
  private final MigrationContainerPopulator populator;
  private final MigrationSteps migrationSteps;
  private final Configuration configuration;

  public MigrationEngineImpl(MigrationHistory migrationHistory, ComponentContainer serverContainer,
    MigrationContainerPopulator populator, MigrationSteps migrationSteps, Configuration configuration) {
    this.migrationHistory = migrationHistory;
    this.serverContainer = serverContainer;
    this.populator = populator;
    this.migrationSteps = migrationSteps;
    this.configuration = configuration;
  }

  @Override
  public void execute() {
    MigrationContainer migrationContainer = new MigrationContainerImpl(serverContainer, populator);
    boolean blueGreen = configuration.getBoolean(ProcessProperties.Property.BLUE_GREEN_ENABLED.getKey()).orElse(false);
    try {
      MigrationStepsExecutor stepsExecutor = migrationContainer.getComponentByType(MigrationStepsExecutor.class);
      Optional<Long> lastMigrationNumber = migrationHistory.getLastMigrationNumber();

      List<RegisteredMigrationStep> steps = lastMigrationNumber
        .map(i -> migrationSteps.readFrom(i + 1))
        .orElse(migrationSteps.readAll());

      if (blueGreen) {
        ensureSupportBlueGreen(steps);
      }

      stepsExecutor.execute(steps);

    } finally {
      migrationContainer.cleanup();
    }
  }

  private static void ensureSupportBlueGreen(List<RegisteredMigrationStep> steps) {
    for (RegisteredMigrationStep step : steps) {
      if (AnnotationUtils.getAnnotation(step.getStepClass(), SupportsBlueGreen.class) == null) {
        throw new IllegalStateException(format("All migrations canceled. #%d does not support blue/green deployment: %s",
          step.getMigrationNumber(), step.getDescription()));
      }
    }
  }
}
