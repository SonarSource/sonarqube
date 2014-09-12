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

import org.sonar.process.ProcessUtils;

class ProcessRef {

  private final String key;
  private final Process process;
  private final StreamGobbler[] gobblers;
  private volatile boolean terminated = false;
  private volatile boolean pingEnabled = true;

  ProcessRef(String key, Process process, StreamGobbler... gobblers) {
    this.key = key;
    this.process = process;
    this.terminated = !ProcessUtils.isAlive(process);
    this.gobblers = gobblers;
  }

  /**
   * Unique logical key (not the pid), for instance "ES"
   */
  String getKey() {
    return key;
  }

  /**
   * The {@link java.lang.Process}
   */
  Process getProcess() {
    return process;
  }

  /**
   * Almost real-time status
   */
  boolean isTerminated() {
    return terminated;
  }

  /**
   * Sending pings can be disabled when requesting for termination or when process is on debug mode (JDWP)
   */
  void setPingEnabled(boolean b) {
    this.pingEnabled = b;
  }

  boolean isPingEnabled() {
    return pingEnabled;
  }

  /**
   * Destroy the process without gracefully asking it to terminate (kill -9).
   * @return true if the process was killed, false if process is already terminated
   */
  boolean hardKill() {
    boolean killed = false;
    terminated = true;
    pingEnabled = false;
    if (ProcessUtils.isAlive(process)) {
      ProcessUtils.destroyQuietly(process);
      killed = true;
    }
    for (StreamGobbler gobbler : gobblers) {
      StreamGobbler.waitUntilFinish(gobbler);
    }
    ProcessUtils.closeStreams(process);
    return killed;
  }

  void setTerminated(boolean b) {
    this.terminated = b;
  }

  @Override
  public String toString() {
    return String.format("Process[%s]", key);
  }
}
