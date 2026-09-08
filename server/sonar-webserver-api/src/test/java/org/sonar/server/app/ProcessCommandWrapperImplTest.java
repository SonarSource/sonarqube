/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.app;

import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.process.sharedmemoryfile.DefaultProcessCommands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_PROCESS_INDEX;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_SHARED_PATH;

public class ProcessCommandWrapperImplTest {
  private static final int PROCESS_NUMBER = 2;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private MapSettings settings = new MapSettings();

  @Test
  public void requestSQRestart_throws_IAE_if_process_index_property_not_set() {
    ProcessCommandWrapperImpl processCommandWrapper = new ProcessCommandWrapperImpl(settings.asConfig());

    assertThatThrownBy(processCommandWrapper::requestSQRestart)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Property process.index is not set");
  }

  @Test
  public void requestSQRestart_throws_IAE_if_process_shared_path_property_not_set() {
    settings.setProperty(PROPERTY_PROCESS_INDEX, 1);
    ProcessCommandWrapperImpl processCommandWrapper = new ProcessCommandWrapperImpl(settings.asConfig());

    assertThatThrownBy(processCommandWrapper::requestSQRestart)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Property process.sharedDir is not set");
  }

  @Test
  public void requestSQRestart_updates_shareMemory_file() throws IOException {
    File tmpDir = temp.newFolder().getAbsoluteFile();
    settings.setProperty(PROPERTY_SHARED_PATH, tmpDir.getAbsolutePath());
    settings.setProperty(PROPERTY_PROCESS_INDEX, PROCESS_NUMBER);

    ProcessCommandWrapperImpl underTest = new ProcessCommandWrapperImpl(settings.asConfig());
    underTest.requestSQRestart();

    try (DefaultProcessCommands processCommands = DefaultProcessCommands.secondary(tmpDir, PROCESS_NUMBER)) {
      assertThat(processCommands.askedForRestart()).isTrue();
    }
  }

  @Test
  public void requestSQStop_throws_IAE_if_process_shared_path_property_not_set() {
    settings.setProperty(PROPERTY_PROCESS_INDEX, 1);
    ProcessCommandWrapperImpl processCommandWrapper = new ProcessCommandWrapperImpl(settings.asConfig());

    assertThatThrownBy(processCommandWrapper::requestHardStop)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Property process.sharedDir is not set");
  }

  @Test
  public void requestSQStop_updates_shareMemory_file() throws IOException {
    File tmpDir = temp.newFolder().getAbsoluteFile();
    settings.setProperty(PROPERTY_SHARED_PATH, tmpDir.getAbsolutePath());
    settings.setProperty(PROPERTY_PROCESS_INDEX, PROCESS_NUMBER);

    ProcessCommandWrapperImpl underTest = new ProcessCommandWrapperImpl(settings.asConfig());
    underTest.requestHardStop();

    try (DefaultProcessCommands processCommands = DefaultProcessCommands.secondary(tmpDir, PROCESS_NUMBER)) {
      assertThat(processCommands.askedForHardStop()).isTrue();
    }
  }

  @Test
  public void notifyOperational_throws_IAE_if_process_sharedDir_property_not_set() {
    settings.setProperty(PROPERTY_PROCESS_INDEX, 1);
    ProcessCommandWrapperImpl processCommandWrapper = new ProcessCommandWrapperImpl(settings.asConfig());

    assertThatThrownBy(processCommandWrapper::notifyOperational)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Property process.sharedDir is not set");
  }

  @Test
  public void notifyOperational_throws_IAE_if_process_index_property_not_set() {
    ProcessCommandWrapperImpl processCommandWrapper = new ProcessCommandWrapperImpl(settings.asConfig());

    assertThatThrownBy(processCommandWrapper::notifyOperational)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Property process.index is not set");
  }

  @Test
  public void notifyOperational_updates_shareMemory_file() throws IOException {
    File tmpDir = temp.newFolder().getAbsoluteFile();
    settings.setProperty(PROPERTY_SHARED_PATH, tmpDir.getAbsolutePath());
    settings.setProperty(PROPERTY_PROCESS_INDEX, PROCESS_NUMBER);

    ProcessCommandWrapperImpl underTest = new ProcessCommandWrapperImpl(settings.asConfig());
    underTest.notifyOperational();

    try (DefaultProcessCommands processCommands = DefaultProcessCommands.secondary(tmpDir, PROCESS_NUMBER)) {
      assertThat(processCommands.isOperational()).isTrue();
    }
  }

  @Test
  public void isCeOperational_returns_false_when_operational_flag_not_set() throws IOException {
    File tmpDir = temp.newFolder().getAbsoluteFile();
    settings.setProperty(PROPERTY_SHARED_PATH, tmpDir.getAbsolutePath());

    ProcessCommandWrapperImpl underTest = new ProcessCommandWrapperImpl(settings.asConfig());

    assertThat(underTest.isCeOperational()).isFalse();
  }

  @Test
  public void isCeOperational_returns_true_when_operational_flag_set_and_ping_is_fresh() throws IOException {
    File tmpDir = temp.newFolder().getAbsoluteFile();
    settings.setProperty(PROPERTY_SHARED_PATH, tmpDir.getAbsolutePath());

    try (DefaultProcessCommands processCommands = DefaultProcessCommands.secondary(tmpDir, 3)) {
      processCommands.setOperational();
      processCommands.ping();
    }

    ProcessCommandWrapperImpl underTest = new ProcessCommandWrapperImpl(settings.asConfig());

    assertThat(underTest.isCeOperational()).isTrue();
  }

  @Test
  public void isCeOperational_returns_false_when_operational_flag_set_but_ping_never_written() throws IOException {
    File tmpDir = temp.newFolder().getAbsoluteFile();
    settings.setProperty(PROPERTY_SHARED_PATH, tmpDir.getAbsolutePath());

    // Operational set but ping never written (lastPing == 0) — simulates CE dying before ever pinging
    try (DefaultProcessCommands processCommands = DefaultProcessCommands.secondary(tmpDir, 3)) {
      processCommands.setOperational();
    }

    ProcessCommandWrapperImpl underTest = new ProcessCommandWrapperImpl(settings.asConfig());

    assertThat(underTest.isCeOperational()).isFalse();
  }

  @Test
  public void isCeOperational_returns_false_when_ping_is_stale() throws IOException {
    File tmpDir = temp.newFolder().getAbsoluteFile();
    settings.setProperty(PROPERTY_SHARED_PATH, tmpDir.getAbsolutePath());

    long staleTimestamp = System.currentTimeMillis() - ProcessCommandWrapperImpl.PING_TIMEOUT_MS - 1_000L;
    try (DefaultProcessCommands processCommands = DefaultProcessCommands.secondary(tmpDir, 3)) {
      processCommands.setOperational();
      processCommands.ping(staleTimestamp);
    }

    ProcessCommandWrapperImpl underTest = new ProcessCommandWrapperImpl(settings.asConfig());

    assertThat(underTest.isCeOperational()).isFalse();
  }

}
