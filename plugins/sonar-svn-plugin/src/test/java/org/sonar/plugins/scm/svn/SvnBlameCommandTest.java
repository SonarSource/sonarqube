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
package org.sonar.plugins.scm.svn;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.scm.BlameCommand.BlameResult;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.command.Command;
import org.sonar.api.utils.command.CommandExecutor;
import org.sonar.api.utils.command.StreamConsumer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SvnBlameCommandTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private DefaultFileSystem fs;
  private File baseDir;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
    fs = new DefaultFileSystem();
    fs.setBaseDir(baseDir);
  }

  @Test
  public void testParsingOfOutput() throws IOException {
    File source = new File(baseDir, "src/foo.xoo");
    FileUtils.write(source, "sample content");
    DefaultInputFile inputFile = new DefaultInputFile("foo", "src/foo.xoo").setAbsolutePath(new File(baseDir, "src/foo.xoo").getAbsolutePath());
    fs.add(inputFile);

    BlameResult result = mock(BlameResult.class);
    CommandExecutor commandExecutor = mock(CommandExecutor.class);

    when(commandExecutor.execute(any(Command.class), any(StreamConsumer.class), any(StreamConsumer.class), anyLong())).thenAnswer(new Answer<Integer>() {

      @Override
      public Integer answer(InvocationOnMock invocation) throws Throwable {
        StreamConsumer outConsumer = (StreamConsumer) invocation.getArguments()[1];
        List<String> lines = FileUtils.readLines(new File("src/test/resources/blame.xml"), "UTF-8");
        for (String line : lines) {
          outConsumer.consumeLine(line);
        }
        return 0;
      }
    });

    new SvnBlameCommand(commandExecutor, mock(SvnConfiguration.class)).blame(fs, Arrays.<InputFile>asList(inputFile), result);
    verify(result).add(inputFile,
      Arrays.asList(
        new BlameLine(DateUtils.parseDateTime("2009-04-18T10:29:59+0000"), "9491", "simon.brandhof"),
        new BlameLine(DateUtils.parseDateTime("2009-04-18T10:29:59+0000"), "9491", "simon.brandhof"),
        new BlameLine(DateUtils.parseDateTime("2009-08-31T22:32:17+0000"), "10558", "david")));
  }

  @Test
  public void testParsingOfOutputWithAnonymousCommit() throws IOException {
    File source = new File(baseDir, "src/foo.xoo");
    FileUtils.write(source, "sample content");
    DefaultInputFile inputFile = new DefaultInputFile("foo", "src/foo.xoo").setAbsolutePath(new File(baseDir, "src/foo.xoo").getAbsolutePath());
    fs.add(inputFile);

    BlameResult result = mock(BlameResult.class);
    CommandExecutor commandExecutor = mock(CommandExecutor.class);

    when(commandExecutor.execute(any(Command.class), any(StreamConsumer.class), any(StreamConsumer.class), anyLong())).thenAnswer(new Answer<Integer>() {

      @Override
      public Integer answer(InvocationOnMock invocation) throws Throwable {
        StreamConsumer outConsumer = (StreamConsumer) invocation.getArguments()[1];
        List<String> lines = FileUtils.readLines(new File("src/test/resources/blame-with-anonymous-commit.xml"), "UTF-8");
        for (String line : lines) {
          outConsumer.consumeLine(line);
        }
        return 0;
      }
    });

    new SvnBlameCommand(commandExecutor, mock(SvnConfiguration.class)).blame(fs, Arrays.<InputFile>asList(inputFile), result);
    verify(result).add(inputFile,
      Arrays.asList(
        new BlameLine(DateUtils.parseDateTime("2009-04-18T10:29:59+0000"), "9491", "simon.brandhof"),
        new BlameLine(DateUtils.parseDateTime("2009-04-01T10:29:59+0000"), "1", null),
        new BlameLine(DateUtils.parseDateTime("2009-08-31T22:32:17+0000"), "10558", "david")));
  }

  @Test
  public void shouldFailIfFileContainsLocalModification() throws IOException {
    File source = new File(baseDir, "src/foo.xoo");
    FileUtils.write(source, "sample content");
    DefaultInputFile inputFile = new DefaultInputFile("foo", "src/foo.xoo").setAbsolutePath(new File(baseDir, "src/foo.xoo").getAbsolutePath());
    fs.add(inputFile);

    BlameResult result = mock(BlameResult.class);
    CommandExecutor commandExecutor = mock(CommandExecutor.class);

    when(commandExecutor.execute(any(Command.class), any(StreamConsumer.class), any(StreamConsumer.class), anyLong())).thenAnswer(new Answer<Integer>() {

      @Override
      public Integer answer(InvocationOnMock invocation) throws Throwable {
        StreamConsumer outConsumer = (StreamConsumer) invocation.getArguments()[1];
        List<String> lines = FileUtils.readLines(new File("src/test/resources/blame-with-uncomitted-changes.xml"), "UTF-8");
        for (String line : lines) {
          outConsumer.consumeLine(line);
        }
        return 0;
      }
    });

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unable to blame file src/foo.xoo. No blame info at line 2. Is file commited?");

    new SvnBlameCommand(commandExecutor, mock(SvnConfiguration.class)).blame(fs, Arrays.<InputFile>asList(inputFile), result);
  }

  @Test
  public void testExecutionError() throws IOException {
    File source = new File(baseDir, "src/foo.xoo");
    FileUtils.write(source, "sample content");
    DefaultInputFile inputFile = new DefaultInputFile("foo", "src/foo.xoo").setAbsolutePath(new File(baseDir, "src/foo.xoo").getAbsolutePath());
    fs.add(inputFile);

    BlameResult result = mock(BlameResult.class);
    CommandExecutor commandExecutor = mock(CommandExecutor.class);

    when(commandExecutor.execute(any(Command.class), any(StreamConsumer.class), any(StreamConsumer.class), anyLong())).thenAnswer(new Answer<Integer>() {

      @Override
      public Integer answer(InvocationOnMock invocation) throws Throwable {
        StreamConsumer errConsumer = (StreamConsumer) invocation.getArguments()[2];
        errConsumer.consumeLine("My error");
        return 1;
      }
    });

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("The svn blame command [svn blame --xml src/foo.xoo --non-interactive] failed: My error");

    new SvnBlameCommand(commandExecutor, mock(SvnConfiguration.class)).blame(fs, Arrays.<InputFile>asList(inputFile), result);
  }

}
