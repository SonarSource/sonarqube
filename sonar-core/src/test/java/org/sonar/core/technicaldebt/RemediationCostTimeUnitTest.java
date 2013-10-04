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
package org.sonar.core.technicaldebt;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class RemediationCostTimeUnitTest {

  private static final int HOURS_IN_DAY = 8;

  @Test
  public void convert_simple_values() {
    checkTimes(RemediationCostTimeUnit.of(15L, HOURS_IN_DAY), 15L, 0L, 0L);
    checkTimes(RemediationCostTimeUnit.of(120L, HOURS_IN_DAY), 0L, 2L, 0L);
    checkTimes(RemediationCostTimeUnit.of(480L, HOURS_IN_DAY), 0L, 0L, 1L);
  }

  @Test
  public void convert_complex_values() {
    checkTimes(RemediationCostTimeUnit.of(70L, HOURS_IN_DAY), 10L, 1L, 0L);
    checkTimes(RemediationCostTimeUnit.of(490L, HOURS_IN_DAY), 10L, 0L, 1L);
    checkTimes(RemediationCostTimeUnit.of(550L, HOURS_IN_DAY), 10L, 1L, 1L);
  }

  private void checkTimes(RemediationCostTimeUnit remediationCostTimeUnit, Long expectedMinutes, Long expectedHours, Long expectedDays ) {
    assertThat(remediationCostTimeUnit.minutes()).isEqualTo(expectedMinutes);
    assertThat(remediationCostTimeUnit.hours()).isEqualTo(expectedHours);
    assertThat(remediationCostTimeUnit.days()).isEqualTo(expectedDays);
  }

}
