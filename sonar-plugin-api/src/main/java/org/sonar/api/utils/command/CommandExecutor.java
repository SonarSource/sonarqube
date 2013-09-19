/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.api.utils.command;

import com.google.common.io.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.*;

/**
 * Synchronously execute a native command line. It's much more limited than the Apache Commons Exec library.
 * For example it does not allow to run asynchronously or to automatically quote command-line arguments.
 *
 * @since 2.7
 */
public class CommandExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(CommandExecutor.class);

  private static final CommandExecutor INSTANCE = new CommandExecutor();

  private CommandExecutor() {
  }

  public static CommandExecutor create() {
    // stateless object, so a single singleton can be shared
    return INSTANCE;
  }

  /**
   * @throws CommandException
   * @since 3.0
   */
  public int execute(Command command, StreamConsumer stdOut, StreamConsumer stdErr, long timeoutMilliseconds) {
    ExecutorService executorService = null;
    Process process = null;
    StreamGobbler outputGobbler = null;
    StreamGobbler errorGobbler = null;
    try {
      ProcessBuilder builder = new ProcessBuilder(command.toStrings());
      if (command.getDirectory() != null) {
        builder.directory(command.getDirectory());
      }
      builder.environment().putAll(command.getEnvironmentVariables());
      process = builder.start();

      outputGobbler = new StreamGobbler(process.getInputStream(), stdOut);
      errorGobbler = new StreamGobbler(process.getErrorStream(), stdErr);
      outputGobbler.start();
      errorGobbler.start();

      final Process finalProcess = process;
      executorService = Executors.newSingleThreadExecutor();
      Future<Integer> ft = executorService.submit(new Callable<Integer>() {
        @Override
        public Integer call() throws Exception {
          return finalProcess.waitFor();
        }
      });
      int exitCode = ft.get(timeoutMilliseconds, TimeUnit.MILLISECONDS);
      waitUntilFinish(outputGobbler);
      waitUntilFinish(errorGobbler);
      verifyGobbler(command, outputGobbler, "stdOut");
      verifyGobbler(command, errorGobbler, "stdErr");
      return exitCode;

    } catch (TimeoutException te) {
      process.destroy();
      throw new CommandException(command, "Timeout exceeded: " + timeoutMilliseconds + " ms", te);

    } catch (CommandException e) {
      throw e;

    } catch (Exception e) {
      throw new CommandException(command, e);

    } finally {
      waitUntilFinish(outputGobbler);
      waitUntilFinish(errorGobbler);
      closeStreams(process);

      if (executorService != null) {
        executorService.shutdown();
      }
    }
  }

  private void verifyGobbler(Command command, StreamGobbler gobbler, String type) {
    if (gobbler.getException() != null) {
      throw new CommandException(command, "Error inside " + type + " stream", gobbler.getException());
    }
  }

  /**
   * Execute command and display error and output streams in log.
   * Method {@link #execute(Command, StreamConsumer, StreamConsumer, long)} is preferable,
   * when fine-grained control of output of command required.
   *
   * @throws CommandException
   */
  public int execute(Command command, long timeoutMilliseconds) {
    LOG.info("Executing command: " + command);
    return execute(command, new DefaultConsumer(), new DefaultConsumer(), timeoutMilliseconds);
  }

  private void closeStreams(Process process) {
    if (process != null) {
      Closeables.closeQuietly(process.getInputStream());
      Closeables.closeQuietly(process.getInputStream());
      Closeables.closeQuietly(process.getOutputStream());
      Closeables.closeQuietly(process.getErrorStream());
    }
  }

  private void waitUntilFinish(StreamGobbler thread) {
    if (thread != null) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        LOG.error("InterruptedException while waiting finish of " + thread.toString(), e);
      }
    }
  }

  private static class StreamGobbler extends Thread {
    private final InputStream is;
    private final StreamConsumer consumer;
    private volatile Exception exception;

    StreamGobbler(InputStream is, StreamConsumer consumer) {
      super("ProcessStreamGobbler");
      this.is = is;
      this.consumer = consumer;
    }

    @Override
    public void run() {
      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader br = new BufferedReader(isr);
      try {
        String line;
        while ((line = br.readLine()) != null) {
          consumeLine(line);
        }
      } catch (IOException ioe) {
        exception = ioe;

      } finally {
        Closeables.closeQuietly(br);
        Closeables.closeQuietly(isr);
      }
    }

    private void consumeLine(String line) {
      if (exception == null) {
        try {
          consumer.consumeLine(line);
        } catch (Exception e) {
          exception = e;
        }
      }
    }

    public Exception getException() {
      return exception;
    }
  }

  private static class DefaultConsumer implements StreamConsumer {
    public void consumeLine(String line) {
      LOG.info(line);
    }
  }
}
