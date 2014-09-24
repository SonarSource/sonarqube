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

import java.util.Collections;
import java.util.List;

/**
 * Terminates all monitored processes. Tries to gracefully terminate each process,
 * then kill if timeout expires. Ping monitoring is disabled so process auto kills (self graceful termination, else self kill)
 * if it does not receive the termination request.
 */
class TerminatorThread extends Thread {

  private final Timeouts timeouts;
  private List<ProcessRef> processes = Collections.emptyList();

  TerminatorThread(Timeouts timeouts) {
    super("Terminator");
    this.timeouts = timeouts;
  }

  /**
   * To be called before {@link #run()}
   */
  void setProcesses(List<ProcessRef> l) {
    this.processes = l;
  }

  @Override
  public void run() {
    // terminate in reverse order of startup (dependency order)
    for (int index = processes.size() - 1; index >= 0; index--) {
      ProcessRef ref = processes.get(index);
      if (!ref.isStopped()) {
        LoggerFactory.getLogger(getClass()).info(String.format("%s is stopping", ref));
        ref.askForGracefulAsyncStop();

        long killAt = System.currentTimeMillis() + timeouts.getTerminationTimeout();
        while (!ref.isStopped() && System.currentTimeMillis() < killAt) {
          try {
            Thread.sleep(100L);
          } catch (InterruptedException e) {
            // stop asking for graceful stops, Monitor will hardly kill all processes
            return;
          }
        }
        if (!ref.isStopped()) {
          LoggerFactory.getLogger(getClass()).info(String.format("%s failed to stop in a timely fashion. Killing it.", ref));
        }
        ref.stop();
        LoggerFactory.getLogger(getClass()).info(String.format("%s is stopped", ref));
      }
    }
  }
}
