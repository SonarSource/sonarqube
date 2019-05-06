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

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonar.application.config.TestAppSettings;
import org.sonar.process.sharedmemoryfile.ProcessCommands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property.ENABLE_STOP_COMMAND;

public class StopRequestWatcherImplTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));

  private TestAppSettings appSettings = new TestAppSettings();
  private ProcessCommands commands = mock(ProcessCommands.class);
  private Scheduler scheduler = mock(Scheduler.class);

  @Test
  public void startWatching_does_not_start_thread_if_stop_command_is_disabled() {
    StopRequestWatcherImpl underTest = new StopRequestWatcherImpl(appSettings, scheduler, commands);

    underTest.startWatching();
    assertThat(underTest.isAlive()).isFalse();
  }

  @Test
  public void watch_stop_command_if_stop_command_is_enabled() {
    appSettings.set(ENABLE_STOP_COMMAND.getKey(), "true");
    StopRequestWatcherImpl underTest = new StopRequestWatcherImpl(appSettings, scheduler, commands);

    underTest.startWatching();
    assertThat(underTest.isAlive()).isTrue();
    verify(scheduler, never()).stop();

    when(commands.askedForStop()).thenReturn(true);
    verify(scheduler, timeout(1_000L)).stop();

    underTest.stopWatching();

    await().until(() -> !underTest.isAlive());
    assertThat(underTest.isAlive()).isFalse();
  }

  @Test
  public void create_instance_with_default_delay() throws IOException {
    FileSystem fs = mock(FileSystem.class);
    when(fs.getTempDir()).thenReturn(temp.newFolder());

    StopRequestWatcherImpl underTest = StopRequestWatcherImpl.create(appSettings, scheduler, fs);

    assertThat(underTest.getDelayMs()).isEqualTo(500L);
  }

  @Test
  public void stop_watching_commands_if_thread_is_interrupted() {
    StopRequestWatcherImpl underTest = new StopRequestWatcherImpl(appSettings, scheduler, commands);

    underTest.startWatching();
    underTest.interrupt();

    await().until(() -> !underTest.isAlive());
    assertThat(underTest.isAlive()).isFalse();
  }

}
