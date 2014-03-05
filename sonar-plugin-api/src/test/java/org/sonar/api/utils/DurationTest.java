/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.api.utils;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

public class DurationTest {

  static final Long ONE_MINUTE = 1L;
  static final Long ONE_HOUR_IN_MINUTES = ONE_MINUTE * 60;
  static final Long ONE_DAY_IN_MINUTES = ONE_HOUR_IN_MINUTES * 24;

  @Test
  public void of_days() throws Exception {
    Duration duration = Duration.ofDays(1);
    assertThat(duration.days()).isEqualTo(1);
    assertThat(duration.hours()).isEqualTo(0);
    assertThat(duration.minutes()).isEqualTo(0);
    assertThat(duration.toMinutes()).isEqualTo(ONE_DAY_IN_MINUTES);
  }

  @Test
  public void of_hours() throws Exception {
    Duration duration = Duration.ofHours(1);
    assertThat(duration.days()).isEqualTo(0);
    assertThat(duration.hours()).isEqualTo(1);
    assertThat(duration.minutes()).isEqualTo(0);
    assertThat(duration.toMinutes()).isEqualTo(ONE_HOUR_IN_MINUTES);

    duration = Duration.ofHours(25);
    assertThat(duration.days()).isEqualTo(1);
    assertThat(duration.hours()).isEqualTo(1);
    assertThat(duration.minutes()).isEqualTo(0);
    assertThat(duration.toMinutes()).isEqualTo(ONE_DAY_IN_MINUTES + ONE_HOUR_IN_MINUTES);
  }

  @Test
  public void of_minutes() throws Exception {
    Duration duration = Duration.ofMinutes(1);
    assertThat(duration.days()).isEqualTo(0);
    assertThat(duration.hours()).isEqualTo(0);
    assertThat(duration.minutes()).isEqualTo(1);
    assertThat(duration.toMinutes()).isEqualTo(ONE_MINUTE);

    duration = Duration.ofMinutes(61);
    assertThat(duration.days()).isEqualTo(0);
    assertThat(duration.hours()).isEqualTo(1);
    assertThat(duration.minutes()).isEqualTo(1);
    assertThat(duration.toMinutes()).isEqualTo(ONE_HOUR_IN_MINUTES + ONE_MINUTE);
  }

  @Test
  public void create_from_duration_in_minutes() throws Exception {
    Duration duration = Duration.create(ONE_DAY_IN_MINUTES + ONE_HOUR_IN_MINUTES + ONE_MINUTE);
    assertThat(duration.days()).isEqualTo(1);
    assertThat(duration.hours()).isEqualTo(1);
    assertThat(duration.minutes()).isEqualTo(1);
    assertThat(duration.toMinutes()).isEqualTo(ONE_DAY_IN_MINUTES + ONE_HOUR_IN_MINUTES + ONE_MINUTE);
  }

  @Test
  public void encode() throws Exception {
    assertThat(Duration.create(2 * ONE_DAY_IN_MINUTES + 5 * ONE_HOUR_IN_MINUTES + 46 * ONE_MINUTE).encode()).isEqualTo("2d5h46min");
    assertThat(Duration.create(ONE_DAY_IN_MINUTES).encode()).isEqualTo("1d");
    assertThat(Duration.create(ONE_HOUR_IN_MINUTES).encode()).isEqualTo("1h");
    assertThat(Duration.create(ONE_MINUTE).encode()).isEqualTo("1min");
  }

  @Test
  public void decode() throws Exception {
    assertThat(Duration.decode("    15 d  26  h     42min  ")).isEqualTo(Duration.create(15 * ONE_DAY_IN_MINUTES + 26 * ONE_HOUR_IN_MINUTES + 42 * ONE_MINUTE));
    assertThat(Duration.decode("26h15d42min")).isEqualTo(Duration.create(15 * ONE_DAY_IN_MINUTES + 26 * ONE_HOUR_IN_MINUTES + 42 * ONE_MINUTE));
    assertThat(Duration.decode("26h")).isEqualTo(Duration.create(26 * ONE_HOUR_IN_MINUTES));
    assertThat(Duration.decode("15d")).isEqualTo(Duration.create(15 * ONE_DAY_IN_MINUTES));
    assertThat(Duration.decode("42min")).isEqualTo(Duration.create(42 * ONE_MINUTE));
  }

  @Test
  public void fail_to_decode_if_not_number() throws Exception {
    try {
    Duration.decode("Xd");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("'Xd' is invalid, it should use the following sample format : 2d 10h 15min");
    }
  }

  @Test
  public void convert_to_minutes_with_given_hours_in_day() throws Exception {
    assertThat(Duration.create(2 * ONE_DAY_IN_MINUTES).toMinutes(8)).isEqualTo(2 * 8 * ONE_HOUR_IN_MINUTES);
    assertThat(Duration.create(2 * ONE_HOUR_IN_MINUTES).toMinutes(8)).isEqualTo(2 * ONE_HOUR_IN_MINUTES);
    assertThat(Duration.create(2 * ONE_MINUTE).toMinutes(8)).isEqualTo(2 * ONE_MINUTE);
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
