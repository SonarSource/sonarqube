/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.core.sensors;

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.test.IsMeasure;

public class UncoveredComplexityDecoratorTest {

  @Test
  public void nothingWhenNoMeasures() {
    DecoratorContext context = mock(DecoratorContext.class);
    UncoveredComplexityDecorator decorator = new UncoveredComplexityDecorator();
    decorator.decorate(null, context);

    verify(context, never()).saveMeasure((Measure) anyObject());
  }

  @Test
  public void quotientWhenMeasures() {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 1048.0));
    when(context.getMeasure(CoreMetrics.COVERAGE)).thenReturn(new Measure(CoreMetrics.COVERAGE, 32.5));

    UncoveredComplexityDecorator decorator = new UncoveredComplexityDecorator();
    decorator.decorate(null, context);

    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.UNCOVERED_COMPLEXITY_BY_TESTS, 707.4)));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.UNCOVERED_COMPLEXITY_BY_TESTS, "CMP=1048;COV=32.5")));
  }

  @Test
  public void declareDependencies() {
    UncoveredComplexityDecorator decorator = new UncoveredComplexityDecorator();
    assertThat(decorator.dependsUponMetrics().size(), greaterThan(0));
    assertThat(decorator.generatesMetric(), not(isNull()));
  }
}
