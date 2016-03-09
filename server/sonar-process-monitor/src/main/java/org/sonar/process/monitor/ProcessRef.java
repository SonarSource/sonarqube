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
package org.sonar.process.monitor;

import org.slf4j.LoggerFactory;
import org.sonar.process.MessageException;
import org.sonar.process.ProcessCommands;
import org.sonar.process.ProcessUtils;

class ProcessRef {

  private final String key;
  private final ProcessCommands commands;
  private final Process process;
  private final StreamGobbler gobbler;
  private volatile boolean stopped = false;

  ProcessRef(String key, ProcessCommands commands, Process process, StreamGobbler gobbler) {
    this.key = key;
    this.commands = commands;
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

  public ProcessCommands getCommands() {
    return commands;
  }

  void waitForUp() {
    boolean up = false;
    while (!up) {
      if (isStopped()) {
        throw new MessageException(String.format("%s failed to start", this));
      }
      up = commands.isUp();
      try {
        Thread.sleep(200L);
      } catch (InterruptedException e) {
        throw new IllegalStateException(String.format("Interrupted while waiting for %s to be up", this), e);
      }
    }
  }

  /**
   * True if process is physically down
   */
  boolean isStopped() {
    return stopped;
  }

  void askForGracefulAsyncStop() {
    commands.askForStop();
  }

  /**
   * Sends kill signal and awaits termination. No guarantee that process is gracefully terminated (=shutdown hooks
   * executed). It depends on OS.
   */
  void stop() {
    if (ProcessUtils.isAlive(process)) {
      try {
        ProcessUtils.sendKillSignal(process);
        // signal is sent, waiting for shutdown hooks to be executed (or not... it depends on OS)
        process.waitFor();

      } catch (InterruptedException e) {
        // can't wait for the termination of process. Let's assume it's down.
        LoggerFactory.getLogger(getClass()).warn(String.format("Interrupted while stopping process %s", key), e);
      }
    }
    ProcessUtils.closeStreams(process);
    StreamGobbler.waitUntilFinish(gobbler);
    stopped = true;
  }

  @Override
  public String toString() {
    return String.format("Process[%s]", key);
  }
}
