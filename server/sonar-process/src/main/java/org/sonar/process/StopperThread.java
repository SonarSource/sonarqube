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

import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Gracefully stops process in a timely fashion
 */
class StopperThread extends Thread {

  private final Monitored monitored;
  private final long terminationTimeout;
  private final ProcessCommands commands;

  StopperThread(Monitored monitored, ProcessCommands commands, long terminationTimeout) {
    super("Stopper");
    this.monitored = monitored;
    this.terminationTimeout = terminationTimeout;
    this.commands = commands;
  }

  @Override
  public void run() {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future future = executor.submit(new Runnable() {
      @Override
      public void run() {
        monitored.stop();
      }
    });
    try {
      future.get(terminationTimeout, TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      LoggerFactory.getLogger(getClass()).error(String.format("Can not stop in %dms", terminationTimeout), e);
    }
    executor.shutdownNow();
    commands.endWatch();
  }
}
