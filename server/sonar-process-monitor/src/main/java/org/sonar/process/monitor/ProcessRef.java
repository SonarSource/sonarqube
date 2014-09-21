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
import org.sonar.process.MessageException;
import org.sonar.process.ProcessUtils;
import org.sonar.process.SharedStatus;

class ProcessRef {

  private final String key;
  private final SharedStatus sharedStatus;
  private final Process process;
  private final StreamGobbler gobbler;
  private long launchedAt;
  private volatile boolean stopped = false;

  ProcessRef(String key, SharedStatus sharedStatus, Process process, StreamGobbler gobbler) {
    this.key = key;
    this.sharedStatus = sharedStatus;
    this.process = process;
    this.stopped = !ProcessUtils.isAlive(process);
    this.gobbler = gobbler;
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

  void waitForReady() {
    boolean ready = false;
    while (!ready) {
      if (isStopped()) {
        throw new MessageException(String.format("%s failed to start", this));
      }
      ready = sharedStatus.wasStartedAfter(launchedAt);
      try {
        Thread.sleep(200L);
      } catch (InterruptedException e) {
        throw new IllegalStateException(String.format("Interrupted while waiting for %s to be ready", this), e);
      }
    }
  }

  void setLaunchedAt(long launchedAt) {
    this.launchedAt = launchedAt;
  }

  /**
   * Almost real-time status
   */
  boolean isStopped() {
    return stopped;
  }

  /**
   * Sends kill signal and awaits termination.
   */
  void kill() {
    if (ProcessUtils.isAlive(process)) {
      LoggerFactory.getLogger(getClass()).info(String.format("%s is stopping", this));
      ProcessUtils.sendKillSignal(process);
      try {
        // signal is sent, waiting for shutdown hooks to be executed
        process.waitFor();
        StreamGobbler.waitUntilFinish(gobbler);
        ProcessUtils.closeStreams(process);
      } catch (InterruptedException ignored) {
        // can't wait for the termination of process. Let's assume it's down.
      }
    }
    stopped = true;
  }

  void setStopped(boolean b) {
    this.stopped = b;
  }

  @Override
  public String toString() {
    return String.format("Process[%s]", key);
  }
}
