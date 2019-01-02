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

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.process.sharedmemoryfile.ProcessCommands;

import static org.mockito.Mockito.*;

public class StopperThreadTest {
  ProcessCommands commands = mock(ProcessCommands.class);
  Monitored monitored = mock(Monitored.class);

  @Test
  public void stop_in_a_timely_fashion() {
    // max stop timeout is 5 seconds, but test fails after 3 seconds
    // -> guarantees that stop is immediate
    StopperThread stopper = new StopperThread(monitored, commands, 5000L);
    stopper.start();

    verify(monitored, timeout(3000)).stop();
  }

  @Test
  public void stop_timeout() {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        Thread.sleep(10000L);
        return null;
      }
    }).when(monitored).stop();

    // max stop timeout is 100 milliseconds
    StopperThread stopper = new StopperThread(monitored, commands, 100L);
    stopper.start();

    verify(monitored, timeout(3000)).stop();
  }
}
