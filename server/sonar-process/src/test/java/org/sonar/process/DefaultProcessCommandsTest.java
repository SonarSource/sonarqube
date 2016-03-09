/*
 * SonarQube
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
package org.sonar.process;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonar.process.ProcessCommands.MAX_PROCESSES;

public class DefaultProcessCommandsTest {

  private static final int PROCESS_NUMBER = 1;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void fail_to_init_if_dir_does_not_exist() throws Exception {
    File dir = temp.newFolder();
    FileUtils.deleteQuietly(dir);

    try {
      DefaultProcessCommands.main(dir, PROCESS_NUMBER);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Not a valid directory: " + dir.getAbsolutePath());
    }
  }

  @Test
  public void child_process_update_the_mapped_memory() throws Exception {
    File dir = temp.newFolder();

    DefaultProcessCommands commands = DefaultProcessCommands.main(dir, PROCESS_NUMBER);
    assertThat(commands.isUp()).isFalse();

    commands.setUp();
    assertThat(commands.isUp()).isTrue();
  }

  @Test
  public void ask_for_stop() throws Exception {
    File dir = temp.newFolder();

    DefaultProcessCommands commands = DefaultProcessCommands.main(dir, PROCESS_NUMBER);
    assertThat(commands.askedForStop()).isFalse();

    commands.askForStop();
    assertThat(commands.askedForStop()).isTrue();
  }

  @Test
  public void ask_for_restart() throws Exception {
    File dir = temp.newFolder();

    DefaultProcessCommands commands = DefaultProcessCommands.main(dir, PROCESS_NUMBER);
    assertThat(commands.askedForRestart()).isFalse();

    commands.askForRestart();
    assertThat(commands.askedForRestart()).isTrue();
  }

  @Test
  public void acknowledgeAskForRestart_has_no_effect_when_no_restart_asked() throws Exception {
    File dir = temp.newFolder();

    DefaultProcessCommands commands = DefaultProcessCommands.main(dir, PROCESS_NUMBER);
    assertThat(commands.askedForRestart()).isFalse();

    commands.acknowledgeAskForRestart();
    assertThat(commands.askedForRestart()).isFalse();
  }

  @Test
  public void acknowledgeAskForRestart_resets_askForRestart_has_no_effect_when_no_restart_asked() throws Exception {
    File dir = temp.newFolder();

    DefaultProcessCommands commands = DefaultProcessCommands.main(dir, PROCESS_NUMBER);

    commands.askForRestart();
    assertThat(commands.askedForRestart()).isTrue();

    commands.acknowledgeAskForRestart();
    assertThat(commands.askedForRestart()).isFalse();
  }

  @Test
  public void main_fails_if_processNumber_is_less_than_0() throws Exception {
    int processNumber = -2;

    expectProcessNumberNoValidIAE(processNumber);

    DefaultProcessCommands.main(temp.newFolder(), processNumber);
  }

  @Test
  public void main_fails_if_processNumber_is_higher_than_MAX_PROCESSES() throws Exception {
    int processNumber = MAX_PROCESSES + 1;

    expectProcessNumberNoValidIAE(processNumber);

    DefaultProcessCommands.main(temp.newFolder(), processNumber);
  }

  @Test
  public void main_fails_if_processNumber_is_MAX_PROCESSES() throws Exception {
    int processNumber = MAX_PROCESSES;

    expectProcessNumberNoValidIAE(processNumber);

    DefaultProcessCommands.main(temp.newFolder(), processNumber);
  }

  @Test
  public void secondary_fails_if_processNumber_is_less_than_0() throws Exception {
    int processNumber = -2;

    expectProcessNumberNoValidIAE(processNumber);

    DefaultProcessCommands.secondary(temp.newFolder(), processNumber);
  }

  @Test
  public void secondary_fails_if_processNumber_is_higher_than_MAX_PROCESSES() throws Exception {
    int processNumber = MAX_PROCESSES + 1;

    expectProcessNumberNoValidIAE(processNumber);

    DefaultProcessCommands.secondary(temp.newFolder(), processNumber);
  }

  private void expectProcessNumberNoValidIAE(int processNumber) {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Process number " + processNumber + " is not valid");
  }
}
