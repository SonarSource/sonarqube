/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.core.util;

import com.google.common.annotations.VisibleForTesting;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * Background thread that logs the state of a counter at fixed intervals.
 */
public class ProgressLogger {

  private static final long MILLIS_PER_SECOND = 1000L;
  public static final long DEFAULT_PERIOD_MS = 60L * MILLIS_PER_SECOND;

  /** We suppose, that the counter starts with zero. */
  private static final long START_COUNTER = 0L;

  private final Timer timer;
  private final LoggerTimerTask task;
  private long periodMs = DEFAULT_PERIOD_MS;
  private boolean hasAlreadyCausedLogging = false;
  private final AtomicLong counter;
  private final Logger logger;
  private String pluralLabel = "rows";
  private long startTime;
  private System2 system;

  public ProgressLogger(String threadName, AtomicLong counter, Logger logger) {
    this(threadName, counter, logger, System2.INSTANCE);
  }

  @VisibleForTesting
  ProgressLogger(String threadName, AtomicLong counter, Logger logger, System2 system) {
    this.counter = counter;
    this.logger = logger;
    this.system = system;
    this.timer = new Timer(threadName);
    this.task = new LoggerTimerTask();
  }

  public static ProgressLogger create(Class<?> clazz, AtomicLong counter) {
    String threadName = String.format("ProgressLogger[%s]", clazz.getSimpleName());
    Logger logger = Loggers.get(clazz);
    return new ProgressLogger(threadName, counter, logger);
  }

  /**
   * Warning, does not check if already started.
   */
  public void start() {
    startTime = now();
    // first log after {periodMs} milliseconds
    timer.schedule(task, periodMs, periodMs);
  }

  public void stop() {
    timer.cancel();
    timer.purge();
    if (hasAlreadyCausedLogging) {
      finalLog();
    }
  }

  private void finalLog() {
    long currentCounter = counter.get();
    long currentTime = now();

    long deltaCounter = currentCounter - START_COUNTER;
    long deltaTime = currentTime - startTime;

    long speed;
    if (deltaTime == 0) {
      speed = 0;
    } else {
      speed = MILLIS_PER_SECOND * deltaCounter / deltaTime;
    }

    logger.info(String.format("The task is done. %d %s processed in %d seconds. (%d items/sec)", currentCounter, pluralLabel, deltaTime / MILLIS_PER_SECOND, speed));
  }

  /**
   * Default is 1 minute
   */
  public ProgressLogger setPeriodMs(long l) {
    this.periodMs = l;
    return this;
  }

  public long getPeriodMs() {
    return periodMs;
  }

  /**
   * For example "issues", "measures", ... Default is "rows".
   */
  public ProgressLogger setPluralLabel(String s) {
    pluralLabel = s;
    return this;
  }

  public String getPluralLabel() {
    return pluralLabel;
  }

  public void log() {
    task.log();
  }

  private long now() {
    return system.now();
  }

  private class LoggerTimerTask extends TimerTask {
    private long previousCounter = START_COUNTER;
    private long previousTime = startTime;

    @Override
    public void run() {
      log();
    }

    private void log() {
      hasAlreadyCausedLogging = true;
      
      long currentCounter = counter.get();
      long currentTime = now();
      
      long deltaCounter = currentCounter - previousCounter;
      long deltaTime = currentTime - previousTime;
      
      long speed;
      if (deltaTime == 0) {
        speed = 0;
      } else {
        speed = MILLIS_PER_SECOND * deltaCounter / deltaTime;
      }
      
      logger.info(String.format("%d %s processed (%d items/sec)", currentCounter, pluralLabel, speed));

      previousCounter = currentCounter;
      previousTime = currentTime;
    }
  }
}
