/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.process.sharedmemoryfile.ProcessCommands;
import org.sonar.process.ProcessId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ProcessCommandsProcessMonitorTest {

  @Test
  public void ProcessMonitorImpl_is_a_proxy_of_Process() throws Exception {
    Process process = mock(Process.class, RETURNS_DEEP_STUBS);
    ProcessCommands commands = mock(ProcessCommands.class, RETURNS_DEEP_STUBS);

    ProcessCommandsProcessMonitor underTest = new ProcessCommandsProcessMonitor(process, ProcessId.WEB_SERVER, commands);

    underTest.waitFor();
    verify(process).waitFor();

    underTest.closeStreams();
    verify(process.getErrorStream()).close();
    verify(process.getInputStream()).close();
    verify(process.getOutputStream()).close();

    underTest.destroyForcibly();
    verify(process).destroyForcibly();

    assertThat(underTest.getInputStream()).isNotNull();

    underTest.isAlive();
    verify(process).isAlive();

    underTest.waitFor(123, TimeUnit.MILLISECONDS);
    verify(process).waitFor(123, TimeUnit.MILLISECONDS);
  }

  @Test
  public void ProcessMonitorImpl_is_a_proxy_of_Commands() {
    Process process = mock(Process.class, RETURNS_DEEP_STUBS);
    ProcessCommands commands = mock(ProcessCommands.class, RETURNS_DEEP_STUBS);

    ProcessCommandsProcessMonitor underTest = new ProcessCommandsProcessMonitor(process, null, commands);

    underTest.askForStop();
    verify(commands).askForStop();

    underTest.acknowledgeAskForRestart();
    verify(commands).acknowledgeAskForRestart();

    underTest.askedForRestart();
    verify(commands).askedForRestart();

    underTest.isOperational();
    verify(commands).isOperational();
  }

  @Test
  public void closeStreams_ignores_null_stream() {
    ProcessCommands commands = mock(ProcessCommands.class);
    Process process = mock(Process.class);
    when(process.getInputStream()).thenReturn(null);

    ProcessCommandsProcessMonitor underTest = new ProcessCommandsProcessMonitor(process, null, commands);

    // no failures
    underTest.closeStreams();
  }

  @Test
  public void closeStreams_ignores_failure_if_stream_fails_to_be_closed() throws Exception {
    InputStream stream = mock(InputStream.class);
    doThrow(new IOException("error")).when(stream).close();
    Process process = mock(Process.class);
    when(process.getInputStream()).thenReturn(stream);

    ProcessCommandsProcessMonitor underTest = new ProcessCommandsProcessMonitor(process, null, mock(ProcessCommands.class, Mockito.RETURNS_MOCKS));

    // no failures
    underTest.closeStreams();
  }
}
