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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.ce.task.projectanalysis.measure.Measure.Level;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricImpl;
import org.sonar.db.measure.MeasureDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.sonar.ce.task.projectanalysis.measure.MeasureDtoToMeasure.toMeasure;

class MeasureDtoToMeasureTest {

  private static final Metric SOME_INT_METRIC = new MetricImpl("42", "int_metric", "name", Metric.MetricType.INT);
  private static final Metric SOME_LONG_METRIC = new MetricImpl("42", "long_metric", "name", Metric.MetricType.WORK_DUR);
  private static final Metric SOME_DOUBLE_METRIC = new MetricImpl("42", "double_metric", "name", Metric.MetricType.FLOAT);
  private static final Metric SOME_STRING_METRIC = new MetricImpl("42", "string_metric", "name", Metric.MetricType.STRING);
  private static final Metric SOME_BOOLEAN_METRIC = new MetricImpl("42", "boolean_metric", "name", Metric.MetricType.BOOL);
  private static final Metric SOME_LEVEL_METRIC = new MetricImpl("42", "level_metric", "name", Metric.MetricType.LEVEL);
  private static final MeasureDto EMPTY_MEASURE_DTO = new MeasureDto();

  @Test
  void toMeasure_returns_absent_for_null_argument() {
    assertThat(toMeasure(null, SOME_INT_METRIC)).isNotPresent();
  }

  @Test
  void toMeasure_throws_NPE_if_metric_argument_is_null() {
    assertThatThrownBy(() -> toMeasure(EMPTY_MEASURE_DTO, null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  void toMeasure_throws_NPE_if_both_arguments_are_null() {
    assertThatThrownBy(() -> toMeasure(null, null))
      .isInstanceOf(NullPointerException.class);
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
  void toMeasure_returns_empty_if_dto_has_no_data_for_metric(Metric metric) {
    Optional<Measure> measure = toMeasure(EMPTY_MEASURE_DTO, metric);

    assertThat(measure).isNotPresent();
  }

  static Stream<Arguments> numericMetricsWithValues() {
    return Stream.of(
      arguments(SOME_INT_METRIC, "int_metric", 42, ValueType.INT, 42),
      arguments(SOME_LONG_METRIC, "long_metric", 123456789L, ValueType.LONG, 123456789L),
      arguments(SOME_DOUBLE_METRIC, "double_metric", 3.14, ValueType.DOUBLE, 3.14));
  }

  @ParameterizedTest
  @MethodSource("numericMetricsWithValues")
  void toMeasure_returns_numeric_value_from_json_map(Metric metric, String metricKey, Object value,
    ValueType expectedType, Object expectedValue) {
    MeasureDto dto = new MeasureDto().addValue(metricKey, value);

    Optional<Measure> measure = toMeasure(dto, metric);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(expectedType);
    switch (expectedType) {
      case INT:
        assertThat(measure.get().getIntValue()).isEqualTo((Integer) expectedValue);
        break;
      case LONG:
        assertThat(measure.get().getLongValue()).isEqualTo((Long) expectedValue);
        break;
      case DOUBLE:
        assertThat(measure.get().getDoubleValue()).isEqualTo((Double) expectedValue);
        break;
      default:
        throw new IllegalArgumentException("Unexpected value type: " + expectedType);
    }
  }

  @Test
  void toMeasure_returns_empty_for_missing_key_in_json_map() {
    MeasureDto dto = new MeasureDto().addValue("other_metric", 42);

    Optional<Measure> measure = toMeasure(dto, SOME_INT_METRIC);

    assertThat(measure).isNotPresent();
  }

  @Test
  void toMeasure_returns_true_for_BOOLEAN_metric_when_value_is_1() {
    MeasureDto dto = new MeasureDto().addValue("boolean_metric", 1.0);

    Optional<Measure> measure = toMeasure(dto, SOME_BOOLEAN_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.BOOLEAN);
    assertThat(measure.get().getBooleanValue()).isTrue();
  }

  @Test
  void toMeasure_returns_false_for_BOOLEAN_metric_when_value_is_0() {
    MeasureDto dto = new MeasureDto().addValue("boolean_metric", 0.0);

    Optional<Measure> measure = toMeasure(dto, SOME_BOOLEAN_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.BOOLEAN);
    assertThat(measure.get().getBooleanValue()).isFalse();
  }

  @Test
  void toMeasure_returns_string_value_from_json_map() {
    MeasureDto dto = new MeasureDto().addValue("string_metric", "test_value");

    Optional<Measure> measure = toMeasure(dto, SOME_STRING_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.STRING);
    assertThat(measure.get().getStringValue()).isEqualTo("test_value");
  }

  @Test
  void toMeasure_returns_empty_if_dto_has_invalid_data_for_LEVEL_metric() {
    MeasureDto dto = new MeasureDto().addValue("level_metric", "INVALID_LEVEL");

    Optional<Measure> measure = toMeasure(dto, SOME_LEVEL_METRIC);

    assertThat(measure).isNotPresent();
  }

  @Test
  void toMeasure_returns_empty_if_dto_has_wrong_case_for_LEVEL_metric() {
    MeasureDto dto = new MeasureDto().addValue("level_metric", "oK");

    Optional<Measure> measure = toMeasure(dto, SOME_LEVEL_METRIC);

    assertThat(measure).isNotPresent();
  }

  static Stream<Level> allLevelValues() {
    return Stream.of(Level.values());
  }

  @ParameterizedTest
  @MethodSource("allLevelValues")
  void toMeasure_returns_level_value_for_all_level_types(Level level) {
    MeasureDto dto = new MeasureDto().addValue("level_metric", level.name());

    Optional<Measure> measure = toMeasure(dto, SOME_LEVEL_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.LEVEL);
    assertThat(measure.get().getLevelValue()).isEqualTo(level);
  }

  @Test
  void toMeasure_does_not_include_quality_gate_status() {
    // MeasureDto doesn't store QG status separately - it's just another metric value
    MeasureDto dto = new MeasureDto()
      .addValue("int_metric", 42)
      .addValue("alert_status", "ERROR");

    Optional<Measure> measure = toMeasure(dto, SOME_INT_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().hasQualityGateStatus()).isFalse();
  }

  @Test
  void toMeasure_does_not_include_data_field() {
    // MeasureDto doesn't support additional data field
    MeasureDto dto = new MeasureDto().addValue("int_metric", 42);

    Optional<Measure> measure = toMeasure(dto, SOME_INT_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getData()).isNull();
  }

  @Test
  void toMeasure_handles_multiple_metrics_in_same_dto() {
    MeasureDto dto = new MeasureDto()
      .addValue("int_metric", 42)
      .addValue("string_metric", "test")
      .addValue("double_metric", 3.14);

    Optional<Measure> intMeasure = toMeasure(dto, SOME_INT_METRIC);
    Optional<Measure> stringMeasure = toMeasure(dto, SOME_STRING_METRIC);
    Optional<Measure> doubleMeasure = toMeasure(dto, SOME_DOUBLE_METRIC);

    assertThat(intMeasure).isPresent();
    assertThat(intMeasure.get().getIntValue()).isEqualTo(42);

    assertThat(stringMeasure).isPresent();
    assertThat(stringMeasure.get().getStringValue()).isEqualTo("test");

    assertThat(doubleMeasure).isPresent();
    assertThat(doubleMeasure.get().getDoubleValue()).isEqualTo(3.14);
  }

  @Test
  void toMeasure_converts_numeric_values_correctly() {
    // JSON deserialization might store integers as doubles
    MeasureDto dto = new MeasureDto().addValue("int_metric", 42.0);

    Optional<Measure> measure = toMeasure(dto, SOME_INT_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getIntValue()).isEqualTo(42);
  }

}
