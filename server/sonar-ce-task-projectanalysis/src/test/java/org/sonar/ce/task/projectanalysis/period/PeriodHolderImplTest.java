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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PeriodHolderImplTest {


  private PeriodHolderImpl underTest = new PeriodHolderImpl();

  @Test
  public void get_period() {
    Period period = createPeriod();
    underTest.setPeriod(period);

    assertThat(underTest.getPeriod()).isEqualTo(period);
  }

  @Test
  public void get_period_throws_illegal_state_exception_if_not_initialized() {
    assertThatThrownBy(() ->  new PeriodHolderImpl().getPeriod())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Period have not been initialized yet");
  }

  @Test
  public void setPeriod_throws_ISE_if_already_initialized() {
    assertThatThrownBy(() -> {
      underTest.setPeriod(createPeriod());
      underTest.setPeriod(createPeriod());
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Period have already been initialized");
  }

  @Test
  public void hasPeriod_returns_false_if_holder_is_empty() {
    underTest.setPeriod(null);
    assertThat(underTest.hasPeriod()).isFalse();
  }

  @Test
  public void hasPeriodDate_returns_false_if_date_is_null() {
    underTest.setPeriod(createPeriodWithoutDate());
    assertThat(underTest.hasPeriod()).isTrue();
    assertThat(underTest.hasPeriodDate()).isFalse();
  }

  @Test
  public void hasPeriod_returns_true_only_if_period_exists_in_holder() {
    underTest.setPeriod(createPeriod());
    assertThat(underTest.hasPeriod()).isTrue();
    assertThat(underTest.hasPeriodDate()).isTrue();
  }

  @Test
  public void hasPeriod_throws_ISE_if_not_initialized() {
    assertThatThrownBy(() -> underTest.hasPeriod())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Period have not been initialized yet");
  }

  private static Period createPeriod() {
    return new Period(1 + "mode", null, 1000L);
  }

  private static Period createPeriodWithoutDate() {
    return new Period(1 + "mode", null, null);
  }
}
