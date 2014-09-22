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

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MeanAggregationFormulaTest {

  private FormulaContext context;
  private FormulaData data;

  @Before
  public void before() {
    context = mock(FormulaContext.class);
    data = mock(FormulaData.class);
  }

  @Test
  public void calculateChildrenMean() {
    when(context.getTargetMetric()).thenReturn(CoreMetrics.COVERAGE);
    when(data.getChildrenMeasures(CoreMetrics.COVERAGE)).thenReturn(Arrays.<Measure>asList(newCoverage(100.0), newCoverage(50.0), newCoverage(30.0)));

    Measure measure = new MeanAggregationFormula().calculate(data, context);
    assertThat(measure.getValue(), is(60.0));
  }

  @Test
  public void doNotForceZero() {
    when(context.getTargetMetric()).thenReturn(CoreMetrics.COVERAGE);
    when(data.getChildrenMeasures(CoreMetrics.COVERAGE)).thenReturn(Collections.<Measure>emptyList());

    Measure measure = new MeanAggregationFormula(false).calculate(data, context);
    assertNull(measure);
  }

  @Test
  public void forceZero() {
    when(context.getTargetMetric()).thenReturn(CoreMetrics.COVERAGE);
    when(data.getChildrenMeasures(CoreMetrics.COVERAGE)).thenReturn(Collections.<Measure>emptyList());

    Measure measure = new MeanAggregationFormula(true).calculate(data, context);
    assertThat(measure.getValue(), is(0.0));
  }

  private Measure newCoverage(double val) {
    return new Measure(CoreMetrics.COVERAGE, val);
  }
}
