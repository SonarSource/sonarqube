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
package org.sonar.api.utils.log;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class DefaultProfilerTest {

  @Rule
  public LogTester tester = new LogTester();

  Profiler sut = Profiler.create(Loggers.get("DefaultProfilerTest"));

  @Test
  public void test_levels() throws Exception {
    // trace by default
    assertThat(sut.isDebugEnabled()).isTrue();
    assertThat(sut.isTraceEnabled()).isTrue();

    tester.setLevel(LoggerLevel.DEBUG);
    assertThat(sut.isDebugEnabled()).isTrue();
    assertThat(sut.isTraceEnabled()).isFalse();

    tester.setLevel(LoggerLevel.INFO);
    assertThat(sut.isDebugEnabled()).isFalse();
    assertThat(sut.isTraceEnabled()).isFalse();
  }

  @Test
  public void stop_reuses_start_message() throws Exception {
    tester.setLevel(LoggerLevel.TRACE);

    // trace
    sut.startTrace("Register rules");
    assertThat(tester.logs()).containsOnly("Register rules");
    sut.stopTrace();
    assertThat(tester.logs()).hasSize(2);
    assertThat(tester.logs().get(1)).startsWith("Register rules (done) | time=");
    tester.clear();

    // debug
    sut.startDebug("Register rules");
    assertThat(tester.logs()).containsOnly("Register rules");
    sut.stopTrace();
    assertThat(tester.logs()).hasSize(2);
    assertThat(tester.logs().get(1)).startsWith("Register rules (done) | time=");
    tester.clear();

    // info
    sut.startInfo("Register rules");
    assertThat(tester.logs()).containsOnly("Register rules");
    sut.stopTrace();
    assertThat(tester.logs()).hasSize(2);
    assertThat(tester.logs().get(1)).startsWith("Register rules (done) | time=");
  }

  @Test
  public void different_start_and_stop_messages() throws Exception {
    tester.setLevel(LoggerLevel.TRACE);

    // start TRACE and stop DEBUG
    sut.startTrace("Register rules");
    sut.stopDebug("Rules registered");
    assertThat(tester.logs()).hasSize(2);
    assertThat(tester.logs().get(0)).contains("Register rules");
    assertThat(tester.logs().get(1)).startsWith("Rules registered | time=");
    tester.clear();

    // start DEBUG and stop INFO
    sut.startDebug("Register rules");
    sut.stopInfo("Rules registered");
    assertThat(tester.logs()).hasSize(2);
    assertThat(tester.logs().get(0)).contains("Register rules");
    assertThat(tester.logs().get(1)).startsWith("Rules registered | time=");
    tester.clear();

    // start INFO and stop TRACE
    sut.startInfo("Register rules");
    sut.stopTrace("Rules registered");
    assertThat(tester.logs()).hasSize(2);
    assertThat(tester.logs().get(0)).contains("Register rules");
    assertThat(tester.logs().get(1)).startsWith("Rules registered | time=");
  }

  @Test
  public void log_on_at_stop() throws Exception {
    tester.setLevel(LoggerLevel.TRACE);

    // trace
    sut.start();
    sut.stopTrace("Rules registered");
    assertThat(tester.logs()).hasSize(1);
    assertThat(tester.logs().get(0)).startsWith("Rules registered | time=");
    tester.clear();

    // debug
    sut.start();
    sut.stopDebug("Rules registered");
    assertThat(tester.logs()).hasSize(1);
    assertThat(tester.logs().get(0)).startsWith("Rules registered | time=");
    tester.clear();

    // info
    sut.start();
    sut.stopInfo("Rules registered");
    assertThat(tester.logs()).hasSize(1);
    assertThat(tester.logs().get(0)).startsWith("Rules registered | time=");
  }

  @Test
  public void add_context() throws Exception {
    Profiler profiler = Profiler.create(Loggers.get("DefaultProfilerTest"));
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
  public void empty_message() throws Exception {
    sut.addContext("foo", "bar");
    sut.startInfo("");
    assertThat(tester.logs()).containsOnly("foo=bar");

    sut.addContext("after_start", true);
    sut.stopInfo("");
    assertThat(tester.logs()).hasSize(2);
    assertThat(tester.logs().get(1))
      .startsWith("time=")
      .endsWith("ms | foo=bar | after_start=true");
  }

  @Test
  public void fail_if_stop_without_message() throws Exception {
    sut.start();
    try {
      sut.stopInfo();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Profiler#stopXXX() can't be called without any message defined in start methods");
    }
  }

  @Test
  public void fail_if_stop_without_start() throws Exception {
    try {
      sut.stopDebug("foo");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Profiler must be started before being stopped");
    }
  }
}
