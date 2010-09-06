/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.JavaFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AverageComplexityFormulaTest {

  private FormulaContext context;
  private FormulaData data;

  @Before
  public void before() {
    context = mock(FormulaContext.class);
    data = mock(FormulaData.class);
  }

  @Test
  public void testAverageCalculation() {
    List<FormulaData> childrenData = new ArrayList<FormulaData>();
    FormulaData data1 = mock(FormulaData.class);
    childrenData.add(data1);
    when(data1.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 43.0));
    when(data1.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 107.0));

    FormulaData data2 = mock(FormulaData.class);
    childrenData.add(data2);
    when(data2.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 127.0));
    when(data2.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 233.0));

    when(data.getChildren()).thenReturn(childrenData);

    Measure measure = new AverageComplexityFormula(CoreMetrics.FUNCTIONS).calculate(data, context);

    assertThat(measure.getValue(), is(2.0));
  }

  @Test
  public void testWhenNoChildrenMesaures() {
    List<FormulaData> childrenData = new ArrayList<FormulaData>();
    when(data.getChildren()).thenReturn(childrenData);
    Measure measure = new AverageComplexityFormula(CoreMetrics.FUNCTIONS).calculate(data, context);
    assertNull(measure);
  }

  @Test
  public void testWhenNoComplexityMesaures() {
    List<FormulaData> childrenData = new ArrayList<FormulaData>();
    FormulaData data1 = mock(FormulaData.class);
    childrenData.add(data1);
    when(data1.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 43.0));

    when(data.getChildren()).thenReturn(childrenData);
    Measure measure = new AverageComplexityFormula(CoreMetrics.FUNCTIONS).calculate(data, context);

    assertNull(measure);
  }

  @Test
  public void testWhenNoByMetricMesaures() {
    List<FormulaData> childrenData = new ArrayList<FormulaData>();
    FormulaData data1 = mock(FormulaData.class);
    childrenData.add(data1);
    when(data1.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 43.0));

    when(data.getChildren()).thenReturn(childrenData);
    Measure measure = new AverageComplexityFormula(CoreMetrics.FUNCTIONS).calculate(data, context);

    assertNull(measure);
  }

  @Test
  public void testWhenMixedMetrics() {
    List<FormulaData> childrenData = new ArrayList<FormulaData>();
    FormulaData data1 = mock(FormulaData.class);
    childrenData.add(data1);
    when(data1.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 43.0));
    when(data1.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 107.0));

    FormulaData data2 = mock(FormulaData.class);
    childrenData.add(data2);
    when(data2.getMeasure(CoreMetrics.PARAGRAPHS)).thenReturn(new Measure(CoreMetrics.PARAGRAPHS, 127.0));
    when(data2.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 233.0));

    when(data.getChildren()).thenReturn(childrenData);

    Measure measure = new AverageComplexityFormula(CoreMetrics.FUNCTIONS).calculate(data, context);

    assertThat(measure.getValue(), is(2.5));
  }

  @Test
  public void testCalculationForFIle() {
    when(data.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 60.0));
    when(data.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 20.0));
    when(context.getResource()).thenReturn(new JavaFile("foo"));

    Measure measure = new AverageComplexityFormula(CoreMetrics.FUNCTIONS).calculate(data, context);
    assertThat(measure.getValue(), is(3.0));
  }
}
