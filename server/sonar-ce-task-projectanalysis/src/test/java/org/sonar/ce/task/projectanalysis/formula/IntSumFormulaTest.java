/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.formula.SumFormula.IntSumFormula;
import org.sonar.ce.task.projectanalysis.formula.counter.IntSumCounter;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.metric.Metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.ce.task.projectanalysis.formula.SumFormula.createIntSumFormula;

public class IntSumFormulaTest {

  private static final IntSumFormula INT_SUM_FORMULA = createIntSumFormula(LINES_KEY);
  private static final IntSumFormula INT_SUM_FORMULA_NULL_DEFAULT_INPUT_VALUE = createIntSumFormula(LINES_KEY, null);
  private static final IntSumFormula INT_SUM_FORMULA_DEFAULT_INPUT_VALUE_15 = createIntSumFormula(LINES_KEY, 15);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  CreateMeasureContext projectCreateMeasureContext = new DumbCreateMeasureContext(
    ReportComponent.builder(Component.Type.PROJECT, 1).build(), mock(Metric.class));
  CreateMeasureContext fileCreateMeasureContext = new DumbCreateMeasureContext(
    ReportComponent.builder(Component.Type.FILE, 2).build(), mock(Metric.class));

  @Test
  public void check_create_new_counter_class() {
    assertThat(INT_SUM_FORMULA.createNewCounter().getClass()).isEqualTo(IntSumCounter.class);
  }

  @Test
  public void fail_with_NPE_when_creating_formula_with_null_metric() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Metric key cannot be null");

    createIntSumFormula(null);
  }

  @Test
  public void check_output_metric_key_is_lines() {
    assertThat(INT_SUM_FORMULA.getOutputMetricKeys()).containsOnly(LINES_KEY);
    assertThat(INT_SUM_FORMULA_DEFAULT_INPUT_VALUE_15.getOutputMetricKeys()).containsOnly(LINES_KEY);
    assertThat(INT_SUM_FORMULA_NULL_DEFAULT_INPUT_VALUE.getOutputMetricKeys()).containsOnly(LINES_KEY);
  }

  @Test
  public void create_measure_when_initialized_and_input_measure_exists() {
    IntSumCounter counter = INT_SUM_FORMULA.createNewCounter();
    counter.initialize(createMeasureInInitContext(10));

    assertCreateMeasureValue(counter, 10);
  }

  @Test
  public void does_not_create_measure_when_only_initialized_and_input_measure_does_not_exist() {
    IntSumCounter counter = INT_SUM_FORMULA.createNewCounter();
    does_not_create_measure_when_only_initialized_and_input_measure_does_not_exist(counter);
  }

  @Test
  public void does_not_create_measure_when_only_initialized_and_input_measure_does_not_exist_and_defaultInputValue_is_null() {
    IntSumCounter counter = INT_SUM_FORMULA_NULL_DEFAULT_INPUT_VALUE.createNewCounter();
    does_not_create_measure_when_only_initialized_and_input_measure_does_not_exist(counter);
  }

  private void does_not_create_measure_when_only_initialized_and_input_measure_does_not_exist(IntSumCounter counter) {
    counter.initialize(createNoMeasureInInitContext());

    assertCreateNoMeasure(counter);
  }

  @Test
  public void creates_measure_when_only_initialized_and_input_measure_does_not_exist_and_defaultInputValue_is_non_null() {
    IntSumCounter counter = INT_SUM_FORMULA_DEFAULT_INPUT_VALUE_15.createNewCounter();
    counter.initialize(createNoMeasureInInitContext());

    assertCreateMeasureValue(counter, 15);
  }

  @Test
  public void create_measure_sum_of_init_and_aggregated_other_counter_when_input_measure_exists() {
    create_measure_sum_of_init_and_aggregated_other_counter(INT_SUM_FORMULA.createNewCounter(), 10, 30);
    create_measure_sum_of_init_and_aggregated_other_counter(INT_SUM_FORMULA_NULL_DEFAULT_INPUT_VALUE.createNewCounter(), 10, 30);
    create_measure_sum_of_init_and_aggregated_other_counter(INT_SUM_FORMULA_DEFAULT_INPUT_VALUE_15.createNewCounter(), 10, 30);
  }

  @Test
  public void create_measure_when_aggregated_other_counter_but_input_measure_does_not_exist() {
    create_measure_sum_of_init_and_aggregated_other_counter(INT_SUM_FORMULA.createNewCounter(), null, 20);
    create_measure_sum_of_init_and_aggregated_other_counter(INT_SUM_FORMULA_NULL_DEFAULT_INPUT_VALUE.createNewCounter(), null, 20);
    create_measure_sum_of_init_and_aggregated_other_counter(INT_SUM_FORMULA_DEFAULT_INPUT_VALUE_15.createNewCounter(), null, 35);
  }

  private void create_measure_sum_of_init_and_aggregated_other_counter(IntSumCounter counter, @Nullable Integer inputMeasure, int expectedMeasureValue) {
    // init with value 10
    if (inputMeasure != null) {
      counter.initialize(createMeasureInInitContext(10));
    } else {
      counter.initialize(createNoMeasureInInitContext());
    }

    // second counter with value 20
    IntSumCounter anotherCounter = INT_SUM_FORMULA.createNewCounter();
    anotherCounter.initialize(createMeasureInInitContext(20));
    counter.aggregate(anotherCounter);

    assertCreateMeasureValue(counter, expectedMeasureValue);
  }

  @Test
  public void initialize_does_not_create_measure_on_file() {
    initialize_does_not_create_measure_on_file(INT_SUM_FORMULA.createNewCounter());
    initialize_does_not_create_measure_on_file(INT_SUM_FORMULA_NULL_DEFAULT_INPUT_VALUE.createNewCounter());
    initialize_does_not_create_measure_on_file(INT_SUM_FORMULA_DEFAULT_INPUT_VALUE_15.createNewCounter());
  }

  private void assertCreateNoMeasure(IntSumCounter counter) {
    assertThat(INT_SUM_FORMULA.createMeasure(counter, projectCreateMeasureContext)).isNotPresent();
  }

  private void assertCreateMeasureValue(IntSumCounter counter, int expectMeasureValue) {
    assertThat(INT_SUM_FORMULA.createMeasure(counter, projectCreateMeasureContext).get().getIntValue()).isEqualTo(expectMeasureValue);
  }

  private void initialize_does_not_create_measure_on_file(IntSumCounter counter) {
    counter.initialize(createMeasureInInitContext(10));

    assertThat(INT_SUM_FORMULA.createMeasure(counter, fileCreateMeasureContext)).isNotPresent();
  }

  private static CounterInitializationContext createMeasureInInitContext(int value) {
    CounterInitializationContext initContext = mock(CounterInitializationContext.class);
    when(initContext.getMeasure(LINES_KEY)).thenReturn(Optional.of(Measure.newMeasureBuilder().create(value)));
    return initContext;
  }

  private static CounterInitializationContext createNoMeasureInInitContext() {
    CounterInitializationContext initContext = mock(CounterInitializationContext.class);
    when(initContext.getMeasure(LINES_KEY)).thenReturn(Optional.empty());
    return initContext;
  }

}
