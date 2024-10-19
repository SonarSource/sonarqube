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
package org.sonar.ce.task.projectanalysis.formula;

import java.util.Optional;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.metric.Metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_KEY;
import static org.sonar.ce.task.projectanalysis.formula.AverageFormula.Builder;

public class AverageFormulaTest {

  private static final AverageFormula BASIC_AVERAGE_FORMULA = Builder.newBuilder()
    .setOutputMetricKey(FUNCTION_COMPLEXITY_KEY)
    .setMainMetricKey(COMPLEXITY_IN_FUNCTIONS_KEY)
    .setByMetricKey(FUNCTIONS_KEY)
    .build();

  CounterInitializationContext counterInitializationContext = mock(CounterInitializationContext.class);
  CreateMeasureContext createMeasureContext = new DumbCreateMeasureContext(
    ReportComponent.builder(Component.Type.PROJECT, 1).build(), mock(Metric.class));


  @Test
  public void fail_with_NPE_when_building_formula_without_output_metric() {
    assertThatThrownBy(() -> {
      Builder.newBuilder()
        .setOutputMetricKey(null)
        .setMainMetricKey(COMPLEXITY_IN_FUNCTIONS_KEY)
        .setByMetricKey(FUNCTIONS_KEY)
        .build();
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Output metric key cannot be null");
  }

  @Test
  public void fail_with_NPE_when_building_formula_without_main_metric() {
    assertThatThrownBy(() -> {
      Builder.newBuilder()
        .setOutputMetricKey(FUNCTION_COMPLEXITY_KEY)
        .setMainMetricKey(null)
        .setByMetricKey(FUNCTIONS_KEY)
        .build();
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Main metric Key cannot be null");
  }

  @Test
  public void fail_with_NPE_when_building_formula_without_by_metric() {
    assertThatThrownBy(() -> {
      Builder.newBuilder()
        .setOutputMetricKey(FUNCTION_COMPLEXITY_KEY)
        .setMainMetricKey(COMPLEXITY_IN_FUNCTIONS_KEY)
        .setByMetricKey(null)
        .build();
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("By metric Key cannot be null");
  }

  @Test
  public void check_new_counter_class() {
    assertThat(BASIC_AVERAGE_FORMULA.createNewCounter().getClass()).isEqualTo(AverageFormula.AverageCounter.class);
  }

  @Test
  public void check_output_metric_key_is_function_complexity_key() {
    assertThat(BASIC_AVERAGE_FORMULA.getOutputMetricKeys()).containsOnly(FUNCTION_COMPLEXITY_KEY);
  }

  @Test
  public void create_measure_when_counter_is_aggregated_from_context() {
    AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
    addMeasure(COMPLEXITY_IN_FUNCTIONS_KEY, 10d);
    addMeasure(FUNCTIONS_KEY, 2d);
    counter.initialize(counterInitializationContext);

    assertThat(BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext).get().getDoubleValue()).isEqualTo(5d);
  }

  @Test
  public void create_measure_when_counter_is_aggregated_from_another_counter() {
    AverageFormula.AverageCounter anotherCounter = BASIC_AVERAGE_FORMULA.createNewCounter();
    addMeasure(COMPLEXITY_IN_FUNCTIONS_KEY, 10d);
    addMeasure(FUNCTIONS_KEY, 2d);
    anotherCounter.initialize(counterInitializationContext);

    AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
    counter.aggregate(anotherCounter);

    assertThat(BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext).get().getDoubleValue()).isEqualTo(5d);
  }

  @Test
  public void create_double_measure() {
    AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
    addMeasure(COMPLEXITY_IN_FUNCTIONS_KEY, 10d);
    addMeasure(FUNCTIONS_KEY, 2d);
    counter.initialize(counterInitializationContext);

    assertThat(BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext).get().getDoubleValue()).isEqualTo(5d);
  }

  @Test
  public void create_integer_measure() {
    AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
    addMeasure(COMPLEXITY_IN_FUNCTIONS_KEY, 10);
    addMeasure(FUNCTIONS_KEY, 2);
    counter.initialize(counterInitializationContext);

    assertThat(BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext).get().getDoubleValue()).isEqualTo(5);
  }

  @Test
  public void create_long_measure() {
    AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
    addMeasure(COMPLEXITY_IN_FUNCTIONS_KEY, 10L);
    addMeasure(FUNCTIONS_KEY, 2L);
    counter.initialize(counterInitializationContext);

    assertThat(BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext).get().getDoubleValue()).isEqualTo(5L);
  }

  @Test
  public void not_create_measure_when_aggregated_measure_has_no_value() {
    AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
    addMeasure(COMPLEXITY_IN_FUNCTIONS_KEY, 10L);
    when(counterInitializationContext.getMeasure(FUNCTIONS_KEY)).thenReturn(Optional.of(Measure.newMeasureBuilder().createNoValue()));
    counter.initialize(counterInitializationContext);

    assertThat(BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext)).isNotPresent();
  }

  @Test
  public void fail_with_IAE_when_aggregate_from_component_and_context_with_not_numeric_measures() {
    assertThatThrownBy(() -> {
      AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
      addMeasure(COMPLEXITY_IN_FUNCTIONS_KEY, 10L);
      when(counterInitializationContext.getMeasure(FUNCTIONS_KEY)).thenReturn(Optional.of(Measure.newMeasureBuilder().create("data")));
      counter.initialize(counterInitializationContext);

      BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Measure of type 'STRING' are not supported");
  }

  @Test
  public void no_measure_created_when_counter_has_no_value() {
    AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
    when(counterInitializationContext.getMeasure(anyString())).thenReturn(Optional.empty());
    counter.initialize(counterInitializationContext);

    assertThat(BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext)).isNotPresent();
  }

  @Test
  public void not_create_measure_when_only_one_measure() {
    AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
    addMeasure(COMPLEXITY_IN_FUNCTIONS_KEY, 10L);
    when(counterInitializationContext.getMeasure(FUNCTIONS_KEY)).thenReturn(Optional.empty());
    counter.initialize(counterInitializationContext);

    assertThat(BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext)).isNotPresent();
  }

  @Test
  public void not_create_measure_when_by_value_is_zero() {
    AverageFormula.AverageCounter counter = BASIC_AVERAGE_FORMULA.createNewCounter();
    addMeasure(COMPLEXITY_IN_FUNCTIONS_KEY, 10d);
    addMeasure(FUNCTIONS_KEY, 0d);
    counter.initialize(counterInitializationContext);

    assertThat(BASIC_AVERAGE_FORMULA.createMeasure(counter, createMeasureContext)).isNotPresent();
  }

  private void addMeasure(String metricKey, double value) {
    when(counterInitializationContext.getMeasure(metricKey)).thenReturn(Optional.of(Measure.newMeasureBuilder().create(value, 1)));
  }

  private void addMeasure(String metricKey, int value) {
    when(counterInitializationContext.getMeasure(metricKey)).thenReturn(Optional.of(Measure.newMeasureBuilder().create(value)));
  }

  private void addMeasure(String metricKey, long value) {
    when(counterInitializationContext.getMeasure(metricKey)).thenReturn(Optional.of(Measure.newMeasureBuilder().create(value)));
  }

}
