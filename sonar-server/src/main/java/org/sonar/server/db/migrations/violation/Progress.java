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

class Progress extends TimerTask {

  static final String THREAD_NAME = "Violation Migration Progress";
  static final long DELAY_MS = 60000L;

  private final AtomicInteger counter = new AtomicInteger(0);
  private final Logger logger;
  private final int totalViolations;
  private long start = System.currentTimeMillis();

  Progress(int totalViolations, Logger logger) {
    this.totalViolations = totalViolations;
    this.logger = logger;
  }

  Progress(int totalViolations) {
    this(totalViolations, LoggerFactory.getLogger(Progress.class));
  }

  void increment(int delta) {
    counter.addAndGet(delta);
  }

  @Override
  public void run() {
    int totalIssues = counter.get();
    long durationMinutes = (System.currentTimeMillis() - start) / 60000L;
    int remaining = 0;
    if (durationMinutes > 0) {
      int frequency = (int) (totalIssues / durationMinutes);
      remaining = (totalViolations - totalIssues) / frequency;
    }
    logger.info(String.format(
      "%d%% [%d/%d violations, %d minutes remaining]", (100 * totalIssues) / totalViolations, totalIssues, totalViolations, remaining)
    );
  }
}
