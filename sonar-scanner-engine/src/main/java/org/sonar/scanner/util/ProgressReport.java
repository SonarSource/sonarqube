/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.util;

import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class ProgressReport implements Runnable {

  private static final Logger LOG = Loggers.get(ProgressReport.class);
  private final long period;
  private String message = "";
  private final Thread thread;
  private String stopMessage = null;

  public ProgressReport(String threadName, long period) {
    this.period = period;
    thread = new Thread(this);
    thread.setName(threadName);
    thread.setDaemon(true);
  }

  @Override
  public void run() {
    while (!Thread.interrupted()) {
      try {
        Thread.sleep(period);
        log(message);
      } catch (InterruptedException e) {
        break;
      }
    }
    if (stopMessage != null) {
      log(stopMessage);
    }
  }

  public void start(String startMessage) {
    log(startMessage);
    thread.start();
  }

  public void message(String message) {
    this.message = message;
  }

  public void stop(@Nullable String stopMessage) {
    this.stopMessage = stopMessage;
    thread.interrupt();
    try {
      thread.join();
    } catch (InterruptedException e) {
      // Ignore
    }
  }

  private static void log(String message) {
    synchronized (LOG) {
      LOG.info(message);
      LOG.notifyAll();
    }
  }

}
