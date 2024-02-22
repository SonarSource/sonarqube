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
package org.sonar.db.purge;

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PurgeProfilerTest {

  private MockedClock clock;
  private PurgeProfiler profiler;

  @BeforeEach
  void prepare() {
    clock = new MockedClock();
    profiler = new PurgeProfiler(clock);
  }

  @Test
  void shouldProfilePurge() {
    profiler.start("foo");
    clock.sleep(10);
    profiler.stop();

    profiler.start("bar");
    clock.sleep(5);
    profiler.stop();

    profiler.start("foo");
    clock.sleep(8);
    profiler.stop();

    List<String> profilingResult = profiler.getProfilingResult(50);
    Assertions.assertThat(profilingResult).hasSize(2);
    assertThat(profilingResult.get(0)).contains("foo: 18ms");
    assertThat(profilingResult.get(1)).contains("bar: 5ms");
  }

  @Test
  void shouldResetPurgeProfiling() {
    profiler.start("foo");
    clock.sleep(10);
    profiler.stop();

    profiler.reset();

    profiler.start("bar");
    clock.sleep(5);
    profiler.stop();

    profiler.start("foo");
    clock.sleep(8);
    profiler.stop();

    List<String> profilingResult = profiler.getProfilingResult(50);
    Assertions.assertThat(profilingResult).hasSize(2);
    assertThat(profilingResult.get(0)).contains("foo: 8ms");
    assertThat(profilingResult.get(1)).contains("bar: 5ms");
  }

  private static class MockedClock extends PurgeProfiler.Clock {
    private long now = 0;

    @Override
    public long now() {
      return now;
    }

    public void sleep(long duration) {
      now += duration;
    }
  }
}
