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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;

/**
 * Stops process in a short time fashion
 */
abstract class AbstractStopperThread extends Thread {

  private final Runnable stopCode;
  private final long terminationTimeoutMs;
  private boolean stop = false;

  AbstractStopperThread(String threadName, Runnable stopCode, long terminationTimeoutMs) {
    super(threadName);
    this.stopCode = stopCode;
    this.terminationTimeoutMs = terminationTimeoutMs;
  }

  @Override
  public void run() {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future future = executor.submit(stopCode);
      future.get(terminationTimeoutMs, TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      if (!stop) {
        LoggerFactory.getLogger(getClass()).error("Can not stop in {}ms", terminationTimeoutMs, e);
      }
    }
    executor.shutdownNow();
  }

  public void stopIt() {
    this.stop = true;
    super.interrupt();
  }
}
