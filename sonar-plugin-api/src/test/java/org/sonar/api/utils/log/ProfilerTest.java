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
package org.sonar.api.utils.log;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProfilerTest {
  @Rule
  public LogTester tester = new LogTester();

  @Test
  public void create() {
    Profiler profiler = Profiler.create(Loggers.get("foo"));
    assertThat(profiler).isInstanceOf(DefaultProfiler.class);
  }

  @Test
  public void create_null_profiler_if_trace_level_is_disabled() {
    tester.setLevel(LoggerLevel.TRACE);
    Profiler profiler = Profiler.createIfTrace(Loggers.get("foo"));
    assertThat(profiler).isInstanceOf(DefaultProfiler.class);

    tester.setLevel(LoggerLevel.DEBUG);
    profiler = Profiler.createIfTrace(Loggers.get("foo"));
    assertThat(profiler).isInstanceOf(NullProfiler.class);
  }

  @Test
  public void create_null_profiler_if_debug_level_is_disabled() {
    tester.setLevel(LoggerLevel.TRACE);
    Profiler profiler = Profiler.createIfDebug(Loggers.get("foo"));
    assertThat(profiler).isInstanceOf(DefaultProfiler.class);

    tester.setLevel(LoggerLevel.INFO);
    profiler = Profiler.createIfDebug(Loggers.get("foo"));
    assertThat(profiler).isInstanceOf(NullProfiler.class);
  }
}
