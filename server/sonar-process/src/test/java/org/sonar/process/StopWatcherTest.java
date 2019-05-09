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
package org.sonar.process;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonar.process.sharedmemoryfile.ProcessCommands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StopWatcherTest {

  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));

  @Test
  public void stop_if_receive_command() throws Exception {

    ProcessCommands commands = mock(ProcessCommands.class);
    when(commands.askedForHardStop()).thenReturn(false, true);
    Stoppable stoppable = mock(Stoppable.class);

    StopWatcher underTest = new StopWatcher("TheThreadName", stoppable::hardStopAsync, commands::askedForHardStop, 1L);
    underTest.start();

    while (underTest.isAlive()) {
      Thread.sleep(1L);
    }
    verify(stoppable).hardStopAsync();
    assertThat(underTest.getName()).isEqualTo("TheThreadName");
  }

  @Test
  public void stop_watching_on_interruption() throws Exception {
    ProcessCommands commands = mock(ProcessCommands.class);
    when(commands.askedForHardStop()).thenReturn(false);
    Stoppable stoppable = mock(Stoppable.class);

    StopWatcher underTest = new StopWatcher("TheThreadName", stoppable::hardStopAsync, commands::askedForHardStop, 1L);
    underTest.start();
    underTest.interrupt();

    while (underTest.isAlive()) {
      Thread.sleep(1L);
    }
    verify(stoppable, never()).hardStopAsync();
    assertThat(underTest.getName()).isEqualTo("TheThreadName");
  }
}
