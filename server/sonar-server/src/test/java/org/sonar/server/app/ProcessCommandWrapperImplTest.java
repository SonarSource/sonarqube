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
package org.sonar.server.app;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.process.sharedmemoryfile.DefaultProcessCommands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_PROCESS_INDEX;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_SHARED_PATH;

public class ProcessCommandWrapperImplTest {
  private static final int PROCESS_NUMBER = 2;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MapSettings settings = new MapSettings();

  @Test
  public void requestSQRestart_throws_IAE_if_process_index_property_not_set() {
    ProcessCommandWrapperImpl processCommandWrapper = new ProcessCommandWrapperImpl(settings.asConfig());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Property process.index is not set");

    processCommandWrapper.requestSQRestart();
  }

  @Test
  public void requestSQRestart_throws_IAE_if_process_shared_path_property_not_set() {
    settings.setProperty(PROPERTY_PROCESS_INDEX, 1);
    ProcessCommandWrapperImpl processCommandWrapper = new ProcessCommandWrapperImpl(settings.asConfig());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Property process.sharedDir is not set");

    processCommandWrapper.requestSQRestart();
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

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Property process.sharedDir is not set");

    processCommandWrapper.requestHardStop();
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

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Property process.sharedDir is not set");

    processCommandWrapper.notifyOperational();
  }

  @Test
  public void notifyOperational_throws_IAE_if_process_index_property_not_set() {
    ProcessCommandWrapperImpl processCommandWrapper = new ProcessCommandWrapperImpl(settings.asConfig());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Property process.index is not set");

    processCommandWrapper.notifyOperational();
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
  public void isCeOperational_reads_shared_memory_operational_flag_in_location_3() throws IOException {
    File tmpDir = temp.newFolder().getAbsoluteFile();
    settings.setProperty(PROPERTY_SHARED_PATH, tmpDir.getAbsolutePath());

    boolean expected = new Random().nextBoolean();
    if (expected) {
      try (DefaultProcessCommands processCommands = DefaultProcessCommands.secondary(tmpDir, 3)) {
        processCommands.setOperational();
      }
    }

    ProcessCommandWrapperImpl underTest = new ProcessCommandWrapperImpl(settings.asConfig());

    assertThat(underTest.isCeOperational()).isEqualTo(expected);
  }

}
