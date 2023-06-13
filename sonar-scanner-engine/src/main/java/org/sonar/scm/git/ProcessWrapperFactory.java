/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ProcessWrapperFactory {
  private static final Logger LOG = LoggerFactory.getLogger(ProcessWrapperFactory.class);

  public ProcessWrapperFactory() {
    // nothing to do
  }

  public ProcessWrapper create(@Nullable Path baseDir, Consumer<String> stdOutLineConsumer, String... command) {
    return new ProcessWrapper(baseDir, stdOutLineConsumer, command);
  }

  static class ProcessWrapper {

    private final Path baseDir;
    private final Consumer<String> stdOutLineConsumer;
    private final String[] command;

    ProcessWrapper(@Nullable Path baseDir, Consumer<String> stdOutLineConsumer, String... command) {
      this.baseDir = baseDir;
      this.stdOutLineConsumer = stdOutLineConsumer;
      this.command = command;
    }

    public void execute() throws IOException {
      ProcessBuilder pb = new ProcessBuilder()
        .command(command)
        .directory(baseDir != null ? baseDir.toFile() : null);

      Process p = pb.start();
      try {
        processInputStream(p.getInputStream(), stdOutLineConsumer);

        processInputStream(p.getErrorStream(), line -> {
          if (!line.isBlank()) {
            LOG.debug(line);
          }
        });

        int exit = p.waitFor();
        if (exit != 0) {
          throw new IllegalStateException(format("Command execution exited with code: %d", exit));
        }
      } catch (InterruptedException e) {
        LOG.warn(format("Command [%s] interrupted", join(" ", command)), e);
        Thread.currentThread().interrupt();
      } finally {
        p.destroy();
      }
    }

    private static void processInputStream(InputStream inputStream, Consumer<String> stringConsumer) {
      try (Scanner scanner = new Scanner(new InputStreamReader(inputStream, UTF_8))) {
        scanner.useDelimiter("\n");
        while (scanner.hasNext()) {
          stringConsumer.accept(scanner.next());
        }
      }
    }
  }

}
