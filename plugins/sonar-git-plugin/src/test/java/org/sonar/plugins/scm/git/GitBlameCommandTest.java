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
package org.sonar.plugins.scm.git;

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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GitBlameCommandTest {

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
        outConsumer.consumeLine("2c68c473da7fc293e12ca50f19380c5118be7ead 68 54 1");
        outConsumer.consumeLine("author Simon Brandhof");
        outConsumer.consumeLine("author-mail <simon.brandhof@gmail.com>");
        outConsumer.consumeLine("author-time 1312534171");
        outConsumer.consumeLine("author-tz +0200");
        outConsumer.consumeLine("committer Simon Brandhof");
        outConsumer.consumeLine("committer-mail <simon.brandhof@gmail.com>");
        outConsumer.consumeLine("committer-time 1312534171");
        outConsumer.consumeLine("committer-tz +0200");
        outConsumer.consumeLine("summary Move to nexus.codehaus.org + configuration of maven release plugin is back");
        outConsumer.consumeLine("previous 1bec1c3a77f6957175be13e4433110f7fc8e387e pom.xml");
        outConsumer.consumeLine("filename pom.xml");
        outConsumer.consumeLine("\t<id>codehaus-nexus-staging</id>");
        outConsumer.consumeLine("2c68c473da7fc293e12ca50f19380c5118be7ead 72 60 1");
        outConsumer.consumeLine("\t<url>${sonar.snapshotRepository.url}</url>");
        return 0;
      }
    });

    new GitBlameCommand(commandExecutor).blame(fs, Arrays.<InputFile>asList(inputFile), result);
    verify(result).add(inputFile,
      Arrays.asList(new BlameLine(DateUtils.parseDateTime("2011-08-05T10:49:31+0200"), "2c68c473da7fc293e12ca50f19380c5118be7ead", "simon.brandhof@gmail.com"),
        new BlameLine(DateUtils.parseDateTime("2011-08-05T10:49:31+0200"), "2c68c473da7fc293e12ca50f19380c5118be7ead", "simon.brandhof@gmail.com")));
  }

  @Test
  public void shouldFailOnFileWithLocalModification() throws IOException {
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
        outConsumer.consumeLine("000000000000000000000000000000000000000 68 54 1");
        outConsumer.consumeLine("author Not Committed Yet");
        outConsumer.consumeLine("author-mail <not.committed.yet>");
        outConsumer.consumeLine("author-time 1312534171");
        outConsumer.consumeLine("author-tz +0200");
        outConsumer.consumeLine("committer Not Committed Yet");
        outConsumer.consumeLine("committer-mail <not.committed.yet>");
        outConsumer.consumeLine("committer-time 1312534171");
        outConsumer.consumeLine("committer-tz +0200");
        outConsumer.consumeLine("summary Move to nexus.codehaus.org + configuration of maven release plugin is back");
        outConsumer.consumeLine("previous 1bec1c3a77f6957175be13e4433110f7fc8e387e pom.xml");
        outConsumer.consumeLine("filename pom.xml");
        outConsumer.consumeLine("\t<id>codehaus-nexus-staging</id>");
        outConsumer.consumeLine("2c68c473da7fc293e12ca50f19380c5118be7ead 72 60 1");
        outConsumer.consumeLine("\t<url>${sonar.snapshotRepository.url}</url>");
        return 0;
      }
    });

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unable to blame file src/foo.xoo. No blame info at line 1. Is file commited?");
    new GitBlameCommand(commandExecutor).blame(fs, Arrays.<InputFile>asList(inputFile), result);
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
    thrown.expectMessage("The git blame command [git blame --porcelain src/foo.xoo -w] failed: My error");

    new GitBlameCommand(commandExecutor).blame(fs, Arrays.<InputFile>asList(inputFile), result);
  }

}
