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

import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.A;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.B;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.C;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.D;

public class RatingVariationValueTest {

  @Test
  public void newly_created_value_is_unset_and_has_value_0() {
    verifyUnsetVariationValue(new RatingVariationValue());
  }

  @Test
  public void increment_sets_value_and_increments_value() {
    verifySetVariationValue(new RatingVariationValue().increment(B), B);
  }

  @Test
  public void increment_has_no_effect_if_arg_is_null() {
    verifyUnsetVariationValue(new RatingVariationValue().increment((RatingVariationValue) null));
  }

  @Test
  public void increment_has_no_effect_if_arg_is_unset() {
    verifyUnsetVariationValue(new RatingVariationValue().increment(new RatingVariationValue()));
  }

  @Test
  public void increment_increments_by_the_value_of_the_arg() {
    RatingVariationValue source = new RatingVariationValue().increment(B);
    RatingVariationValue target = new RatingVariationValue().increment(source);

    verifySetVariationValue(target, B);
  }

  @Test
  public void multiple_calls_to_increment_increments_by_the_value_of_the_arg() {
    RatingVariationValue target = new RatingVariationValue()
      .increment(new RatingVariationValue().increment(B))
      .increment(new RatingVariationValue().increment(D));

    verifySetVariationValue(target, D);
  }

  @Test
  public void multiples_calls_to_increment_increments_the_value() {
    RatingVariationValue variationValue = new RatingVariationValue()
      .increment(B)
      .increment(C);

    verifySetVariationValue(variationValue, C);
  }

  private static void verifyUnsetVariationValue(RatingVariationValue variationValue) {
    assertThat(variationValue.isSet()).isFalse();
    assertThat(variationValue.getValue()).isEqualTo(A);
  }

  private static void verifySetVariationValue(RatingVariationValue variationValue, Rating expected) {
    assertThat(variationValue.isSet()).isTrue();
    assertThat(variationValue.getValue()).isEqualTo(expected);
  }

}
