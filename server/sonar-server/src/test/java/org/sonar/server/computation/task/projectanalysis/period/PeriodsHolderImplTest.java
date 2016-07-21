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
package org.sonar.server.computation.task.projectanalysis.period;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class PeriodsHolderImplTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private PeriodsHolderImpl underTest = new PeriodsHolderImpl();

  @Test
  public void get_periods() {
    List<Period> periods = new ArrayList<>();
    periods.add(createPeriod(1));

    underTest.setPeriods(periods);

    assertThat(underTest.getPeriods()).hasSize(1);
  }

  @Test
  public void get_periods_throws_illegal_state_exception_if_not_initialized() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Periods have not been initialized yet");

    new PeriodsHolderImpl().getPeriods();
  }

  @Test
  public void setPeriods_throws_NPE_if_arg_is_null() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Periods cannot be null");

    underTest.setPeriods(null);
  }

  @Test
  public void setPeriods_throws_NPE_if_arg_contains_null() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("No null Period can be added to the holder");

    ArrayList<Period> periods = new ArrayList<>();
    periods.add(null);
    underTest.setPeriods(periods);
  }

  @Test
  public void setPeriods_throws_NPE_if_arg_contains_null_among_others() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("No null Period can be added to the holder");

    List<Period> periods = new ArrayList<>();
    periods.add(createPeriod(1));
    periods.add(createPeriod(2));
    periods.add(null);
    periods.add(createPeriod(3));
    underTest.setPeriods(periods);
  }

  @Test
  public void setPeriods_supports_empty_arg_is_empty() {
    underTest.setPeriods(ImmutableList.<Period>of());
  }

  @Test
  public void setPeriods_throws_IAE_if_arg_has_more_than_5_elements() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("There can not be more than 5 periods");

    underTest.setPeriods(ImmutableList.of(createPeriod(1), createPeriod(2), createPeriod(3), createPeriod(4), createPeriod(5), createPeriod(5)));
  }

  @Test
  public void setPeriods_throws_ISE_if_already_initialized() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Periods have already been initialized");

    List<Period> periods = new ArrayList<>();
    periods.add(createPeriod(1));

    underTest.setPeriods(periods);
    underTest.setPeriods(periods);
  }

  @Test
  public void setPeriods_throws_IAE_if_two_periods_have_the_same_index() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("More than one period has the index 1");

    underTest.setPeriods(ImmutableList.of(createPeriod(1), createPeriod(2), createPeriod(1)));
  }

  @Test
  public void update_periods_throws_unsupported_operation_exception() {
    thrown.expect(UnsupportedOperationException.class);

    underTest.setPeriods(ImmutableList.of(createPeriod(1)));

    underTest.getPeriods().add(createPeriod(2));
  }

  @Test
  public void getPeriods_returns_Periods_sorted_by_index_no_matter_order_they_were_added() {

    underTest.setPeriods(ImmutableList.of(createPeriod(2), createPeriod(1), createPeriod(3)));

    assertThat(underTest.getPeriods()).extracting("index").containsOnly(1, 2, 3);
  }

  @Test
  public void hasPeriod_returns_false_if_holder_is_empty() {
    underTest.setPeriods(Collections.<Period>emptyList());

    for (int i = 1; i < 6; i++) {
      assertThat(underTest.hasPeriod(i)).isFalse();
    }
  }

  @Test
  public void hasPeriod_returns_true_only_if_period_exists_in_holder_with_specific_index() {
    for (int i = 1; i < 6; i++) {
      PeriodsHolderImpl periodsHolder = new PeriodsHolderImpl();
      periodsHolder.setPeriods(ImmutableList.of(createPeriod(i)));
      for (int j = 1; j < 6; j++) {
        assertThat(periodsHolder.hasPeriod(j)).isEqualTo(j == i);
      }
    }
  }

  @Test
  public void getPeriod_returns_the_period_with_the_right_index() {
    underTest.setPeriods(ImmutableList.of(createPeriod(1), createPeriod(2), createPeriod(3), createPeriod(4), createPeriod(5)));

    for (int i = 1; i < 6; i++) {
      assertThat(underTest.getPeriod(i).getIndex()).isEqualTo(i);
    }
  }

  @Test
  public void getPeriod_throws_ISE_if_period_does_not_exist_in_holder() {
    for (int i = 1; i < 6; i++) {
      PeriodsHolderImpl periodsHolder = new PeriodsHolderImpl();
      periodsHolder.setPeriods(ImmutableList.of(createPeriod(i)));

      for (int j = 1; j < 6; j++) {
        if (j == i) {
          continue;
        }

        try {
          periodsHolder.getPeriod(j);
          fail("an IllegalStateException should have been raised");
        }
        catch (IllegalStateException e) {
          assertThat(e).hasMessage("Holder has no Period for index " + j);
        }
      }
    }
  }

  private static Period createPeriod(int index) {
    return new Period(index, index + "mode", null, 1000L, "U1");
  }
}
