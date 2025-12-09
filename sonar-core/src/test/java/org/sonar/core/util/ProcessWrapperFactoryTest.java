/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessWrapperFactoryTest {

  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();
  private final ProcessWrapperFactory underTest = new ProcessWrapperFactory();

  @Test
  void should_log_error_output_in_debug_mode(@TempDir Path root) {
    logTester.setLevel(Level.DEBUG);

    Consumer<String> stdoutConsumer = s -> {
    };
    Consumer<String> stderrConsumer = LoggerFactory.getLogger(ProcessWrapperFactoryTest.class)::debug;
    var processWrapper = underTest.create(root, stdoutConsumer, stderrConsumer, Map.of("LANG", "en_US"), "git", "blame");
    assertThatThrownBy(processWrapper::execute)
      .isInstanceOf(IllegalStateException.class);

    assertThat(logTester.logs(Level.DEBUG).get(0)).startsWith("fatal:");
  }

  // SONAR-24376
  @Test
  void should_not_freeze_when_destroying_in_the_middle_of_big_stdout_stdin(@TempDir Path temp) throws IOException {
    var bigFile = temp.resolve("stdout.txt");
    for (int i = 0; i < 1024; i++) {
      Files.writeString(bigFile, StringUtils.repeat("a", 1024), StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
      Files.writeString(bigFile, "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    var stdoutHandler = new DestroyProcessAfter10Lines();
    var stderrHandler = new DestroyProcessAfter10Lines();

    var processWrapper = underTest.create(temp, stdoutHandler::process, stderrHandler::process,
      SystemUtils.IS_OS_WINDOWS ? new String[] {"cmd.exe", "/c", "type stdout.txt"} : new String[] {"cat", "stdout.txt"});
    stdoutHandler.wrapper = processWrapper;

    assertThatCode(processWrapper::execute).doesNotThrowAnyException();
    // A few lines might be processed before the process is destroyed
    assertThat(stdoutHandler.lineCounter.get()).isGreaterThanOrEqualTo(10);
  }

  @Test
  void should_not_deadlock_when_stream_handler_throw_exception(@TempDir Path temp) throws IOException {
    var bigFile = temp.resolve("stdout.txt");
    for (int i = 0; i < 1024; i++) {
      Files.writeString(bigFile, StringUtils.repeat("a", 1024), StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
      Files.writeString(bigFile, "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    var stdoutHandler = new ThrowExceptionForEveryLine();

    var processWrapper = underTest.create(temp, stdoutHandler::process, l -> {
    },
      SystemUtils.IS_OS_WINDOWS ? new String[] {"cmd.exe", "/c", "type stdout.txt"} : new String[] {"cat", "stdout.txt"});

    assertThatThrownBy(processWrapper::execute)
      .hasMessage("Error while processing stream for command")
      .hasCauseInstanceOf(IllegalStateException.class)
      .hasStackTraceContaining("Some error");
  }

  @Test
  void should_apply_env_overrides_on_top_of_parent_env(@TempDir Path temp) throws IOException {
    ConcurrentLinkedDeque<String> logs = new ConcurrentLinkedDeque<>();

    Consumer<String> stdoutHandler = logs::add;
    Consumer<String> stderrHandler = logs::add;

    var processWrapper = underTest.create(temp, stdoutHandler, stderrHandler, Map.of("FOO", "BAR"),
      SystemUtils.IS_OS_WINDOWS ? new String[] {"cmd.exe", "/c", "echo %PATH% & echo %FOO%"} : new String[] {"/bin/bash", "-c", "echo $PATH; echo $FOO;"});
    processWrapper.execute();

    // Trim all output lines to avoid issues with trailing spaces on Windows environment variables
    var trimmedLogs = logs.stream().map(String::trim).toList();
    assertThat(trimmedLogs).containsExactly(System.getenv("PATH").trim(), "BAR");
  }

  private static class DestroyProcessAfter10Lines {
    private final AtomicInteger lineCounter = new AtomicInteger();
    private ProcessWrapperFactory.ProcessWrapper wrapper;

    void process(String line) {
      if (lineCounter.incrementAndGet() == 10) {
        wrapper.destroy();
      }
    }
  }

  private static class ThrowExceptionForEveryLine {

    void process(String line) {
      throw new IllegalStateException("Some error");
    }
  }

}
