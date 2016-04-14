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
package org.sonar.server.db.migrations;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.db.version.DatabaseMigration;
import org.sonar.server.platform.Platform;
import org.sonar.server.ruby.RubyBridge;

/**
 * Handles concurrency to make sure only one DB migration can run at a time.
 */
public class PlatformDatabaseMigration implements DatabaseMigration {

  private static final Logger LOGGER = Loggers.get(PlatformDatabaseMigration.class);

  private final RubyBridge rubyBridge;
  /**
   * ExecutorService implements threads management.
   */
  private final PlatformDatabaseMigrationExecutorService executorService;
  private final Platform platform;
  /**
   * This lock implements thread safety from concurrent calls of method {@link #startIt()}
   */
  private final ReentrantLock lock = new ReentrantLock();

  /**
   * This property acts as a semaphore to make sure at most one db migration task is created at a time.
   * <p>
   * It is set to {@code true} by the first thread to execute the {@link #startIt()} method and set to {@code false}
   * by the thread executing the db migration.
   * </p>
   */
  private final AtomicBoolean running = new AtomicBoolean(false);
  private Status status = Status.NONE;
  @Nullable
  private Date startDate;
  @Nullable
  private Throwable failureError;

  public PlatformDatabaseMigration(RubyBridge rubyBridge,
    PlatformDatabaseMigrationExecutorService executorService, Platform platform) {
    this.rubyBridge = rubyBridge;
    this.executorService = executorService;
    this.platform = platform;
  }

  @Override
  public void startIt() {
    if (lock.isLocked() || this.running.get()) {
      LOGGER.trace("{}: lock is already taken or process is already running", Thread.currentThread().getName());
      return;
    }

    if (lock.tryLock()) {
      try {
        startAsynchronousDBMigration();
      } finally {
        lock.unlock();
      }
    }
  }

  /**
   * This method is not thread safe and must be externally protected from concurrent executions.
   */
  private void startAsynchronousDBMigration() {
    if (this.running.get()) {
      return;
    }

    running.set(true);
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        doDatabaseMigration();
      }
    });
  }

  private void doDatabaseMigration() {
    status = Status.RUNNING;
    startDate = new Date();
    failureError = null;
    Profiler profiler = Profiler.create(LOGGER);
    try {
      profiler.startInfo("Starting DB Migration");
      doUpgradeDb();
      doRestartContainer();
      doRecreateWebRoutes();
      status = Status.SUCCEEDED;
      profiler.stopInfo("DB Migration ended successfully");
    } catch (Throwable t) {
      profiler.stopInfo("DB migration failed");
      LOGGER.error("DB Migration or container restart failed. Process ended with an exception", t);
      status = Status.FAILED;
      failureError = t;
    } finally {
      running.getAndSet(false);
    }
  }

  private void doUpgradeDb() {
    Profiler profiler = Profiler.createIfTrace(LOGGER);
    profiler.startTrace("Starting DB Migration");
    rubyBridge.databaseMigration().trigger();
    profiler.stopTrace("DB Migration ended");
  }

  private void doRestartContainer() {
    Profiler profiler = Profiler.createIfTrace(LOGGER);
    profiler.startTrace("Restarting container");
    platform.doStart();
    profiler.stopTrace("Container restarted successfully");
  }

  private void doRecreateWebRoutes() {
    Profiler profiler = Profiler.createIfTrace(LOGGER);
    profiler.startTrace("Recreating web routes");
    rubyBridge.railsRoutes().recreate();
    profiler.startTrace("Routes recreated successfully");
  }

  @Override
  @CheckForNull
  public Date startedAt() {
    return this.startDate;
  }

  @Override
  public Status status() {
    return this.status;
  }

  @Override
  @CheckForNull
  public Throwable failureError() {
    return this.failureError;
  }
}
