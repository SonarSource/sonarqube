/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.period;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PeriodPredicatesTest {

  @Test(expected = NullPointerException.class)
  public void viewsRestrictedPeriods_throws_NPE_if_Period_is_null() {
    PeriodPredicates.viewsRestrictedPeriods().apply(null);
  }

  @Test
  public void viewsRestrictedPeriods() {
    assertThat(PeriodPredicates.viewsRestrictedPeriods().apply(createPeriod(1))).isTrue();
    assertThat(PeriodPredicates.viewsRestrictedPeriods().apply(createPeriod(2))).isTrue();
    assertThat(PeriodPredicates.viewsRestrictedPeriods().apply(createPeriod(3))).isTrue();
    assertThat(PeriodPredicates.viewsRestrictedPeriods().apply(createPeriod(4))).isFalse();
    assertThat(PeriodPredicates.viewsRestrictedPeriods().apply(createPeriod(5))).isFalse();
  }

  private Period createPeriod(int index) {
    return new Period(index, "don't care", null, 1l, 1l);
  }
}
