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
package org.sonar.server.util;

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgressTask extends TimerTask {
  private final Logger logger;
  public static final long PERIOD_MS = 60000L;
  private final AtomicLong counter;
  private String rowName = "rows";

  public ProgressTask(AtomicLong counter, Logger logger) {
    this.counter = counter;
    this.logger = logger;
  }

  public ProgressTask setRowPluralName(String s) {
    this.rowName = s;
    return this;
  }

  @Override
  public void run() {
    log();
  }

  public void log() {
    logger.info(String.format("%d %s processed", counter.get(), rowName));
  }
}
