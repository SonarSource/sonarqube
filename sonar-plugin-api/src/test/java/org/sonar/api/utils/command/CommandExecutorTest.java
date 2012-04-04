/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.utils.command;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.number.OrderingComparisons.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

public class CommandExecutorTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Rule
  public TestName testName = new TestName();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private File workDir;

  @Before
  public void setUp() {
    workDir = tempFolder.newFolder(testName.getMethodName());
  }

  @Test
  public void shouldConsumeStdOutAndStdErr() throws Exception {
    final StringBuilder stdOutBuilder = new StringBuilder();
    StreamConsumer stdOutConsumer = new StreamConsumer() {
      public void consumeLine(String line) {
        stdOutBuilder.append(line).append(SystemUtils.LINE_SEPARATOR);
      }
    };
    final StringBuilder stdErrBuilder = new StringBuilder();
    StreamConsumer stdErrConsumer = new StreamConsumer() {
      public void consumeLine(String line) {
        stdErrBuilder.append(line).append(SystemUtils.LINE_SEPARATOR);
      }
    };
    Command command = Command.create(getScript("output")).setDirectory(workDir);
    int exitCode = CommandExecutor.create().execute(command, stdOutConsumer, stdErrConsumer, 1000L);
    assertThat(exitCode, is(0));

    String stdOut = stdOutBuilder.toString();
    String stdErr = stdErrBuilder.toString();
    assertThat(stdOut, containsString("stdOut: first line"));
    assertThat(stdOut, containsString("stdOut: second line"));
    assertThat(stdErr, containsString("stdErr: first line"));
    assertThat(stdErr, containsString("stdErr: second line"));
  }

  @Test
  public void stdOutConsumerCanThrowException() throws Exception {
    Command command = Command.create(getScript("output")).setDirectory(workDir);
    thrown.expect(CommandException.class);
    thrown.expectMessage("Error inside stdOut parser");
    CommandExecutor.create().execute(command, BAD_CONSUMER, NOP_CONSUMER, 1000L);
  }

  @Test
  public void stdErrConsumerCanThrowException() throws Exception {
    Command command = Command.create(getScript("output")).setDirectory(workDir);
    thrown.expect(CommandException.class);
    thrown.expectMessage("Error inside stdErr parser");
    CommandExecutor.create().execute(command, NOP_CONSUMER, BAD_CONSUMER, 1000L);
  }

  private static final StreamConsumer NOP_CONSUMER = new StreamConsumer() {
    public void consumeLine(String line) {
      // nop
    }
  };

  private static final StreamConsumer BAD_CONSUMER = new StreamConsumer() {
    public void consumeLine(String line) {
      throw new RuntimeException();
    }
  };

  @Test
  public void shouldEchoArguments() throws IOException {
    String executable = getScript("echo");
    int exitCode = CommandExecutor.create().execute(Command.create(executable), 1000L);
    assertThat(exitCode, is(0));

    // the script generates a log in current directory
    FileUtils.deleteQuietly(new File("echo.log"));
  }

  @Test
  public void shouldConfigureWorkingDirectory() throws IOException {
    String executable = getScript("echo");

    int exitCode = CommandExecutor.create().execute(Command.create(executable).setDirectory(workDir), 1000L);
    assertThat(exitCode, is(0));

    File log = new File(workDir, "echo.log");
    assertThat(FileUtils.readFileToString(log), containsString(workDir.getCanonicalPath()));
  }

  @Test
  public void shouldStopWithTimeout() throws IOException {
    String executable = getScript("forever");
    long start = System.currentTimeMillis();
    try {
      CommandExecutor.create().execute(Command.create(executable).setDirectory(workDir), 300L);
      fail();
    } catch (CommandException e) {
      long duration = System.currentTimeMillis() - start;
      // should test >= 300 but it strangly fails during build on windows.
      // The timeout is raised after 297ms (??)
      assertThat(e.getMessage(), duration, greaterThanOrEqualTo(290L));
    }
  }

  @Test
  public void shouldFailIfScriptNotFound() {
    thrown.expect(CommandException.class);
    CommandExecutor.create().execute(Command.create("notfound").setDirectory(workDir), 1000L);
  }

  private static String getScript(String name) throws IOException {
    String filename;
    if (SystemUtils.IS_OS_WINDOWS) {
      filename = name + ".bat";
    } else {
      filename = name + ".sh";
    }
    return new File("src/test/scripts/" + filename).getCanonicalPath();
  }

}
