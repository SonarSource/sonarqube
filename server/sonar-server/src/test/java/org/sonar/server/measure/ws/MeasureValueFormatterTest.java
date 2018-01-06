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
package org.sonar.server.measure.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.Metric;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.Metric.ValueType.BOOL;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.FLOAT;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.MILLISEC;
import static org.sonar.api.measures.Metric.ValueType.PERCENT;
import static org.sonar.api.measures.Metric.ValueType.WORK_DUR;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.server.measure.ws.MeasureValueFormatter.formatMeasureValue;
import static org.sonar.server.measure.ws.MeasureValueFormatter.formatNumericalValue;

public class MeasureValueFormatterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_formatNumericalValue() {
    assertThat(formatNumericalValue(-1.0d, newMetric(BOOL))).isEqualTo("false");
    assertThat(formatNumericalValue(1.0d, newMetric(BOOL))).isEqualTo("true");
    assertThat(formatNumericalValue(1_000.123d, newMetric(FLOAT))).isEqualTo("1000.123");
    assertThat(formatNumericalValue(1_000.0d, newMetric(INT))).isEqualTo("1000");
    assertThat(formatNumericalValue(1_000.0d, newMetric(WORK_DUR))).isEqualTo("1000");
    assertThat(formatNumericalValue(6_000_000_000_000.0d, newMetric(MILLISEC))).isEqualTo("6000000000000");
  }

  @Test
  public void test_formatMeasureValue() {
    assertThat(formatMeasureValue(newNumericMeasure(-1.0d), newMetric(BOOL))).isEqualTo("false");
    assertThat(formatMeasureValue(newNumericMeasure(1.0d), newMetric(BOOL))).isEqualTo("true");
    assertThat(formatMeasureValue(newNumericMeasure(1000.123d), newMetric(PERCENT))).isEqualTo("1000.123");
    assertThat(formatMeasureValue(newNumericMeasure(1000.0d), newMetric(WORK_DUR))).isEqualTo("1000");
    assertThat(formatMeasureValue(newNumericMeasure(6_000_000_000_000.0d), newMetric(MILLISEC))).isEqualTo("6000000000000");
    assertThat(formatMeasureValue(newTextMeasure("text-value"), newMetric(DATA))).isEqualTo("text-value");
  }

  @Test
  public void fail_if_text_value_type_for_numeric_formatter() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unsupported metric type 'DATA' for numerical value");

    formatNumericalValue(42.0d, newMetric(DATA));
  }

  private static MetricDto newMetric(Metric.ValueType valueType) {
    return newMetricDto().setValueType(valueType.name());
  }

  private static MeasureDto newNumericMeasure(Double value) {
    return new MeasureDto().setValue(value);
  }

  private static MeasureDto newTextMeasure(String data) {
    return new MeasureDto().setData(data);
  }
}
