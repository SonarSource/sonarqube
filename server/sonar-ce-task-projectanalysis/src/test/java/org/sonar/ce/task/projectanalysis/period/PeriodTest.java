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
package org.sonar.ce.task.projectanalysis.period;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_DAYS;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_VERSION;

public class PeriodTest {

  private static final String SOME_MODE_PARAM = "mode_para";
  private static final long SOME_SNAPSHOT_DATE = 1000l;
  private static final String SOME_ANALYSIS_UUID = "U1";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_some_setters_and_getters() {
    Period period = new Period(LEAK_PERIOD_MODE_VERSION, SOME_MODE_PARAM, SOME_SNAPSHOT_DATE, SOME_ANALYSIS_UUID);

    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_VERSION);
    assertThat(period.getModeParameter()).isEqualTo(SOME_MODE_PARAM);
    assertThat(period.getSnapshotDate()).isEqualTo(SOME_SNAPSHOT_DATE);
    assertThat(period.getAnalysisUuid()).isEqualTo(SOME_ANALYSIS_UUID);
  }

  @Test
  public void verify_to_string() {
    assertThat(new Period(LEAK_PERIOD_MODE_VERSION, "2.3", 1420034400000L, "U10").toString())
      .isEqualTo("Period{mode=version, modeParameter=2.3, snapshotDate=1420034400000, analysisUuid=U10}");
  }

  @Test
  public void equals_is_done_on_all_fields() {
    Period period = new Period(LEAK_PERIOD_MODE_VERSION, "2.3", 1420034400000L, "U10");

    assertThat(period).isEqualTo(new Period(LEAK_PERIOD_MODE_VERSION, "2.3", 1420034400000L, "U10"));

    assertThat(period).isNotEqualTo(null);
    assertThat(period).isNotEqualTo("sdsd");
    assertThat(period).isNotEqualTo(new Period(LEAK_PERIOD_MODE_DAYS, "2.3", 1420034400000L, "U10"));
    assertThat(period).isNotEqualTo(new Period(LEAK_PERIOD_MODE_VERSION, "2.4", 1420034400000L, "U10"));
    assertThat(period).isNotEqualTo(new Period(LEAK_PERIOD_MODE_VERSION, "2.3", 555L, "U10"));
    assertThat(period).isNotEqualTo(new Period(LEAK_PERIOD_MODE_VERSION, "2.3", 1420034400000L, "9632554"));

  }
}
