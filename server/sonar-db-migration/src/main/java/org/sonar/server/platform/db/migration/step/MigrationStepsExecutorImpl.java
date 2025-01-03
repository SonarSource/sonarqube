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
package org.sonar.server.platform.db.migration.step;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.Container;
import org.sonar.core.util.logs.Profiler;
import org.sonar.server.platform.db.migration.DatabaseMigrationLoggerContext;
import org.sonar.server.platform.db.migration.MutableDatabaseMigrationState;
import org.sonar.server.platform.db.migration.history.MigrationHistory;

import static com.google.common.base.Preconditions.checkState;

public class MigrationStepsExecutorImpl implements MigrationStepsExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger("DbMigrations");
  private static final String GLOBAL_START_MESSAGE = "Executing {} DB migrations...";
  private static final String GLOBAL_END_MESSAGE = "Executed {}/{} DB migrations: {}";
  private static final String STEP_START_PATTERN = "{}/{} {}...";
  private static final String STEP_STOP_PATTERN = "{}/{} {}: {}";

  private final Container migrationContainer;
  private final MigrationHistory migrationHistory;
  private final MutableDatabaseMigrationState databaseMigrationState;
  private final DatabaseMigrationLoggerContext databaseMigrationLoggerContext;
  private final Server server;
  private final System2 system2;

  public MigrationStepsExecutorImpl(Container migrationContainer, MigrationHistory migrationHistory, MutableDatabaseMigrationState databaseMigrationState,
    DatabaseMigrationLoggerContext databaseMigrationLoggerContext, Server server, System2 system2) {
    this.migrationContainer = migrationContainer;
    this.migrationHistory = migrationHistory;
    this.databaseMigrationState = databaseMigrationState;
    this.databaseMigrationLoggerContext = databaseMigrationLoggerContext;
    this.server = server;
    this.system2 = system2;
  }

  @Override
  public void execute(List<RegisteredMigrationStep> steps, MigrationStatusListener listener) {
    Profiler globalProfiler = Profiler.create(LOGGER);
    globalProfiler.startInfo(GLOBAL_START_MESSAGE, databaseMigrationState.getTotalMigrations());
    boolean allStepsExecuted = false;
    try {
      for (RegisteredMigrationStep step : steps) {
        this.execute(step);
        listener.onMigrationStepCompleted();
      }
      allStepsExecuted = true;
    } finally {
      if (allStepsExecuted) {
        globalProfiler.stopInfo(GLOBAL_END_MESSAGE,
          databaseMigrationState.getCompletedMigrations(),
          databaseMigrationState.getTotalMigrations(),
          "success");
      } else {
        globalProfiler.stopError(GLOBAL_END_MESSAGE,
          databaseMigrationState.getCompletedMigrations(),
          databaseMigrationState.getTotalMigrations(),
          "failure");
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
    stepProfiler.startInfo(STEP_START_PATTERN,
      databaseMigrationState.getCompletedMigrations() + 1,
      databaseMigrationState.getTotalMigrations(),
      step);
    boolean done = false;
    long timeStarted = system2.now();
    try {
      migrationStep.execute();
      migrationHistory.done(step);
      done = true;
    } catch (Exception e) {
      throw new MigrationStepExecutionException(step, e);
    } finally {
      long durationInMiliseconds = 0L;
      if (done) {
        durationInMiliseconds = stepProfiler.stopInfo(STEP_STOP_PATTERN,
          databaseMigrationState.getCompletedMigrations() + 1,
          databaseMigrationState.getTotalMigrations(),
          step,
          "success");
      } else {
        stepProfiler.stopError(STEP_STOP_PATTERN,
          databaseMigrationState.getCompletedMigrations() + 1,
          databaseMigrationState.getTotalMigrations(),
          step,
          "failure");
      }

      databaseMigrationLoggerContext.addMigrationData(
        String.valueOf(step.getMigrationNumber()),
        durationInMiliseconds,
        done,
        timeStarted,
        server.getVersion()
      );
    }
  }
}
