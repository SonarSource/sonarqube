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
import org.sonar.core.platform.SpringComponentContainer;
import org.sonar.server.platform.db.migration.history.MigrationHistory;
import org.sonar.server.platform.db.migration.step.MigrationSteps;
import org.sonar.server.platform.db.migration.step.MigrationStepsExecutor;
import org.sonar.server.platform.db.migration.step.MigrationStepsExecutorImpl;
import org.sonar.server.platform.db.migration.step.RegisteredMigrationStep;

public class MigrationEngineImpl implements MigrationEngine {
  private final MigrationHistory migrationHistory;
  private final SpringComponentContainer serverContainer;
  private final MigrationSteps migrationSteps;

  public MigrationEngineImpl(MigrationHistory migrationHistory, SpringComponentContainer serverContainer, MigrationSteps migrationSteps) {
    this.migrationHistory = migrationHistory;
    this.serverContainer = serverContainer;
    this.migrationSteps = migrationSteps;
  }

  @Override
  public void execute() {
    MigrationContainer migrationContainer = new MigrationContainerImpl(serverContainer, MigrationStepsExecutorImpl.class);
    try {
      MigrationStepsExecutor stepsExecutor = migrationContainer.getComponentByType(MigrationStepsExecutor.class);
      Optional<Long> lastMigrationNumber = migrationHistory.getLastMigrationNumber();

      List<RegisteredMigrationStep> steps = lastMigrationNumber
        .map(i -> migrationSteps.readFrom(i + 1))
        .orElse(migrationSteps.readAll());


      stepsExecutor.execute(steps);

    } finally {
      migrationContainer.cleanup();
    }
  }
}
