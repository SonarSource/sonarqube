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

import java.util.List;

/**
 * Terminates all monitored processes. Tries to gracefully terminate each process,
 * then kill if timeout expires. Ping monitoring is disabled so process auto kills (self graceful termination, else self kill)
 * if it does not receive the termination request.
 */
class TerminatorThread extends Thread {

  private final List<ProcessRef> processes;

  TerminatorThread(List<ProcessRef> processes) {
    super("Terminator");
    this.processes = processes;
  }

  @Override
  public void run() {
    // terminate in reverse order of startup (dependency order)
    for (int index = processes.size() - 1; index >= 0; index--) {
      ProcessRef processRef = processes.get(index);
      processRef.kill();
    }
  }
}
