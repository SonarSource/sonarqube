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

public class LongVariationValueTest {
  @Test
  public void newly_created_LongVariationValue_is_unset_and_has_value_0() {
    verifyUnsetVariationValue(new LongVariationValue());
  }

  @Test
  public void increment_long_sets_LongVariationValue_and_increments_value() {
    verifySetVariationValue(new LongVariationValue().increment(10L), 10L);
  }

  @Test
  public void increment_LongVariationValue_has_no_effect_if_arg_is_null() {
    verifyUnsetVariationValue(new LongVariationValue().increment(null));
  }

  @Test
  public void increment_LongVariationValue_has_no_effect_if_arg_is_unset() {
    verifyUnsetVariationValue(new LongVariationValue().increment(new LongVariationValue()));
  }

  @Test
  public void increment_LongVariationValue_increments_by_the_value_of_the_arg() {
    LongVariationValue source = new LongVariationValue().increment(10L);
    LongVariationValue target = new LongVariationValue().increment(source);

    verifySetVariationValue(target, 10L);
  }

  @Test
  public void multiple_calls_to_increment_LongVariationValue_increments_by_the_value_of_the_arg() {
    LongVariationValue target = new LongVariationValue()
      .increment(new LongVariationValue().increment(35L))
      .increment(new LongVariationValue().increment(10L));

    verifySetVariationValue(target, 45L);
  }

  @Test
  public void multiples_calls_to_increment_long_increment_the_value() {
    LongVariationValue variationValue = new LongVariationValue()
      .increment(10L)
      .increment(95L);

    verifySetVariationValue(variationValue, 105L);
  }

  private static void verifyUnsetVariationValue(LongVariationValue variationValue) {
    assertThat(variationValue.isSet()).isFalse();
    assertThat(variationValue.getValue()).isEqualTo(0L);
  }

  private static void verifySetVariationValue(LongVariationValue variationValue, long expected) {
    assertThat(variationValue.isSet()).isTrue();
    assertThat(variationValue.getValue()).isEqualTo(expected);
  }
}
