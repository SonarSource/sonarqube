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
package org.sonar.api.measures;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.File;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AverageFormulaTest {

  private FormulaContext context;
  private FormulaData data;

  @Before
  public void before() {
    context = mock(FormulaContext.class);
    when(context.getTargetMetric()).thenReturn(CoreMetrics.FUNCTION_COMPLEXITY);
    data = mock(FormulaData.class);
  }

  @Test
  public void test_depends_upon_metrics() throws Exception {
    AverageFormula formula = AverageFormula.create(CoreMetrics.COMPLEXITY, CoreMetrics.FUNCTIONS);
    assertThat(formula.dependsUponMetrics()).containsOnly(CoreMetrics.COMPLEXITY, CoreMetrics.FUNCTIONS);
  }

  @Test
  public void test_depends_upon_fallback_metric() throws Exception {
    AverageFormula formula = AverageFormula.create(CoreMetrics.COMPLEXITY_IN_FUNCTIONS, CoreMetrics.FUNCTIONS).setFallbackForMainMetric(CoreMetrics.COMPLEXITY);
    assertThat(formula.dependsUponMetrics()).containsOnly(CoreMetrics.COMPLEXITY_IN_FUNCTIONS, CoreMetrics.COMPLEXITY, CoreMetrics.FUNCTIONS);
  }

  @Test
  public void test_average_calculation() {
    List<FormulaData> childrenData = newArrayList();
    FormulaData data1 = mock(FormulaData.class);
    childrenData.add(data1);
    when(data1.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 43.0));
    when(data1.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 107.0));

    FormulaData data2 = mock(FormulaData.class);
    childrenData.add(data2);
    when(data2.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 127.0));
    when(data2.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 233.0));

    when(data.getChildren()).thenReturn(childrenData);

    Measure measure = AverageFormula.create(CoreMetrics.COMPLEXITY, CoreMetrics.FUNCTIONS).calculate(data, context);

    assertThat(measure.getValue()).isEqualTo(2.0);
  }

  @Test
  public void should_not_compute_if_not_target_metric() {
    when(data.getMeasure(CoreMetrics.FUNCTION_COMPLEXITY)).thenReturn(new Measure(CoreMetrics.FUNCTION_COMPLEXITY, 2.0));
    Measure measure = AverageFormula.create(CoreMetrics.COMPLEXITY, CoreMetrics.FUNCTIONS).calculate(data, context);
    assertThat(measure).isNull();
  }

  @Test
  public void test_when_no_children_measures() {
    List<FormulaData> childrenData = newArrayList();
    when(data.getChildren()).thenReturn(childrenData);
    Measure measure = AverageFormula.create(CoreMetrics.COMPLEXITY, CoreMetrics.FUNCTIONS).calculate(data, context);
    assertThat(measure).isNull();
  }

  @Test
  public void test_when_no_complexity_measures() {
    List<FormulaData> childrenData = newArrayList();
    FormulaData data1 = mock(FormulaData.class);
    childrenData.add(data1);
    when(data1.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 43.0));

    when(data.getChildren()).thenReturn(childrenData);
    Measure measure = AverageFormula.create(CoreMetrics.COMPLEXITY, CoreMetrics.FUNCTIONS).calculate(data, context);

    assertThat(measure).isNull();
  }

  @Test
  public void test_when_no_by_metric_measures() {
    List<FormulaData> childrenData = newArrayList();
    FormulaData data1 = mock(FormulaData.class);
    childrenData.add(data1);
    when(data1.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 43.0));

    when(data.getChildren()).thenReturn(childrenData);
    Measure measure = AverageFormula.create(CoreMetrics.COMPLEXITY, CoreMetrics.FUNCTIONS).calculate(data, context);

    assertThat(measure).isNull();
  }

  @Test
  public void test_when_mixed_metrics() {
    List<FormulaData> childrenData = newArrayList();
    FormulaData data1 = mock(FormulaData.class);
    childrenData.add(data1);
    when(data1.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 43.0));
    when(data1.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 107.0));

    FormulaData data2 = mock(FormulaData.class);
    childrenData.add(data2);
    when(data2.getMeasure(CoreMetrics.STATEMENTS)).thenReturn(new Measure(CoreMetrics.STATEMENTS, 127.0));
    when(data2.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 233.0));

    when(data.getChildren()).thenReturn(childrenData);

    Measure measure = AverageFormula.create(CoreMetrics.COMPLEXITY, CoreMetrics.FUNCTIONS).calculate(data, context);

    assertThat(measure.getValue()).isEqualTo(2.5);
  }

  @Test
  public void test_calculation_for_file() {
    when(data.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 60.0));
    when(data.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 20.0));
    when(context.getResource()).thenReturn(File.create("foo"));

    Measure measure = AverageFormula.create(CoreMetrics.COMPLEXITY, CoreMetrics.FUNCTIONS).calculate(data, context);
    assertThat(measure.getValue()).isEqualTo(3.0);
  }

  @Test
  public void should_use_fallback_metric_when_no_data_on_main_metric_for_file() {
    when(data.getMeasure(CoreMetrics.COMPLEXITY_IN_FUNCTIONS)).thenReturn(null);
    when(data.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 60.0));
    when(data.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 20.0));
    when(context.getResource()).thenReturn(File.create("foo"));

    Measure measure = AverageFormula.create(CoreMetrics.COMPLEXITY_IN_FUNCTIONS, CoreMetrics.FUNCTIONS)
      .setFallbackForMainMetric(CoreMetrics.COMPLEXITY)
      .calculate(data, context);
    assertThat(measure.getValue()).isEqualTo(3.0);
  }

  @Test
  public void should_use_main_metric_even_if_fallback_metric_provided() {
    when(data.getMeasure(CoreMetrics.COMPLEXITY_IN_FUNCTIONS)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 60.0));
    when(data.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 42.0));
    when(data.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 20.0));
    when(context.getResource()).thenReturn(File.create("foo"));

    Measure measure = AverageFormula.create(CoreMetrics.COMPLEXITY_IN_FUNCTIONS, CoreMetrics.FUNCTIONS)
      .setFallbackForMainMetric(CoreMetrics.COMPLEXITY)
      .calculate(data, context);
    assertThat(measure.getValue()).isEqualTo(3.0);
  }

  @Test
  public void should_use_fallback_metric_when_no_data_on_main_metric_for_children() {
    List<FormulaData> childrenData = newArrayList();
    FormulaData data1 = mock(FormulaData.class);
    childrenData.add(data1);
    when(data1.getMeasure(CoreMetrics.COMPLEXITY_IN_FUNCTIONS)).thenReturn(null);
    when(data1.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 107.0));
    when(data1.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 43.0));

    when(data.getChildren()).thenReturn(childrenData);

    Measure measure = AverageFormula.create(CoreMetrics.COMPLEXITY_IN_FUNCTIONS, CoreMetrics.FUNCTIONS)
      .setFallbackForMainMetric(CoreMetrics.COMPLEXITY)
      .calculate(data, context);

    assertThat(measure.getValue()).isEqualTo(2.5);
  }

}
