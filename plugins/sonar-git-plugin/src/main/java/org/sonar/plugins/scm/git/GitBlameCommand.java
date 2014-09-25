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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.scm.BlameCommand;
import org.sonar.api.utils.command.Command;
import org.sonar.api.utils.command.CommandExecutor;
import org.sonar.api.utils.command.StreamConsumer;

import java.io.File;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class GitBlameCommand implements BlameCommand, BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(GitBlameCommand.class);
  private final CommandExecutor commandExecutor;

  public GitBlameCommand() {
    this(CommandExecutor.create());
  }

  GitBlameCommand(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }

  @Override
  public void blame(FileSystem fs, Iterable<InputFile> files, BlameResult result) {
    for (InputFile inputFile : files) {
      String filename = inputFile.relativePath();
      Command cl = createCommandLine(fs.baseDir(), filename);
      GitBlameConsumer consumer = new GitBlameConsumer(LOG);
      StringStreamConsumer stderr = new StringStreamConsumer();

      int exitCode = execute(cl, consumer, stderr);
      if (exitCode != 0) {
        throw new IllegalStateException("The git blame command [" + cl.toString() + "] failed: " + stderr.getOutput());
      }
      result.add(inputFile, consumer.getLines());
    }
  }

  public int execute(Command cl, StreamConsumer consumer, StreamConsumer stderr) {
    LOG.info("Executing: " + cl);
    LOG.info("Working directory: " + cl.getDirectory().getAbsolutePath());

    return commandExecutor.execute(cl, consumer, stderr, 10 * 1000);
  }

  private Command createCommandLine(File workingDirectory, String filename) {
    Command cl = Command.create("git");
    cl.addArgument("blame");
    if (workingDirectory != null) {
      cl.setDirectory(workingDirectory);
    }
    cl.addArgument("--porcelain");
    cl.addArgument(filename);
    cl.addArgument("-w");
    return cl;
  }

  private static class StringStreamConsumer implements StreamConsumer {
    private StringBuffer string = new StringBuffer();

    private String ls = System.getProperty("line.separator");

    @Override
    public void consumeLine(String line) {
      string.append(line + ls);
    }

    public String getOutput() {
      return string.toString();
    }
  }

}
