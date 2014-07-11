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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Map;

public class MonitorService extends Thread {

  private final static Logger LOGGER = LoggerFactory.getLogger(ProcessWrapper.class);

  private final static Long MAX_ELAPSED_TIME = 10000L;

  final DatagramSocket socket;
  final Map<String, ProcessWrapper> processes;
  final Map<String, Long> processesPing;

  public MonitorService(DatagramSocket socket) {
    LOGGER.info("Monitor service is listening on socket:{}", socket.getLocalPort());
    this.socket = socket;
    processes = new HashMap<String, ProcessWrapper>();
    processesPing = new HashMap<String, Long>();
  }

  public void register(ProcessWrapper process) {
    this.processes.put(process.getName(), process);
    this.processesPing.put(process.getName(), System.currentTimeMillis());
  }

  @Override
  public void run() {
    LOGGER.info("Launching Monitoring Thread");
    long time;
    while (!Thread.currentThread().isInterrupted()) {
      DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
      time = System.currentTimeMillis();
      try {
        socket.setSoTimeout(200);
        socket.receive(packet);
        String message = new String(packet.getData()).trim();
        processesPing.put(message, time);
      } catch (Exception e) {
        ; // To not do anything.
      }
      if (!checkAllProcessPing(time)) {
        break;
      }
    }
    LOGGER.error("Not all process have checked-in. Aborting node");
    for (ProcessWrapper process : processes.values()) {
      process.shutdown();
    }
  }


  private boolean checkAllProcessPing(long now) {

    //check that all thread wrapper are running
    for (Thread thread : processes.values()) {
      if (thread.isInterrupted()) {
        LOGGER.error("process {} has been interrupted. Aborting node",
          thread.getName());
        return false;
      }
    }

    //check that all heartbeats are OK
    for (Map.Entry<String, Long> processPing : processesPing.entrySet()) {
      if ((now - processPing.getValue()) > MAX_ELAPSED_TIME) {
        LOGGER.error("process {} has not checked-in since {}ms. Aborting node",
          processPing.getKey(), (now - processPing.getValue()));
        return false;
      } else {
        LOGGER.debug("process {} has last checked-in {}ms ago.",
          processPing.getKey(), (now - processPing.getValue()));
      }
    }
    return true;
  }

  public Integer getMonitoringPort() {
    return socket.getLocalPort();
  }
}
