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
package org.sonar.application;

import org.sonar.application.config.AppSettings;
import org.sonar.process.ProcessId;
import org.sonar.process.sharedmemoryfile.DefaultProcessCommands;
import org.sonar.process.sharedmemoryfile.ProcessCommands;

import static org.sonar.process.ProcessProperties.Property.ENABLE_STOP_COMMAND;

public class StopRequestWatcherImpl extends AbstractStopRequestWatcher {
  private final AppSettings settings;

  StopRequestWatcherImpl(AppSettings settings, Scheduler scheduler, ProcessCommands commands) {
    super("SQ stop request watcher", commands::askedForStop, scheduler::stop);

    this.settings = settings;
  }

  public static StopRequestWatcherImpl create(AppSettings settings, Scheduler scheduler, FileSystem fs) {
    DefaultProcessCommands commands = DefaultProcessCommands.secondary(fs.getTempDir(), ProcessId.APP.getIpcIndex());
    return new StopRequestWatcherImpl(settings, scheduler, commands);
  }

  @Override
  public void startWatching() {
    if (settings.getProps().valueAsBoolean(ENABLE_STOP_COMMAND.getKey())) {
      super.startWatching();
    }
  }
}
