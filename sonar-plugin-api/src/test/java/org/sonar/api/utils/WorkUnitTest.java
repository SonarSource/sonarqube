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

public class WorkUnitTest {

  @Test
  public void create() throws Exception {
    WorkUnit workUnit = WorkUnit.create(2.0, "mn");
    assertThat(workUnit.getUnit()).isEqualTo("mn");
    assertThat(workUnit.getValue()).isEqualTo(2.0);
  }

  @Test
  public void create_default() throws Exception {
    WorkUnit workUnit = WorkUnit.create();
    assertThat(workUnit.getValue()).isEqualTo(0.0);
  }

  @Test
  public void fail_with_bad_unit() throws Exception {
    try {
      WorkUnit.create(2.0, "z");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void fail_with_bad_value() throws Exception {
    try {
      WorkUnit.create(-2.0, "mn");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void from_long_on_simple_values() {
    checkTimes(WorkUnit.fromLong(1L), 0, 0, 1);
    checkTimes(WorkUnit.fromLong(100L), 0, 1, 0);
    checkTimes(WorkUnit.fromLong(10000L), 1, 0, 0);
  }

  @Test
  public void from_long_on_complex_values() {
    checkTimes(WorkUnit.fromLong(10101L), 1, 1, 1);
    checkTimes(WorkUnit.fromLong(101L), 0, 1, 1);
    checkTimes(WorkUnit.fromLong(10001L), 1, 0, 1);
    checkTimes(WorkUnit.fromLong(10100L), 1, 1, 0);

    checkTimes(WorkUnit.fromLong(112233L), 11, 22, 33);
  }

  @Test
  public void to_long() {
    assertThat(new WorkUnit.Builder().setDays(1).setHours(1).setMinutes(1).build().toLong()).isEqualTo(10101L);
  }

  @Test
  public void test_equals_and_hashCode() throws Exception {
    WorkUnit oneMinute = WorkUnit.fromLong(1L);
    WorkUnit oneHours = WorkUnit.fromLong(100L);
    WorkUnit oneDay = WorkUnit.fromLong(10000L);

    assertThat(oneMinute).isEqualTo(oneMinute);
    assertThat(oneMinute).isEqualTo(WorkUnit.fromLong(1L));
    assertThat(oneHours).isEqualTo(WorkUnit.fromLong(100L));
    assertThat(oneDay).isEqualTo(WorkUnit.fromLong(10000L));

    assertThat(oneMinute).isNotEqualTo(oneHours);
    assertThat(oneHours).isNotEqualTo(oneDay);

    assertThat(oneMinute.hashCode()).isEqualTo(oneMinute.hashCode());
  }

  private void checkTimes(WorkUnit technicalDebt, int expectedDays, int expectedHours, int expectedMinutes) {
    assertThat(technicalDebt.days()).isEqualTo(expectedDays);
    assertThat(technicalDebt.hours()).isEqualTo(expectedHours);
    assertThat(technicalDebt.minutes()).isEqualTo(expectedMinutes);
  }

}
