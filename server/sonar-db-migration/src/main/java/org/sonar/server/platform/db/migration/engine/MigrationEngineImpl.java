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

import java.util.Optional;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.platform.db.migration.history.MigrationHistory;
import org.sonar.server.platform.db.migration.step.MigrationSteps;
import org.sonar.server.platform.db.migration.step.MigrationStepsExecutor;

public class MigrationEngineImpl implements MigrationEngine {
  private final MigrationHistory migrationHistory;
  private final ComponentContainer serverContainer;
  private final MigrationContainerPopulator populator;
  private final MigrationSteps migrationSteps;

  public MigrationEngineImpl(MigrationHistory migrationHistory, ComponentContainer serverContainer,
    MigrationContainerPopulator populator, MigrationSteps migrationSteps) {
    this.migrationHistory = migrationHistory;
    this.serverContainer = serverContainer;
    this.populator = populator;
    this.migrationSteps = migrationSteps;
  }

  @Override
  public void execute() {
    MigrationContainer migrationContainer = new MigrationContainerImpl(serverContainer, populator);

    try {
      MigrationStepsExecutor stepsExecutor = migrationContainer.getComponentByType(MigrationStepsExecutor.class);
      Optional<Long> lastMigrationNumber = migrationHistory.getLastMigrationNumber();
      if (lastMigrationNumber.isPresent()) {
        stepsExecutor.execute(migrationSteps.readFrom(lastMigrationNumber.get() + 1));
      } else {
        stepsExecutor.execute(migrationSteps.readAll());
      }
    } finally {
      migrationContainer.cleanup();
    }
  }
}
