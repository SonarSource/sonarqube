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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;

import static org.assertj.core.api.Assertions.assertThat;

public class PeriodTest {

  private static final String SOME_MODE = "mode";
  private static final String SOME_MODE_PARAM = "mode_para";
  private static final long SOME_SNAPSHOT_DATE = 1000l;
  private static final long SOME_SNAPSHOT_ID = 42l;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_some_setters_and_getters() {
    Period period = new Period(1, CoreProperties.TIMEMACHINE_MODE_VERSION, SOME_MODE_PARAM, SOME_SNAPSHOT_DATE, SOME_SNAPSHOT_ID);

    assertThat(period.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_VERSION);
    assertThat(period.getModeParameter()).isEqualTo(SOME_MODE_PARAM);
    assertThat(period.getIndex()).isEqualTo(1);
    assertThat(period.getSnapshotDate()).isEqualTo(SOME_SNAPSHOT_DATE);
    assertThat(period.getSnapshotId()).isEqualTo(SOME_SNAPSHOT_ID);
  }

  @Test
  public void constructor_throws_IAE_if_index_is_0() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Period index (0) must be > 0 and < 6");

    new Period(0, SOME_MODE, SOME_MODE_PARAM, SOME_SNAPSHOT_DATE, SOME_SNAPSHOT_ID);
  }

  @Test
  public void constructor_throws_IAE_if_index_is_6() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Period index (6) must be > 0 and < 6");

    new Period(6, SOME_MODE, SOME_MODE_PARAM, SOME_SNAPSHOT_DATE, SOME_SNAPSHOT_ID);
  }

  @Test
  public void constructor_throws_IAE_if_index_is_less_then_1() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Period index (-156) must be > 0 and < 6");

    new Period(-156, SOME_MODE, SOME_MODE_PARAM, SOME_SNAPSHOT_DATE, SOME_SNAPSHOT_ID);
  }

  @Test
  public void verify_to_string() {
    assertThat(new Period(1, CoreProperties.TIMEMACHINE_MODE_VERSION, "2.3", 1420034400000L, 10L).toString())
      .isEqualTo("Period{index=1, mode=version, modeParameter=2.3, snapshotDate=1420034400000, snapshotId=10}");
  }
}
