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

  private final static Logger LOGGER = LoggerFactory.getLogger(Monitor.class);

  private volatile List<ProcessWrapper> processes;
  private volatile Map<String, Long> pings;

  private ScheduledFuture<?> watch;
  private final ScheduledExecutorService monitor;

  public Monitor() {
    processes = new ArrayList<ProcessWrapper>();
    pings = new HashMap<String, Long>();
    monitor = Executors.newScheduledThreadPool(1);
    watch = monitor.scheduleWithFixedDelay(new ProcessWatch(), 0, 3, TimeUnit.SECONDS);
  }

  public void registerProcess(ProcessWrapper processWrapper) {
    LOGGER.trace("Monitor::registerProcess() START");
    processes.add(processWrapper);
    pings.put(processWrapper.getName(), System.currentTimeMillis());
    processWrapper.start();
    for(int i=0; i<10; i++){
      if(processWrapper.getProcessMXBean() == null
        || !processWrapper.getProcessMXBean().isReady()){
        try {
          Thread.sleep(500L);
        } catch (InterruptedException e) {
          throw new IllegalStateException("Could not register process in Monitor", e);
        }
      }
    }
    LOGGER.trace("Monitor::registerProcess() END");
  }

  private class ProcessWatch implements Runnable {
    public void run() {
      LOGGER.trace("Monitor::ProcessWatch PINGING for map: {}", processes);
      for (ProcessWrapper process : processes) {
        try {
          long time = process.getProcessMXBean().ping();
          LOGGER.info("Monitor::ProcessWatch PINGED '{}'", process.getName());
          pings.put(process.getName(), time);
        } catch (Exception e) {
          LOGGER.error("Error while pinging {}", process.getName(), e);
        }
      }
    }
  }

  private boolean processIsValid(ProcessWrapper process) {
    long now = System.currentTimeMillis();
    LOGGER.debug("Monitor::processIsValid() -- Time since last ping for '{}': {}ms",
      process.getName(), (now - pings.get(process.getName())));
    return (now - pings.get(process.getName())) < 5000L;
  }

  public void run() {
    LOGGER.trace("Monitor::run() START");
    boolean everythingOK = true;
    while (everythingOK) {
      for(ProcessWrapper process: processes){
        if(!processIsValid(process)){
          LOGGER.warn("Monitor::run() -- Process '{}' is not valid. Exiting monitor", process.getName());
          everythingOK = false;
          break;
        }
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    watch.cancel(true);
    monitor.shutdownNow();
    LOGGER.trace("Monitor::run() END");
  }
}
