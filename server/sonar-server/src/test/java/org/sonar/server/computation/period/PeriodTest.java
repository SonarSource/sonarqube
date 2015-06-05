/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonar.server.computation.period;

import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.core.component.SnapshotDto;

import static org.assertj.core.api.Assertions.assertThat;

public class PeriodTest {

  @Test
  public void test_some_setters_and_getters() {
    Long date = System.currentTimeMillis();
    SnapshotDto snapshotDto = new SnapshotDto().setCreatedAt(date);
    Period period = new Period(CoreProperties.TIMEMACHINE_MODE_VERSION, 1000L, snapshotDto)
      .setModeParameter("2.3")
      .setIndex(1);

    assertThat(period.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_VERSION);
    assertThat(period.getModeParameter()).isEqualTo("2.3");
    assertThat(period.getIndex()).isEqualTo(1);
    assertThat(period.getProjectSnapshot()).isEqualTo(snapshotDto);
    assertThat(period.getSnapshotDate()).isEqualTo(date);
    assertThat(period.getTargetDate()).isEqualTo(1000L);
  }

  @Test
  public void to_string_for_version() {
    Period period = new Period(CoreProperties.TIMEMACHINE_MODE_VERSION, 1000L, new SnapshotDto().setCreatedAt(System.currentTimeMillis())).setModeParameter("2.3");
    assertThat(period.toString()).startsWith("Compare to version 2.3");
  }

  @Test
  public void to_string_for_version_without_date() {
    Period period = new Period(CoreProperties.TIMEMACHINE_MODE_VERSION, null, new SnapshotDto().setCreatedAt(System.currentTimeMillis())).setModeParameter("2.3");
    assertThat(period.toString()).isEqualTo("Compare to version 2.3");
  }

  @Test
  public void to_string_for_number_of_days() {
    Period period = new Period(CoreProperties.TIMEMACHINE_MODE_DAYS, 1000L, new SnapshotDto().setCreatedAt(System.currentTimeMillis())).setModeParameter("30");
    assertThat(period.toString()).startsWith("Compare over 30 days (");
  }

  @Test
  public void to_string_for_number_of_days_with_snapshot() {
    Period period = new Period(CoreProperties.TIMEMACHINE_MODE_DAYS, 1000L, new SnapshotDto().setCreatedAt(System.currentTimeMillis())).setModeParameter("30");
    assertThat(period.toString()).startsWith("Compare over 30 days (");
  }

  @Test
  public void to_string_for_date() {
    Period period = new Period(CoreProperties.TIMEMACHINE_MODE_DATE, 1000L, new SnapshotDto().setCreatedAt(System.currentTimeMillis()));
    assertThat(period.toString()).startsWith("Compare to date ");
  }

  @Test
  public void to_string_for_date_with_snapshot() {
    Period period = new Period(CoreProperties.TIMEMACHINE_MODE_DATE, 1000L, new SnapshotDto().setCreatedAt(System.currentTimeMillis()));
    assertThat(period.toString()).startsWith("Compare to date ");
  }

  @Test
  public void to_string_for_previous_analysis() {
    Period period = new Period(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, 1000L, new SnapshotDto().setCreatedAt(System.currentTimeMillis()));
    assertThat(period.toString()).startsWith("Compare to previous analysis ");
  }

}
