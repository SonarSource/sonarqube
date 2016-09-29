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

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import org.assertj.guava.api.Assertions;
import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureVariations;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodsHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.A;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.B;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.C;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.D;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.E;

public class RatingVariationValueArrayTest {

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
    assertThat(RatingVariationValue.newArray()).isNotSameAs(RatingVariationValue.newArray());
  }

  @Test
  public void get_returns_unset_value_for_each_period_index() {
    RatingVariationValue.Array array = RatingVariationValue.newArray();
    PERIODS.forEach(p -> verifyUnsetVariation(array.get(p)));
  }

  @Test
  public void get_returns_set_value_for_each_period_index_if_increment_has_been_called() {
    RatingVariationValue.Array array = RatingVariationValue.newArray();
    PERIODS.forEach(period -> {
      array.increment(period, C);
      verifySetVariation(array.get(period), C);
    });
  }

  @Test
  public void incrementAll_increments_internals_from_all_set_value_from_source() {
    RatingVariationValue.Array source = RatingVariationValue.newArray()
      .increment(createPeriod(2), B)
      .increment(createPeriod(5), C);

    RatingVariationValue.Array target = RatingVariationValue.newArray()
      .increment(createPeriod(1), D)
      .increment(createPeriod(5), E);
    target.incrementAll(source);

    verifySetVariation(target.get(createPeriod(1)), D);
    verifySetVariation(target.get(createPeriod(2)), B);
    verifyUnsetVariation(target.get(createPeriod(3)));
    verifyUnsetVariation(target.get(createPeriod(4)));
    verifySetVariation(target.get(createPeriod(5)), E);
  }

  @Test
  public void toMeasureVariations_returns_absent_if_no_value_has_been_set() {
    Assertions.assertThat(LongVariationValue.newArray().toMeasureVariations()).isAbsent();
  }

  @Test
  public void toMeasureVariations_returns_value_of_set_value_as_double() {
    Optional<MeasureVariations> variationsOptional = RatingVariationValue.newArray()
      .increment(createPeriod(2), B)
      .increment(createPeriod(5), A)
      .toMeasureVariations();

    assertThat(variationsOptional).isPresent();
    MeasureVariations variations = variationsOptional.get();
    assertThat(variations.hasVariation1()).isFalse();
    assertThat(variations.getVariation2()).isEqualTo(2d);
    assertThat(variations.hasVariation3()).isFalse();
    assertThat(variations.hasVariation4()).isFalse();
    assertThat(variations.getVariation5()).isEqualTo(1d);
  }

  private static void verifyUnsetVariation(RatingVariationValue value) {
    assertThat(value.isSet()).isFalse();
    assertThat(value.getValue()).isEqualTo(A);
  }

  private static void verifySetVariation(RatingVariationValue value, Rating expectedValue) {
    assertThat(value.isSet()).isTrue();
    assertThat(value.getValue()).isEqualTo(expectedValue);
  }

  private static Period createPeriod(int i) {
    return new Period(i, "mode " + i, null, 100L + i, String.valueOf(753L + i));
  }

}
