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
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY;

public class DistributionFormulaTest {

  private static final DistributionFormula BASIC_DISTRIBUTION_FORMULA = new DistributionFormula(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  CounterContext counterContext = mock(CounterContext.class);

  @Test
  public void check_new_counter_class() {
    assertThat(BASIC_DISTRIBUTION_FORMULA.createNewCounter().getClass()).isEqualTo(DistributionFormula.DistributionCounter.class);
  }

  @Test
  public void fail_with_NPE_when_creating_counter_with_null_metric() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Metric key cannot be null");

    new DistributionFormula(null);
  }

  @Test
  public void check_output_metric_key_is_function_complexity_distribution() {
    assertThat(BASIC_DISTRIBUTION_FORMULA.getOutputMetricKey()).isEqualTo(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY);
  }

  @Test
  public void create_measure() {
    DistributionFormula.DistributionCounter counter = BASIC_DISTRIBUTION_FORMULA.createNewCounter();
    addMeasure(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, "0=3;3=7;6=10");
    counter.aggregate(counterContext);

    assertThat(BASIC_DISTRIBUTION_FORMULA.createMeasure(counter, Component.Type.PROJECT).get().getData()).isEqualTo("0=3;3=7;6=10");
  }

  @Test
  public void create_measure_when_counter_is_aggregating_from_another_counter() {
    DistributionFormula.DistributionCounter anotherCounter = BASIC_DISTRIBUTION_FORMULA.createNewCounter();
    addMeasure(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, "0=3;3=7;6=10");
    anotherCounter.aggregate(counterContext);

    DistributionFormula.DistributionCounter counter = BASIC_DISTRIBUTION_FORMULA.createNewCounter();
    counter.aggregate(anotherCounter);

    assertThat(BASIC_DISTRIBUTION_FORMULA.createMeasure(counter, Component.Type.PROJECT).get().getData()).isEqualTo("0=3;3=7;6=10");
  }

  @Test
  public void create_no_measure_when_no_value() {
    DistributionFormula.DistributionCounter counter = BASIC_DISTRIBUTION_FORMULA.createNewCounter();
    when(counterContext.getMeasure(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY)).thenReturn(Optional.<Measure>absent());
    counter.aggregate(counterContext);

    Assertions.assertThat(BASIC_DISTRIBUTION_FORMULA.createMeasure(counter, Component.Type.PROJECT)).isAbsent();
  }

  @Test
  public void not_create_measure_when_on_file() {
    DistributionFormula.DistributionCounter counter = BASIC_DISTRIBUTION_FORMULA.createNewCounter();
    addMeasure(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, "0=3;3=7;6=10");
    counter.aggregate(counterContext);

    Assertions.assertThat(BASIC_DISTRIBUTION_FORMULA.createMeasure(counter, Component.Type.FILE)).isAbsent();
  }

  private void addMeasure(String metricKey, String value) {
    when(counterContext.getMeasure(metricKey)).thenReturn(Optional.of(Measure.newMeasureBuilder().create(value)));
  }

}
