/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.stream.Stream;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.logs.Profiler;
import org.sonar.server.platform.db.migration.engine.MigrationContainer;
import org.sonar.server.platform.db.migration.history.MigrationHistory;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

public class MigrationStepsExecutorImpl implements MigrationStepsExecutor {
  private static final Logger LOG = Loggers.get(MigrationStepsExecutorImpl.class);
  private static final String STEP_LOG_PATTERN = "=== {} - '{}'";

  private final MigrationContainer migrationContainer;
  private final MigrationHistory migrationHistory;

  public MigrationStepsExecutorImpl(MigrationContainer migrationContainer, MigrationHistory migrationHistory) {
    this.migrationContainer = migrationContainer;
    this.migrationHistory = migrationHistory;
  }

  @Override
  public void execute(Stream<RegisteredMigrationStep> steps) {
    LOG.info("Executing migrations...");
    try {
      steps.forEachOrdered(this::execute);
    } finally {
      LOG.info("Executing migrations done");
    }
  }

  private void execute(RegisteredMigrationStep step) {
    MigrationStep migrationStep = migrationContainer.getComponentByType(step.getStepClass());
    checkState(migrationStep != null, "Can not find instance of " + step.getStepClass());

    execute(step, migrationStep);
  }

  private void execute(RegisteredMigrationStep step, MigrationStep migrationStep) {
    Profiler stepProfiler = Profiler.create(LOG);
    stepProfiler.startInfo(STEP_LOG_PATTERN, step.getMigrationNumber(), step.getDescription());
    try {
      migrationStep.execute();
      migrationHistory.done(step);
      stepProfiler.stopInfo(STEP_LOG_PATTERN, step.getMigrationNumber(), step.getDescription());
    } catch (Throwable e) {
      stepProfiler.stopError(STEP_LOG_PATTERN, step.getMigrationNumber(), step.getDescription());
      LOG.error(format("Migration %s - '%s' failed", step.getMigrationNumber(), step.getDescription()), e);
      throw new MigrationStepExecutionException(step, e);
    }
  }
}
