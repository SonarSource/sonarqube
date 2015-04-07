/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.db.migrations;

import org.sonar.api.utils.log.Loggers;
import org.sonar.server.ruby.RubyBridge;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handles concurrency to make sure only one DB migration can run at a time.
 */
public class PlatformDatabaseMigration implements DatabaseMigration {

  private final RubyBridge rubyBridge;
  /**
   * ExecutorService implements threads management.
   */
  private final PlatformDatabaseMigrationExecutorService executorService;
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
  private AtomicBoolean running = new AtomicBoolean(false);
  private Status status = Status.NONE;
  @Nullable
  private Date startDate;
  @Nullable
  private Throwable failureError;

  public PlatformDatabaseMigration(RubyBridge rubyBridge, PlatformDatabaseMigrationExecutorService executorService) {
    this.rubyBridge = rubyBridge;
    this.executorService = executorService;
  }

  @Override
  public void startIt() {
    if (lock.isLocked() || this.running.get() /* fail-fast if db migration is running */) {
      return;
    }

    lock.lock();
    try {
      startAsynchronousDBMigration();
    } finally {
      lock.unlock();
    }
  }

  /**
   * This method is not thread safe and must be external protected from concurrent executions.
   */
  private void startAsynchronousDBMigration() {
    if (this.running.get()) {
      return;
    }

    running.getAndSet(true);
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        status = Status.RUNNING;
        startDate = new Date();
        failureError = null;
        try {
          Loggers.get(PlatformDatabaseMigration.class).info("Starting DB Migration at {}", startDate);
          rubyBridge.databaseMigration().trigger();
          Loggers.get(PlatformDatabaseMigration.class).info("DB Migration ended successfully at {}", new Date());
          status = Status.SUCCEEDED;
        } catch (Throwable t) {
          Loggers.get(PlatformDatabaseMigration.class).error("DB Migration failed and ended at " + startDate + " with an exception", t);
          status = Status.FAILED;
          failureError = t;
        } finally {
          running.getAndSet(false);
        }
      }
    });
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
