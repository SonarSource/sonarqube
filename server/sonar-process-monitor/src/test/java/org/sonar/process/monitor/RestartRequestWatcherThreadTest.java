/*
 * SonarQube :: Process Monitor
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
package org.sonar.process.monitor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.DefaultProcessCommands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RestartRequestWatcherThreadTest {
  private static final long TEST_DELAYS_MS = 5L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private Monitor monitor = mock(Monitor.class);

  @Test
  public void constructor_throws_NPE_if_monitor_arg_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("monitor can not be null");

    new RestartRequestWatcherThread(null, Collections.<ProcessRef>emptyList());
  }

  @Test
  public void constructor_throws_NPE_if_processes_arg_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("processes can not be null");

    new RestartRequestWatcherThread(monitor, null);
  }

  @Test
  public void each_RestartRequestWatcherThread_instance_get_a_unique_thread_name() {
    assertThat(newSingleProcessRefRestartWatcher().getName())
        .isNotEqualTo(newSingleProcessRefRestartWatcher().getName());
  }

  @Test
  public void does_not_stop_watching_when_no_processRef_requests_restart() throws Exception {
    RestartRequestWatcherThread underTest = newSingleProcessRefRestartWatcher();

    underTest.start();

    Thread.sleep(200L);

    assertThat(underTest.isWatching()).isTrue();
    assertThat(underTest.isAlive()).isTrue();
  }

  @Test(timeout = 500L)
  public void stops_watching_when_any_processRef_requests_restart() throws Exception {
    ProcessRef processRef1 = newProcessRef(1);
    ProcessRef processRef2 = newProcessRef(2);
    RestartRequestWatcherThread underTest = newSingleProcessRefRestartWatcher(processRef1, processRef2);

    underTest.start();

    Thread.sleep(123L);

    if (new Random().nextInt() % 2 == 1) {
      processRef1.getCommands().askForRestart();
    } else {
      processRef2.getCommands().askForRestart();
    }

    underTest.join();

    assertThat(underTest.isWatching()).isFalse();
    verify(monitor).restartAsync();
  }

  private RestartRequestWatcherThread newSingleProcessRefRestartWatcher(ProcessRef... processRefs) {
    return new RestartRequestWatcherThread(monitor, Arrays.asList(processRefs), TEST_DELAYS_MS);
  }

  private ProcessRef newProcessRef(int id) {
    try {
      return new ProcessRef(String.valueOf(id), new DefaultProcessCommands(temp.newFolder(), id), mock(Process.class), mock(StreamGobbler.class));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
