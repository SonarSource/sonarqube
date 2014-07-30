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

public class Monitor extends Thread implements Terminatable {

  private static final long PING_DELAY_MS = 3000L;
  private static final long TIMEOUT_MS = 15000L;
  private final static Logger LOGGER = LoggerFactory.getLogger(Monitor.class);

  private volatile List<ProcessWrapper> processes;
  private volatile Map<String, Long> pings;
  private final ScheduledFuture<?> watch;
  private final ScheduledExecutorService monitor;

  /**
   * Starts another thread to send ping to all registered processes
   */
  public Monitor() {
    super("Process Monitor");
    processes = new ArrayList<ProcessWrapper>();
    pings = new HashMap<String, Long>();
    monitor = Executors.newScheduledThreadPool(1);
    watch = monitor.scheduleWithFixedDelay(new ProcessWatch(), 0L, PING_DELAY_MS, TimeUnit.MILLISECONDS);
  }

  private class ProcessWatch extends Thread {
    private ProcessWatch() {
      super("Process Ping");
    }

    @Override
    public void run() {
      for (ProcessWrapper process : processes) {
        try {
          ProcessMXBean mBean = process.getProcessMXBean();
          if (mBean != null) {
            long time = mBean.ping();
            pings.put(process.getName(), time);
          }
        } catch (Exception e) {
          // fail to ping, do nothing
        }
      }
    }
  }

  /**
   * Registers and monitors process. Note that process is probably not ready yet.
   */
  public void registerProcess(ProcessWrapper process) throws InterruptedException {
    processes.add(process);
    pings.put(process.getName(), System.currentTimeMillis());
    // starts a monitoring thread
    process.start();
  }

  private boolean processIsValid(ProcessWrapper processWrapper) {
    if (ProcessUtils.isAlive(processWrapper.process())) {
      long now = System.currentTimeMillis();
      return now - pings.get(processWrapper.getName()) < TIMEOUT_MS;
    }
    return false;
  }

  /**
   * Check continuously that registered processes are still up. If any process is down or does not answer to pings
   * during the max allowed period, then thread exits. 
   */
  @Override
  public void run() {
    try {
      while (true) {
        for (ProcessWrapper process : processes) {
          if (!processIsValid(process)) {
            LOGGER.warn("Monitor::run() -- Process '{}' is not valid. Exiting monitor", process.getName());
            interrupt();
          }
        }
        Thread.sleep(PING_DELAY_MS);
      }
    } catch (InterruptedException e) {
      LOGGER.debug("Monitoring thread is interrupted");
    } finally {
      terminate();
    }
  }

  @Override
  public void terminate() {
    processes.clear();
    if (!monitor.isShutdown()) {
      monitor.shutdownNow();
    }
    if (!watch.isCancelled()) {
      watch.cancel(true);
    }
  }
}
