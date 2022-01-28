/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.api.ce.measure.test;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestMeasureTest {

  @Test
  public void create_double_measure() {
    assertThat(TestMeasure.createMeasure(10d).getDoubleValue()).isEqualTo(10d);
  }

  @Test
  public void create_int_measure() {
    assertThat(TestMeasure.createMeasure(10).getIntValue()).isEqualTo(10);
  }

  @Test
  public void create_long_measure() {
    assertThat(TestMeasure.createMeasure(10L).getLongValue()).isEqualTo(10L);
  }

  @Test
  public void create_string_measure() {
    assertThat(TestMeasure.createMeasure("value").getStringValue()).isEqualTo("value");
  }

  @Test
  public void create_boolean_measure() {
    assertThat(TestMeasure.createMeasure(true).getBooleanValue()).isTrue();
  }

  @Test
  public void getDoubleValue_fails_with_ISE_when_not_a_double() {
    assertThatThrownBy(() -> TestMeasure.createMeasure(10).getDoubleValue())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Not a double measure");
  }

  @Test
  public void getIntValue_fails_with_ISE_when_not_an_int() {
    assertThatThrownBy(() -> TestMeasure.createMeasure(10L).getIntValue())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Not an integer measure");
  }

  @Test
  public void getLongValue_fails_with_ISE_when_not_a_long() {
    assertThatThrownBy(() -> TestMeasure.createMeasure(10).getLongValue())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Not a long measure");
  }

  @Test
  public void getStringValue_fails_with_ISE_when_not_a_string() {
    assertThatThrownBy(() -> TestMeasure.createMeasure(10).getStringValue())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Not a string measure");
  }

  @Test
  public void getBooleanValue_fails_with_ISE_when_not_a_boolean() {
    assertThatThrownBy(() -> TestMeasure.createMeasure(10).getBooleanValue())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Not a boolean measure");
  }
}
