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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class StopperThreadTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();


  @Test(timeout = 3000L)
  public void stop_in_a_timely_fashion() throws Exception {
    File file = temp.newFile();
    SharedStatus sharedStatus = new SharedStatus(file);
    assertThat(file).exists();
    Monitored monitored = mock(Monitored.class);

    // max stop timeout is 5 seconds, but test fails after 3 seconds
    // -> guarantees that stop is immediate
    StopperThread stopper = new StopperThread(monitored, sharedStatus, 5000L);
    stopper.start();
    stopper.join();

    verify(monitored).stop();
    assertThat(file).doesNotExist();
  }

  @Test(timeout = 3000L)
  public void stop_timeout() throws Exception {
    File file = temp.newFile();
    SharedStatus sharedStatus = new SharedStatus(file);
    assertThat(file).exists();
    Monitored monitored = mock(Monitored.class);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        Thread.sleep(10000L);
        return null;
      }
    }).when(monitored).stop();

    // max stop timeout is 10 milliseconds
    StopperThread stopper = new StopperThread(monitored, sharedStatus, 10L);
    stopper.start();
    stopper.join();

    verify(monitored).stop();
    assertThat(file).doesNotExist();
  }
}
