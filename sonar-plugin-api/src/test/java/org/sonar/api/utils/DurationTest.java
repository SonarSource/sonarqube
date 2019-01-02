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
package org.sonar.api.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class DurationTest {

  static final int HOURS_IN_DAY = 8;

  static final Long ONE_MINUTE = 1L;
  static final Long ONE_HOUR_IN_MINUTES = ONE_MINUTE * 60;
  static final Long ONE_DAY_IN_MINUTES = ONE_HOUR_IN_MINUTES * HOURS_IN_DAY;

  @Test
  public void create_from_duration_in_minutes() {
    Duration duration = Duration.create(ONE_DAY_IN_MINUTES + ONE_HOUR_IN_MINUTES + ONE_MINUTE);
    assertThat(duration.toMinutes()).isEqualTo(ONE_DAY_IN_MINUTES + ONE_HOUR_IN_MINUTES + ONE_MINUTE);
  }

  @Test
  public void encode() {
    assertThat(Duration.create(2 * ONE_DAY_IN_MINUTES + 5 * ONE_HOUR_IN_MINUTES + 46 * ONE_MINUTE).encode(HOURS_IN_DAY)).isEqualTo("2d5h46min");
    assertThat(Duration.create(ONE_DAY_IN_MINUTES).encode(HOURS_IN_DAY)).isEqualTo("1d");
    assertThat(Duration.create(ONE_HOUR_IN_MINUTES).encode(HOURS_IN_DAY)).isEqualTo("1h");
    assertThat(Duration.create(HOURS_IN_DAY * ONE_HOUR_IN_MINUTES).encode(HOURS_IN_DAY)).isEqualTo("1d");
    assertThat(Duration.create(ONE_MINUTE).encode(HOURS_IN_DAY)).isEqualTo("1min");
    assertThat(Duration.create(0).encode(HOURS_IN_DAY)).isEqualTo("0min");
  }

  @Test
  public void decode() {
    assertThat(Duration.decode("    15 d  23  h     42min  ", HOURS_IN_DAY)).isEqualTo(Duration.create(15 * ONE_DAY_IN_MINUTES + 23 * ONE_HOUR_IN_MINUTES + 42 * ONE_MINUTE));
    assertThat(Duration.decode("15d23h42min", HOURS_IN_DAY)).isEqualTo(Duration.create(15 * ONE_DAY_IN_MINUTES + 23 * ONE_HOUR_IN_MINUTES + 42 * ONE_MINUTE));
    assertThat(Duration.decode("23h", HOURS_IN_DAY)).isEqualTo(Duration.create(23 * ONE_HOUR_IN_MINUTES));
    assertThat(Duration.decode("15d", HOURS_IN_DAY)).isEqualTo(Duration.create(15 * ONE_DAY_IN_MINUTES));
    assertThat(Duration.decode("42min", HOURS_IN_DAY)).isEqualTo(Duration.create(42 * ONE_MINUTE));
    assertThat(Duration.decode("0min", HOURS_IN_DAY)).isEqualTo(Duration.create(0));

    assertThat(Duration.decode("25h61min", HOURS_IN_DAY)).isEqualTo(Duration.create(25 * ONE_HOUR_IN_MINUTES + 61));
  }

  @Test
  public void fail_to_decode_if_unit_with_invalid_number() {
    try {
      Duration.decode("Xd", HOURS_IN_DAY);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Duration 'Xd' is invalid, it should use the following sample format : 2d 10h 15min");
    }
  }

  @Test
  public void fail_to_decode_if_no_valid_duration() {
    try {
      Duration.decode("foo", HOURS_IN_DAY);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Duration 'foo' is invalid, it should use the following sample format : 2d 10h 15min");
    }
  }

  @Test
  public void fail_to_decode_if_only_number() {
    try {
      Duration.decode("15", HOURS_IN_DAY);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Duration '15' is invalid, it should use the following sample format : 2d 10h 15min");
    }
  }

  @Test
  public void fail_to_decode_if_valid_unit_with_invalid_duration() {
    try {
      Duration.decode("15min foo", HOURS_IN_DAY);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Duration '15min foo' is invalid, it should use the following sample format : 2d 10h 15min");
    }
  }

  @Test
  public void is_greater_than() {
    assertThat(Duration.decode("1h", HOURS_IN_DAY).isGreaterThan(Duration.decode("1min", HOURS_IN_DAY))).isTrue();
    assertThat(Duration.decode("1min", HOURS_IN_DAY).isGreaterThan(Duration.decode("1d", HOURS_IN_DAY))).isFalse();
    assertThat(Duration.decode("1d", HOURS_IN_DAY).isGreaterThan(Duration.decode("1d", HOURS_IN_DAY))).isFalse();
    assertThat(Duration.decode("1d", 10).isGreaterThan(Duration.decode("1d", 8))).isTrue();
  }

  @Test
  public void add() {
    assertThat(Duration.decode("1h", HOURS_IN_DAY).add(Duration.decode("1min", HOURS_IN_DAY))).isEqualTo(Duration.decode("1h1min", HOURS_IN_DAY));
  }

  @Test
  public void subtract() {
    assertThat(Duration.decode("1h", HOURS_IN_DAY).subtract(Duration.decode("1min", HOURS_IN_DAY))).isEqualTo(Duration.decode("59min", HOURS_IN_DAY));
  }

  @Test
  public void multiply() {
    assertThat(Duration.decode("1h", HOURS_IN_DAY).multiply(2)).isEqualTo(Duration.decode("2h", HOURS_IN_DAY));
  }

  @Test
  public void test_equals_and_hashcode() throws Exception {
    Duration duration = Duration.create(ONE_DAY_IN_MINUTES + ONE_HOUR_IN_MINUTES + ONE_MINUTE);
    Duration durationWithSameValue = Duration.create(ONE_DAY_IN_MINUTES + ONE_HOUR_IN_MINUTES + ONE_MINUTE);
    Duration durationWithDifferentValue = Duration.create(ONE_DAY_IN_MINUTES + ONE_HOUR_IN_MINUTES);

    assertThat(duration).isEqualTo(duration);
    assertThat(durationWithSameValue).isEqualTo(duration);
    assertThat(durationWithDifferentValue).isNotEqualTo(duration);
    assertThat(duration).isNotEqualTo(null);

    assertThat(duration.hashCode()).isEqualTo(duration.hashCode());
    assertThat(durationWithSameValue.hashCode()).isEqualTo(duration.hashCode());
    assertThat(durationWithDifferentValue.hashCode()).isNotEqualTo(duration.hashCode());
  }

  @Test
  public void test_toString() throws Exception {
    assertThat(Duration.create(ONE_DAY_IN_MINUTES + ONE_HOUR_IN_MINUTES + ONE_MINUTE).toString()).isNotNull();
  }

}
