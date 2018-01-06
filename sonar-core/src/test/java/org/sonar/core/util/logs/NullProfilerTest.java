/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NullProfilerTest {

  NullProfiler underTest = NullProfiler.NULL_INSTANCE;

  @Test
  public void do_not_fail() {
    assertThat(underTest.start()).isSameAs(underTest);
    assertThat(underTest.startTrace("")).isSameAs(underTest);
    assertThat(underTest.startDebug("")).isSameAs(underTest);
    assertThat(underTest.startInfo("")).isSameAs(underTest);

    assertThat(underTest.isDebugEnabled()).isFalse();
    assertThat(underTest.isTraceEnabled()).isFalse();
    assertThat(underTest.addContext("foo", "bar")).isSameAs(underTest);
    assertThat(underTest.logTimeLast(true)).isSameAs(underTest);
    assertThat(underTest.logTimeLast(false)).isSameAs(underTest);
  }

  @Test
  public void stop_methods_returns_0() {
    underTest.start();
    assertThat(underTest.stopInfo()).isEqualTo(0);
    assertThat(underTest.stopInfo("msg")).isEqualTo(0);
    assertThat(underTest.stopDebug()).isEqualTo(0);
    assertThat(underTest.stopDebug("msg")).isEqualTo(0);
    assertThat(underTest.stopTrace()).isEqualTo(0);
    assertThat(underTest.stopTrace("msg")).isEqualTo(0);
    assertThat(underTest.stopError("msg")).isEqualTo(0);

  }
}
