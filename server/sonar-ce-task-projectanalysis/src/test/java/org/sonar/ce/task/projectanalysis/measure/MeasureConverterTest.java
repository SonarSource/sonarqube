/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.measure;

import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.ce.task.projectanalysis.measure.Measure.Level;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class MeasureConverterTest {

  private static final Metric SOME_INT_METRIC = new MetricImpl("42", "int", "name", Metric.MetricType.INT);
  private static final Metric SOME_LONG_METRIC = new MetricImpl("42", "long", "name", Metric.MetricType.WORK_DUR);
  private static final Metric SOME_DOUBLE_METRIC = new MetricImpl("42", "double", "name", Metric.MetricType.FLOAT);
  private static final Metric SOME_STRING_METRIC = new MetricImpl("42", "string", "name", Metric.MetricType.STRING);
  private static final Metric SOME_BOOLEAN_METRIC = new MetricImpl("42", "boolean", "name", Metric.MetricType.BOOL);
  private static final Metric SOME_LEVEL_METRIC = new MetricImpl("42", "level", "name", Metric.MetricType.LEVEL);
  private static final Metric SOME_NO_VALUE_METRIC = new MetricImpl("42", "novalue", "name", Metric.MetricType.DATA);

  private static final String SOME_DATA = "some_data";
  private static final String SOME_ALERT_TEXT = "some alert text";

  @Test
  void toMeasure_throws_NPE_if_adapter_is_null() {
    assertThatThrownBy(() -> MeasureConverter.toMeasure(null, SOME_INT_METRIC))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  void toMeasure_throws_NPE_if_metric_is_null() {
    MeasureConverter.MeasureDataAdapter adapter = createAdapter(42, null, null, false);
    assertThatThrownBy(() -> MeasureConverter.toMeasure(adapter, null))
      .isInstanceOf(NullPointerException.class);
  }

  static Stream<Arguments> numericMetricsWithValues() {
    return Stream.of(
      arguments(SOME_INT_METRIC, 42, ValueType.INT),
      arguments(SOME_LONG_METRIC, 123456789L, ValueType.LONG),
      arguments(SOME_DOUBLE_METRIC, 3.14d, ValueType.DOUBLE));
  }

  @ParameterizedTest
  @MethodSource("numericMetricsWithValues")
  void toMeasure_returns_measure_for_numeric_metrics(Metric metric, Object value, ValueType expectedType) {
    TestAdapter adapter = createAdapter(value, SOME_DATA, null, false);

    Optional<Measure> measure = MeasureConverter.toMeasure(adapter, metric);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(expectedType);
    assertThat(measure.get().getData()).isEqualTo(SOME_DATA);
  }

  static Stream<Metric> allMetricTypes() {
    return Stream.of(
      SOME_INT_METRIC,
      SOME_LONG_METRIC,
      SOME_DOUBLE_METRIC,
      SOME_BOOLEAN_METRIC,
      SOME_STRING_METRIC,
      SOME_LEVEL_METRIC);
  }

  @ParameterizedTest
  @MethodSource("allMetricTypes")
  void toMeasure_returns_empty_when_value_is_null_and_useNoValueForMissing_is_false(Metric metric) {
    TestAdapter adapter = createAdapter(null, null, null, false);

    Optional<Measure> measure = MeasureConverter.toMeasure(adapter, metric);

    assertThat(measure).isNotPresent();
  }

  @ParameterizedTest
  @MethodSource("allMetricTypes")
  void toMeasure_returns_no_value_when_value_is_null_and_useNoValueForMissing_is_true(Metric metric) {
    TestAdapter adapter = createAdapter(null, null, null, true);

    Optional<Measure> measure = MeasureConverter.toMeasure(adapter, metric);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.NO_VALUE);
  }

  @Test
  void toMeasure_returns_boolean_measure_for_BOOLEAN_metric_with_true_value() {
    TestAdapter adapter = createAdapter(1.0d, SOME_DATA, null, false);

    Optional<Measure> measure = MeasureConverter.toMeasure(adapter, SOME_BOOLEAN_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.BOOLEAN);
    assertThat(measure.get().getBooleanValue()).isTrue();
    assertThat(measure.get().getData()).isEqualTo(SOME_DATA);
  }

  @Test
  void toMeasure_returns_boolean_measure_for_BOOLEAN_metric_with_false_value() {
    TestAdapter adapter = createAdapter(0.0d, SOME_DATA, null, false);

    Optional<Measure> measure = MeasureConverter.toMeasure(adapter, SOME_BOOLEAN_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.BOOLEAN);
    assertThat(measure.get().getBooleanValue()).isFalse();
  }

  @Test
  void toMeasure_returns_string_measure_for_STRING_metric() {
    TestAdapter adapter = createAdapter("test_value", null, null, false);

    Optional<Measure> measure = MeasureConverter.toMeasure(adapter, SOME_STRING_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.STRING);
    assertThat(measure.get().getStringValue()).isEqualTo("test_value");
  }

  @Test
  void toMeasure_returns_level_measure_for_LEVEL_metric() {
    TestAdapter adapter = createAdapter("OK", null, null, false);

    Optional<Measure> measure = MeasureConverter.toMeasure(adapter, SOME_LEVEL_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.LEVEL);
    assertThat(measure.get().getLevelValue()).isEqualTo(Level.OK);
  }

  @Test
  void toMeasure_returns_empty_for_LEVEL_metric_with_invalid_value_and_useNoValueForMissing_is_false() {
    TestAdapter adapter = createAdapter("INVALID", null, null, false);

    Optional<Measure> measure = MeasureConverter.toMeasure(adapter, SOME_LEVEL_METRIC);

    assertThat(measure).isNotPresent();
  }

  @Test
  void toMeasure_returns_no_value_for_LEVEL_metric_with_invalid_value_and_useNoValueForMissing_is_true() {
    TestAdapter adapter = createAdapter("INVALID", null, null, true);

    Optional<Measure> measure = MeasureConverter.toMeasure(adapter, SOME_LEVEL_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.NO_VALUE);
  }

  @Test
  void toMeasure_returns_no_value_measure_for_NO_VALUE_metric() {
    TestAdapter adapter = createAdapter(null, SOME_DATA, null, true);

    Optional<Measure> measure = MeasureConverter.toMeasure(adapter, SOME_NO_VALUE_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.NO_VALUE);
  }

  @Test
  void toMeasure_includes_quality_gate_status_when_present() {
    QualityGateStatus qgStatus = new QualityGateStatus(Level.ERROR, SOME_ALERT_TEXT);
    TestAdapter adapter = createAdapter(42, SOME_DATA, qgStatus, false);

    Optional<Measure> measure = MeasureConverter.toMeasure(adapter, SOME_INT_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().hasQualityGateStatus()).isTrue();
    assertThat(measure.get().getQualityGateStatus().getStatus()).isEqualTo(Level.ERROR);
    assertThat(measure.get().getQualityGateStatus().getText()).isEqualTo(SOME_ALERT_TEXT);
  }

  @Test
  void toMeasure_does_not_include_quality_gate_status_when_null() {
    TestAdapter adapter = createAdapter(42, SOME_DATA, null, false);

    Optional<Measure> measure = MeasureConverter.toMeasure(adapter, SOME_INT_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().hasQualityGateStatus()).isFalse();
  }

  private static TestAdapter createAdapter(@Nullable Object value, @Nullable String data,
    @Nullable QualityGateStatus qgStatus, boolean useNoValueForMissing) {
    return new TestAdapter(value, data, qgStatus, useNoValueForMissing);
  }

  /**
   * Simple test adapter for testing MeasureConverter in isolation
   */
  private static class TestAdapter implements MeasureConverter.MeasureDataAdapter {
    private final Object value;
    private final String data;
    private final QualityGateStatus qgStatus;
    private final boolean useNoValueForMissing;

    TestAdapter(@Nullable Object value, @Nullable String data, @Nullable QualityGateStatus qgStatus, boolean useNoValueForMissing) {
      this.value = value;
      this.data = data;
      this.qgStatus = qgStatus;
      this.useNoValueForMissing = useNoValueForMissing;
    }

    @Override
    public Integer getIntValue() {
      return value instanceof Integer ? (Integer) value : null;
    }

    @Override
    public Long getLongValue() {
      return value instanceof Long ? (Long) value : null;
    }

    @Override
    public Double getDoubleValue() {
      return value instanceof Double ? (Double) value : null;
    }

    @Override
    public String getStringValue() {
      return value instanceof String ? (String) value : null;
    }

    @Override
    public String getData() {
      return data;
    }

    @Override
    public QualityGateStatus getQualityGateStatus() {
      return qgStatus;
    }

    @Override
    public boolean useNoValueForMissing() {
      return useNoValueForMissing;
    }
  }

}
