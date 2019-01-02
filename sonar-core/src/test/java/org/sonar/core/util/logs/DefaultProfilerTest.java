/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.core.util.logs;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.Loggers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@RunWith(DataProviderRunner.class)
public class DefaultProfilerTest {

  @Rule
  public LogTester tester = new LogTester();

  Profiler underTest = Profiler.create(Loggers.get("DefaultProfilerTest"));

  @DataProvider
  public static Object[][] logTimeLastValues() {
    return new Object[][] {
      {true},
      {false}
    };
  }

  @Test
  public void test_levels() {
    // info by default
    assertThat(underTest.isDebugEnabled()).isFalse();
    assertThat(underTest.isTraceEnabled()).isFalse();

    tester.setLevel(LoggerLevel.DEBUG);
    assertThat(underTest.isDebugEnabled()).isTrue();
    assertThat(underTest.isTraceEnabled()).isFalse();

    tester.setLevel(LoggerLevel.TRACE);
    assertThat(underTest.isDebugEnabled()).isTrue();
    assertThat(underTest.isTraceEnabled()).isTrue();
  }

  @Test
  @UseDataProvider("logTimeLastValues")
  public void stop_reuses_start_message(boolean logTimeLast) throws InterruptedException {
    underTest.logTimeLast(logTimeLast);
    tester.setLevel(LoggerLevel.TRACE);

    // trace
    underTest.startTrace("Register rules {}", 1);
    Thread.sleep(2);
    assertThat(tester.logs()).containsOnly("Register rules 1");
    long timing = underTest.stopTrace();
    assertThat(timing).isGreaterThan(0);
    assertThat(tester.logs()).hasSize(2);
    assertThat(tester.logs().get(1)).startsWith("Register rules 1 (done) | time=" + timing);
    tester.clear();

    // debug
    underTest.startDebug("Register rules");
    Thread.sleep(2);
    assertThat(tester.logs()).containsOnly("Register rules");
    timing = underTest.stopTrace();
    assertThat(timing).isGreaterThan(0);
    assertThat(tester.logs()).hasSize(2);
    assertThat(tester.logs().get(1)).startsWith("Register rules (done) | time=" + timing);
    tester.clear();

    // info
    underTest.startInfo("Register rules");
    Thread.sleep(2);
    assertThat(tester.logs()).containsOnly("Register rules");
    timing = underTest.stopTrace();
    assertThat(timing).isGreaterThan(0);
    assertThat(tester.logs()).hasSize(2);
    assertThat(tester.logs().get(1)).startsWith("Register rules (done) | time=" + timing);
  }

  @Test
  @UseDataProvider("logTimeLastValues")
  public void different_start_and_stop_messages(boolean logTimeLast) {
    underTest.logTimeLast(logTimeLast);
    tester.setLevel(LoggerLevel.TRACE);

    // start TRACE and stop DEBUG
    underTest.startTrace("Register rules");
    underTest.stopDebug("Rules registered");
    assertThat(tester.logs()).hasSize(2);
    assertThat(tester.logs().get(0)).contains("Register rules");
    assertThat(tester.logs().get(1)).startsWith("Rules registered | time=");
    tester.clear();

    // start DEBUG and stop INFO
    underTest.startDebug("Register rules {}", 10);
    underTest.stopInfo("Rules registered");
    assertThat(tester.logs()).hasSize(2);
    assertThat(tester.logs().get(0)).contains("Register rules 10");
    assertThat(tester.logs().get(1)).startsWith("Rules registered | time=");
    tester.clear();

    // start INFO and stop TRACE
    underTest.startInfo("Register rules");
    underTest.stopTrace("Rules registered");
    assertThat(tester.logs()).hasSize(2);
    assertThat(tester.logs().get(0)).contains("Register rules");
    assertThat(tester.logs().get(1)).startsWith("Rules registered | time=");
  }

  @Test
  @UseDataProvider("logTimeLastValues")
  public void log_on_at_stop(boolean logTimeLast) {
    underTest.logTimeLast(logTimeLast);
    tester.setLevel(LoggerLevel.TRACE);

    // trace
    underTest.start();
    underTest.stopTrace("Rules registered");
    assertThat(tester.logs()).hasSize(1);
    assertThat(tester.logs().get(0)).startsWith("Rules registered | time=");
    tester.clear();

    // debug
    underTest.start();
    underTest.stopDebug("Rules registered {} on {}", 6, 10);
    assertThat(tester.logs()).hasSize(1);
    assertThat(tester.logs().get(0)).startsWith("Rules registered 6 on 10 | time=");
    tester.clear();

    // info
    underTest.start();
    underTest.stopInfo("Rules registered");
    assertThat(tester.logs()).hasSize(1);
    assertThat(tester.logs().get(0)).startsWith("Rules registered | time=");
  }

  @Test
  public void start_writes_no_log_even_if_there_is_context() {
    underTest.addContext("a_string", "bar");
    underTest.addContext("null_value", null);
    underTest.addContext("an_int", 42);
    underTest.start();

    // do not write context as there's no message
    assertThat(tester.logs()).isEmpty();
  }

  @Test
  public void startInfo_writes_log_with_context_appended_when_there_is_a_message() {
    addSomeContext(underTest);
    underTest.startInfo("Foo");

    assertThat(tester.logs(LoggerLevel.INFO)).containsOnly("Foo | a_string=bar | an_int=42 | after_start=true");
  }

  @Test
  public void startDebug_writes_log_with_context_appended_when_there_is_a_message() {
    tester.setLevel(LoggerLevel.DEBUG);
    addSomeContext(underTest);
    underTest.startDebug("Foo");

    assertThat(tester.logs(LoggerLevel.DEBUG)).containsOnly("Foo | a_string=bar | an_int=42 | after_start=true");
  }

  @Test
  public void startTrace_writes_log_with_context_appended_when_there_is_a_message() {
    tester.setLevel(LoggerLevel.TRACE);
    addSomeContext(underTest);
    underTest.startTrace("Foo");

    assertThat(tester.logs(LoggerLevel.TRACE)).containsOnly("Foo | a_string=bar | an_int=42 | after_start=true");
  }

  @Test
  public void stopError_adds_context_after_time_by_default() {
    addSomeContext(underTest);
    underTest.start().stopError("Rules registered");

    assertThat(tester.logs()).hasSize(1);
    assertThat(tester.logs(LoggerLevel.ERROR).get(0))
      .startsWith("Rules registered | time=")
      .endsWith("ms | a_string=bar | an_int=42 | after_start=true");
  }

  @Test
  public void stopInfo_adds_context_after_time_by_default() {
    addSomeContext(underTest);
    underTest.start().stopInfo("Rules registered");

    assertThat(tester.logs()).hasSize(1);
    assertThat(tester.logs(LoggerLevel.INFO).get(0))
      .startsWith("Rules registered | time=")
      .endsWith("ms | a_string=bar | an_int=42 | after_start=true");
  }

  @Test
  public void stopTrace_adds_context_after_time_by_default() {
    tester.setLevel(LoggerLevel.TRACE);
    addSomeContext(underTest);
    underTest.start().stopTrace("Rules registered");

    assertThat(tester.logs()).hasSize(1);
    assertThat(tester.logs(LoggerLevel.TRACE).get(0))
      .startsWith("Rules registered | time=")
      .endsWith("ms | a_string=bar | an_int=42 | after_start=true");
  }

  @Test
  public void stopError_adds_context_before_time_if_logTimeLast_is_true() {
    addSomeContext(underTest);
    underTest.logTimeLast(true);
    underTest.start().stopError("Rules registered");

    assertThat(tester.logs()).hasSize(1);
    assertThat(tester.logs(LoggerLevel.ERROR).get(0))
      .startsWith("Rules registered | a_string=bar | an_int=42 | after_start=true | time=")
      .endsWith("ms");
  }

  @Test
  public void stopInfo_adds_context_before_time_if_logTimeLast_is_true() {
    addSomeContext(underTest);
    underTest.logTimeLast(true);
    underTest.start().stopInfo("Rules registered");

    assertThat(tester.logs()).hasSize(1);
    assertThat(tester.logs(LoggerLevel.INFO).get(0))
      .startsWith("Rules registered | a_string=bar | an_int=42 | after_start=true | time=")
      .endsWith("ms");
  }

  @Test
  public void stopTrace_adds_context_before_time_if_logTimeLast_is_true() {
    tester.setLevel(LoggerLevel.TRACE);
    addSomeContext(underTest);
    underTest.logTimeLast(true);
    underTest.start().stopTrace("Rules registered");

    assertThat(tester.logs()).hasSize(1);
    assertThat(tester.logs(LoggerLevel.TRACE).get(0))
      .startsWith("Rules registered | a_string=bar | an_int=42 | after_start=true | time=")
      .endsWith("ms");
  }

  @Test
  public void stopInfo_clears_context() {
    addSomeContext(underTest);
    underTest.logTimeLast(true);
    underTest.start().stopInfo("Foo");
    underTest.start().stopInfo("Bar");

    assertThat(tester.logs()).hasSize(2);
    List<String> logs = tester.logs(LoggerLevel.INFO);
    assertThat(logs.get(0))
        .startsWith("Foo | a_string=bar | an_int=42 | after_start=true | time=")
        .endsWith("ms");
    assertThat(logs.get(1))
        .startsWith("Bar | time=")
        .endsWith("ms");
  }

  @Test
  public void stopDebug_clears_context() {
    tester.setLevel(LoggerLevel.DEBUG);
    addSomeContext(underTest);
    underTest.logTimeLast(true);
    underTest.start().stopDebug("Foo");
    underTest.start().stopDebug("Bar");

    assertThat(tester.logs()).hasSize(2);
    List<String> logs = tester.logs(LoggerLevel.DEBUG);
    assertThat(logs.get(0))
        .startsWith("Foo | a_string=bar | an_int=42 | after_start=true | time=")
        .endsWith("ms");
    assertThat(logs.get(1))
        .startsWith("Bar | time=")
        .endsWith("ms");
  }

  @Test
  public void stopTrace_clears_context() {
    tester.setLevel(LoggerLevel.TRACE);
    addSomeContext(underTest);
    underTest.logTimeLast(true);
    underTest.start().stopTrace("Foo");
    underTest.start().stopTrace("Bar");

    assertThat(tester.logs()).hasSize(2);
    List<String> logs = tester.logs(LoggerLevel.TRACE);
    assertThat(logs.get(0))
        .startsWith("Foo | a_string=bar | an_int=42 | after_start=true | time=")
        .endsWith("ms");
    assertThat(logs.get(1))
        .startsWith("Bar | time=")
        .endsWith("ms");
  }

  private static void addSomeContext(Profiler profiler) {
    profiler.addContext("a_string", "bar");
    profiler.addContext("null_value", null);
    profiler.addContext("an_int", 42);
    profiler.addContext("after_start", true);
  }

  @Test
  public void empty_message() {
    underTest.addContext("foo", "bar");
    underTest.startInfo("");

    assertThat(tester.logs()).containsOnly("foo=bar");

    underTest.addContext("after_start", true);
    underTest.stopInfo("");
    assertThat(tester.logs()).hasSize(2);
    assertThat(tester.logs().get(1))
      .startsWith("time=")
      .endsWith("ms | foo=bar | after_start=true");
  }

  @Test
  public void fail_if_stop_without_message() {
    underTest.start();
    try {
      underTest.stopInfo();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Profiler#stopXXX() can't be called without any message defined in start methods");
    }
  }

  @Test
  public void fail_if_stop_without_start() {
    try {
      underTest.stopDebug("foo");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Profiler must be started before being stopped");
    }
  }

  @Test
  public void hasContext() {
    assertThat(underTest.hasContext("foo")).isFalse();

    underTest.addContext("foo", "bar");
    assertThat(underTest.hasContext("foo")).isTrue();

    underTest.addContext("foo", null);
    assertThat(underTest.hasContext("foo")).isFalse();
  }
}
