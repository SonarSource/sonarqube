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

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.scm.BlameCommand;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.utils.command.Command;
import org.sonar.api.utils.command.CommandExecutor;
import org.sonar.api.utils.command.StreamConsumer;
import org.sonar.api.utils.command.StringStreamConsumer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GitBlameCommand extends BlameCommand {

  private static final Logger LOG = LoggerFactory.getLogger(GitBlameCommand.class);
  private final CommandExecutor commandExecutor;

  public GitBlameCommand() {
    this(CommandExecutor.create());
  }

  GitBlameCommand(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }

  @Override
  public void blame(BlameInput input, BlameOutput output) {
    FileSystem fs = input.fileSystem();
    LOG.debug("Working directory: " + fs.baseDir().getAbsolutePath());
    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
    List<Future<Void>> tasks = new ArrayList<Future<Void>>();
    for (InputFile inputFile : input.filesToBlame()) {
      tasks.add(submitTask(fs.baseDir(), output, inputFile, executorService));
    }
    for (Future<Void> task : tasks) {
      try {
        task.get();
      } catch (ExecutionException e) {
        // Unwrap ExecutionException
        throw e.getCause() instanceof RuntimeException ? (RuntimeException) e.getCause() : new IllegalStateException(e.getCause());
      } catch (InterruptedException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private Future<Void> submitTask(final File baseDir, final BlameOutput output, final InputFile inputFile, ExecutorService executorService) {
    return executorService.submit(new Callable<Void>() {
      @Override
      public Void call() throws GitAPIException {
        blame(baseDir, output, inputFile);
        return null;
      }

    });
  }

  private void blame(File baseDir, BlameOutput output, InputFile inputFile) {
    String filename = inputFile.relativePath();
    Command cl = createCommandLine(baseDir, filename);
    GitBlameConsumer consumer = new GitBlameConsumer(filename);
    StringStreamConsumer stderr = new StringStreamConsumer();

    int exitCode = execute(cl, consumer, stderr);
    if (exitCode != 0) {
      throw new IllegalStateException("The git blame command [" + cl.toString() + "] failed: " + stderr.getOutput());
    }
    List<BlameLine> lines = consumer.getLines();
    if (lines.size() == inputFile.lines() - 1) {
      // SONARPLUGINS-3097 Git do not report blame on last empty line
      lines.add(lines.get(lines.size() - 1));
    }
    output.blameResult(inputFile, lines);

  }

  public int execute(Command cl, StreamConsumer consumer, StreamConsumer stderr) {
    LOG.debug("Executing: " + cl);
    return commandExecutor.execute(cl, consumer, stderr, -1);
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

}
