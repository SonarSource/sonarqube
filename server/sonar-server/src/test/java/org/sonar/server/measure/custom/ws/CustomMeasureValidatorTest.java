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
package org.sonar.server.measure.custom.ws;

import org.assertj.core.data.Offset;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.db.measure.custom.CustomMeasureTesting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.Metric.Level.WARN;
import static org.sonar.api.measures.Metric.ValueType.BOOL;
import static org.sonar.api.measures.Metric.ValueType.FLOAT;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.LEVEL;
import static org.sonar.api.measures.Metric.ValueType.STRING;
import static org.sonar.api.measures.Metric.ValueType.WORK_DUR;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.server.util.TypeValidationsTesting.newFullTypeValidations;

public class CustomMeasureValidatorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  CustomMeasureValidator underTest = new CustomMeasureValidator(newFullTypeValidations());
  CustomMeasureDto customMeasure = CustomMeasureTesting.newCustomMeasureDto();

  @Test
  public void set_boolean_true_value() {
    underTest.setMeasureValue(customMeasure, "true", newMetricDto().setValueType(BOOL.name()));

    assertThat(customMeasure.getValue()).isCloseTo(1.0d, defaultOffset());
  }

  @Test
  public void set_boolean_false_value() {
    underTest.setMeasureValue(customMeasure, "false", newMetricDto().setValueType(BOOL.name()));

    assertThat(customMeasure.getValue()).isCloseTo(0.0d, defaultOffset());
  }

  @Test
  public void set_integer_value() {
    underTest.setMeasureValue(customMeasure, "1984", newMetricDto().setValueType(INT.name()));

    assertThat(customMeasure.getValue()).isCloseTo(1984d, defaultOffset());
  }

  @Test
  public void set_float_value() {
    underTest.setMeasureValue(customMeasure, "3.14", newMetricDto().setValueType(FLOAT.name()));

    assertThat(customMeasure.getValue()).isCloseTo(3.14d, defaultOffset());
  }

  @Test
  public void set_long_value() {
    underTest.setMeasureValue(customMeasure, "123456789", newMetricDto().setValueType(WORK_DUR.name()));

    assertThat(customMeasure.getValue()).isCloseTo(123456789d, defaultOffset());
  }

  @Test
  public void set_level_value() {
    underTest.setMeasureValue(customMeasure, WARN.name(), newMetricDto().setValueType(LEVEL.name()));

    assertThat(customMeasure.getTextValue()).isEqualTo(WARN.name());
  }

  @Test
  public void set_string_value() {
    underTest.setMeasureValue(customMeasure, "free-text-string", newMetricDto().setValueType(STRING.name()));

    assertThat(customMeasure.getTextValue()).isEqualTo("free-text-string");
  }

  @Test
  public void fail_when_non_compliant_value() {
    expectedException.expect(BadRequestException.class);

    underTest.setMeasureValue(customMeasure, "non-compliant-boolean-value", newMetricDto().setValueType(BOOL.name()));
  }

  @Test
  public void fail_when_non_compliant_level_value() {
    expectedException.expect(BadRequestException.class);

    underTest.setMeasureValue(customMeasure, "non-compliant-level-value", newMetricDto().setValueType(LEVEL.name()));
  }

  private Offset<Double> defaultOffset() {
    return Offset.offset(0.01d);
  }
}
