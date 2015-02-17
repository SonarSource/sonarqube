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
package org.sonar.batch.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgressReport implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(ProgressReport.class);
  private final long period;
  private String message = "";
  private final Thread thread;
  private String stopMessage = "";

  public ProgressReport(String threadName, long period) {
    this.period = period;
    thread = new Thread(this);
    thread.setName(threadName);
  }

  @Override
  public void run() {
    while (!Thread.interrupted()) {
      try {
        Thread.sleep(period);
        log(message);
      } catch (InterruptedException e) {
        thread.interrupt();
      }
    }
    log(stopMessage);
  }

  public void start(String startMessage) {
    log(startMessage);
    thread.start();
  }

  public void message(String message) {
    this.message = message;
  }

  public void stop(String stopMessage) {
    this.stopMessage = stopMessage;
    thread.interrupt();
    try {
      thread.join();
    } catch (InterruptedException e) {
      // Ignore
    }
  }

  private void log(String message) {
    synchronized (LOG) {
      LOG.info(message);
      LOG.notifyAll();
    }
  }

}
