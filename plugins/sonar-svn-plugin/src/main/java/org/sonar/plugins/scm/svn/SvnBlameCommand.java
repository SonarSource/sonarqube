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

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.scm.BlameCommand;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.utils.command.Command;
import org.sonar.api.utils.command.CommandException;
import org.sonar.api.utils.command.CommandExecutor;
import org.sonar.api.utils.command.StreamConsumer;
import org.sonar.api.utils.command.StringStreamConsumer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class SvnBlameCommand implements BlameCommand, BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(SvnBlameCommand.class);
  private final CommandExecutor commandExecutor;
  private final SvnConfiguration configuration;

  public SvnBlameCommand(SvnConfiguration configuration) {
    this(CommandExecutor.create(), configuration);
  }

  SvnBlameCommand(CommandExecutor commandExecutor, SvnConfiguration configuration) {
    this.commandExecutor = commandExecutor;
    this.configuration = configuration;
  }

  @Override
  public void blame(final FileSystem fs, Iterable<InputFile> files, final BlameResult result) {
    LOG.debug("Working directory: " + fs.baseDir().getAbsolutePath());
    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
    List<Future<Void>> tasks = new ArrayList<Future<Void>>();
    for (InputFile inputFile : files) {
      tasks.add(submitTask(fs, result, executorService, inputFile));
    }

    for (Future<Void> task : tasks) {
      try {
        task.get();
      } catch (ExecutionException e) {
        // Unwrap ExecutionException
        throw e.getCause() instanceof RuntimeException ? (RuntimeException) e.getCause() : new IllegalStateException(e.getCause());
      } catch (InterruptedException e) {
        // Ignore
      }
    }
  }

  private Future<Void> submitTask(final FileSystem fs, final BlameResult result, ExecutorService executorService, final InputFile inputFile) {
    return executorService.submit(new Callable<Void>() {
      public Void call() {
        String filename = inputFile.relativePath();
        Command cl = createCommandLine(fs.baseDir(), filename);
        SvnBlameConsumer consumer = new SvnBlameConsumer(filename);
        StringStreamConsumer stderr = new StringStreamConsumer();
        int exitCode;
        try {
          exitCode = execute(cl, consumer, stderr);
        } catch (CommandException e) {
          // Unwrap CommandException
          throw e.getCause() instanceof RuntimeException ? (RuntimeException) e.getCause() : new IllegalStateException(e.getCause());
        }
        if (exitCode != 0) {
          throw new IllegalStateException("The svn blame command [" + cl.toString() + "] failed: " + stderr.getOutput());
        }
        List<BlameLine> lines = consumer.getLines();
        if (lines.size() == inputFile.lines() - 1) {
          // SONARPLUGINS-3097 SVN do not report blame on last empty line
          lines.add(lines.get(lines.size() - 1));
        }
        result.add(inputFile, lines);
        return null;
      }
    });
  }

  private int execute(Command cl, StreamConsumer consumer, StreamConsumer stderr) {
    LOG.debug("Executing: " + cl);
    return commandExecutor.execute(cl, consumer, stderr, -1);
  }

  @VisibleForTesting
  Command createCommandLine(File baseDir, String filename) {
    Command cl = Command.create("svn");
    for (Entry<String, String> env : System.getenv().entrySet()) {
      cl.setEnvironmentVariable(env.getKey(), env.getValue());
    }
    cl.setEnvironmentVariable("LC_MESSAGES", "en");

    cl.setDirectory(baseDir);
    cl.addArgument("blame");
    cl.addArgument("--xml");
    cl.addArgument(filename);
    cl.addArgument("--non-interactive");
    String configDir = configuration.configDir();
    if (configDir != null) {
      cl.addArgument("--config-dir");
      cl.addArgument(configDir);
    }
    String username = configuration.username();
    if (username != null) {
      cl.addArgument("--username");
      cl.addMaskedArgument(username);
      String password = configuration.password();
      if (password != null) {
        cl.addArgument("--password");
        cl.addMaskedArgument(password);
      }
    }
    if (configuration.trustServerCert()) {
      cl.addArgument("--trust-server-cert");
    }
    return cl;
  }
}
