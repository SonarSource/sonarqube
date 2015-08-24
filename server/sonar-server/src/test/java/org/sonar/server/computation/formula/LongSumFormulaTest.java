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
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.formula.SumFormula.LongSumFormula;
import org.sonar.server.computation.formula.counter.LongSumCounter;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.period.PeriodsHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.server.computation.formula.SumFormula.createLongSumFormula;

public class LongSumFormulaTest {

  private static final LongSumFormula LONG_SUM_FORMULA = createLongSumFormula(LINES_KEY);
  private static final long MEASURE_VALUE = 10;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  CounterInitializationContext counterInitializationContext = mock(CounterInitializationContext.class);
  CreateMeasureContext projectCreateMeasureContext = new DumbCreateMeasureContext(
    ReportComponent.builder(Component.Type.PROJECT, 1).build(), mock(Metric.class), mock(PeriodsHolder.class));
  CreateMeasureContext fileCreateMeasureContext = new DumbCreateMeasureContext(
    ReportComponent.builder(Component.Type.FILE, 2).build(), mock(Metric.class), mock(PeriodsHolder.class));

  @Test
  public void check_create_new_counter_class() {
    assertThat(LONG_SUM_FORMULA.createNewCounter().getClass()).isEqualTo(LongSumCounter.class);
  }

  @Test
  public void fail_with_NPE_when_creating_formula_with_null_metric() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Metric key cannot be null");

    createLongSumFormula(null);
  }

  @Test
  public void check_output_metric_key_is_lines() {
    assertThat(LONG_SUM_FORMULA.getOutputMetricKeys()).containsOnly(LINES_KEY);
  }

  @Test
  public void create_measure() {
    LongSumCounter counter = LONG_SUM_FORMULA.createNewCounter();
    addMeasure(LINES_KEY, MEASURE_VALUE);
    counter.initialize(counterInitializationContext);

    assertThat(LONG_SUM_FORMULA.createMeasure(counter, projectCreateMeasureContext).get().getLongValue()).isEqualTo(MEASURE_VALUE);
  }

  @Test
  public void create_measure_when_counter_is_aggregating_from_another_counter() {
    LongSumCounter anotherCounter = LONG_SUM_FORMULA.createNewCounter();
    addMeasure(LINES_KEY, MEASURE_VALUE);
    anotherCounter.initialize(counterInitializationContext);

    LongSumCounter counter = LONG_SUM_FORMULA.createNewCounter();
    counter.aggregate(anotherCounter);

    assertThat(LONG_SUM_FORMULA.createMeasure(counter, projectCreateMeasureContext).get().getLongValue()).isEqualTo(MEASURE_VALUE);
  }

  @Test
  public void not_create_measure_on_file() {
    LongSumCounter counter = LONG_SUM_FORMULA.createNewCounter();
    addMeasure(LINES_KEY, MEASURE_VALUE);
    counter.initialize(counterInitializationContext);

    Assertions.assertThat(LONG_SUM_FORMULA.createMeasure(counter, fileCreateMeasureContext)).isAbsent();
  }

  @Test
  public void do_not_create_measures_when_no_values() {
    LongSumCounter counter = LONG_SUM_FORMULA.createNewCounter();
    when(counterInitializationContext.getMeasure(LINES_KEY)).thenReturn(Optional.<Measure>absent());
    counter.initialize(counterInitializationContext);

    Assertions.assertThat(LONG_SUM_FORMULA.createMeasure(counter, projectCreateMeasureContext)).isAbsent();
  }

  private void addMeasure(String metricKey, long value) {
    when(counterInitializationContext.getMeasure(metricKey)).thenReturn(Optional.of(Measure.newMeasureBuilder().create(value)));
  }

}
