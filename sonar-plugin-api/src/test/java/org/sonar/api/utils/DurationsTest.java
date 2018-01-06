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

public class DurationsTest {

  private static final int HOURS_IN_DAY = 8;

  private static final long ONE_MINUTE = 1L;
  private static final long ONE_HOUR = ONE_MINUTE * 60;
  private static final long ONE_DAY = HOURS_IN_DAY * ONE_HOUR;

  private Durations underTest = new Durations();

  @Test
  public void create_from_minutes() {
    assertThat(underTest.create(10L).toMinutes()).isEqualTo(10L);
  }

  @Test
  public void decode() {
    // 1 working day -> 8 hours
    assertThat(underTest.decode("1d").toMinutes()).isEqualTo(8L * ONE_HOUR);
    // 8 hours
    assertThat(underTest.decode("8h").toMinutes()).isEqualTo(8L * ONE_HOUR);
  }

  @Test
  public void format() {
    assertThat(underTest.format(Duration.create(5 * ONE_DAY))).isEqualTo("5d");
    assertThat(underTest.format(Duration.create(2 * ONE_HOUR))).isEqualTo("2h");
    assertThat(underTest.format(Duration.create(ONE_MINUTE))).isEqualTo("1min");

    assertThat(underTest.format(Duration.create(5 * ONE_DAY + 2 * ONE_HOUR))).isEqualTo("5d 2h");
    assertThat(underTest.format(Duration.create(2 * ONE_HOUR + ONE_MINUTE))).isEqualTo("2h 1min");
    assertThat(underTest.format(Duration.create(5 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE))).isEqualTo("5d 2h");
  }

  @Test
  public void not_display_following_element_when_bigger_than_ten() {
    assertThat(underTest.format(Duration.create(15 * ONE_DAY + 7 * ONE_HOUR + ONE_MINUTE))).isEqualTo("15d");
  }

  @Test
  public void display_zero_without_unit() {
    assertThat(underTest.format(Duration.create(0))).isEqualTo("0");
  }

  @Test
  public void display_negative_duration() {
    assertThat(underTest.format(Duration.create(-5 * ONE_DAY))).isEqualTo("-5d");
    assertThat(underTest.format(Duration.create(-2 * ONE_HOUR))).isEqualTo("-2h");
    assertThat(underTest.format(Duration.create(-1 * ONE_MINUTE))).isEqualTo("-1min");
  }

}
