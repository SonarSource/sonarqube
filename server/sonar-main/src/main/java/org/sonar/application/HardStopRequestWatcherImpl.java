/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.application;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.process.ProcessId;
import org.sonar.process.sharedmemoryfile.DefaultProcessCommands;
import org.sonar.process.sharedmemoryfile.ProcessCommands;

public class HardStopRequestWatcherImpl extends AbstractStopRequestWatcher {

  HardStopRequestWatcherImpl(Scheduler scheduler, ProcessCommands commands) {
    super("SQ Hard stop request watcher", commands::askedForHardStop, scheduler::hardStop);
  }

  @VisibleForTesting
  HardStopRequestWatcherImpl(Scheduler scheduler, ProcessCommands commands, long delayMs) {
    super("SQ Hard stop request watcher", commands::askedForHardStop, scheduler::hardStop, delayMs);
  }

  public static HardStopRequestWatcherImpl create(Scheduler scheduler, FileSystem fs) {
    DefaultProcessCommands commands = DefaultProcessCommands.secondary(fs.getTempDir(), ProcessId.APP.getIpcIndex());
    return new HardStopRequestWatcherImpl(scheduler, commands);
  }

}
