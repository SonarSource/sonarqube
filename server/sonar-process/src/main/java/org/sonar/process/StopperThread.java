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
package org.sonar.process;

import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.sonar.process.sharedmemoryfile.ProcessCommands;

/**
 * Gracefully stops process in a timely fashion
 */
class StopperThread extends Thread {

  private final Monitored monitored;
  private final long terminationTimeoutMs;
  private final ProcessCommands commands;

  StopperThread(Monitored monitored, ProcessCommands commands, long terminationTimeoutMs) {
    super("Stopper");
    this.monitored = monitored;
    this.terminationTimeoutMs = terminationTimeoutMs;
    this.commands = commands;
  }

  @Override
  public void run() {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future future = executor.submit(monitored::stop);
      future.get(terminationTimeoutMs, TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      LoggerFactory.getLogger(getClass()).error("Can not stop in {}ms", terminationTimeoutMs, e);
    }
    executor.shutdownNow();
    commands.endWatch();
  }
}
