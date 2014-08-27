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
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Monitor extends Thread implements Terminable {

  private static final Logger LOGGER = LoggerFactory.getLogger(Monitor.class);

  private static final long PING_DELAY_MS = 3000L;

  private long pingDelayMs = PING_DELAY_MS;
  private volatile List<ProcessWrapper> processes;
  private final ScheduledFuture<?> watch;
  private final ScheduledExecutorService monitorExecutionService;

  /**
   * Starts another thread to send ping to all registered processes
   */
  public Monitor() {
    super("Process Monitor");
    processes = new ArrayList<ProcessWrapper>();
    monitorExecutionService = Executors.newScheduledThreadPool(1);
    watch = monitorExecutionService.scheduleAtFixedRate(new ProcessWatch(), 0L, getPingDelayMs(), TimeUnit.MILLISECONDS);
  }

  private long getPingDelayMs() {
    return pingDelayMs;
  }

  public Monitor setPingDelayMs(long pingDelayMs) {
    this.pingDelayMs = pingDelayMs;
    return this;
  }

  private class ProcessWatch extends Thread {
    private ProcessWatch() {
      super("Process Ping");
    }

    @Override
    public void run() {
      for (ProcessWrapper process : processes) {
        LOGGER.debug("Pinging process[{}]", process.getName());
        try {
          ProcessMXBean mBean = process.getProcessMXBean();
          if (mBean != null) {
            mBean.ping();
          }
        } catch (Exception e) {
          LOGGER.debug("Could not ping process[{}]", process.getName());
          LOGGER.trace("Ping failure", e);
        }
      }
    }
  }

  /**
   * Registers and monitors process. Note that process is probably not ready yet.
   */
  public void registerProcess(ProcessWrapper process) throws InterruptedException {
    LOGGER.info("Registering process[{}] for monitoring.", process.getName());
    synchronized (processes) {
      processes.add(process);
    }
    // starts a monitoring thread
    process.start();
  }

  /**
   * Check continuously that registered processes are still up. If any process is down or does not answer to pings
   * during the max allowed period, then thread exits.
   */
  @Override
  public void run() {
    try {
      boolean ok = true;
      while (isRunning && ok) {
        synchronized (processes) {
          LOGGER.debug("Monitoring {} processes.", processes.size());
          for (ProcessWrapper process : processes) {
            if (!ProcessUtils.isAlive(process.process())) {
              LOGGER.info("{} is down, stopping all other processes", process.getName());
              ok = false;
              interrupt();
            }
          }
        }
        if (ok) {
          Thread.sleep(PING_DELAY_MS);
        }
      }
    } catch (InterruptedException e) {
      LOGGER.debug("Monitoring thread is interrupted");
    } finally {
      terminate();
    }
  }

  volatile Boolean isRunning = true;

  @Override
  public synchronized void terminate() {
    LOGGER.debug("Monitoring thread is terminating");

    if (!monitorExecutionService.isShutdown()) {
      monitorExecutionService.shutdownNow();
    }
    if (!watch.isCancelled()) {
      watch.cancel(true);
    }

    for (int i = processes.size() - 1; i >= 0; i--) {
      processes.get(i).terminate();
    }
    processes.clear();
    interruptAndWait();
  }

  private void interruptAndWait() {
    this.interrupt();
    try {
      if (this.isAlive()) {
        this.join();
      }
    } catch (InterruptedException e) {
      //Expected to be interrupted :)
    }
  }
}
