/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.task.projectanalysis.measure;

import com.google.common.base.Predicate;
import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;

public class BestValueOptimizationTest {

  private static final ReportComponent FILE_COMPONENT = ReportComponent.builder(Component.Type.FILE, 1).build();
  private static final ReportComponent SOME_NON_FILE_COMPONENT = ReportComponent.builder(Component.Type.MODULE, 2).build();
  private static final String SOME_DATA = "some_data";
  private static final MetricImpl METRIC_BOOLEAN_FALSE = createMetric(Metric.MetricType.BOOL, 6d);
  private static final MetricImpl METRIC_BOOLEAN_TRUE = createMetric(Metric.MetricType.BOOL, 1d);
  private static final MeasureVariations SOME_EMPTY_VARIATIONS = new MeasureVariations(null, 0d, null, 0d, null);

  public static Measure.NewMeasureBuilder[] builders_of_non_bestValueOptimized_measures() {
    QualityGateStatus someQualityGateStatus = new QualityGateStatus(Measure.Level.ERROR, null);
    MeasureVariations someVariations = new MeasureVariations(1d);
    String someDescription = "desc";
    return new Measure.NewMeasureBuilder[] {
      newMeasureBuilder().setQualityGateStatus(someQualityGateStatus),
      newMeasureBuilder().setQualityGateStatus(someQualityGateStatus).setVariations(someVariations),
      newMeasureBuilder().setVariations(someVariations),
      newMeasureBuilder().setQualityGateStatus(someQualityGateStatus).setDescription(someDescription),
      newMeasureBuilder().setDescription(someDescription),
      newMeasureBuilder().setQualityGateStatus(someQualityGateStatus).setVariations(someVariations).setDescription(someDescription),
    };
  }

  @Test
  public void apply_returns_true_for_value_true_for_Boolean_Metric_and_best_value_1() {
    Predicate<Measure> underTest = BestValueOptimization.from(METRIC_BOOLEAN_TRUE, FILE_COMPONENT);

    assertThat(underTest.apply(newMeasureBuilder().create(true))).isTrue();
    assertThat(underTest.apply(newMeasureBuilder().setVariations(SOME_EMPTY_VARIATIONS).create(true))).isTrue();
    assertThat(underTest.apply(newMeasureBuilder().create(false))).isFalse();
    assertThat(underTest.apply(newMeasureBuilder().setVariations(SOME_EMPTY_VARIATIONS).create(false))).isFalse();
  }

  @Test
  public void apply_returns_false_if_component_is_not_a_FILE_for_Boolean_Metric_and_best_value_1() {
    Predicate<Measure> underTest = BestValueOptimization.from(METRIC_BOOLEAN_TRUE, SOME_NON_FILE_COMPONENT);

    assertThat(underTest.apply(newMeasureBuilder().create(true))).isFalse();
    assertThat(underTest.apply(newMeasureBuilder().create(false))).isFalse();
  }

  @Test
  public void apply_returns_false_if_measure_has_anything_else_than_value_for_Boolean_Metric_and_best_value_1() {
    Predicate<Measure> underTest = BestValueOptimization.from(METRIC_BOOLEAN_TRUE, FILE_COMPONENT);

    for (Measure.NewMeasureBuilder builder : builders_of_non_bestValueOptimized_measures()) {
      assertThat(underTest.apply(builder.create(true))).isFalse();
      assertThat(underTest.apply(builder.create(false))).isFalse();
    }
  }

  @Test
  public void apply_returns_false_if_measure_has_data_for_Boolean_Metric_and_best_value_1() {
    Predicate<Measure> underTest = BestValueOptimization.from(METRIC_BOOLEAN_TRUE, FILE_COMPONENT);

    assertThat(underTest.apply(newMeasureBuilder().create(true, SOME_DATA))).isFalse();
    assertThat(underTest.apply(newMeasureBuilder().create(false, SOME_DATA))).isFalse();
  }

  @Test
  public void apply_returns_true_for_value_false_for_Boolean_Metric_and_best_value_not_1() {
    Predicate<Measure> underTest = BestValueOptimization.from(METRIC_BOOLEAN_FALSE, FILE_COMPONENT);

    assertThat(underTest.apply(newMeasureBuilder().create(true))).isFalse();
    assertThat(underTest.apply(newMeasureBuilder().create(false))).isTrue();
    assertThat(underTest.apply(newMeasureBuilder().setVariations(SOME_EMPTY_VARIATIONS).create(false))).isTrue();
  }

  @Test
  public void apply_returns_false_if_component_is_not_a_FILE_for_Boolean_Metric_and_best_value_not_1() {
    Predicate<Measure> underTest = BestValueOptimization.from(METRIC_BOOLEAN_FALSE, SOME_NON_FILE_COMPONENT);

    assertThat(underTest.apply(newMeasureBuilder().create(true))).isFalse();
    assertThat(underTest.apply(newMeasureBuilder().create(false))).isFalse();
  }

  @Test
  public void apply_returns_false_if_measure_has_anything_else_than_value_for_Boolean_Metric_and_best_value_not_1() {
    Predicate<Measure> underTest = BestValueOptimization.from(METRIC_BOOLEAN_FALSE, FILE_COMPONENT);

    for (Measure.NewMeasureBuilder builder : builders_of_non_bestValueOptimized_measures()) {
      assertThat(underTest.apply(builder.create(true))).isFalse();
      assertThat(underTest.apply(builder.create(false))).isFalse();
    }
  }

  @Test
  public void apply_returns_false_if_measure_has_data_for_Boolean_Metric_and_best_value_not_1() {
    Predicate<Measure> underTest = BestValueOptimization.from(METRIC_BOOLEAN_FALSE, FILE_COMPONENT);

    assertThat(underTest.apply(newMeasureBuilder().create(true, SOME_DATA))).isFalse();
    assertThat(underTest.apply(newMeasureBuilder().create(false, SOME_DATA))).isFalse();
  }

  @Test
  public void verify_value_comparison_for_int_metric() {
    Predicate<Measure> underTest = BestValueOptimization.from(createMetric(Metric.MetricType.INT, 10), FILE_COMPONENT);

    assertThat(underTest.apply(newMeasureBuilder().create(10))).isTrue();
    assertThat(underTest.apply(newMeasureBuilder().setVariations(SOME_EMPTY_VARIATIONS).create(10))).isTrue();
    assertThat(underTest.apply(newMeasureBuilder().create(11))).isFalse();
  }

  @Test
  public void verify_value_comparison_for_long_metric() {
    Predicate<Measure> underTest = BestValueOptimization.from(createMetric(Metric.MetricType.WORK_DUR, 9511L), FILE_COMPONENT);

    assertThat(underTest.apply(newMeasureBuilder().create(9511L))).isTrue();
    assertThat(underTest.apply(newMeasureBuilder().setVariations(SOME_EMPTY_VARIATIONS).create(9511L))).isTrue();
    assertThat(underTest.apply(newMeasureBuilder().create(963L))).isFalse();
  }

  @Test
  public void verify_value_comparison_for_double_metric() {
    Predicate<Measure> underTest = BestValueOptimization.from(createMetric(Metric.MetricType.FLOAT, 36.5d), FILE_COMPONENT);

    assertThat(underTest.apply(newMeasureBuilder().create(36.5d, 1))).isTrue();
    assertThat(underTest.apply(newMeasureBuilder().setVariations(SOME_EMPTY_VARIATIONS).create(36.5d, 1))).isTrue();
    assertThat(underTest.apply(newMeasureBuilder().create(36.6d, 1))).isFalse();
  }

  @Test
  public void apply_returns_false_for_String_measure() {
    Predicate<Measure> underTest = BestValueOptimization.from(createMetric(Metric.MetricType.FLOAT, 36.5d), FILE_COMPONENT);

    assertThat(underTest.apply(newMeasureBuilder().create("aaa"))).isFalse();
  }

  @Test
  public void apply_returns_false_for_LEVEL_measure() {
    Predicate<Measure> underTest = BestValueOptimization.from(createMetric(Metric.MetricType.STRING, 36.5d), FILE_COMPONENT);

    assertThat(underTest.apply(newMeasureBuilder().create(Measure.Level.OK))).isFalse();
  }

  private static MetricImpl createMetric(Metric.MetricType metricType, double bestValue) {
    return new MetricImpl(metricType.hashCode() + (int) bestValue, "key" + metricType + bestValue, "name" + metricType + bestValue, metricType, null, bestValue, true);
  }
}
