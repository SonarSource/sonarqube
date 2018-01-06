/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * Synchronously execute a native command line. It's much more limited than the Apache Commons Exec library.
 * For example it does not allow to run asynchronously or to automatically quote command-line arguments.
 *
 * @since 2.7
 */
public class CommandExecutor {

  private static final Logger LOG = Loggers.get(CommandExecutor.class);

  private static final CommandExecutor INSTANCE = new CommandExecutor();

  private CommandExecutor() {
  }

  public static CommandExecutor create() {
    // stateless object, so a single singleton can be shared
    return INSTANCE;
  }

  /**
   * @throws org.sonar.api.utils.command.TimeoutException on timeout, since 4.4
   * @throws CommandException on any other error
   * @param timeoutMilliseconds any negative value means no timeout.
   * @since 3.0
   */
  public int execute(Command command, StreamConsumer stdOut, StreamConsumer stdErr, long timeoutMilliseconds) {
    ExecutorService executorService = null;
    Process process = null;
    StreamGobbler outputGobbler = null;
    StreamGobbler errorGobbler = null;
    try {
      ProcessBuilder builder = new ProcessBuilder(command.toStrings(false));
      if (command.getDirectory() != null) {
        builder.directory(command.getDirectory());
      }
      builder.environment().putAll(command.getEnvironmentVariables());
      process = builder.start();

      outputGobbler = new StreamGobbler(process.getInputStream(), stdOut);
      errorGobbler = new StreamGobbler(process.getErrorStream(), stdErr);
      outputGobbler.start();
      errorGobbler.start();

      executorService = Executors.newSingleThreadExecutor();
      Future<Integer> ft = executorService.submit((Callable<Integer>) process::waitFor);
      int exitCode;
      if (timeoutMilliseconds < 0) {
        exitCode = ft.get();
      } else {
        exitCode = ft.get(timeoutMilliseconds, TimeUnit.MILLISECONDS);
      }
      waitUntilFinish(outputGobbler);
      waitUntilFinish(errorGobbler);
      verifyGobbler(command, outputGobbler, "stdOut");
      verifyGobbler(command, errorGobbler, "stdErr");
      return exitCode;

    } catch (java.util.concurrent.TimeoutException te) {
      throw new TimeoutException(command, "Timeout exceeded: " + timeoutMilliseconds + " ms", te);

    } catch (CommandException e) {
      throw e;

    } catch (Exception e) {
      throw new CommandException(command, e);

    } finally {
      if (process != null) {
        process.destroy();
      }
      waitUntilFinish(outputGobbler);
      waitUntilFinish(errorGobbler);
      closeStreams(process);

      if (executorService != null) {
        executorService.shutdown();
      }
    }
  }

  private static void verifyGobbler(Command command, StreamGobbler gobbler, String type) {
    if (gobbler.getException() != null) {
      throw new CommandException(command, "Error inside " + type + " stream", gobbler.getException());
    }
  }

  /**
   * Execute command and display error and output streams in log.
   * Method {@link #execute(Command, StreamConsumer, StreamConsumer, long)} is preferable,
   * when fine-grained control of output of command required.
   * @param timeoutMilliseconds any negative value means no timeout.
   *
   * @throws CommandException
   */
  public int execute(Command command, long timeoutMilliseconds) {
    LOG.info("Executing command: " + command);
    return execute(command, new DefaultConsumer(), new DefaultConsumer(), timeoutMilliseconds);
  }

  private static void closeStreams(@Nullable Process process) {
    if (process != null) {
      IOUtils.closeQuietly(process.getInputStream());
      IOUtils.closeQuietly(process.getOutputStream());
      IOUtils.closeQuietly(process.getErrorStream());
    }
  }

  private static void waitUntilFinish(@Nullable StreamGobbler thread) {
    if (thread != null) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        // considered as finished, restore the interrupted flag
        Thread.currentThread().interrupt();
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
      try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
        String line;
        while ((line = br.readLine()) != null) {
          consumeLine(line);
        }
      } catch (IOException ioe) {
        exception = ioe;
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
    @Override
    public void consumeLine(String line) {
      LOG.info(line);
    }
  }
}
