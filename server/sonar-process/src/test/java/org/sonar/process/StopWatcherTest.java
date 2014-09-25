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
package org.sonar.process;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.mockito.Mockito.*;

public class StopWatcherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test(timeout = 1000L)
  public void stop_if_receive_command() throws Exception {
    ProcessCommands commands = mock(ProcessCommands.class);
    when(commands.askedForStop()).thenReturn(false).thenReturn(true);
    Stoppable stoppable = mock(Stoppable.class);

    StopWatcher watcher = new StopWatcher(commands, stoppable, 10L);
    watcher.start();
    watcher.join();

    verify(stoppable).stopAsync();
  }

  @Test(timeout = 1000L)
  public void stop_watching_on_interruption() throws Exception {
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
