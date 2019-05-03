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

@FunctionalInterface
public interface ManagedProcessEventListener {

  enum Type {
    OPERATIONAL,
    ASK_FOR_RESTART
  }

  /**
   * This method is called when the process with the specified {@link ProcessId}
   * sends the event through the ipc shared memory.
   * Note that there can be a delay since the instant the process sets the flag
   * (see {@link ManagedProcessHandler#WATCHER_DELAY_MS}).
   *
   * Call blocks the process watcher. Implementations should be asynchronous and
   * fork a new thread if call can be long.
   */
  void onManagedProcessEvent(ProcessId processId, Type type);

}
