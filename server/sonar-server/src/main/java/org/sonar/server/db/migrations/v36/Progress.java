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
package org.sonar.server.db.migrations.v36;

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * This task logs every minute the status of migration. It is destroyed
 * when migration is finished.
 */
class Progress extends TimerTask {

  static final String THREAD_NAME = "Violation Migration Progress";
  static final long DELAY_MS = 60000L;

  private final AtomicLong counter = new AtomicLong(0L);
  private final Logger logger;
  private final long totalViolations;
  private final long start;

  Progress(long totalViolations, Logger logger, long startDate) {
    this.totalViolations = totalViolations;
    this.logger = logger;
    this.start = startDate;
  }

  Progress(long totalViolations) {
    this(totalViolations, Loggers.get(Progress.class), System.currentTimeMillis());
  }

  void increment(int delta) {
    counter.addAndGet(delta);
  }

  @Override
  public void run() {
    long totalIssues = counter.get();
    long durationMinutes = (System.currentTimeMillis() - start) / 60000L;
    int percents = (int) ((100L * totalIssues) / totalViolations);
    if (totalIssues > 0 && durationMinutes > 0) {
      int frequency = (int) (totalIssues / durationMinutes);
      long remaining = (totalViolations - totalIssues) / frequency;
      logger.info(String.format(
        "%d%% [%d/%d violations, %d minutes remaining]", percents, totalIssues, totalViolations, remaining)
        );
    } else {
      logger.info(String.format("%d%% [%d/%d violations]", percents, totalIssues, totalViolations));
    }

  }
}
