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

public class PeriodHolderImplTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private PeriodHolderImpl underTest = new PeriodHolderImpl();

  @Test
  public void get_period() {
    Period period = createPeriod();
    underTest.setPeriod(period);

    assertThat(underTest.getPeriod()).isEqualTo(period);
  }

  @Test
  public void get_period_throws_illegal_state_exception_if_not_initialized() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Period have not been initialized yet");

    new PeriodHolderImpl().getPeriod();
  }

  @Test
  public void setPeriod_throws_ISE_if_already_initialized() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Period have already been initialized");

    underTest.setPeriod(createPeriod());
    underTest.setPeriod(createPeriod());
  }

  @Test
  public void hasPeriod_returns_false_if_holder_is_empty() {
    underTest.setPeriod(null);
    assertThat(underTest.hasPeriod()).isFalse();
  }

  @Test
  public void hasPeriod_returns_true_only_if_period_exists_in_holder() {
    underTest.setPeriod(createPeriod());
    assertThat(underTest.hasPeriod()).isTrue();
  }

  @Test
  public void hasPeriod_throws_ISE_if_not_initialized() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Period have not been initialized yet");

    underTest.hasPeriod();
  }

  private static Period createPeriod() {
    return new Period(1 + "mode", null, 1000L, "U1");
  }
}
