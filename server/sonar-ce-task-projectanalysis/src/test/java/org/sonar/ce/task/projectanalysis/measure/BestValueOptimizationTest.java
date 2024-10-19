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
package org.sonar.ce.task.projectanalysis.measure;

import java.util.function.Predicate;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.server.measure.Rating.A;
import static org.sonar.server.measure.Rating.B;

public class BestValueOptimizationTest {

  private static final ReportComponent FILE_COMPONENT = ReportComponent.builder(Component.Type.FILE, 1).build();
  private static final ReportComponent SOME_NON_FILE_COMPONENT = ReportComponent.builder(Component.Type.DIRECTORY, 2).build();
  private static final String SOME_DATA = "some_data";
  private static final MetricImpl METRIC_BOOLEAN_FALSE = createMetric(Metric.MetricType.BOOL, 6d);
  private static final MetricImpl METRIC_BOOLEAN_TRUE = createMetric(Metric.MetricType.BOOL, 1d);

  @Test
  public void apply_returns_true_for_value_true_for_Boolean_Metric_and_best_value_1() {
    Predicate<Measure> underTest = BestValueOptimization.from(METRIC_BOOLEAN_TRUE, FILE_COMPONENT);

    assertThat(underTest.test(newMeasureBuilder().create(true))).isTrue();
    assertThat(underTest.test(newMeasureBuilder().create(false))).isFalse();
  }

  @Test
  public void apply_returns_false_if_component_is_not_a_FILE_for_Boolean_Metric_and_best_value_1() {
    Predicate<Measure> underTest = BestValueOptimization.from(METRIC_BOOLEAN_TRUE, SOME_NON_FILE_COMPONENT);

    assertThat(underTest.test(newMeasureBuilder().create(true))).isFalse();
    assertThat(underTest.test(newMeasureBuilder().create(false))).isFalse();
  }

  @Test
  public void apply_returns_false_if_measure_has_anything_else_than_value_for_Boolean_Metric_and_best_value_1() {
    Predicate<Measure> underTest = BestValueOptimization.from(METRIC_BOOLEAN_TRUE, FILE_COMPONENT);

    Measure.NewMeasureBuilder builder = newMeasureBuilder().setQualityGateStatus(new QualityGateStatus(Measure.Level.ERROR, null));
    assertThat(underTest.test(builder.create(true))).isFalse();
    assertThat(underTest.test(builder.create(false))).isFalse();
  }

  @Test
  public void apply_returns_false_if_measure_has_data_for_Boolean_Metric_and_best_value_1() {
    Predicate<Measure> underTest = BestValueOptimization.from(METRIC_BOOLEAN_TRUE, FILE_COMPONENT);

    assertThat(underTest.test(newMeasureBuilder().create(true, SOME_DATA))).isFalse();
    assertThat(underTest.test(newMeasureBuilder().create(false, SOME_DATA))).isFalse();
  }

  @Test
  public void apply_returns_true_for_value_false_for_Boolean_Metric_and_best_value_not_1() {
    Predicate<Measure> underTest = BestValueOptimization.from(METRIC_BOOLEAN_FALSE, FILE_COMPONENT);

    assertThat(underTest.test(newMeasureBuilder().create(true))).isFalse();
    assertThat(underTest.test(newMeasureBuilder().create(false))).isTrue();
  }

  @Test
  public void apply_returns_false_if_component_is_not_a_FILE_for_Boolean_Metric_and_best_value_not_1() {
    Predicate<Measure> underTest = BestValueOptimization.from(METRIC_BOOLEAN_FALSE, SOME_NON_FILE_COMPONENT);

    assertThat(underTest.test(newMeasureBuilder().create(true))).isFalse();
    assertThat(underTest.test(newMeasureBuilder().create(false))).isFalse();
  }

  @Test
  public void apply_returns_false_if_measure_has_anything_else_than_value_for_Boolean_Metric_and_best_value_not_1() {
    Predicate<Measure> underTest = BestValueOptimization.from(METRIC_BOOLEAN_FALSE, FILE_COMPONENT);

    Measure.NewMeasureBuilder builder = newMeasureBuilder().setQualityGateStatus(new QualityGateStatus(Measure.Level.ERROR, null));
    assertThat(underTest.test(builder.create(true))).isFalse();
    assertThat(underTest.test(builder.create(false))).isFalse();
  }

  @Test
  public void apply_returns_false_if_measure_has_data_for_Boolean_Metric_and_best_value_not_1() {
    Predicate<Measure> underTest = BestValueOptimization.from(METRIC_BOOLEAN_FALSE, FILE_COMPONENT);

    assertThat(underTest.test(newMeasureBuilder().create(true, SOME_DATA))).isFalse();
    assertThat(underTest.test(newMeasureBuilder().create(false, SOME_DATA))).isFalse();
  }

  @Test
  public void verify_value_comparison_for_int_metric() {
    Predicate<Measure> underTest = BestValueOptimization.from(createMetric(Metric.MetricType.INT, 10), FILE_COMPONENT);

    assertThat(underTest.test(newMeasureBuilder().create(10))).isTrue();
    assertThat(underTest.test(newMeasureBuilder().create(11))).isFalse();
  }

  @Test
  public void verify_value_comparison_for_long_metric() {
    Predicate<Measure> underTest = BestValueOptimization.from(createMetric(Metric.MetricType.WORK_DUR, 9511L), FILE_COMPONENT);

    assertThat(underTest.test(newMeasureBuilder().create(9511L))).isTrue();
    assertThat(underTest.test(newMeasureBuilder().create(963L))).isFalse();
  }

  @Test
  public void verify_value_comparison_for_rating_metric() {
    Predicate<Measure> underTest = BestValueOptimization.from(createMetric(Metric.MetricType.RATING, A.getIndex()), FILE_COMPONENT);

    assertThat(underTest.test(newMeasureBuilder().create(A.getIndex()))).isTrue();
    assertThat(underTest.test(newMeasureBuilder().create(B.getIndex()))).isFalse();
  }

  @Test
  public void verify_value_comparison_for_double_metric() {
    Predicate<Measure> underTest = BestValueOptimization.from(createMetric(Metric.MetricType.FLOAT, 36.5d), FILE_COMPONENT);

    assertThat(underTest.test(newMeasureBuilder().create(36.5d, 1))).isTrue();
    assertThat(underTest.test(newMeasureBuilder().create(36.6d, 1))).isFalse();
  }

  @Test
  public void apply_returns_false_for_String_measure() {
    Predicate<Measure> underTest = BestValueOptimization.from(createMetric(Metric.MetricType.FLOAT, 36.5d), FILE_COMPONENT);

    assertThat(underTest.test(newMeasureBuilder().create("aaa"))).isFalse();
  }

  @Test
  public void apply_returns_false_for_LEVEL_measure() {
    Predicate<Measure> underTest = BestValueOptimization.from(createMetric(Metric.MetricType.STRING, 36.5d), FILE_COMPONENT);

    assertThat(underTest.test(newMeasureBuilder().create(Measure.Level.OK))).isFalse();
  }

  private static MetricImpl createMetric(Metric.MetricType metricType, double bestValue) {
    return new MetricImpl(metricType.name() + bestValue, "key" + metricType + bestValue, "name" + metricType + bestValue, metricType, null,
      bestValue, true, false);
  }
}
