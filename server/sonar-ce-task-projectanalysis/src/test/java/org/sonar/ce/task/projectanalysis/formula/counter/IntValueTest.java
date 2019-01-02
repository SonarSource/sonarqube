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
package org.sonar.ce.task.projectanalysis.formula.counter;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IntValueTest {
  @Test
  public void newly_created_IntVariationValue_is_unset_and_has_value_0() {
    verifyUnsetVariationValue(new IntValue());
  }

  @Test
  public void increment_int_sets_IntVariationValue_and_increments_value() {
    verifySetVariationValue(new IntValue().increment(10), 10);
  }

  @Test
  public void increment_IntVariationValue_has_no_effect_if_arg_is_null() {
    verifyUnsetVariationValue(new IntValue().increment(null));
  }

  @Test
  public void increment_IntVariationValue_has_no_effect_if_arg_is_unset() {
    verifyUnsetVariationValue(new IntValue().increment(new IntValue()));
  }

  @Test
  public void increment_IntVariationValue_increments_by_the_value_of_the_arg() {
    IntValue source = new IntValue().increment(10);
    IntValue target = new IntValue().increment(source);

    verifySetVariationValue(target, 10);
  }

  @Test
  public void multiple_calls_to_increment_IntVariationValue_increments_by_the_value_of_the_arg() {
    IntValue target = new IntValue()
      .increment(new IntValue().increment(35))
      .increment(new IntValue().increment(10));

    verifySetVariationValue(target, 45);
  }

  @Test
  public void multiples_calls_to_increment_int_increment_the_value() {
    IntValue variationValue = new IntValue()
      .increment(10)
      .increment(95);

    verifySetVariationValue(variationValue, 105);
  }

  private static void verifyUnsetVariationValue(IntValue variationValue) {
    assertThat(variationValue.isSet()).isFalse();
    assertThat(variationValue.getValue()).isEqualTo(0);
  }

  private static void verifySetVariationValue(IntValue variationValue, int expected) {
    assertThat(variationValue.isSet()).isTrue();
    assertThat(variationValue.getValue()).isEqualTo(expected);
  }
}
