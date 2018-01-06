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
package org.sonar.api.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeUtilsTest {
  @Test
  public void formatDuration() {
    assertThat(TimeUtils.formatDuration(0)).isEqualTo("0ms");
    assertThat(TimeUtils.formatDuration(100)).isEqualTo("100ms");
    assertThat(TimeUtils.formatDuration(1000)).isEqualTo("1s");
    assertThat(TimeUtils.formatDuration(10000)).isEqualTo("10s");
    assertThat(TimeUtils.formatDuration(100000)).isEqualTo("1min 40s");
    assertThat(TimeUtils.formatDuration(600000)).isEqualTo("10min");
    assertThat(TimeUtils.formatDuration(1000000)).isEqualTo("16min 40s");
    assertThat(TimeUtils.formatDuration(10000000)).isEqualTo("166min 40s");
  }
}
