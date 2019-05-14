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
package org.sonar.process;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopWatcher extends Thread {
  private static final Logger LOG = LoggerFactory.getLogger(StopWatcher.class);
  private final Runnable stopCommand;
  private final BooleanSupplier shouldStopTest;
  private final long delayMs;
  private volatile boolean watching = true;

  public StopWatcher(String threadName, Runnable stopCommand, BooleanSupplier shouldStopTest) {
    this(threadName, stopCommand, shouldStopTest, 500L);
  }

  @VisibleForTesting
  StopWatcher(String threadName, Runnable stopCommand, BooleanSupplier shouldStopTest, long delayMs) {
    super(threadName);
    this.stopCommand = stopCommand;
    this.shouldStopTest = shouldStopTest;
    this.delayMs = delayMs;
  }

  @Override
  public void run() {
    while (watching) {
      if (shouldStopTest.getAsBoolean()) {
        LOG.trace("{} triggering stop command", this.getName());
        stopCommand.run();
        watching = false;
      } else {
        try {
          Thread.sleep(delayMs);
        } catch (InterruptedException ignored) {
          watching = false;
          // restore interrupted flag
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  public void stopWatching() {
    super.interrupt();
    watching = false;
  }
}
