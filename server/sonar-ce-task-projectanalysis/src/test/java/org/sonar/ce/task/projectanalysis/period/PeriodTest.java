/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.period;

import org.junit.Test;
import org.sonar.db.newcodeperiod.NewCodePeriodType;

import static org.assertj.core.api.Assertions.assertThat;

public class PeriodTest {

  private static final String SOME_MODE_PARAM = "mode_para";
  private static final long SOME_SNAPSHOT_DATE = 1000L;


  @Test
  public void test_some_setters_and_getters() {
    Period period = new Period(NewCodePeriodType.PREVIOUS_VERSION.name(), SOME_MODE_PARAM, SOME_SNAPSHOT_DATE);

    assertThat(period.getMode()).isEqualTo(NewCodePeriodType.PREVIOUS_VERSION.name());
    assertThat(period.getModeParameter()).isEqualTo(SOME_MODE_PARAM);
    assertThat(period.getDate()).isEqualTo(SOME_SNAPSHOT_DATE);
  }

  @Test
  public void verify_to_string() {
    assertThat(new Period(NewCodePeriodType.PREVIOUS_VERSION.name(), "2.3", 1420034400000L))
      .hasToString("Period{mode=PREVIOUS_VERSION, modeParameter=2.3, date=1420034400000}");
  }

  @Test
  public void equals_is_done_on_all_fields() {
    Period period = new Period(NewCodePeriodType.NUMBER_OF_DAYS.name(), "2.3", 1420034400000L);

    assertThat(period)
      .isEqualTo(new Period(NewCodePeriodType.NUMBER_OF_DAYS.name(), "2.3", 1420034400000L))
      .isNotNull()
      .isNotEqualTo("sdsd")
      .isNotEqualTo(new Period(NewCodePeriodType.PREVIOUS_VERSION.name(), "2.3", 1420034400000L))
      .isNotEqualTo(new Period(NewCodePeriodType.NUMBER_OF_DAYS.name(), "2.4", 1420034400000L))
      .isNotEqualTo(new Period(NewCodePeriodType.NUMBER_OF_DAYS.name(), "2.3", 1420034410000L));

  }
}
