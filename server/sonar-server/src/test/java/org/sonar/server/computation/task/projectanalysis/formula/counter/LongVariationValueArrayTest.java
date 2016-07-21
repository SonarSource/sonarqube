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
package org.sonar.server.computation.task.projectanalysis.formula.counter;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureVariations;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodsHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;

public class LongVariationValueArrayTest {
  private static final List<Period> PERIODS;

  static {
    ImmutableList.Builder<Period> builder = ImmutableList.builder();
    for (int i = 1; i <= PeriodsHolder.MAX_NUMBER_OF_PERIODS; i++) {
      builder.add(createPeriod(i));
    }
    PERIODS = builder.build();
  }

  @Test
  public void newArray_always_returns_a_new_instance() {
    assertThat(LongVariationValue.newArray()).isNotSameAs(LongVariationValue.newArray());
  }

  @Test
  public void get_returns_unset_LongVariationValue_for_each_Period_index() {
    LongVariationValue.Array array = LongVariationValue.newArray();
    for (Period period : PERIODS) {
      LongVariationValue value = array.get(period);
      verifyUnsetLongVariation(value);
    }
  }

  @Test
  public void get_returns_set_LongVariationValue_for_each_Period_index_if_increment_has_been_called() {
    LongVariationValue.Array array = LongVariationValue.newArray();
    for (Period period : PERIODS) {
      array.increment(period, 66L);
      LongVariationValue value = array.get(period);
      verifySetLongVariation(value, 66L);
    }
  }

  @Test
  public void incrementAll_increments_internals_from_all_set_LongVariationValue_from_source() {
    LongVariationValue.Array source = LongVariationValue.newArray()
      .increment(createPeriod(2), 20l)
      .increment(createPeriod(5), 5l);

    LongVariationValue.Array target = LongVariationValue.newArray()
      .increment(createPeriod(1), 1L)
      .increment(createPeriod(5), 30L);
    target.incrementAll(source);

    verifySetLongVariation(target.get(createPeriod(1)), 1L);
    verifySetLongVariation(target.get(createPeriod(2)), 20L);
    verifyUnsetLongVariation(target.get(createPeriod(3)));
    verifyUnsetLongVariation(target.get(createPeriod(4)));
    verifySetLongVariation(target.get(createPeriod(5)), 35L);
  }

  @Test
  public void toMeasureVariations_returns_absent_if_no_LongVariationValue_has_been_set() {
    assertThat(LongVariationValue.newArray().toMeasureVariations()).isAbsent();
  }

  @Test
  public void toMeasureVariations_returns_value_of_set_LongVariationValue_as_double() {
    Optional<MeasureVariations> variationsOptional = LongVariationValue.newArray()
      .increment(createPeriod(2), 2L)
      .increment(createPeriod(5), 15L)
      .toMeasureVariations();

    assertThat(variationsOptional).isPresent();
    MeasureVariations variations = variationsOptional.get();
    assertThat(variations.hasVariation1()).isFalse();
    assertThat(variations.getVariation2()).isEqualTo(2d);
    assertThat(variations.hasVariation3()).isFalse();
    assertThat(variations.hasVariation4()).isFalse();
    assertThat(variations.getVariation5()).isEqualTo(15d);
  }

  private static void verifyUnsetLongVariation(LongVariationValue value) {
    assertThat(value.isSet()).isFalse();
    assertThat(value.getValue()).isEqualTo(0L);
  }

  private static void verifySetLongVariation(LongVariationValue value, long expectedValue) {
    assertThat(value.isSet()).isTrue();
    assertThat(value.getValue()).isEqualTo(expectedValue);
  }

  private static Period createPeriod(int i) {
    return new Period(i, "mode " + i, null, 100L + i, String.valueOf(753L + i));
  }
}
