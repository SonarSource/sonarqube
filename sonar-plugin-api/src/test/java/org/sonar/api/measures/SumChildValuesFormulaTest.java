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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SumChildValuesFormulaTest {
  private FormulaContext context;
  private FormulaData data;

  @Before
  public void before() {
    context = mock(FormulaContext.class);
    data = mock(FormulaData.class);
  }

  @Test
  public void sumChildValues() {
    when(context.getTargetMetric()).thenReturn(CoreMetrics.NCLOC);
    when(data.getChildrenMeasures(CoreMetrics.NCLOC)).thenReturn(
      Arrays.<Measure>asList(new Measure(CoreMetrics.NCLOC, 100.0), new Measure(CoreMetrics.NCLOC, 50.0)));

    Measure measure = new SumChildValuesFormula(true).calculate(data, context);

    assertThat(measure.getMetric()).isEqualTo(CoreMetrics.NCLOC);
    assertThat(measure.getValue()).isEqualTo(150.0);
  }

  @Test
  public void doNotInsertZero() {
    when(context.getTargetMetric()).thenReturn(CoreMetrics.NCLOC);
    when(data.getChildrenMeasures(CoreMetrics.NCLOC)).thenReturn(Collections.<Measure>emptyList());

    Measure measure = new SumChildValuesFormula(false).calculate(data, context);

    assertThat(measure).isNull();
  }

  @Test
  public void doInsertZero() {
    when(context.getTargetMetric()).thenReturn(CoreMetrics.NCLOC);
    when(data.getChildrenMeasures(CoreMetrics.NCLOC)).thenReturn(Collections.<Measure>emptyList());

    Measure measure = new SumChildValuesFormula(true).calculate(data, context);

    assertThat(measure.getMetric()).isEqualTo(CoreMetrics.NCLOC);
    assertThat(measure.getValue()).isEqualTo(0.0);
  }
}
