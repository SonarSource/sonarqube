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

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonar.application.FileSystem;
import org.sonar.application.Scheduler;
import org.sonar.application.config.AppSettings;
import org.sonar.process.sharedmemoryfile.ProcessCommands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property.ENABLE_STOP_COMMAND;

public class StopRequestWatcherImplTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));

  private AppSettings settings = mock(AppSettings.class, RETURNS_DEEP_STUBS);
  private ProcessCommands commands = mock(ProcessCommands.class);
  private Scheduler scheduler = mock(Scheduler.class);

  @Test
  public void do_not_watch_command_if_disabled() {
    enableSetting(false);
    StopRequestWatcherImpl underTest = new StopRequestWatcherImpl(settings, scheduler, commands);

    underTest.startWatching();
    assertThat(underTest.isAlive()).isFalse();

    underTest.stopWatching();
    verifyZeroInteractions(commands, scheduler);
  }

  @Test
  public void watch_stop_command_if_enabled() throws Exception {
    enableSetting(true);
    StopRequestWatcherImpl underTest = new StopRequestWatcherImpl(settings, scheduler, commands);
    underTest.setDelayMs(1L);

    underTest.startWatching();
    assertThat(underTest.isAlive()).isTrue();
    verify(scheduler, never()).terminate();

    when(commands.askedForStop()).thenReturn(true);
    verify(scheduler, timeout(1_000L)).terminate();

    underTest.stopWatching();
    while (underTest.isAlive()) {
      Thread.sleep(1L);
    }
  }

  @Test
  public void create_instance_with_default_delay() throws IOException {
    FileSystem fs = mock(FileSystem.class);
    when(fs.getTempDir()).thenReturn(temp.newFolder());

    StopRequestWatcherImpl underTest = StopRequestWatcherImpl.create(settings, scheduler, fs);

    assertThat(underTest.getDelayMs()).isEqualTo(500L);
  }

  @Test
  public void stop_watching_commands_if_thread_is_interrupted() throws Exception {
    enableSetting(true);
    StopRequestWatcherImpl underTest = new StopRequestWatcherImpl(settings, scheduler, commands);

    underTest.startWatching();
    underTest.interrupt();

    while (underTest.isAlive()) {
      Thread.sleep(1L);
    }
    assertThat(underTest.isAlive()).isFalse();
  }

  private void enableSetting(boolean b) {
    when(settings.getProps().valueAsBoolean(ENABLE_STOP_COMMAND.getKey())).thenReturn(b);
  }

}
