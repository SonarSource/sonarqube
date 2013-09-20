/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.db.migrations.violation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This task logs every minute the status of migration. It is destroyed
 * when migration is finished.
 */
class Progress extends TimerTask {

  static final String THREAD_NAME = "Violation Migration Progress";
  static final long DELAY_MS = 60000L;

  private final AtomicInteger counter = new AtomicInteger(0);
  private final Logger logger;
  private final int totalViolations;
  private final long start;

  Progress(int totalViolations, Logger logger, long startDate) {
    this.totalViolations = totalViolations;
    this.logger = logger;
    this.start = startDate;
  }

  Progress(int totalViolations) {
    this(totalViolations, LoggerFactory.getLogger(Progress.class), System.currentTimeMillis());
  }

  void increment(int delta) {
    counter.addAndGet(delta);
  }

  @Override
  public void run() {
    int totalIssues = counter.get();
    long durationMinutes = (System.currentTimeMillis() - start) / 60000L;
    int percents = (100 * totalIssues) / totalViolations;
    if (totalIssues>0 && durationMinutes > 0) {
      int frequency = (int) (totalIssues / durationMinutes);
      int remaining = (totalViolations - totalIssues) / frequency;
      logger.info(String.format(
        "%d%% [%d/%d violations, %d minutes remaining]", percents, totalIssues, totalViolations, remaining)
      );
    } else {
      logger.info(String.format("%d%% [%d/%d violations]", percents, totalIssues, totalViolations));
    }

  }
}
