/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.measures;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.JavaFile;

import java.util.List;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;
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
  public void testDependsUponMetrics() throws Exception {
    AverageFormula formula = AverageFormula.create(CoreMetrics.COMPLEXITY, CoreMetrics.FUNCTIONS);
    assertThat(formula.dependsUponMetrics(), hasItems(CoreMetrics.COMPLEXITY, CoreMetrics.FUNCTIONS));
  }

  @Test
  public void testAverageCalculation() {
    List<FormulaData> childrenData = Lists.newArrayList();
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

    assertThat(measure.getValue(), is(2.0));
  }

  @Test
  public void shouldNotComputeIfNotTargetMetric() {
    when(data.getMeasure(CoreMetrics.FUNCTION_COMPLEXITY)).thenReturn(new Measure(CoreMetrics.FUNCTION_COMPLEXITY, 2.0));
    Measure measure = AverageFormula.create(CoreMetrics.COMPLEXITY, CoreMetrics.FUNCTIONS).calculate(data, context);
    assertNull(measure);
  }

  @Test
  public void testWhenNoChildrenMesaures() {
    List<FormulaData> childrenData = Lists.newArrayList();
    when(data.getChildren()).thenReturn(childrenData);
    Measure measure = AverageFormula.create(CoreMetrics.COMPLEXITY, CoreMetrics.FUNCTIONS).calculate(data, context);
    assertNull(measure);
  }

  @Test
  public void testWhenNoComplexityMesaures() {
    List<FormulaData> childrenData = Lists.newArrayList();
    FormulaData data1 = mock(FormulaData.class);
    childrenData.add(data1);
    when(data1.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 43.0));

    when(data.getChildren()).thenReturn(childrenData);
    Measure measure = AverageFormula.create(CoreMetrics.COMPLEXITY, CoreMetrics.FUNCTIONS).calculate(data, context);

    assertNull(measure);
  }

  @Test
  public void testWhenNoByMetricMesaures() {
    List<FormulaData> childrenData = Lists.newArrayList();
    FormulaData data1 = mock(FormulaData.class);
    childrenData.add(data1);
    when(data1.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 43.0));

    when(data.getChildren()).thenReturn(childrenData);
    Measure measure = AverageFormula.create(CoreMetrics.COMPLEXITY, CoreMetrics.FUNCTIONS).calculate(data, context);

    assertNull(measure);
  }

  @Test
  public void testWhenMixedMetrics() {
    List<FormulaData> childrenData = Lists.newArrayList();
    FormulaData data1 = mock(FormulaData.class);
    childrenData.add(data1);
    when(data1.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 43.0));
    when(data1.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 107.0));

    FormulaData data2 = mock(FormulaData.class);
    childrenData.add(data2);
    when(data2.getMeasure(CoreMetrics.STATEMENTS)).thenReturn(new Measure(CoreMetrics.STATEMENTS, 127.0));
    when(data2.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 233.0));

    when(data.getChildren()).thenReturn(childrenData);

    Measure measure = AverageFormula.create(CoreMetrics.COMPLEXITY, CoreMetrics.FUNCTIONS).calculate(data, context);

    assertThat(measure.getValue(), is(2.5));
  }

  @Test
  public void testCalculationForFIle() {
    when(data.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 60.0));
    when(data.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 20.0));
    when(context.getResource()).thenReturn(new JavaFile("foo"));

    Measure measure = AverageFormula.create(CoreMetrics.COMPLEXITY, CoreMetrics.FUNCTIONS).calculate(data, context);
    assertThat(measure.getValue(), is(3.0));
  }
}
