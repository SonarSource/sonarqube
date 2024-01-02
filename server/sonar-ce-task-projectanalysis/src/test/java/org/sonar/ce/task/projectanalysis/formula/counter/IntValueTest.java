/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
  public void newly_created_IntValue_is_unset_and_has_value_0() {
    verifyUnsetValue(new IntValue());
  }

  @Test
  public void increment_int_sets_IntValue_and_increments_value() {
    verifySetValue(new IntValue().increment(10), 10);
  }

  @Test
  public void increment_IntValue_has_no_effect_if_arg_is_null() {
    verifyUnsetValue(new IntValue().increment(null));
  }

  @Test
  public void increment_IntValue_has_no_effect_if_arg_is_unset() {
    verifyUnsetValue(new IntValue().increment(new IntValue()));
  }

  @Test
  public void increment_IntValue_increments_by_the_value_of_the_arg() {
    IntValue source = new IntValue().increment(10);
    IntValue target = new IntValue().increment(source);

    verifySetValue(target, 10);
  }

  @Test
  public void multiple_calls_to_increment_IntValue_increments_by_the_value_of_the_arg() {
    IntValue target = new IntValue()
      .increment(new IntValue().increment(35))
      .increment(new IntValue().increment(10));

    verifySetValue(target, 45);
  }

  @Test
  public void multiples_calls_to_increment_int_increment_the_value() {
    IntValue value = new IntValue()
      .increment(10)
      .increment(95);

    verifySetValue(value, 105);
  }

  private static void verifyUnsetValue(IntValue value) {
    assertThat(value.isSet()).isFalse();
    assertThat(value.getValue()).isZero();
  }

  private static void verifySetValue(IntValue value, int expected) {
    assertThat(value.isSet()).isTrue();
    assertThat(value.getValue()).isEqualTo(expected);
  }
}
