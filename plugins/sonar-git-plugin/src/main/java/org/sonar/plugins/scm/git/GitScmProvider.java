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
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.api.utils.command.Command;
import org.sonar.api.utils.command.CommandExecutor;
import org.sonar.api.utils.command.StreamConsumer;

import java.io.File;

public class GitScmProvider implements ScmProvider {

  private static final Logger LOG = LoggerFactory.getLogger(GitScmProvider.class);

  @Override
  public String key() {
    return "git";
  }

  @Override
  public boolean supports(File baseDir) {
    return new File(baseDir, ".git").exists();
  }

  @Override
  public void blame(FileSystem fs, Iterable<InputFile> files, BlameResult result) {
    for (InputFile inputFile : files) {
      String filename = inputFile.relativePath();
      Command cl = createCommandLine(fs.baseDir(), filename);
      SonarGitBlameConsumer consumer = new SonarGitBlameConsumer(LOG);
      StringStreamConsumer stderr = new StringStreamConsumer();

      int exitCode = execute(cl, consumer, stderr);
      if (exitCode != 0) {
        throw new IllegalStateException("The git blame command [" + cl.toString() + "] failed: " + stderr.getOutput());
      }
      result.add(inputFile, consumer.getLines());
    }
  }

  public static int execute(Command cl, StreamConsumer consumer, StreamConsumer stderr) {
    LOG.info("Executing: " + cl);
    LOG.info("Working directory: " + cl.getDirectory().getAbsolutePath());

    return CommandExecutor.create().execute(cl, consumer, stderr, 10 * 1000);
  }

  private static Command createCommandLine(File workingDirectory, String filename) {
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
