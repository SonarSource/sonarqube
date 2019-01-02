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
package org.sonar.core.util;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * Background thread that logs the state of a counter at fixed intervals.
 */
public class ProgressLogger {

  public static final long DEFAULT_PERIOD_MS = 60_000L;

  private final Timer timer;
  private final LoggerTimerTask task;
  private long periodMs = DEFAULT_PERIOD_MS;

  public ProgressLogger(String threadName, AtomicLong counter, Logger logger) {
    this.timer = new Timer(threadName);
    this.task = new LoggerTimerTask(counter, logger);
  }

  public static ProgressLogger create(Class clazz, AtomicLong counter) {
    String threadName = String.format("ProgressLogger[%s]", clazz.getSimpleName());
    Logger logger = Loggers.get(clazz);
    return new ProgressLogger(threadName, counter, logger);
  }

  /**
   * Warning, does not check if already started.
   */
  public void start() {
    // first log after {periodMs} milliseconds
    timer.schedule(task, periodMs, periodMs);
  }

  public void stop() {
    timer.cancel();
    timer.purge();
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
    task.pluralLabel = s;
    return this;
  }

  public String getPluralLabel() {
    return task.pluralLabel;
  }

  public void log() {
    task.log();
  }

  private class LoggerTimerTask extends TimerTask {
    private final AtomicLong counter;
    private final Logger logger;
    private String pluralLabel = "rows";
    private long previousCounter = 0L;

    private LoggerTimerTask(AtomicLong counter, Logger logger) {
      this.counter = counter;
      this.logger = logger;
    }

    @Override
    public void run() {
      log();
    }

    private void log() {
      long current = counter.get();
      logger.info(String.format("%d %s processed (%d items/sec)", current, pluralLabel, 1000 * (current - previousCounter) / periodMs));
      previousCounter = current;
    }
  }
}
