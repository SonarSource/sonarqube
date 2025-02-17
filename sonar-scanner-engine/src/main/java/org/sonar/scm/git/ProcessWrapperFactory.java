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
package org.sonar.scm.git;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.lang.String.join;

public class ProcessWrapperFactory {
  private static final Logger LOG = LoggerFactory.getLogger(ProcessWrapperFactory.class);

  public ProcessWrapperFactory() {
    // nothing to do
  }

  public ProcessWrapper create(@Nullable Path baseDir, Consumer<String> stdOutLineConsumer, String... command) {
    return new ProcessWrapper(baseDir, stdOutLineConsumer, Map.of(), command);
  }

  public ProcessWrapper create(@Nullable Path baseDir, Consumer<String> stdOutLineConsumer, Map<String, String> envVariables, String... command) {
    return new ProcessWrapper(baseDir, stdOutLineConsumer, envVariables, command);
  }

  static class ProcessWrapper {

    private final Path baseDir;
    private final Consumer<String> stdOutLineConsumer;
    private final String[] command;
    private final Map<String, String> envVariables = new HashMap<>();

    ProcessWrapper(@Nullable Path baseDir, Consumer<String> stdOutLineConsumer, Map<String, String> envVariables, String... command) {
      this.baseDir = baseDir;
      this.stdOutLineConsumer = stdOutLineConsumer;
      this.envVariables.putAll(envVariables);
      this.command = command;
    }

    public void execute() throws IOException {
      Process process = null;
      try {
        ProcessBuilder pb = new ProcessBuilder()
          .command(command)
          .directory(baseDir != null ? baseDir.toFile() : null);
        envVariables.forEach(pb.environment()::put);

        process = pb.start();

        var stdoutConsumer = new StreamGobbler(process.getInputStream(), stdOutLineConsumer);
        var stdErrConsumer = new StreamGobbler(process.getErrorStream(), stderr -> LOG.debug("[stderr] {}", stderr));
        stdErrConsumer.start();
        stdoutConsumer.start();

        int exitCode = process.waitFor();
        stdoutConsumer.join();
        stdErrConsumer.join();

        if (exitCode != 0) {
          throw new IllegalStateException(format("Command execution exited with code: %d", exitCode));
        }

      } catch (InterruptedException e) {
        LOG.warn("Command [{}] interrupted", join(" ", command), e);
        Thread.currentThread().interrupt();
      } finally {
        if (process != null) {
          process.destroy();
        }
      }
    }

  }

  private static class StreamGobbler extends Thread {
    private final InputStream inputStream;
    private final Consumer<String> consumer;

    public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
      this.inputStream = inputStream;
      this.consumer = consumer;
    }

    @Override
    public void run() {
      new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
        .lines()
        .forEach(consumer);
    }

  }

}
