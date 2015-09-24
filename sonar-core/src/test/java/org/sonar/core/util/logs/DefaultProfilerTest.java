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
package org.sonar.core.util.logs;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.Loggers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class DefaultProfilerTest {

  @Rule
  public LogTester tester = new LogTester();

  Profiler underTest = Profiler.create(Loggers.get("DefaultProfilerTest"));

  @Test
  public void test_levels() throws Exception {
    // trace by default
    assertThat(underTest.isDebugEnabled()).isTrue();
    assertThat(underTest.isTraceEnabled()).isTrue();

    tester.setLevel(LoggerLevel.DEBUG);
    assertThat(underTest.isDebugEnabled()).isTrue();
    assertThat(underTest.isTraceEnabled()).isFalse();

    tester.setLevel(LoggerLevel.INFO);
    assertThat(underTest.isDebugEnabled()).isFalse();
    assertThat(underTest.isTraceEnabled()).isFalse();
  }

  @Test
  public void stop_reuses_start_message() throws InterruptedException {
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
    assertThat(tester.logs().get(1)).startsWith("Register rules (done) | time="  + timing);
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
  public void different_start_and_stop_messages() {
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
  public void log_on_at_stop() {
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
  public void add_context() {
    org.sonar.core.util.logs.Profiler profiler = Profiler.create(Loggers.get("DefaultProfilerTest"));
    profiler.addContext("a_string", "bar");
    profiler.addContext("null_value", null);
    profiler.addContext("an_int", 42);
    profiler.start();
    // do not write context as there's no message
    assertThat(tester.logs()).isEmpty();

    profiler.addContext("after_start", true);
    profiler.stopInfo("Rules registered");
    assertThat(tester.logs()).hasSize(1);
    assertThat(tester.logs().get(0))
      .startsWith("Rules registered | time=")
      .endsWith("ms | a_string=bar | an_int=42 | after_start=true");
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
}
