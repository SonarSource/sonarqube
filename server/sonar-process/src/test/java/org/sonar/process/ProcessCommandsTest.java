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

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.junit.Assert.fail;

public class ProcessCommandsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void fail_to_init_if_dir_does_not_exist() throws Exception {
    File dir = temp.newFolder();
    FileUtils.deleteQuietly(dir);

    try {
      new ProcessCommands(dir, 1);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Not a valid directory: " + dir.getAbsolutePath());
    }
  }

  @Test
  public void child_process_update_the_mapped_memory() throws Exception {
    File dir = temp.newFolder();

    ProcessCommands commands = new ProcessCommands(dir, 1);
    assertThat(commands.isReady()).isFalse();
    assertThat(commands.mappedByteBuffer.get(commands.offset())).isEqualTo(ProcessCommands.EMPTY);
    assertThat(commands.mappedByteBuffer.getLong(2 + commands.offset())).isEqualTo(0L);

    commands.setReady();
    assertThat(commands.isReady()).isTrue();
    assertThat(commands.mappedByteBuffer.get(commands.offset())).isEqualTo(ProcessCommands.READY);

    long currentTime = System.currentTimeMillis();
    commands.ping();
    assertThat(commands.mappedByteBuffer.getLong(2 + commands.offset())).isGreaterThanOrEqualTo(currentTime);
  }

  @Test
  public void ask_for_stop() throws Exception {
    File dir = temp.newFolder();

    ProcessCommands commands = new ProcessCommands(dir, 1);
    assertThat(commands.mappedByteBuffer.get(commands.offset() + 1)).isNotEqualTo(ProcessCommands.STOP);
    assertThat(commands.askedForStop()).isFalse();

    commands.askForStop();
    assertThat(commands.askedForStop()).isTrue();
    assertThat(commands.mappedByteBuffer.get(commands.offset() + 1)).isEqualTo(ProcessCommands.STOP);
  }

  @Test
  public void test_max_processes() throws Exception {
    File dir = temp.newFolder();
    try {
      new ProcessCommands(dir, -2);
      failBecauseExceptionWasNotThrown(AssertionError.class);
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Incorrect process number");
    }
    try {
      new ProcessCommands(dir, ProcessCommands.getMaxProcesses() + 1);
      failBecauseExceptionWasNotThrown(AssertionError.class);
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Incorrect process number");
    }
  }
}
