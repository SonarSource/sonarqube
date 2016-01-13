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
package org.sonar.server.benchmark;

import org.slf4j.Logger;

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

class ProgressTask extends TimerTask {

  public static final long PERIOD_MS = 60000L;

  private final Logger logger;
  private final String label;
  private final AtomicLong counter;
  private long previousCount = 0L;
  private long previousTime = 0L;

  public ProgressTask(Logger logger, String label, AtomicLong counter) {
    this.logger = logger;
    this.label = label;
    this.counter = counter;
    this.previousTime = System.currentTimeMillis();
  }

  @Override
  public void run() {
    long currentCount = counter.get();
    long now = System.currentTimeMillis();
    logger.info("{} {} indexed ({} docs/second)",
      currentCount, label, documentsPerSecond(currentCount - previousCount, now - previousTime), label);
    this.previousCount = currentCount;
    this.previousTime = now;
  }

  private int documentsPerSecond(long nbDocs, long time) {
    return (int) Math.round(nbDocs / (time / 1000.0));
  }
}
