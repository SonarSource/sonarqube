/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.scanner.util;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;

import static org.assertj.core.api.Assertions.assertThat;

public class ProgressReportTest {
  private static final String THREAD_NAME = "progress";

  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));
  @Rule
  public LogTester logTester = new LogTester();

  private ProgressReport underTest = new ProgressReport(THREAD_NAME, 1);

  @Test
  public void stop_thread_on_stop() {
    underTest.start("start");
    assertThat(isThreadAlive(THREAD_NAME)).isTrue();
    underTest.stop("stop");
    assertThat(isThreadAlive(THREAD_NAME)).isFalse();
  }

  @Test
  public void do_not_block_app() {
    underTest.start("start");
    assertThat(isDaemon(THREAD_NAME)).isTrue();
    underTest.stop("stop");
  }

  @Test
  public void do_log() {
    logTester.setLevel(Level.DEBUG);
    underTest.start("start");
    underTest.message("Some message");
    boolean logged = false;
    while (!logged) {
      logged = containsWithRetry("Some message");
    }
    underTest.stop("stop");
    assertThat(containsWithRetry("stop")).isTrue();
  }

  private boolean containsWithRetry(String message) {
    try {
      Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> logTester.logs().contains(message));
    } catch (ConditionTimeoutException e) {
      return false;
    }
    return true;
  }

  @Test
  public void do_log_with_time() {
    underTest.start("start");
    underTest.stopAndLogTotalTime("stop");
    assertThat(logTester.logs().stream().anyMatch(s -> Pattern.matches("stop \\(done\\) \\| time=[0-9]+ms", s))).isTrue();
  }

  private static boolean isDaemon(String name) {
    Thread t = getThread(name);
    return (t != null) && t.isDaemon();
  }

  private static boolean isThreadAlive(String name) {
    Thread t = getThread(name);
    return (t != null) && t.isAlive();
  }

  private static Thread getThread(String name) {
    Set<Thread> threads = Thread.getAllStackTraces().keySet();

    for (Thread t : threads) {
      if (t.getName().equals(name)) {
        return t;
      }
    }
    return null;
  }
}
