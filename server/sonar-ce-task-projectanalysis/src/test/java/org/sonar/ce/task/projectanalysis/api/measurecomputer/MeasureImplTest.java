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
package org.sonar.ce.task.projectanalysis.api.measurecomputer;

import org.junit.Test;
import org.sonar.ce.task.projectanalysis.measure.Measure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MeasureImplTest {


  @Test
  public void get_int_value() {
    MeasureImpl measure = new MeasureImpl(Measure.newMeasureBuilder().create(1));
    assertThat(measure.getIntValue()).isOne();
  }

  @Test
  public void fail_with_ISE_when_not_int_value() {
    assertThatThrownBy(() -> {
      MeasureImpl measure = new MeasureImpl(Measure.newMeasureBuilder().create(1d, 1));
      measure.getIntValue();
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Value can not be converted to int because current value type is a DOUBLE");
  }

  @Test
  public void get_double_value() {
    MeasureImpl measure = new MeasureImpl(Measure.newMeasureBuilder().create(1d, 1));
    assertThat(measure.getDoubleValue()).isEqualTo(1d);
  }

  @Test
  public void fail_with_ISE_when_not_double_value() {
    assertThatThrownBy(() -> {
      MeasureImpl measure = new MeasureImpl(Measure.newMeasureBuilder().create(1));
      measure.getDoubleValue();
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Value can not be converted to double because current value type is a INT");
  }

  @Test
  public void get_long_value() {
    MeasureImpl measure = new MeasureImpl(Measure.newMeasureBuilder().create(1L));
    assertThat(measure.getLongValue()).isOne();
  }

  @Test
  public void fail_with_ISE_when_not_long_value() {
    assertThatThrownBy(() -> {
      MeasureImpl measure = new MeasureImpl(Measure.newMeasureBuilder().create("value"));
      measure.getLongValue();
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Value can not be converted to long because current value type is a STRING");
  }

  @Test
  public void get_string_value() {
    MeasureImpl measure = new MeasureImpl(Measure.newMeasureBuilder().create("value"));
    assertThat(measure.getStringValue()).isEqualTo("value");
  }

  @Test
  public void fail_with_ISE_when_not_string_value() {
    assertThatThrownBy(() -> {
      MeasureImpl measure = new MeasureImpl(Measure.newMeasureBuilder().create(1L));
      measure.getStringValue();
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Value can not be converted to string because current value type is a LONG");
  }

  @Test
  public void get_boolean_value() {
    MeasureImpl measure = new MeasureImpl(Measure.newMeasureBuilder().create(true));
    assertThat(measure.getBooleanValue()).isTrue();
  }

  @Test
  public void fail_with_ISE_when_not_boolean_value() {
    assertThatThrownBy(() -> {
      MeasureImpl measure = new MeasureImpl(Measure.newMeasureBuilder().create(1d, 1));
      measure.getBooleanValue();
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Value can not be converted to boolean because current value type is a DOUBLE");
  }

  @Test
  public void fail_with_ISE_when_creating_measure_with_no_value() {
    assertThatThrownBy(() -> new MeasureImpl(Measure.newMeasureBuilder().createNoValue()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Only following types are allowed [BOOLEAN, INT, LONG, DOUBLE, STRING]");
  }

  @Test
  public void fail_with_ISE_when_creating_measure_with_not_allowed_value() {
    assertThatThrownBy(() -> new MeasureImpl(Measure.newMeasureBuilder().create(Measure.Level.ERROR)))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Only following types are allowed [BOOLEAN, INT, LONG, DOUBLE, STRING]");
  }

  @Test
  public void fail_with_NPE_when_creating_measure_with_null_measure() {
    assertThatThrownBy(() -> new MeasureImpl(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Measure couldn't be null");
  }
}
