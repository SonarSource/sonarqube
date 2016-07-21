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
package org.sonar.server.computation.task.projectanalysis.measure;

import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodsHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureVariations.newMeasureVariationsBuilder;

public class MeasureVariationsTest {
  public static final String NO_VARIATION_ERROR_MESSAGE = "There must be at least one variation";
  public static final String NAN_ERROR_MESSAGE = "NaN is not allowed in MeasureVariation";
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void constructor_throws_IAE_if_array_arg_has_more_than_5_elements() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("There can not be more than 5 variations");

    new MeasureVariations(1d, 2d, 3d, 4d, 5d, 6d);
  }

  @Test
  public void constructor_throws_IAE_if_no_arg() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(NO_VARIATION_ERROR_MESSAGE);

    new MeasureVariations();
  }

  @Test
  public void constructor_throws_IAE_if_single_arg_is_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(NO_VARIATION_ERROR_MESSAGE);

    new MeasureVariations((Double) null);
  }

  @Test
  public void constructor_throws_IAE_if_two_args_are_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(NO_VARIATION_ERROR_MESSAGE);

    new MeasureVariations((Double) null, (Double) null);
  }

  @Test
  public void constructor_throws_IAE_if_three_args_are_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(NO_VARIATION_ERROR_MESSAGE);

    new MeasureVariations((Double) null, (Double) null, (Double) null);
  }

  @Test
  public void constructor_throws_IAE_if_four_args_are_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(NO_VARIATION_ERROR_MESSAGE);

    new MeasureVariations((Double) null, (Double) null, (Double) null, (Double) null);
  }

  @Test
  public void constructor_throws_IAE_if_five_args_are_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(NO_VARIATION_ERROR_MESSAGE);

    new MeasureVariations((Double) null, (Double) null, (Double) null, (Double) null, (Double) null);
  }

  @Test
  public void constructor_throws_IAE_if_any_arg_1_is_NaN() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(NAN_ERROR_MESSAGE);

    new MeasureVariations(Double.NaN);
  }

  @Test
  public void constructor_throws_IAE_if_any_arg_2_is_NaN() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(NAN_ERROR_MESSAGE);

    new MeasureVariations(1d, Double.NaN);
  }

  @Test
  public void constructor_throws_IAE_if_any_arg_3_is_NaN() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(NAN_ERROR_MESSAGE);

    new MeasureVariations(1d, 2d, Double.NaN);
  }

  @Test
  public void constructor_throws_IAE_if_any_arg_4_is_NaN() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(NAN_ERROR_MESSAGE);

    new MeasureVariations(1d, 2d, 3d, Double.NaN);
  }

  @Test
  public void constructor_throws_IAE_if_any_arg_5_is_NaN() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(NAN_ERROR_MESSAGE);

    new MeasureVariations(1d, 2d, 3d, 5d, Double.NaN);
  }

  @Test
  public void verify_has_variationX_and_getVariationX() {
    verifyAsVariations(new MeasureVariations(1d), 1d, null, null, null, null);

    verifyAsVariations(new MeasureVariations(1d, 2d), 1d, 2d, null, null, null);
    verifyAsVariations(new MeasureVariations(null, 2d), null, 2d, null, null, null);
    verifyAsVariations(new MeasureVariations(1d, null), 1d, null, null, null, null);

    verifyAsVariations(new MeasureVariations(1d, 2d, 3d), 1d, 2d, 3d, null, null);
    verifyAsVariations(new MeasureVariations(null, 2d, 3d), null, 2d, 3d, null, null);
    verifyAsVariations(new MeasureVariations(1d, null, 3d), 1d, null, 3d, null, null);
    verifyAsVariations(new MeasureVariations(null, null, 3d), null, null, 3d, null, null);
    verifyAsVariations(new MeasureVariations(1d, 2d, null), 1d, 2d, null, null, null);

    verifyAsVariations(new MeasureVariations(1d, 2d, 3d, 4d), 1d, 2d, 3d, 4d, null);
    verifyAsVariations(new MeasureVariations(null, 2d, 3d, 4d), null, 2d, 3d, 4d, null);
    verifyAsVariations(new MeasureVariations(null, 2d, null, 4d), null, 2d, null, 4d, null);
    verifyAsVariations(new MeasureVariations(null, null, 3d, 4d), null, null, 3d, 4d, null);
    verifyAsVariations(new MeasureVariations(null, null, null, 4d), null, null, null, 4d, null);
    verifyAsVariations(new MeasureVariations(1d, null, 3d, 4d), 1d, null, 3d, 4d, null);
    verifyAsVariations(new MeasureVariations(1d, 2d, null, 4d), 1d, 2d, null, 4d, null);
    verifyAsVariations(new MeasureVariations(1d, 2d, 3d, null), 1d, 2d, 3d, null, null);

    verifyAsVariations(new MeasureVariations(1d, 2d, 3d, 4d, 5d), 1d, 2d, 3d, 4d, 5d);
    verifyAsVariations(new MeasureVariations(null, 2d, 3d, 4d, 5d), null, 2d, 3d, 4d, 5d);
    verifyAsVariations(new MeasureVariations(1d, null, 3d, 4d, 5d), 1d, null, 3d, 4d, 5d);
    verifyAsVariations(new MeasureVariations(1d, 2d, null, 4d, 5d), 1d, 2d, null, 4d, 5d);
    verifyAsVariations(new MeasureVariations(1d, 2d, 3d, null, 5d), 1d, 2d, 3d, null, 5d);
    verifyAsVariations(new MeasureVariations(1d, 2d, 3d, 4d, null), 1d, 2d, 3d, 4d, null);
  }

  private static void verifyAsVariations(MeasureVariations measureVariations,
    @Nullable Double variation1, @Nullable Double variation2, @Nullable Double variation3, @Nullable Double variation4, @Nullable Double variation5) {
    assertThat(measureVariations.hasVariation1()).isEqualTo(variation1 != null);
    try {
      if (variation1 == null) {
        measureVariations.getVariation1();
        fail("An exception should have been raised");
      }
      assertThat(measureVariations.getVariation1()).isEqualTo(variation1);
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Variation 1 has not been set");
    }
    assertThat(measureVariations.hasVariation2()).isEqualTo(variation2 != null);
    try {
      if (variation2 == null) {
        measureVariations.getVariation2();
        fail("An exception should have been raised");
      }
      assertThat(measureVariations.getVariation2()).isEqualTo(variation2);
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Variation 2 has not been set");
    }
    assertThat(measureVariations.hasVariation3()).isEqualTo(variation3 != null);
    try {
      if (variation3 == null) {
        measureVariations.getVariation3();
        fail("An exception should have been raised");
      }
      assertThat(measureVariations.getVariation3()).isEqualTo(variation3);
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Variation 3 has not been set");
    }
    assertThat(measureVariations.hasVariation4()).isEqualTo(variation4 != null);
    try {
      if (variation4 == null) {
        measureVariations.getVariation4();
        fail("An exception should have been raised");
      }
      assertThat(measureVariations.getVariation4()).isEqualTo(variation4);
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Variation 4 has not been set");
    }
    assertThat(measureVariations.hasVariation5()).isEqualTo(variation5 != null);
    try {
      if (variation5 == null) {
        measureVariations.getVariation5();
        fail("An exception should have been raised");
      }
      assertThat(measureVariations.getVariation5()).isEqualTo(variation5);
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Variation 5 has not been set");
    }
  }

  @Test
  public void verify_toString() {
    assertThat(new MeasureVariations(1d).toString()).isEqualTo("MeasureVariations{1=1.0, 2=null, 3=null, 4=null, 5=null}");
    assertThat(new MeasureVariations(1d, 2d, 3d, 4d, 5d).toString()).isEqualTo("MeasureVariations{1=1.0, 2=2.0, 3=3.0, 4=4.0, 5=5.0}");
  }

  @Test
  public void equals_takes_values_into_account() {
    MeasureVariations variations = new MeasureVariations(1d);

    assertThat(variations).isEqualTo(variations);
    assertThat(variations).isEqualTo(new MeasureVariations(1d));
    assertThat(new MeasureVariations(null, 1d)).isEqualTo(new MeasureVariations(null, 1d));

    assertThat(new MeasureVariations(null, 2d)).isNotEqualTo(new MeasureVariations(null, 1d));
  }

  @Test
  public void verify_MeasureVariations_built_by_builder_from_Period() {
    MeasureVariations.Builder builder = newMeasureVariationsBuilder();
    for (int i = 1; i <= PeriodsHolder.MAX_NUMBER_OF_PERIODS; i++) {
      builder.setVariation(createPeriod(i), i);
    }
    MeasureVariations variations = builder.build();

    verifyAsVariations(variations, 1d, 2d, 3d, 4d, 5d);
  }

  @Test
  public void builder_isEmpty_returns_true_if_builder_has_not_been_used() {
    assertThat(newMeasureVariationsBuilder().isEmpty()).isTrue();
  }

  @Test
  public void builder_isEmpty_returns_false_if_any_variation_has_been_set() {
    for (int i = 1; i <= PeriodsHolder.MAX_NUMBER_OF_PERIODS; i++) {
      assertThat(newMeasureVariationsBuilder().setVariation(createPeriod(i), i).isEmpty()).isFalse();
    }
  }

  @Test
  public void builder_setVariation_for_Period_throws_IAE_if_value_is_NaN() {
    for (int i = 1; i <= PeriodsHolder.MAX_NUMBER_OF_PERIODS; i++) {
      try {
        newMeasureVariationsBuilder().setVariation(createPeriod(i), Double.NaN);
        fail("An IAE should have been raised");
      } catch (IllegalArgumentException e) {
        assertThat(e).hasMessage(NAN_ERROR_MESSAGE);
      }
    }
  }

  private static Period createPeriod(int i) {
    return new Period(i, "mode " + i, null, 100L + i, String.valueOf(952L + i));
  }
}
