/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.formula.counter;

import org.junit.Test;
import org.sonar.server.measure.Rating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.measure.Rating.A;
import static org.sonar.server.measure.Rating.B;
import static org.sonar.server.measure.Rating.C;
import static org.sonar.server.measure.Rating.D;

public class RatingValueTest {

  @Test
  public void newly_created_value_is_unset_and_has_value_0() {
    verifyUnsetValue(new RatingValue());
  }

  @Test
  public void increment_sets_value_and_increments_value() {
    verifySetValue(new RatingValue().increment(B), B);
  }

  @Test
  public void increment_has_no_effect_if_arg_is_null() {
    verifyUnsetValue(new RatingValue().increment((RatingValue) null));
  }

  @Test
  public void increment_has_no_effect_if_arg_is_unset() {
    verifyUnsetValue(new RatingValue().increment(new RatingValue()));
  }

  @Test
  public void increment_increments_by_the_value_of_the_arg() {
    RatingValue source = new RatingValue().increment(B);
    RatingValue target = new RatingValue().increment(source);

    verifySetValue(target, B);
  }

  @Test
  public void multiple_calls_to_increment_increments_by_the_value_of_the_arg() {
    RatingValue target = new RatingValue()
      .increment(new RatingValue().increment(B))
      .increment(new RatingValue().increment(D));

    verifySetValue(target, D);
  }

  @Test
  public void multiples_calls_to_increment_increments_the_value() {
    RatingValue variationValue = new RatingValue()
      .increment(B)
      .increment(C);

    verifySetValue(variationValue, C);
  }

  private static void verifyUnsetValue(RatingValue ratingValue) {
    assertThat(ratingValue.isSet()).isFalse();
    assertThat(ratingValue.getValue()).isEqualTo(A);
  }

  private static void verifySetValue(RatingValue ratingValue, Rating expected) {
    assertThat(ratingValue.isSet()).isTrue();
    assertThat(ratingValue.getValue()).isEqualTo(expected);
  }

}
