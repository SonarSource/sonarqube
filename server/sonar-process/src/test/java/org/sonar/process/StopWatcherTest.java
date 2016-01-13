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
package org.sonar.process;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StopWatcherTest {
  @Test
  public void stop_if_receive_command() {
    ProcessCommands commands = mock(ProcessCommands.class);
    when(commands.askedForStop()).thenReturn(false, true);
    Stoppable stoppable = mock(Stoppable.class);

    StopWatcher watcher = new StopWatcher(commands, stoppable, 1L);
    watcher.start();

    verify(stoppable, timeout(5000)).stopAsync();
  }

  @Test(timeout = 5000)
  public void stop_watching_on_interruption() throws InterruptedException {
    ProcessCommands commands = mock(ProcessCommands.class);
    when(commands.askedForStop()).thenReturn(false);
    Stoppable stoppable = mock(Stoppable.class);

    StopWatcher watcher = new StopWatcher(commands, stoppable, 1000L);
    watcher.start();
    Thread.sleep(50L);
    watcher.interrupt();

    verify(stoppable, never()).stopAsync();
  }
}
