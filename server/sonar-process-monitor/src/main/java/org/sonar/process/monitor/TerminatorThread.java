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
package org.sonar.process.monitor;

import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Terminates all monitored processes. Tries to gracefully terminate each process,
 * then kill if timeout expires. Ping monitoring is disabled so process auto kills (self graceful termination, else self kill)
 * if it does not receive the termination request.
 */
class TerminatorThread extends Thread {

  private final List<ProcessRef> processes;
  private final JmxConnector jmxConnector;
  private final Timeouts timeouts;

  TerminatorThread(List<ProcessRef> processes, JmxConnector jmxConnector, Timeouts timeouts) {
    super("Terminator");
    this.processes = processes;
    this.jmxConnector = jmxConnector;
    this.timeouts = timeouts;
  }

  @Override
  public void run() {
    // terminate in reverse order of startup (dependency order)
    for (int index = processes.size() - 1; index >= 0; index--) {
      final ProcessRef processRef = processes.get(index);
      if (!processRef.isTerminated()) {
        processRef.setPingEnabled(false);

        try {
          jmxConnector.terminate(processRef, timeouts.getTerminationTimeout());
        } catch (Exception ignored) {
          // failed to gracefully stop in a timely fashion
          LoggerFactory.getLogger(getClass()).info(String.format("Kill %s", processRef));
        } finally {
          // kill even if graceful termination was done, just to be sure that physical process is really down
          processRef.hardKill();
        }
      }
    }
  }
}
