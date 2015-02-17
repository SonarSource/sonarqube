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
package org.sonar.batch.cpd.decorators;

import org.sonar.batch.cpd.decorators.DuplicationDensityDecorator;

import org.junit.Test;
import static org.mockito.Mockito.*;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;

public class DuplicationDensityDecoratorTest {

  @Test
  public void densityIsBalancedByNclocAndCommentLines() {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 40.0));
    when(context.getMeasure(CoreMetrics.COMMENT_LINES)).thenReturn(new Measure(CoreMetrics.COMMENT_LINES, 10.0));
    when(context.getMeasure(CoreMetrics.DUPLICATED_LINES)).thenReturn(new Measure(CoreMetrics.DUPLICATED_LINES, 10.0));

    DuplicationDensityDecorator decorator = new DuplicationDensityDecorator();
    decorator.decorate(null, context);

    verify(context).saveMeasure(CoreMetrics.DUPLICATED_LINES_DENSITY, 20.0);
  }


  @Test
  public void densityEvenIfNoComments() {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 40.0));
    when(context.getMeasure(CoreMetrics.DUPLICATED_LINES)).thenReturn(new Measure(CoreMetrics.DUPLICATED_LINES, 10.0));

    DuplicationDensityDecorator decorator = new DuplicationDensityDecorator();
    decorator.decorate(null, context);

    verify(context).saveMeasure(CoreMetrics.DUPLICATED_LINES_DENSITY, 25.0);
  }

  @Test
  public void noDensityIfNoDuplicationMeasure() {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 45.0));

    DuplicationDensityDecorator decorator = new DuplicationDensityDecorator();
    decorator.decorate(null, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.DUPLICATED_LINES_DENSITY), anyDouble());
  }

  @Test
  public void noDensityWhenZeroNclocAndComments() {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 0.0));
    when(context.getMeasure(CoreMetrics.DUPLICATED_LINES)).thenReturn(new Measure(CoreMetrics.COMMENT_LINES, 0.0));
    when(context.getMeasure(CoreMetrics.DUPLICATED_LINES)).thenReturn(new Measure(CoreMetrics.DUPLICATED_LINES, 10.0));

    DuplicationDensityDecorator decorator = new DuplicationDensityDecorator();
    decorator.decorate(null, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.DUPLICATED_LINES_DENSITY), anyDouble());
  }

}
