/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.batch;

import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.test.IsMeasure;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractSumChildrenDecoratorTest {

  @Test
  public void sumChildren() {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getChildrenMeasures(CoreMetrics.LINES)).thenReturn(Arrays.<Measure>asList(
      new Measure(CoreMetrics.LINES, 100.0),
      new Measure(CoreMetrics.LINES, 50.0)));

    create(false).decorate(null, context);

    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.LINES, 150.0)));
  }

  @Test
  public void doNotSaveZeroIfNoChildren() {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getChildrenMeasures(CoreMetrics.LINES)).thenReturn(Arrays.<Measure>asList());

    create(false).decorate(null, context);

    verify(context, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void saveZeroIfNoChildren() {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getChildrenMeasures(CoreMetrics.LINES)).thenReturn(Arrays.<Measure>asList());

    create(true).decorate(null, context);

    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.LINES, 0.0)));
  }

  private AbstractSumChildrenDecorator create(final boolean zeroIfNoChildMeasures) {
    return new AbstractSumChildrenDecorator() {

      @Override
      @DependedUpon
      public List<Metric> generatesMetrics() {
        return Arrays.<Metric>asList(CoreMetrics.LINES);
      }

      @Override
      protected boolean shouldSaveZeroIfNoChildMeasures() {
        return zeroIfNoChildMeasures;
      }
    };
  }
}
