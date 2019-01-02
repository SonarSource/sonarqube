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
package org.sonar.server.platform.db.migration.step;

import java.util.List;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.logs.Profiler;
import org.sonar.server.platform.db.migration.engine.MigrationContainer;
import org.sonar.server.platform.db.migration.history.MigrationHistory;

import static com.google.common.base.Preconditions.checkState;

public class MigrationStepsExecutorImpl implements MigrationStepsExecutor {
  private static final Logger LOGGER = Loggers.get("DbMigrations");
  private static final String GLOBAL_START_MESSAGE = "Executing DB migrations...";
  private static final String GLOBAL_END_MESSAGE = "Executed DB migrations: {}";
  private static final String STEP_START_PATTERN = "{}...";
  private static final String STEP_STOP_PATTERN = "{}: {}";

  private final MigrationContainer migrationContainer;
  private final MigrationHistory migrationHistory;

  public MigrationStepsExecutorImpl(MigrationContainer migrationContainer, MigrationHistory migrationHistory) {
    this.migrationContainer = migrationContainer;
    this.migrationHistory = migrationHistory;
  }

  @Override
  public void execute(List<RegisteredMigrationStep> steps) {
    Profiler globalProfiler = Profiler.create(LOGGER);
    globalProfiler.startInfo(GLOBAL_START_MESSAGE);
    boolean allStepsExecuted = false;
    try {
      steps.forEach(this::execute);
      allStepsExecuted = true;
    } finally {
      if (allStepsExecuted) {
        globalProfiler.stopInfo(GLOBAL_END_MESSAGE, "success");
      } else {
        globalProfiler.stopError(GLOBAL_END_MESSAGE, "failure");
      }
    }
  }

  private void execute(RegisteredMigrationStep step) {
    MigrationStep migrationStep = migrationContainer.getComponentByType(step.getStepClass());
    checkState(migrationStep != null, "Can not find instance of " + step.getStepClass());

    execute(step, migrationStep);
  }

  private void execute(RegisteredMigrationStep step, MigrationStep migrationStep) {
    Profiler stepProfiler = Profiler.create(LOGGER);
    stepProfiler.startInfo(STEP_START_PATTERN, step);
    boolean done = false;
    try {
      migrationStep.execute();
      migrationHistory.done(step);
      done = true;
    } catch (Exception e) {
      throw new MigrationStepExecutionException(step, e);
    } finally {
      if (done) {
        stepProfiler.stopInfo(STEP_STOP_PATTERN, step, "success");
      } else {
        stepProfiler.stopError(STEP_STOP_PATTERN, step, "failure");
      }
    }
  }
}
