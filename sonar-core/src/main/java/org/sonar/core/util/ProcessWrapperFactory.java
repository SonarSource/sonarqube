/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.core.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.apache.commons.exec.ExecuteWatchdog.INFINITE_TIMEOUT_DURATION;

public class ProcessWrapperFactory {
  private static final Logger LOG = LoggerFactory.getLogger(ProcessWrapperFactory.class);

  public ProcessWrapperFactory() {
    // nothing to do
  }

  public ProcessWrapper create(@Nullable Path baseDir, Consumer<String> stdOutLineConsumer, String... command) {
    return new ProcessWrapper(baseDir, stdOutLineConsumer, LOG::debug, Map.of(), command);
  }

  public ProcessWrapper create(@Nullable Path baseDir, Consumer<String> stdOutLineConsumer, Map<String, String> envVariablesOverrides, String... command) {
    return new ProcessWrapper(baseDir, stdOutLineConsumer, LOG::debug, envVariablesOverrides, command);
  }

  public ProcessWrapper create(@Nullable Path baseDir, Consumer<String> stdOutLineConsumer, Consumer<String> stdErrLineConsumer, Map<String, String> envVariablesOverrides,
    String... command) {
    return new ProcessWrapper(baseDir, stdOutLineConsumer, stdErrLineConsumer, envVariablesOverrides, command);
  }

  public static class ProcessWrapper {

    private final Path baseDir;
    private final Consumer<String> stdOutLineConsumer;
    private final Consumer<String> stdErrLineConsumer;
    private final String[] command;
    private final Map<String, String> envVariables = new HashMap<>();
    private ExecuteWatchdog watchdog = null;

    ProcessWrapper(@Nullable Path baseDir, Consumer<String> stdOutLineConsumer, Consumer<String> stdErrLineConsumer, Map<String, String> envVariablesOverrides, String... command) {
      this.baseDir = baseDir;
      this.stdOutLineConsumer = stdOutLineConsumer;
      this.stdErrLineConsumer = stdErrLineConsumer;
      this.envVariables.putAll(System.getenv());
      this.envVariables.putAll(envVariablesOverrides);
      this.command = command;
    }

    public void execute() throws IOException {
      CommandLine cmdLine = new CommandLine(command[0]);
      for (int i = 1; i < command.length; i++) {
        cmdLine.addArgument(command[i], false);
      }

      var executor = buildExecutor();

      DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
      executor.execute(cmdLine, envVariables, resultHandler);

      try {
        resultHandler.waitFor();
        int exitValue = resultHandler.getExitValue();
        if (exitValue == Executor.INVALID_EXITVALUE) {
          throw resultHandler.getException();
        }
        if (exitValue != 0 && !watchdog.killedProcess()) {
          throw new IllegalStateException(format("Command execution exited with code: %d", exitValue), resultHandler.getException());
        }
      } catch (InterruptedException e) {
        LOG.warn("Command [{}] interrupted", join(" ", command), e);
        Thread.currentThread().interrupt();
      }
    }

    private DefaultExecutor buildExecutor() {
      DefaultExecutor.Builder<?> builder = DefaultExecutor.builder();
      if (baseDir != null) {
        builder.setWorkingDirectory(baseDir.toFile());
      }

      PumpStreamHandler psh = new PumpStreamHandler(new LogOutputStream() {
        @Override
        protected void processLine(String line, int logLevel) {
          stdOutLineConsumer.accept(line);
        }
      }, new LogOutputStream() {
        @Override
        protected void processLine(String line, int logLevel) {
          stdErrLineConsumer.accept("[stderr] %s".formatted(line));
        }
      });
      builder.setExecuteStreamHandler(psh);

      var executor = builder.get();
      watchdog = ExecuteWatchdog.builder().setTimeout(INFINITE_TIMEOUT_DURATION).get();
      executor.setWatchdog(watchdog);
      return executor;
    }

    public void destroy() {
      watchdog.destroyProcess();
    }
  }
}
