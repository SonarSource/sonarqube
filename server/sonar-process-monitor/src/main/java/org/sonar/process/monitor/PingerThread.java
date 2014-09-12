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
package org.sonar.process.monitor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This thread pings a process - through RMI - at fixed delay
 */
class PingerThread extends Thread {

  private final ProcessRef processRef;
  private final JmxConnector jmxConnector;

  private PingerThread(ProcessRef process, JmxConnector jmxConnector) {
    // it's important to give a name for traceability in profiling tools like visualVM
    super(String.format("Ping[%s]", process.getKey()));
    setDaemon(true);
    this.processRef = process;
    this.jmxConnector = jmxConnector;
  }

  @Override
  public void run() {
    if (!processRef.isTerminated() && processRef.isPingEnabled()) {
      try {
        jmxConnector.ping(processRef);
      } catch (Exception ignored) {
        // failed to ping
      }
    } else {
      interrupt();
    }
  }

  static void startPinging(ProcessRef processRef, JmxConnector jmxConnector, Timeouts timeouts) {
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    PingerThread pinger = new PingerThread(processRef, jmxConnector);
    scheduler.scheduleAtFixedRate(pinger, 0L, timeouts.getMonitorPingInterval(), TimeUnit.MILLISECONDS);
  }
}
