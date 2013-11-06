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
package org.sonar.api.issue.internal;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class WorkDayDurationTest {

  @Test
  public void from_long_on_simple_values(){
    checkTimes(WorkDayDuration.fromLong(1L), 0, 0, 1);
    checkTimes(WorkDayDuration.fromLong(100L), 0, 1, 0);
    checkTimes(WorkDayDuration.fromLong(10000L), 1, 0, 0);
  }

  @Test
  public void from_long_on_complex_values(){
    checkTimes(WorkDayDuration.fromLong(10101L), 1, 1, 1);
    checkTimes(WorkDayDuration.fromLong(101L), 0, 1, 1);
    checkTimes(WorkDayDuration.fromLong(10001L), 1, 0, 1);
    checkTimes(WorkDayDuration.fromLong(10100L), 1, 1, 0);

    checkTimes(WorkDayDuration.fromLong(112233L), 11, 22, 33);
  }

  @Test
  public void to_long(){
    assertThat(WorkDayDuration.of(1, 1, 1).toLong()).isEqualTo(10101L);
  }

  @Test
  public void test_equals_and_hashCode() throws Exception {
    WorkDayDuration oneMinute = WorkDayDuration.fromLong(1L);
    WorkDayDuration oneHours = WorkDayDuration.fromLong(100L);
    WorkDayDuration oneDay = WorkDayDuration.fromLong(10000L);

    assertThat(oneMinute).isEqualTo(oneMinute);
    assertThat(oneMinute).isEqualTo(WorkDayDuration.fromLong(1L));
    assertThat(oneHours).isEqualTo(WorkDayDuration.fromLong(100L));
    assertThat(oneDay).isEqualTo(WorkDayDuration.fromLong(10000L));

    assertThat(oneMinute).isNotEqualTo(oneHours);
    assertThat(oneHours).isNotEqualTo(oneDay);

    assertThat(oneMinute.hashCode()).isEqualTo(oneMinute.hashCode());
  }

  private void checkTimes(WorkDayDuration technicalDebt, int expectedDays, int expectedHours, int expectedMinutes){
    assertThat(technicalDebt.days()).isEqualTo(expectedDays);
    assertThat(technicalDebt.hours()).isEqualTo(expectedHours);
    assertThat(technicalDebt.minutes()).isEqualTo(expectedMinutes);
  }

}
