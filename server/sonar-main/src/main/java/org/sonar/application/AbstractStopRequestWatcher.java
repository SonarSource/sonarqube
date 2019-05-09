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
package org.sonar.application;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.BooleanSupplier;

public abstract class AbstractStopRequestWatcher extends Thread implements StopRequestWatcher {
  private static final long DEFAULT_WATCHER_DELAY_MS = 500L;
  private final BooleanSupplier stopRequestedTest;
  private final Runnable stopAction;
  private final long delayMs;

  protected AbstractStopRequestWatcher(String threadName, BooleanSupplier stopRequestedTest, Runnable stopAction) {
    this(threadName, stopRequestedTest, stopAction, DEFAULT_WATCHER_DELAY_MS);
  }

  @VisibleForTesting
  AbstractStopRequestWatcher(String threadName, BooleanSupplier stopRequestedTest, Runnable stopAction, long delayMs) {
    super(threadName);
    this.stopRequestedTest = stopRequestedTest;
    this.stopAction = stopAction;
    this.delayMs = delayMs;

    // safeguard, do not block the JVM if thread is not interrupted
    // (method stopWatching() never called).
    setDaemon(true);
  }

  @Override
  public void run() {
    try {
      while (true) {
        if (stopRequestedTest.getAsBoolean()) {
          stopAction.run();
          return;
        }
        Thread.sleep(delayMs);
      }
    } catch (InterruptedException e) {
      interrupt();
      // stop watching the commands
    }
  }

  @VisibleForTesting
  long getDelayMs() {
    return delayMs;
  }

  public void startWatching() {
    start();
  }

  public void stopWatching() {
    // does nothing if not started
    interrupt();
  }
}
