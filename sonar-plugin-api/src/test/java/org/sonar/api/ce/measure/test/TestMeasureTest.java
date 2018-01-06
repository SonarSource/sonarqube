/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class TestMeasureTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void create_double_measure() throws Exception {
    assertThat(TestMeasure.createMeasure(10d).getDoubleValue()).isEqualTo(10d);
  }

  @Test
  public void create_int_measure() throws Exception {
    assertThat(TestMeasure.createMeasure(10).getIntValue()).isEqualTo(10);
  }

  @Test
  public void create_long_measure() throws Exception {
    assertThat(TestMeasure.createMeasure(10L).getLongValue()).isEqualTo(10L);
  }

  @Test
  public void create_string_measure() throws Exception {
    assertThat(TestMeasure.createMeasure("value").getStringValue()).isEqualTo("value");
  }

  @Test
  public void create_boolean_measure() throws Exception {
    assertThat(TestMeasure.createMeasure(true).getBooleanValue()).isTrue();
  }

  @Test
  public void getDoubleValue_fails_with_ISE_when_not_a_double() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Not a double measure");

    TestMeasure.createMeasure(10).getDoubleValue();
  }

  @Test
  public void getIntValue_fails_with_ISE_when_not_an_int() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Not an integer measure");

    TestMeasure.createMeasure(10L).getIntValue();
  }

  @Test
  public void getLongValue_fails_with_ISE_when_not_a_long() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Not a long measure");

    TestMeasure.createMeasure(10).getLongValue();
  }

  @Test
  public void getStringValue_fails_with_ISE_when_not_a_string() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Not a string measure");

    TestMeasure.createMeasure(10).getStringValue();
  }

  @Test
  public void getBooleanValue_fails_with_ISE_when_not_a_boolean() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Not a boolean measure");

    TestMeasure.createMeasure(10).getBooleanValue();
  }
}
