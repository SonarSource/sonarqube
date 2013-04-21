/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.batch;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Resource;
import org.sonar.api.test.IsMeasure;

public class AbstractDivisionDecoratorTest {

  @Test
  public void divide() {
    AbstractDivisionDecorator decorator = createDecorator();

    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.CLASSES)).thenReturn(new Measure(CoreMetrics.CLASSES, 20.0));
    when(context.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 120.0));

    decorator.decorate(mock(Resource.class), context);

    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.CLASS_COMPLEXITY, 6.0)));
  }

  @Test
  public void nothingWhenMissingData() {
    AbstractDivisionDecorator decorator = createDecorator();

    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.CLASSES)).thenReturn(new Measure(CoreMetrics.CLASSES, 20.0));

    decorator.decorate(mock(Resource.class), context);

    verify(context, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void zeroWhenDivisorIsZero() {
    AbstractDivisionDecorator decorator = createDecorator();

    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.CLASSES)).thenReturn(new Measure(CoreMetrics.CLASSES, 20.0));
    when(context.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 0.0));

    decorator.decorate(mock(Resource.class), context);

    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.CLASS_COMPLEXITY, 0.0)));
  }


  @Test
  public void doNotOverrideExistingMeasure() {
    AbstractDivisionDecorator decorator = createDecorator();

    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.CLASSES)).thenReturn(new Measure(CoreMetrics.CLASSES, 20.0));
    when(context.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 120.0));
    when(context.getMeasure(CoreMetrics.CLASS_COMPLEXITY)).thenReturn(new Measure(CoreMetrics.CLASS_COMPLEXITY, 3.0));

    decorator.decorate(mock(Resource.class), context);

    verify(context, never()).saveMeasure(any(Measure.class));
  }


  @Test
  public void defineDependencies() {
    AbstractDivisionDecorator decorator = createDecorator();
    assertThat(decorator.dependsUponMetrics(), hasItem(CoreMetrics.CLASSES));
    assertThat(decorator.dependsUponMetrics(), hasItem(CoreMetrics.COMPLEXITY));
    assertThat(decorator.generatesMetric(), is(CoreMetrics.CLASS_COMPLEXITY));
  }


  private AbstractDivisionDecorator createDecorator() {
    AbstractDivisionDecorator decorator = new AbstractDivisionDecorator() {
      @Override
      protected Metric getQuotientMetric() {
        return CoreMetrics.CLASS_COMPLEXITY;
      }

      @Override
      protected Metric getDivisorMetric() {
        return CoreMetrics.CLASSES;
      }

      @Override
      protected Metric getDividendMetric() {
        return CoreMetrics.COMPLEXITY;
      }
    };
    return decorator;
  }
}
