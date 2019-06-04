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
package org.sonar.api.utils.command;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.sonar.api.utils.System2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class CommandExecutorTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Rule
  public TestName testName = new TestName();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private File workDir;

  @Before
  public void before() throws IOException {
    workDir = tempFolder.newFolder(testName.getMethodName());
  }

  @Test(timeout = 3000L)
  public void should_consume_StdOut_and_StdErr() throws Exception {
    // too many false-positives on MS windows
    if (!SystemUtils.IS_OS_WINDOWS) {
      StringBuilder stdOutBuilder = new StringBuilder();
      StreamConsumer stdOutConsumer = line -> stdOutBuilder.append(line).append(SystemUtils.LINE_SEPARATOR);
      StringBuilder stdErrBuilder = new StringBuilder();
      StreamConsumer stdErrConsumer = line -> stdErrBuilder.append(line).append(SystemUtils.LINE_SEPARATOR);
      Command command = Command.create(getScript("output")).setDirectory(workDir);
      int exitCode = CommandExecutor.create().execute(command, stdOutConsumer, stdErrConsumer, 1000L);
      assertThat(exitCode).isEqualTo(0);

      String stdOut = stdOutBuilder.toString();
      String stdErr = stdErrBuilder.toString();
      assertThat(stdOut).contains("stdOut: first line");
      assertThat(stdOut).contains("stdOut: second line");
      assertThat(stdErr).contains("stdErr: first line");
      assertThat(stdErr).contains("stdErr: second line");
    }
  }

  @Test(timeout = 3000L)
  public void stdOut_consumer_can_throw_exception() throws Exception {
    Command command = Command.create(getScript("output")).setDirectory(workDir);
    thrown.expect(CommandException.class);
    thrown.expectMessage("Error inside stdOut stream");
    CommandExecutor.create().execute(command, BAD_CONSUMER, NOP_CONSUMER, 1000L);
  }

  @Test(timeout = 3000L)
  public void stdErr_consumer_can_throw_exception() throws Exception {
    Command command = Command.create(getScript("output")).setDirectory(workDir);
    thrown.expect(CommandException.class);
    thrown.expectMessage("Error inside stdErr stream");
    CommandExecutor.create().execute(command, NOP_CONSUMER, BAD_CONSUMER, 1500L);
  }

  private static final StreamConsumer NOP_CONSUMER = line -> {
  };

  private static final StreamConsumer BAD_CONSUMER = line -> {
    throw new RuntimeException();
  };

  @Test
  public void should_use_working_directory_to_store_argument_and_environment_variable() throws Exception {
    Command command = Command.create(getScript("echo"))
      .setDirectory(workDir)
      .addArgument("1")
      .setEnvironmentVariable("ENVVAR", "2");
    int exitCode = CommandExecutor.create().execute(command, 1000L);
    assertThat(exitCode).isEqualTo(0);
    File logFile = new File(workDir, "echo.log");
    assertThat(logFile).exists();
    String log = FileUtils.readFileToString(logFile);
    assertThat(log).contains(workDir.getAbsolutePath());
    assertThat(log).contains("Parameter: 1");
    assertThat(log).contains("Environment variable: 2");
  }

  @Test(timeout = 6000L)
  public void should_stop_after_timeout() throws IOException {
    try {
      String executable = getScript("forever");
      CommandExecutor.create().execute(Command.create(executable).setDirectory(workDir), 100);
      fail();
    } catch (TimeoutException e) {
      // ok
    }
  }

  @Test(timeout = 6000L)
  public void should_stop_after_timeout_and_new_shell() throws IOException {
    try {
      String executable = getScript("forever");
      CommandExecutor.create().execute(Command.create(executable).setNewShell(true).setDirectory(workDir), 100);
      fail();
    } catch (TimeoutException e) {
      // ok
    }
  }

  @Test
  public void should_fail_if_script_not_found() {
    thrown.expect(CommandException.class);
    CommandExecutor.create().execute(Command.create("notfound").setDirectory(workDir), 1000L);
  }

  private static String getScript(String name) throws IOException {
    String filename;
    if (System2.INSTANCE.isOsWindows()) {
      filename = name + ".bat";
    } else {
      filename = name + ".sh";
    }
    return new File("src/test/scripts/" + filename).getCanonicalPath();
  }

}
