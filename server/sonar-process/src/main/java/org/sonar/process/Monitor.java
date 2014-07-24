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
package org.sonar.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Monitor extends Thread {

  private static final long MAX_TIME = 15000L;

  private final static Logger LOGGER = LoggerFactory.getLogger(Monitor.class);

  private volatile List<ProcessWrapper> processes;
  private volatile Map<String, Long> pings;

  private ProcessWatch processWatch;
  private ScheduledFuture<?> watch;
  private ScheduledExecutorService monitor;

  public Monitor() {
    processes = new ArrayList<ProcessWrapper>();
    pings = new HashMap<String, Long>();
    monitor = Executors.newScheduledThreadPool(1);
    processWatch = new ProcessWatch();
    watch = monitor.scheduleWithFixedDelay(processWatch, 0, 3, TimeUnit.SECONDS);
  }

  public void registerProcess(ProcessWrapper processWrapper) {
    processes.add(processWrapper);
    pings.put(processWrapper.getName(), System.currentTimeMillis());
    processWrapper.start();
    for (int i = 0; i < 10; i++) {
      if (processWrapper.getProcessMXBean() == null || !processWrapper.getProcessMXBean().isReady()) {
        try {
          Thread.sleep(500L);
        } catch (InterruptedException e) {
          throw new IllegalStateException("Could not register process in Monitor", e);
        }
      }
    }
  }

  private class ProcessWatch implements Runnable {
    public void run() {
      for (ProcessWrapper process : processes) {
        try {
          if (process.getProcessMXBean() != null) {
            long time = process.getProcessMXBean().ping();
            LOGGER.debug("PINGED '{}'", process.getName());
            pings.put(process.getName(), time);
          }
        } catch (Exception e) {
          LOGGER.error("Error while pinging {}", process.getName(), e);
        }
      }
    }
  }

  private boolean processIsValid(ProcessWrapper process) {
    long now = System.currentTimeMillis();
    return (now - pings.get(process.getName())) < MAX_TIME;
  }

  public void run() {
    try {
      while (true) {
        for (ProcessWrapper process : processes) {
          if (!processIsValid(process)) {
            LOGGER.warn("Monitor::run() -- Process '{}' is not valid. Exiting monitor", process.getName());
            this.interrupt();
          }
        }
        Thread.sleep(3000L);
      }
    } catch (InterruptedException e) {
      LOGGER.debug("Monitoring thread is interrupted.");
    } finally {
      terminate();
    }
  }

  public void terminate() {
    if (monitor != null) {
      monitor.shutdownNow();
      watch.cancel(true);
      watch = null;
      processWatch = null;
    }
  }

}
