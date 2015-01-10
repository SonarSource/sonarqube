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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProcessCommandsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void fail_to_init_if_dir_does_not_exist() throws Exception {
    File dir = temp.newFolder();
    FileUtils.deleteQuietly(dir);

    try {
      new ProcessCommands(dir, "web");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Not a valid directory: " + dir.getAbsolutePath());
    }
  }

  @Test
  public void delete_files_on_prepare() throws Exception {
    File dir = temp.newFolder();
    assertThat(dir).exists();
    FileUtils.touch(new File(dir, "web.ready"));
    FileUtils.touch(new File(dir, "web.stop"));

    ProcessCommands commands = new ProcessCommands(dir, "web");
    commands.prepare();

    assertThat(commands.getReadyFile()).doesNotExist();
    assertThat(commands.getStopFile()).doesNotExist();
  }

  @Test
  public void fail_to_prepare_if_file_is_locked() throws Exception {
    File readyFile = mock(File.class);
    when(readyFile.exists()).thenReturn(true);
    when(readyFile.delete()).thenReturn(false);

    ProcessCommands commands = new ProcessCommands(readyFile, temp.newFile());
    try {
      commands.prepare();
      fail();
    } catch (MessageException e) {
      // ok
    }
  }

  @Test
  public void child_process_create_file_when_ready() throws Exception {
    File dir = temp.newFolder();

    ProcessCommands commands = new ProcessCommands(dir, "web");
    commands.prepare();
    assertThat(commands.isReady()).isFalse();
    assertThat(commands.getReadyFile()).doesNotExist();

    commands.setReady();
    assertThat(commands.isReady()).isTrue();
    assertThat(commands.getReadyFile()).exists().isFile();

    commands.endWatch();
    assertThat(commands.getReadyFile()).doesNotExist();
  }

  @Test
  public void ask_for_stop() throws Exception {
    File dir = temp.newFolder();

    ProcessCommands commands = new ProcessCommands(dir, "web");
    assertThat(commands.askedForStop()).isFalse();
    assertThat(commands.getStopFile()).doesNotExist();

    commands.askForStop();
    assertThat(commands.askedForStop()).isTrue();
    assertThat(commands.getStopFile()).exists().isFile();
    assertThat(commands.getStopFile().getName()).isEqualTo("web.stop");
  }
}
