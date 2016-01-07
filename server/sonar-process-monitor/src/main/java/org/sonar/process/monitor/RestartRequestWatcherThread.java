/*
 * SonarQube :: Process Monitor
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.process.monitor;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

public class RestartRequestWatcherThread extends Thread {
  private static final Logger LOG = LoggerFactory.getLogger(RestartRequestWatcherThread.class);
  private static int instanceCounter = 0;

  private final Monitor monitor;
  private final List<ProcessRef> processes;
  private final long delayMs;

  private boolean watching = true;

  public RestartRequestWatcherThread(Monitor monitor, List<ProcessRef> processes) {
    this(monitor, processes, 500);
  }

  public RestartRequestWatcherThread(Monitor monitor, List<ProcessRef> processes, long delayMs) {
    super("Restart watcher " + (instanceCounter++));
    this.monitor = requireNonNull(monitor, "monitor can not be null");
    this.processes = requireNonNull(processes, "processes can not be null");
    this.delayMs = delayMs;
  }

  @Override
  public void run() {
    while (watching) {
      for (ProcessRef processCommands : processes) {
        if (processCommands.getCommands().askedForRestart()) {
          LOG.info("Process [{}] requested restart", processCommands.getKey());
          monitor.restartAsync();
          watching = false;
        } else {
          try {
            Thread.sleep(delayMs);
          } catch (InterruptedException ignored) {
            // keep watching
          }
        }
      }
    }
  }

  public void stopWatching() {
    this.watching = false;
  }

  public boolean isWatching() {
    return watching;
  }
}
