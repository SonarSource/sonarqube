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
package org.sonar.server.platform.db.migration;

import java.util.Date;
import java.util.concurrent.Semaphore;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.logs.Profiler;
import org.sonar.server.platform.Platform;
import org.sonar.server.platform.db.migration.DatabaseMigrationState.Status;
import org.sonar.server.platform.db.migration.engine.MigrationEngine;
import org.sonar.server.platform.db.migration.step.MigrationStepExecutionException;

/**
 * Handles concurrency to make sure only one DB migration can run at a time.
 */
public class DatabaseMigrationImpl implements DatabaseMigration {

  private static final Logger LOGGER = Loggers.get(DatabaseMigrationImpl.class);

  /**
   * ExecutorService implements threads management.
   */
  private final DatabaseMigrationExecutorService executorService;
  private final MigrationEngine migrationEngine;
  private final Platform platform;
  private final MutableDatabaseMigrationState migrationState;
  /**
   * This semaphore implements thread safety from concurrent calls of method {@link #startIt()}
   */
  private final Semaphore semaphore = new Semaphore(1);

  public DatabaseMigrationImpl(DatabaseMigrationExecutorService executorService, MutableDatabaseMigrationState migrationState,
    MigrationEngine migrationEngine, Platform platform) {
    this.executorService = executorService;
    this.migrationState = migrationState;
    this.migrationEngine = migrationEngine;
    this.platform = platform;
  }

  @Override
  public void startIt() {
    if (semaphore.tryAcquire()) {
      try {
        executorService.execute(this::doDatabaseMigration);
      } catch (RuntimeException e) {
        semaphore.release();
        throw e;
      }
    } else {
      LOGGER.trace("{}: lock is already taken or process is already running", Thread.currentThread().getName());
    }
  }

  private void doDatabaseMigration() {
    migrationState.setStatus(Status.RUNNING);
    migrationState.setStartedAt(new Date());
    migrationState.setError(null);
    Profiler profiler = Profiler.create(LOGGER);
    try {
      profiler.startInfo("Starting DB Migration and container restart");
      doUpgradeDb();
      doRestartContainer();
      migrationState.setStatus(Status.SUCCEEDED);
      profiler.stopInfo("DB Migration and container restart: success");
    } catch (MigrationStepExecutionException e) {
      profiler.stopError("DB migration failed");
      LOGGER.error("DB migration ended with an exception", e);
      saveStatus(e);
    } catch (Throwable t) {
      profiler.stopError("Container restart failed");
      LOGGER.error("Container restart failed", t);
      saveStatus(t);
    } finally {
      semaphore.release();
    }
  }

  private void saveStatus(Throwable e) {
    migrationState.setStatus(Status.FAILED);
    migrationState.setError(e);
  }

  private void doUpgradeDb() {
    Profiler profiler = Profiler.createIfTrace(LOGGER);
    profiler.startTrace("Starting DB Migration");
    migrationEngine.execute();
    profiler.stopTrace("DB Migration ended");
  }

  private void doRestartContainer() {
    Profiler profiler = Profiler.createIfTrace(LOGGER);
    profiler.startTrace("Restarting container");
    platform.doStart();
    profiler.stopTrace("Container restarted successfully");
  }

}
