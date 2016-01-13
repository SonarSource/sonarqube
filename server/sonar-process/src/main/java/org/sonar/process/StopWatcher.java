/*
 * SonarQube
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
package org.sonar.process;

import org.slf4j.LoggerFactory;

/**
 * This watchdog asks for graceful termination of process when the file
 * &lt;process_key&gt;.stop is created in temp directory.
 */
public class StopWatcher extends Thread {

  private final Stoppable stoppable;
  private final ProcessCommands commands;
  private boolean watching = true;
  private final long delayMs;

  public StopWatcher(ProcessCommands commands, Stoppable stoppable) {
    this(commands, stoppable, 500L);
  }

  StopWatcher(ProcessCommands commands, Stoppable stoppable, long delayMs) {
    super("Stop Watcher");
    this.commands = commands;
    this.stoppable = stoppable;
    this.delayMs = delayMs;
  }

  @Override
  public void run() {
    try {
      while (watching) {
        if (commands.askedForStop()) {
          LoggerFactory.getLogger(getClass()).info("Stopping process");
          stoppable.stopAsync();
          watching = false;
        } else {
          try {
            Thread.sleep(delayMs);
          } catch (InterruptedException ignored) {
            watching = false;
          }
        }
      }
    } finally {
      commands.endWatch();
    }
  }

  public void stopWatching() {
    watching = false;
  }
}
