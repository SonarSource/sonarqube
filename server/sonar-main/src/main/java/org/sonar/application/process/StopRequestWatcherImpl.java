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

import org.sonar.application.FileSystem;
import org.sonar.application.Scheduler;
import org.sonar.application.config.AppSettings;
import org.sonar.process.ProcessId;
import org.sonar.process.sharedmemoryfile.DefaultProcessCommands;
import org.sonar.process.sharedmemoryfile.ProcessCommands;

import static org.sonar.process.ProcessProperties.Property.ENABLE_STOP_COMMAND;

public class StopRequestWatcherImpl extends Thread implements StopRequestWatcher {

  private static final long DEFAULT_WATCHER_DELAY_MS = 500L;

  private final ProcessCommands commands;
  private final Scheduler scheduler;
  private final AppSettings settings;
  private long delayMs = DEFAULT_WATCHER_DELAY_MS;

  StopRequestWatcherImpl(AppSettings settings, Scheduler scheduler, ProcessCommands commands) {
    super("StopRequestWatcherImpl");
    this.settings = settings;
    this.commands = commands;
    this.scheduler = scheduler;

    // safeguard, do not block the JVM if thread is not interrupted
    // (method stopWatching() never called).
    setDaemon(true);
  }

  public static StopRequestWatcherImpl create(AppSettings settings, Scheduler scheduler, FileSystem fs) {
    DefaultProcessCommands commands = DefaultProcessCommands.secondary(fs.getTempDir(), ProcessId.APP.getIpcIndex());
    return new StopRequestWatcherImpl(settings, scheduler, commands);
  }

  long getDelayMs() {
    return delayMs;
  }

  void setDelayMs(long delayMs) {
    this.delayMs = delayMs;
  }

  @Override
  public void run() {
    try {
      while (true) {
        if (commands.askedForStop()) {
          scheduler.terminate();
          return;
        }
        Thread.sleep(delayMs);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      // stop watching the commands
    }
  }

  @Override
  public void startWatching() {
    if (settings.getProps().valueAsBoolean(ENABLE_STOP_COMMAND.getKey())) {
      start();
    }
  }

  @Override
  public void stopWatching() {
    // does nothing is not started
    interrupt();
  }
}
