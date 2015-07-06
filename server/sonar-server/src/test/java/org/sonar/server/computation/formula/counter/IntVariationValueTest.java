/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.computation.formula.counter;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IntVariationValueTest {
  @Test
  public void newly_created_IntVariationValue_is_unset_and_has_value_0() {
    verifyUnsetVariationValue(new IntVariationValue());
  }

  @Test
  public void increment_int_sets_IntVariationValue_and_increments_value() {
    verifySetVariationValue(new IntVariationValue().increment(10), 10);
  }

  @Test
  public void increment_IntVariationValue_has_no_effect_if_arg_is_null() {
    verifyUnsetVariationValue(new IntVariationValue().increment(null));
  }

  @Test
  public void increment_IntVariationValue_has_no_effect_if_arg_is_unset() {
    verifyUnsetVariationValue(new IntVariationValue().increment(new IntVariationValue()));
  }

  @Test
  public void increment_IntVariationValue_increments_by_the_value_of_the_arg() {
    IntVariationValue source = new IntVariationValue().increment(10);
    IntVariationValue target = new IntVariationValue().increment(source);

    verifySetVariationValue(target, 10);
  }

  @Test
  public void multiple_calls_to_increment_IntVariationValue_increments_by_the_value_of_the_arg() {
    IntVariationValue target = new IntVariationValue()
      .increment(new IntVariationValue().increment(35))
      .increment(new IntVariationValue().increment(10));

    verifySetVariationValue(target, 45);
  }

  @Test
  public void multiples_calls_to_increment_int_increment_the_value() {
    IntVariationValue variationValue = new IntVariationValue()
      .increment(10)
      .increment(95);

    verifySetVariationValue(variationValue, 105);
  }

  private static void verifyUnsetVariationValue(IntVariationValue variationValue) {
    assertThat(variationValue.isSet()).isFalse();
    assertThat(variationValue.getValue()).isEqualTo(0);
  }

  private static void verifySetVariationValue(IntVariationValue variationValue, int expected) {
    assertThat(variationValue.isSet()).isTrue();
    assertThat(variationValue.getValue()).isEqualTo(expected);
  }
}
