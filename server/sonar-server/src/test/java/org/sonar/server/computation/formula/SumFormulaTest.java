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
import org.sonar.server.computation.measure.Measure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;

public class SumFormulaTest {

  private static final SumFormula BASIC_SUM_FORMULA = new SumFormula(LINES_KEY);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  CounterContext counterContext = mock(CounterContext.class);

  @Test
  public void check_create_new_counter_class() throws Exception {
    assertThat(BASIC_SUM_FORMULA.createNewCounter().getClass()).isEqualTo(SumFormula.SumCounter.class);
  }

  @Test
  public void fail_with_NPE_when_creating_formula_with_null_metric() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Metric key cannot be null");

    new SumFormula(null);
  }

  @Test
  public void check_output_metric_key_is_lines() throws Exception {
    assertThat(BASIC_SUM_FORMULA.getOutputMetricKey()).isEqualTo(LINES_KEY);
  }

  @Test
  public void create_measure() throws Exception {
    SumFormula.SumCounter counter = BASIC_SUM_FORMULA.createNewCounter();
    addMeasure(LINES_KEY, 10);
    counter.aggregate(counterContext);

    assertThat(BASIC_SUM_FORMULA.createMeasure(counter, Component.Type.PROJECT).get().getIntValue()).isEqualTo(10);
  }

  @Test
  public void create_measure_when_counter_is_aggregating_from_another_counter() throws Exception {
    SumFormula.SumCounter anotherCounter = BASIC_SUM_FORMULA.createNewCounter();
    addMeasure(LINES_KEY, 10);
    anotherCounter.aggregate(counterContext);

    SumFormula.SumCounter counter = BASIC_SUM_FORMULA.createNewCounter();
    counter.aggregate(anotherCounter);

    assertThat(BASIC_SUM_FORMULA.createMeasure(counter, Component.Type.PROJECT).get().getIntValue()).isEqualTo(10);
  }

  @Test
  public void not_create_measure_on_file() throws Exception {
    SumFormula.SumCounter counter = BASIC_SUM_FORMULA.createNewCounter();
    addMeasure(LINES_KEY, 10);
    counter.aggregate(counterContext);

    Assertions.assertThat(BASIC_SUM_FORMULA.createMeasure(counter, Component.Type.FILE)).isAbsent();
  }

  @Test
  public void not_create_measure_when_value_is_zero() throws Exception {
    SumFormula.SumCounter counter = BASIC_SUM_FORMULA.createNewCounter();
    when(counterContext.getMeasure(LINES_KEY)).thenReturn(Optional.<Measure>absent());
    counter.aggregate(counterContext);

    Assertions.assertThat(BASIC_SUM_FORMULA.createMeasure(counter, Component.Type.PROJECT)).isAbsent();
  }

  private void addMeasure(String metricKey, int value) {
    when(counterContext.getMeasure(metricKey)).thenReturn(Optional.of(Measure.newMeasureBuilder().create(value)));
  }

}
