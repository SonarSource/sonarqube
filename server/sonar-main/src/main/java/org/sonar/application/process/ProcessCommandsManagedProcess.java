/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.application.process;

import org.sonar.process.ProcessId;
import org.sonar.process.sharedmemoryfile.ProcessCommands;

import static java.util.Objects.requireNonNull;

public class ProcessCommandsManagedProcess extends AbstractManagedProcess {

  private final ProcessCommands commands;

  public ProcessCommandsManagedProcess(Process process, ProcessId processId, ProcessCommands commands) {
    super(process, processId);
    this.commands = requireNonNull(commands, "commands can't be null");
  }

  /**
   * Whether the process has set the operational flag (in ipc shared memory)
   */
  @Override
  public boolean isOperational() {
    return commands.isOperational();
  }

  /**
   * Send request to gracefully stop to the process (via ipc shared memory)
   */
  @Override
  public void askForStop() {
    commands.askForStop();
  }

  /**
   * Send request to quickly stop to the process (via ipc shared memory)
   */
  @Override
  public void askForHardStop() {
    commands.askForHardStop();
  }

  /**
   * Whether the process asked for a full restart (via ipc shared memory)
   */
  @Override
  public boolean askedForRestart() {
    return commands.askedForRestart();
  }

  /**
   * Removes the flag in ipc shared memory so that next call to {@link #askedForRestart()}
   * returns {@code false}, except if meanwhile process asks again for restart.
   */
  @Override
  public void acknowledgeAskForRestart() {
    commands.acknowledgeAskForRestart();
  }

}
