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
package org.sonar.server.badge.ws;

import org.junit.Test;
import org.sonar.test.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.badge.ws.SvgFormatter.formatDuration;
import static org.sonar.server.badge.ws.SvgFormatter.formatNumeric;
import static org.sonar.server.badge.ws.SvgFormatter.formatPercent;

public class SvgFormatterTest {

  private static final int HOURS_IN_DAY = 8;

  private static final long ONE_MINUTE = 1L;
  private static final long ONE_HOUR = ONE_MINUTE * 60;
  private static final long ONE_DAY = HOURS_IN_DAY * ONE_HOUR;

  @Test
  public void format_numeric() {
    assertThat(formatNumeric(0L)).isEqualTo("0");

    assertThat(formatNumeric(5L)).isEqualTo("5");
    assertThat(formatNumeric(950L)).isEqualTo("950");

    assertThat(formatNumeric(1_000L)).isEqualTo("1k");
    assertThat(formatNumeric(1_010L)).isEqualTo("1k");
    assertThat(formatNumeric(1_100L)).isEqualTo("1.1k");
    assertThat(formatNumeric(1_690L)).isEqualTo("1.7k");
    assertThat(formatNumeric(950_000L)).isEqualTo("950k");

    assertThat(formatNumeric(1_000_000L)).isEqualTo("1m");
    assertThat(formatNumeric(1_010_000L)).isEqualTo("1m");

    assertThat(formatNumeric(1_000_000_000L)).isEqualTo("1b");

    assertThat(formatNumeric(1_000_000_000_000L)).isEqualTo("1t");
  }

  @Test
  public void format_percent() {
    assertThat(formatPercent(0d)).isEqualTo("0%");
    assertThat(formatPercent(12.345)).isEqualTo("12.3%");
    assertThat(formatPercent(12.56)).isEqualTo("12.6%");
  }

  @Test
  public void format_duration() {
    assertThat(formatDuration(0)).isEqualTo("0");
    assertThat(formatDuration(ONE_DAY)).isEqualTo("1d");
    assertThat(formatDuration(ONE_HOUR)).isEqualTo("1h");
    assertThat(formatDuration(ONE_MINUTE)).isEqualTo("1min");

    assertThat(formatDuration(5 * ONE_DAY)).isEqualTo("5d");
    assertThat(formatDuration(2 * ONE_HOUR)).isEqualTo("2h");
    assertThat(formatDuration(ONE_MINUTE)).isEqualTo("1min");

    assertThat(formatDuration(5 * ONE_DAY + 3 * ONE_HOUR)).isEqualTo("5d");
    assertThat(formatDuration(3 * ONE_HOUR + 25 * ONE_MINUTE)).isEqualTo("3h");
    assertThat(formatDuration(5 * ONE_DAY + 3 * ONE_HOUR + 40 * ONE_MINUTE)).isEqualTo("5d");
  }

  @Test
  public void format_duration_is_rounding_result() {
    // When starting to add more than 4 hours, the result will be rounded to the next day (as 4 hour is a half day)
    assertThat(formatDuration(5 * ONE_DAY + 4 * ONE_HOUR)).isEqualTo("6d");
    assertThat(formatDuration(5 * ONE_DAY + 5 * ONE_HOUR)).isEqualTo("6d");

    // When starting to add more than 30 minutes, the result will be rounded to the next hour
    assertThat(formatDuration(3 * ONE_HOUR + 30 * ONE_MINUTE)).isEqualTo("4h");
    assertThat(formatDuration(3 * ONE_HOUR + 40 * ONE_MINUTE)).isEqualTo("4h");

    // When duration is close to next unit (0.9), the result is rounded to next unit
    assertThat(formatDuration(7 * ONE_HOUR + 20 + ONE_MINUTE)).isEqualTo("1d");
    assertThat(formatDuration(55 * ONE_MINUTE)).isEqualTo("1h");
  }

  @Test
  public void only_statics() {
    assertThat(TestUtils.hasOnlyPrivateConstructors(SvgFormatter.class)).isTrue();
  }
}
