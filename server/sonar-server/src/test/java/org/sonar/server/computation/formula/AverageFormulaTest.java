/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.formula;

import com.google.common.base.Optional;
import org.assertj.guava.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.period.PeriodsHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_KEY;
import static org.sonar.server.computation.formula.AverageFormula.Builder;

public class AverageFormulaTest {

  private static final AverageFormula BASIC_AVERAGE_FORMULA = Builder.newBuilder()
    .setOutputMetricKey(FUNCTION_COMPLEXITY_KEY)
    .setMainMetricKey(COMPLEXITY_IN_FUNCTIONS_KEY)
    .setByMetricKey(FUNCTIONS_KEY)
    .build();

  FileAggregateContext fileAggregateContext = mock(FileAggregateContext.class);
  CreateMeasureContext createMeasureContext = new DumbCreateMeasureContext(
      DumbComponent.builder(Component.Type.PROJECT, 1).build(), mock(PeriodsHolder.class));

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void fail_with_NPE_when_building_formula_without_output_metric() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Output metric key cannot be null");

    Builder.newBuilder()
      .setOutputMetricKey(null)
      .setMainMetricKey(COMPLEXITY_IN_FUNCTIONS_KEY)
      .setByMetricKey(FUNCTIONS_KEY)
      .build();
  }

  @Test
  public void fail_with_NPE_when_building_formula_without_main_metric() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Main metric Key cannot be null");

    Builder.newBuilder()
      .setOutputMetricKey(FUNCTION_COMPLEXITY_KEY)
      .setMainMetricKey(null)
      .setByMetricKey(FUNCTIONS_KEY)
      .build();
  }

  @Test
  public void fail_with_NPE_when_building_formula_without_by_metric() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("By metric Key cannot be null");

    Builder.newBuilder()
      .setOutputMetricKey(FUNCTION_COMPLEXITY_KEY)
      .setMainMetricKey(COMPLEXITY_IN_FUNCTIONS_KEY)
      .setByMetricKey(null)
      .build();
  }

  @Test
  public void check_new_counter_class() {
    assertThat(BASIC_AVERAGE_FORMULA.createNewCounter().getClass()).isEqualTo(AverageFormula.AverageCounter.class);
  }

  @Test
  public void check_output_metric_key_is_function_complexity_key() {
    assertThat(BASIC_AVERAGE_FORMULA.getOutputMetricKey()).isEqualTo(FUNCTION_COMPLEXITY_KEY);
  }

  @Test
  public void create_measure_when_counter_is_aggregated_from_context() {
    AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
    addMeasure(COMPLEXITY_IN_FUNCTIONS_KEY, 10d);
    addMeasure(FUNCTIONS_KEY, 2d);
    counter.aggregate(fileAggregateContext);

    assertThat(BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext).get().getDoubleValue()).isEqualTo(5d);
  }

  @Test
  public void create_measure_when_counter_is_aggregated_from_another_counter() {
    AverageFormula.AverageCounter anotherCounter = BASIC_AVERAGE_FORMULA.createNewCounter();
    addMeasure(COMPLEXITY_IN_FUNCTIONS_KEY, 10d);
    addMeasure(FUNCTIONS_KEY, 2d);
    anotherCounter.aggregate(fileAggregateContext);

    AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
    counter.aggregate(anotherCounter);

    assertThat(BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext).get().getDoubleValue()).isEqualTo(5d);
  }

  @Test
  public void create_double_measure() {
    AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
    addMeasure(COMPLEXITY_IN_FUNCTIONS_KEY, 10d);
    addMeasure(FUNCTIONS_KEY, 2d);
    counter.aggregate(fileAggregateContext);

    assertThat(BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext).get().getDoubleValue()).isEqualTo(5d);
  }

  @Test
  public void create_integer_measure() {
    AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
    addMeasure(COMPLEXITY_IN_FUNCTIONS_KEY, 10);
    addMeasure(FUNCTIONS_KEY, 2);
    counter.aggregate(fileAggregateContext);

    assertThat(BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext).get().getDoubleValue()).isEqualTo(5);
  }

  @Test
  public void create_long_measure() {
    AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
    addMeasure(COMPLEXITY_IN_FUNCTIONS_KEY, 10L);
    addMeasure(FUNCTIONS_KEY, 2L);
    counter.aggregate(fileAggregateContext);

    assertThat(BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext).get().getDoubleValue()).isEqualTo(5L);
  }

  @Test
  public void not_create_measure_when_aggregated_measure_has_no_value() {
    AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
    addMeasure(COMPLEXITY_IN_FUNCTIONS_KEY, 10L);
    when(fileAggregateContext.getMeasure(FUNCTIONS_KEY)).thenReturn(Optional.of(Measure.newMeasureBuilder().createNoValue()));
    counter.aggregate(fileAggregateContext);

    Assertions.assertThat(BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext)).isAbsent();
  }

  @Test
  public void fail_with_IAE_when_aggregate_from_component_and_context_with_not_numeric_measures() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Measure of type 'STRING' are not supported");

    AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
    addMeasure(COMPLEXITY_IN_FUNCTIONS_KEY, 10L);
    when(fileAggregateContext.getMeasure(FUNCTIONS_KEY)).thenReturn(Optional.of(Measure.newMeasureBuilder().create("data")));
    counter.aggregate(fileAggregateContext);

    BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext);
  }

  @Test
  public void no_measure_created_when_counter_has_no_value() {
    AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
    when(fileAggregateContext.getMeasure(anyString())).thenReturn(Optional.<Measure>absent());
    counter.aggregate(fileAggregateContext);

    Assertions.assertThat(BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext)).isAbsent();
  }

  @Test
  public void not_create_measure_when_only_one_measure() {
    AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
    addMeasure(COMPLEXITY_IN_FUNCTIONS_KEY, 10L);
    when(fileAggregateContext.getMeasure(FUNCTIONS_KEY)).thenReturn(Optional.<Measure>absent());
    counter.aggregate(fileAggregateContext);

    Assertions.assertThat(BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext)).isAbsent();
  }

  @Test
  public void not_create_measure_when_by_value_is_zero() {
    AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
    addMeasure(COMPLEXITY_IN_FUNCTIONS_KEY, 10d);
    addMeasure(FUNCTIONS_KEY, 0d);
    counter.aggregate(fileAggregateContext);

    Assertions.assertThat(BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext)).isAbsent();
  }

  @Test
  public void create_measure_from_fall_back_measure() {
    AverageFormula sut = Builder.newBuilder()
      .setOutputMetricKey(FUNCTION_COMPLEXITY_KEY)
      .setMainMetricKey(COMPLEXITY_IN_FUNCTIONS_KEY)
      .setByMetricKey(FUNCTIONS_KEY)
      .setFallbackMetricKey(CoreMetrics.COMPLEXITY_KEY)
      .build();

    AverageFormula.AverageCounter counter = sut.createNewCounter();
    when(fileAggregateContext.getMeasure(COMPLEXITY_IN_FUNCTIONS_KEY)).thenReturn(Optional.<Measure>absent());
    addMeasure(COMPLEXITY_KEY, 10d);
    addMeasure(FUNCTIONS_KEY, 2d);
    counter.aggregate(fileAggregateContext);

    assertThat(sut.createMeasure(counter, createMeasureContext).get().getDoubleValue()).isEqualTo(5d);
  }

  @Test
  public void not_use_fallback_measure_if_main_measure_exists() {
    AverageFormula sut = Builder.newBuilder()
      .setOutputMetricKey(FUNCTION_COMPLEXITY_KEY)
      .setMainMetricKey(COMPLEXITY_IN_FUNCTIONS_KEY)
      .setByMetricKey(FUNCTIONS_KEY)
      .setFallbackMetricKey(CoreMetrics.COMPLEXITY_KEY)
      .build();

    AverageFormula.AverageCounter counter = sut.createNewCounter();
    addMeasure(COMPLEXITY_IN_FUNCTIONS_KEY, 10d);
    addMeasure(COMPLEXITY_KEY, 12d);
    addMeasure(FUNCTIONS_KEY, 2d);
    counter.aggregate(fileAggregateContext);

    assertThat(sut.createMeasure(counter, createMeasureContext).get().getDoubleValue()).isEqualTo(5d);
  }

  private void addMeasure(String metricKey, double value) {
    when(fileAggregateContext.getMeasure(metricKey)).thenReturn(Optional.of(Measure.newMeasureBuilder().create(value)));
  }

  private void addMeasure(String metricKey, int value) {
    when(fileAggregateContext.getMeasure(metricKey)).thenReturn(Optional.of(Measure.newMeasureBuilder().create(value)));
  }

  private void addMeasure(String metricKey, long value) {
    when(fileAggregateContext.getMeasure(metricKey)).thenReturn(Optional.of(Measure.newMeasureBuilder().create(value)));
  }

}
